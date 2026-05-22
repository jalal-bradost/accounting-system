package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ProcessedIntegrationEventEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ProcessedIntegrationEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedIntegrationEventJpaRepository
        extends JpaRepository<ProcessedIntegrationEventEntity, ProcessedIntegrationEventId> {
}
