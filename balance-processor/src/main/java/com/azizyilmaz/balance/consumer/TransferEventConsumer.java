package com.azizyilmaz.balance.consumer;

import com.azizyilmaz.balance.service.BalanceUpdateService;
import com.azizyilmaz.common.event.TransferEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferEventConsumer.class);

    private final BalanceUpdateService balanceUpdateService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "transfer.initiated.v1", groupId = "balance-processor-group", concurrency = "3",                 // 3 thread → 3 partition
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        LOGGER.info("Received transfer event partition={} offset={}", record.partition(), record.offset());
        try {
            TransferEvent event = mapper.readValue(record.value(), TransferEvent.class);
            balanceUpdateService.applyTransfer(event);
            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error("Failed to process transfer event offset={}", record.offset(), ex);
            throw new RuntimeException("Event processing failed", ex);
        }
    }
}
