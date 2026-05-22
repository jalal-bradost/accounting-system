package com.jalaldeveloper.accountingsystem.platform.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ProcessedIntegrationEventId implements Serializable {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, length = 120)
    private String consumerName;

    protected ProcessedIntegrationEventId() {
    }

    public ProcessedIntegrationEventId(UUID eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessedIntegrationEventId that = (ProcessedIntegrationEventId) o;
        return Objects.equals(eventId, that.eventId) && Objects.equals(consumerName, that.consumerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, consumerName);
    }
}
