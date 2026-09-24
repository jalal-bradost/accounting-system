package com.bradox.delin.platform.dataaccess.repository;

import com.bradox.delin.platform.dataaccess.entity.ProcessedIntegrationEventEntity;
import com.bradox.delin.platform.dataaccess.entity.ProcessedIntegrationEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedIntegrationEventJpaRepository
        extends JpaRepository<ProcessedIntegrationEventEntity, ProcessedIntegrationEventId> {
}
