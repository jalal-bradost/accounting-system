package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockPickingEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.StockPickingDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockPickingJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockPicking;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockPickingId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockPickingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StockPickingRepositoryImpl implements StockPickingRepository {

    private final StockPickingJpaRepository jpa;
    private final StockPickingDataAccessMapper mapper;

    public StockPickingRepositoryImpl(StockPickingJpaRepository jpa, StockPickingDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public StockPicking save(StockPicking picking) {
        StockPickingEntity existing = jpa.findById(picking.getId().getId()).orElse(null);
        StockPickingEntity toSave = mapper.domainToEntity(picking, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<StockPicking> findById(StockPickingId id) {
        return jpa.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Page<StockPicking> search(CompanyId companyId, PickingType pickingType, PickingState state, Pageable pageable) {
        return jpa.search(companyId.getId(), pickingType, state, pageable)
                .map(mapper::entityToDomain);
    }
}
