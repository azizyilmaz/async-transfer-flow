package com.azizyilmaz.transfer.service;

import com.azizyilmaz.common.dto.TransferRequest;
import com.azizyilmaz.common.event.TransferEvent;
import com.azizyilmaz.transfer.domain.Transfer;
import com.azizyilmaz.transfer.outbox.OutboxEvent;
import com.azizyilmaz.transfer.outbox.OutboxEventRepository;
import com.azizyilmaz.transfer.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferService.class);
    private final TransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID initiateTransfer(TransferRequest transferRequest) {
        var transfer = Transfer.create(transferRequest.correlationId(), transferRequest.amount(), transferRequest.firmAccountId(), transferRequest.customerAccountId());
        try {
            transferRepository.save(transfer);
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Duplicate correlationId [{}]", transferRequest.correlationId());
            return transferRepository.findByCorrelationId(transferRequest.correlationId()).map(Transfer::getId).orElseThrow(() -> new IllegalStateException("Transfer not found: " + transferRequest.correlationId()));

        }
        outboxEventRepository.save(Objects.requireNonNull(buildOutboxEvent(transfer)));
        LOGGER.info("Transfer initiated successfully transferId[{}] correlationId=[{}]", transfer.getId(), transfer.getCorrelationId());
        return transfer.getId();
    }

    private OutboxEvent buildOutboxEvent(Transfer transfer) {
        var event = new TransferEvent(transfer.getId(), transfer.getFirmAccountId(), transfer.getCustomerAccountId(), transfer.getAmount(), TransferEvent.TransferType.DEBIT, Instant.now(), transfer.getCorrelationId());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize event: " + event, e);
        }
        var outbox = new OutboxEvent();
        outbox.setAggregateId(transfer.getId().toString());
        outbox.setAggregateType("Transfer");
        outbox.setEventType("TransferInitiated");
        outbox.setPayload(payload);
        return null;
    }
}
