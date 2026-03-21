package com.azizyilmaz.transfer.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers", indexes = {@Index(name = "index_transfers_correlation", columnList = "correlation_id", unique = true), @Index(name = "index_transfers_firm", columnList = "firm_account_id, created_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    @Id
    private final UUID id = UUID.randomUUID();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private final TransferStatus status = TransferStatus.INITIATED;
    @Column(name = "firm_account_id", nullable = false)
    private String firmAccountId;
    @Column(name = "customer_account_id", nullable = false)
    private String customerAccountId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "correlation_id", nullable = false, unique = true)
    private String correlationId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    public static Transfer create(String correlationId, BigDecimal amount, String firmAccountId, String customerAccountId) {
        var transfer = new Transfer();
        transfer.correlationId = correlationId;
        transfer.amount = amount;
        transfer.firmAccountId = firmAccountId;
        transfer.customerAccountId = customerAccountId;
        return transfer;
    }

    public enum TransferStatus {
        INITIATED, PROCESSING, COMPLETED, FAILED
    }
}
