package com.bradox.erp.assistant.dataaccess.repository;

import com.bradox.erp.assistant.dataaccess.entity.AssistantSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssistantSettingsJpaRepository extends JpaRepository<AssistantSettingsEntity, UUID> {
}
