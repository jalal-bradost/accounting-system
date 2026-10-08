package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.LineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LineJpaRepository extends JpaRepository<LineEntity, UUID> {

    List<LineEntity> findByCompanyIdAndOrderId(UUID companyId, UUID orderId);

    Optional<LineEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
