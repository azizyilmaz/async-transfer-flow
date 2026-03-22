package com.azizyilmaz.transfer.outbox;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelayScheduler.class);
    private static final String TOPIC = "transfer.initiated.v1";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingEvents();
        if (pending.isEmpty()) return;

        LOGGER.debug("Relaying {} outbox events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload()).get();
                event.setStatus(OutboxEvent.Status.PUBLISHED);
                event.setProcessedAt(Instant.now());
            } catch (Exception ex) {
                LOGGER.error("Failed to publish outbox event id={}", event.getId(), ex);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxEvent.Status.FAILED);
                    LOGGER.error("Outbox event permanently failed id={}", event.getId());
                }
            }
            outboxEventRepository.save(event);
        }
    }
}
