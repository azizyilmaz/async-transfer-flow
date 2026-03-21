package com.azizyilmaz.transfer.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "index_outbox_status_created", columnList = "status, created_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id = UUID.randomUUID();
    @Column(nullable = false)
    private String aggregateType; // "Transfer"
    @Column(nullable = false)
    private String aggregateId; // transferId (Kafka partition key)
    @Column(nullable = false)
    private String eventType; // "TransferInitiated"
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON - TransferEvent
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "retry_count")
    private int retryCount = 0;

    public enum Status {
        PENDING, PUBLISHED, FAILED
    }
}
