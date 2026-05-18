package com.jalaldeveloper.accountingsystem.messaging.consumer;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ProcessedIntegrationEventEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.ProcessedIntegrationEventJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class IntegrationEventDedupService {

    private final ProcessedIntegrationEventJpaRepository processedEventRepository;

    public IntegrationEventDedupService(ProcessedIntegrationEventJpaRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean shouldProcess(UUID eventId, String eventType, String consumerName) {
        if (processedEventRepository.existsById(eventId)) {
            return false;
        }
        ProcessedIntegrationEventEntity entity = new ProcessedIntegrationEventEntity();
        entity.setEventId(eventId);
        entity.setEventType(eventType);
        entity.setConsumerName(consumerName);
        entity.setProcessedAt(Instant.now());
        processedEventRepository.save(entity);
        return true;
    }
}
