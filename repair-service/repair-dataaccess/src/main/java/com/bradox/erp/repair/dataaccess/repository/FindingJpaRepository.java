package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.FindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindingJpaRepository extends JpaRepository<FindingEntity, UUID> {

    List<FindingEntity> findByCompanyIdAndOrderIdOrderByCreatedAtAsc(UUID companyId, UUID orderId);

    Optional<FindingEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
