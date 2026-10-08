package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.InspectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionJpaRepository extends JpaRepository<InspectionEntity, UUID> {

    Optional<InspectionEntity> findByCompanyIdAndOrderId(UUID companyId, UUID orderId);
}
