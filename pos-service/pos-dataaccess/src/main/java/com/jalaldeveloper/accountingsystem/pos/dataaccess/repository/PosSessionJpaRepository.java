package com.jalaldeveloper.accountingsystem.pos.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosSessionEntity;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosSessionState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PosSessionJpaRepository extends JpaRepository<PosSessionEntity, UUID> {
    Optional<PosSessionEntity> findFirstByConfigIdAndStateOrderByOpenedAtDesc(UUID configId, PosSessionState state);
}
