package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.RepairOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairOrderJpaRepository extends JpaRepository<RepairOrderEntity, UUID> {

    List<RepairOrderEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Optional<RepairOrderEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
