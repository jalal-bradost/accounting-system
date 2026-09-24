package com.bradox.erp.platform.dataaccess.repository;

import com.bradox.erp.platform.dataaccess.entity.ProcessedIntegrationEventEntity;
import com.bradox.erp.platform.dataaccess.entity.ProcessedIntegrationEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedIntegrationEventJpaRepository
        extends JpaRepository<ProcessedIntegrationEventEntity, ProcessedIntegrationEventId> {
}
