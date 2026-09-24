package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.dataaccess.entity.StockLocationEntity;
import com.bradox.erp.inventory.dataaccess.mapper.WarehouseDataAccessMapper;
import com.bradox.erp.inventory.dataaccess.repository.StockLocationJpaRepository;
import com.bradox.erp.inventory.domain.core.entity.StockLocation;
import com.bradox.erp.inventory.domain.core.valueobject.StockLocationId;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockLocationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class StockLocationRepositoryImpl implements StockLocationRepository {

    private final StockLocationJpaRepository jpa;
    private final WarehouseDataAccessMapper mapper;

    public StockLocationRepositoryImpl(StockLocationJpaRepository jpa, WarehouseDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public StockLocation save(StockLocation location) {
        StockLocationEntity existing = jpa.findById(location.getId().getId()).orElse(null);
        StockLocationEntity toSave = mapper.locationDomainToEntity(location, existing);
        return mapper.locationEntityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<StockLocation> findById(StockLocationId id) {
        return jpa.findById(id.getId())
                .filter(StockLocationEntity::isActive)
                .map(mapper::locationEntityToDomain);
    }

    @Override
    public Optional<StockLocation> findByIdIncludingArchived(StockLocationId id) {
        return jpa.findById(id.getId()).map(mapper::locationEntityToDomain);
    }

    @Override
    public List<StockLocation> findByCompany(CompanyId companyId, boolean includeArchived) {
        return jpa.findByCompany(companyId.getId(), includeArchived).stream()
                .map(mapper::locationEntityToDomain)
                .toList();
    }

    @Override
    public List<StockLocation> findByWarehouse(WarehouseId warehouseId, boolean includeArchived) {
        return jpa.findByWarehouse(warehouseId.getId(), includeArchived).stream()
                .map(mapper::locationEntityToDomain)
                .toList();
    }
}
