package com.azizyilmaz.balance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private UUID transferId;
    @Column(nullable = false)
    private Instant processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(UUID transferId, Instant processedAt) {
        this.transferId = transferId;
        this.processedAt = processedAt;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
