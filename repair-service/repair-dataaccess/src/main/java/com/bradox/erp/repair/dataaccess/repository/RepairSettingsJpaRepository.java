package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.RepairSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairSettingsJpaRepository extends JpaRepository<RepairSettingsEntity, UUID> {

    Optional<RepairSettingsEntity> findByCompanyId(UUID companyId);
}
