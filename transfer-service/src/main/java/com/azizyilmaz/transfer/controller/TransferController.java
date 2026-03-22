package com.azizyilmaz.transfer.controller;

import com.azizyilmaz.common.dto.TransferRequest;
import com.azizyilmaz.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<@NotNull TransferResponse> initiate(@Valid @RequestBody TransferRequest transferRequest) {
        UUID transferId = transferService.initiateTransfer(transferRequest);
        return ResponseEntity
                .accepted()
                .location(URI.create("/api/v1/transfers/" + transferId + "/status"))
                .body(new TransferResponse(transferId, "INITIATED",
                        "Transfer initiated successfully. Please query /status endpoint for status."));
    }

    public record TransferResponse(UUID transferId, String status, String message) {
    }
}
