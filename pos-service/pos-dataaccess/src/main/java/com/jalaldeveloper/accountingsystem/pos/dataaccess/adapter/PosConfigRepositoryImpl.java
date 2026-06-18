package com.jalaldeveloper.accountingsystem.pos.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.mapper.PosDataAccessMapper;
import com.jalaldeveloper.accountingsystem.pos.dataaccess.repository.PosConfigJpaRepository;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosConfig;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository.PosConfigRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PosConfigRepositoryImpl implements PosConfigRepository {

    private final PosConfigJpaRepository jpa;
    private final PosDataAccessMapper mapper;

    public PosConfigRepositoryImpl(PosConfigJpaRepository jpa, PosDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PosConfig save(PosConfig config) {
        var existing = jpa.findById(config.getId()).orElse(null);
        return mapper.entityToDomain(jpa.save(mapper.domainToEntity(config, existing)));
    }

    @Override
    public Optional<PosConfig> findById(UUID id) {
        return jpa.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public Optional<PosConfig> findByCompanyIdAndName(UUID companyId, String name) {
        return jpa.findByCompanyIdAndName(companyId, name).map(mapper::entityToDomain);
    }

    @Override
    public List<PosConfig> findByCompanyIdAndActiveTrueOrderByNameAsc(UUID companyId) {
        return jpa.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId).stream()
                .map(mapper::entityToDomain)
                .toList();
    }
}
