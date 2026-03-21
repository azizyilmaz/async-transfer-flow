package com.azizyilmaz.transfer.repository;

import com.azizyilmaz.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    Optional<Transfer> findByCorrelationId(String correlationId);
}
