package com.azizyilmaz.transfer.controller;

import com.azizyilmaz.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferStatusController {

    private final TransferRepository transferRepository;

    @GetMapping("/{id}/status")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable UUID id) {
        return transferRepository.findById(id).map(t -> ResponseEntity.ok(new StatusResponse(t.getId(), t.getStatus().name(), t.getCreatedAt(), t.getCompletedAt()))).orElse(ResponseEntity.notFound().build());
    }

    public record StatusResponse(UUID transferId, String status, Instant createdAt, Instant completedAt) {
    }
}
