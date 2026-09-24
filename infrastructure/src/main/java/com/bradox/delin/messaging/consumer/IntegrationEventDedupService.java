package com.bradox.delin.messaging.consumer;

import com.bradox.delin.platform.dataaccess.entity.ProcessedIntegrationEventEntity;
import com.bradox.delin.platform.dataaccess.entity.ProcessedIntegrationEventId;
import com.bradox.delin.platform.dataaccess.repository.ProcessedIntegrationEventJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        ProcessedIntegrationEventId id = new ProcessedIntegrationEventId(eventId, consumerName);
        if (processedEventRepository.existsById(id)) {
            return false;
        }
        try {
            processedEventRepository.save(
                    new ProcessedIntegrationEventEntity(eventId, eventType, consumerName, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent delivery of the same event to this consumer
            return false;
        }
    }
}
