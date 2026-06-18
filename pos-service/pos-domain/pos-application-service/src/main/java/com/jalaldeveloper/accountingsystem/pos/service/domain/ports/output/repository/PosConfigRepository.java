package com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosConfigRepository {
    PosConfig save(PosConfig config);

    Optional<PosConfig> findById(UUID id);

    Optional<PosConfig> findByCompanyIdAndName(UUID companyId, String name);

    List<PosConfig> findByCompanyIdAndActiveTrueOrderByNameAsc(UUID companyId);
}
