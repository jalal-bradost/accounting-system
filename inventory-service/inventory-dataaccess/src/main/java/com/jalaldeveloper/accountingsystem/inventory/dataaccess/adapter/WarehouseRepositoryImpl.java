package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.WarehouseEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.WarehouseDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.WarehouseJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.WarehouseRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WarehouseRepositoryImpl implements WarehouseRepository {

    private final WarehouseJpaRepository jpa;
    private final WarehouseDataAccessMapper mapper;

    public WarehouseRepositoryImpl(WarehouseJpaRepository jpa, WarehouseDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        WarehouseEntity existing = jpa.findById(warehouse.getId().getId()).orElse(null);
        WarehouseEntity toSave = mapper.warehouseDomainToEntity(warehouse, existing);
        return mapper.warehouseEntityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<Warehouse> findById(WarehouseId id) {
        return jpa.findById(id.getId())
                .filter(WarehouseEntity::isActive)
                .map(mapper::warehouseEntityToDomain);
    }

    @Override
    public Optional<Warehouse> findByIdIncludingArchived(WarehouseId id) {
        return jpa.findById(id.getId()).map(mapper::warehouseEntityToDomain);
    }

    @Override
    public List<Warehouse> findByCompany(CompanyId companyId, boolean includeArchived) {
        return jpa.findByCompany(companyId.getId(), includeArchived).stream()
                .map(mapper::warehouseEntityToDomain)
                .toList();
    }
}
