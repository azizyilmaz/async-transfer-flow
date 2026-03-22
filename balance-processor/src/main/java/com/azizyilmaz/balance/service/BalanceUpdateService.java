package com.azizyilmaz.balance.service;

import com.azizyilmaz.balance.domain.Account;
import com.azizyilmaz.balance.domain.ProcessedEvent;
import com.azizyilmaz.balance.repository.AccountRepository;
import com.azizyilmaz.balance.repository.ProcessedEventRepository;
import com.azizyilmaz.common.event.TransferEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BalanceUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceUpdateService.class);

    private final AccountRepository accountRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void applyTransfer(TransferEvent event) {
        if (processedEventRepository.existsByTransferId(event.transferId())) {
            LOGGER.warn("Duplicate event, skipping transferId={}", event.transferId());
            return;
        }
        Account firmAccount = accountRepository.findByAccountIdForUpdate(event.firmAccountId()).orElseThrow(() -> new IllegalArgumentException("Firm account not found: " + event.firmAccountId()));
        switch (event.type()) {
            case DEBIT -> firmAccount.debit(event.amount());
            case CREDIT -> firmAccount.credit(event.amount());
        }
        accountRepository.save(firmAccount);
        processedEventRepository.save(new ProcessedEvent(event.transferId(), Instant.now()));
        LOGGER.info("Balance updated firmAccountId={} transferId={} newBalance={}", event.firmAccountId(), event.transferId(), firmAccount.getBalance());
    }
}
