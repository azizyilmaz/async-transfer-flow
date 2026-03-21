package com.azizyilmaz.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferEvent(UUID transferId,
                            String firmAccountId,
                            String customerAccountId,
                            BigDecimal amount,
                            TransferType type,
                            Instant occurredAt,
                            String correlationId) {
    public enum TransferType {
        DEBIT, CREDIT
    }
}
