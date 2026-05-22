package com.jalaldeveloper.accountingsystem.platform.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_processed_event")
public class ProcessedIntegrationEventEntity {

    @EmbeddedId
    private ProcessedIntegrationEventId id;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedIntegrationEventEntity() {
    }

    public ProcessedIntegrationEventEntity(UUID eventId, String eventType, String consumerName, Instant processedAt) {
        this.id = new ProcessedIntegrationEventId(eventId, consumerName);
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public ProcessedIntegrationEventId getId() {
        return id;
    }

    public UUID getEventId() {
        return id.getEventId();
    }

    public String getConsumerName() {
        return id.getConsumerName();
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
