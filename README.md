# Transfer System
### Spring Boot 4.0.4 · Maven 4.0.0 · Java 21 · Apache Kafka · PostgreSQL 16

A production-ready, event-driven money transfer system built with the **Transactional Outbox Pattern**.  
It solves a real-world problem: when many transfer requests arrive at the same time, a single firm account row in the database gets locked by all of them, causing timeouts. This project eliminates that problem by making balance updates asynchronous and sequential through Kafka.

---

## How It Works — In Simple Terms

**Old approach (broken):**  
100 requests arrive → 100 threads try to `UPDATE` the same account row at the same time → database locks → timeouts.

**New approach (this project):**  
100 requests arrive → each one quickly writes a new row to the `outbox_events` table → a relay picks those rows up and sends them to Kafka → a single consumer reads them **one by one** and updates the balance → no lock conflicts, no timeouts.

---

## Requirements

| Tool | Minimum Version | Notes |
|------|----------------|-------|
| Java | 21 | Eclipse Temurin recommended |
| Docker Desktop | 4.x | Runs PostgreSQL and Kafka |
| IntelliJ IDEA | 2024.1+ | Community or Ultimate edition |

---

## Step 1 — Open the Project

```
File → Open → select the transfer-system/ folder
```

> The root `pom.xml` uses Maven 4.0.0 multi-project format.  
> If IntelliJ shows a Maven import popup, click **Trust Project** and then **Load Maven Changes**.

---

## Step 2 — Set Java 21 SDK

```
File → Project Structure → Project
  SDK            : Java 21
  Language level : 21
```

---

## Step 3 — Start the Infrastructure

Open a terminal in the project root and run:

```bash
docker compose up -d
```

This starts PostgreSQL, Kafka, and Kafka UI. Wait about 20 seconds for everything to become healthy.

```bash
docker compose ps
# postgres  → healthy
# kafka     → healthy
```

| Service | Port | URL |
|---------|------|-----|
| PostgreSQL | 5432 | `localhost:5432` |
| Kafka | 9092 | `localhost:9092` |
| Kafka UI | 8090 | http://localhost:8090 |

---

## Step 4 — Run the Applications

Two run configurations are already set up in `.idea/runConfigurations/`. IntelliJ will find them automatically.

Start them in this order:

1. **`TransferServiceApplication`** → click Run ▶ (starts on port **8080**)
2. **`BalanceProcessorApplication`** → click Run ▶ (starts on port **8081**, no HTTP — listens to Kafka only)

Both configurations already include the correct environment variables (database URL, Kafka address, credentials), so you do not need to set anything extra.

---

## Step 5 — Send a Transfer

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "firmAccountId":     "FIRM-001",
    "customerAccountId": "CUST-123",
    "amount":            500.00,
    "correlationId":     "my-unique-request-id-001"
  }'
```

The response will be `202 Accepted`:

```json
{
  "transferId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status":     "INITIATED",
  "message":    "Transfer kuyruğa alındı. Durum için /status endpoint'ini sorgulayın."
}
```

### Check the Transfer Status

```bash
curl http://localhost:8080/api/v1/transfers/{transferId}/status
```

The `status` field moves from `INITIATED` → `PROCESSING` → `COMPLETED`.

### Watch Messages in Kafka UI

Open http://localhost:8090, find the **transfer.initiated.v1** topic, and you can see each message as it flows through.

---

## Step 6 — Run the Tests

> Docker must be running. The tests use Testcontainers, which starts a real PostgreSQL container automatically.

```bash
# Run all modules
./mvnw test

# Run only transfer-service tests
./mvnw test -pl transfer-service

# Run only balance-processor tests
./mvnw test -pl balance-processor
```

In IntelliJ: right-click on a test class → **Run '...'**

---

## Project Layout

```
transfer-system/
├── pom.xml                            ← Maven 4.0.0 root (multi-project)
├── docker-compose.yml                 ← PostgreSQL + Kafka infrastructure
├── mvnw / mvnw.cmd                    ← Maven wrapper scripts
│
├── common-lib/                        ← Shared DTOs and Kafka event classes
│   └── TransferRequest.java           ← REST input validation
│   └── TransferEvent.java             ← Kafka message schema
│
├── transfer-service/                  ← REST API + Outbox Relay  (port 8080)
│   ├── TransferController.java        ← POST /api/v1/transfers
│   ├── TransferStatusController.java  ← GET  /api/v1/transfers/{id}/status
│   ├── TransferService.java           ← Writes transfer + outbox in one transaction
│   ├── OutboxRelayScheduler.java      ← Polls DB every 500ms, publishes to Kafka
│   └── V1__initial_schema.sql         ← Flyway database migration
│
└── balance-processor/                 ← Kafka Consumer  (port 8081, headless)
    ├── TransferEventConsumer.java     ← Reads messages one by one from Kafka
    ├── BalanceUpdateService.java      ← Updates account balance, checks for duplicates
    └── KafkaConsumerConfig.java       ← Error handling + Dead Letter Topic setup
```

---

## Key Design Decisions

**Why 202 Accepted instead of 200 OK?**  
The balance update happens asynchronously. When the API responds, the money has not moved yet — it is queued. Returning 202 is honest about this. The firm can poll `/status` to know when the transfer is complete.

**What happens if the same request is sent twice?**  
Each request includes a `correlationId` chosen by the caller. If the same ID arrives again, the service recognises it as a duplicate and returns the original `transferId` without creating a second transfer. This is called *idempotency*.

**What happens if a Kafka message is delivered twice?**  
Kafka guarantees *at-least-once* delivery, meaning a message can occasionally arrive more than once. The `processed_events` table in the database keeps a record of every `transferId` that has been handled. Before updating the balance, the consumer checks this table. If the ID is already there, it skips the message safely.

**What happens if a message cannot be processed after several retries?**  
After three failed attempts, the message is moved to a separate Kafka topic called `transfer.initiated.v1.DLT` (Dead Letter Topic). It stays there until someone investigates and handles it manually.

---

## Version Reference

| Component | Version     |
|-----------|-------------|
| Spring Boot | 4.0.4       |
| Spring Framework | 7.0.6       |
| Spring Kafka | 4.0.4       |
| Hibernate | 7.2.7.Final |
| Flyway | 11.14.1     |
| HikariCP | 7.0.2       |
| Jackson | 3.1.0       |
| Maven | 4.0.0       |
| Java | 21          |

---

## Common Problems

**"Connection refused: localhost:5432"**  
PostgreSQL is not ready yet. Run `docker compose ps` and wait until you see `healthy`.

**"Flyway migration failed — relation already exists"**  
A previous run left data in the database. Clean it with:
```bash
docker compose down -v
docker compose up -d
```

**"No such topic: transfer.initiated.v1"**  
The Kafka topic initialisation container did not finish. Check its output:
```bash
docker compose logs kafka-init
```

**Maven dependencies are not downloading**  
In IntelliJ, open the **Maven** panel on the right side and click the **Reload All Maven Projects** button (the circular arrow icon).

**"modelVersion 4.0.0 is not recognised"**  
This project needs Maven 4.x. IntelliJ 2024.1+ supports Maven 4. Verify the wrapper version with `./mvnw --version`.
