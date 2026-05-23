package com.jalaldeveloper.accountingsystem.pos.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosConfigJpaRepository extends JpaRepository<PosConfigEntity, UUID> {
    List<PosConfigEntity> findByCompanyIdAndActiveTrueOrderByNameAsc(UUID companyId);

    Optional<PosConfigEntity> findByCompanyIdAndName(UUID companyId, String name);
}
