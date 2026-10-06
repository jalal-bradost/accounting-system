package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.dataaccess.entity.StockQuantEntity;
import com.bradox.erp.inventory.dataaccess.mapper.StockQuantDataAccessMapper;
import com.bradox.erp.inventory.dataaccess.repository.StockQuantJpaRepository;
import com.bradox.erp.inventory.domain.core.entity.StockQuant;
import com.bradox.erp.inventory.domain.core.valueobject.LocationType;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.StockLocationId;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockQuantRepository;
import org.springframework.stereotype.Component;

import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockQuantRepositoryImpl implements StockQuantRepository {

    private final StockQuantJpaRepository jpa;
    private final StockQuantDataAccessMapper mapper;

    public StockQuantRepositoryImpl(StockQuantJpaRepository jpa, StockQuantDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public StockQuant save(StockQuant quant) {
        StockQuantEntity existing = jpa.findById(quant.getId().getId()).orElse(null);
        StockQuantEntity toSave = mapper.domainToEntity(quant, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<StockQuant> findByProductLocation(CompanyId companyId, ProductId productId, StockLocationId locationId) {
        return jpa.findByProductLocation(companyId.getId(), productId.getId(), locationId.getId())
                .map(mapper::entityToDomain);
    }

    @Override
    public BigDecimal sumOnHandInternal(CompanyId companyId, ProductId productId) {
        BigDecimal sum = jpa.sumOnHandInternal(companyId.getId(), productId.getId(), LocationType.INTERNAL);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    
    @Override
    public Map<UUID, BigDecimal> sumOnHandInternalByProductIds(CompanyId companyId, java.util.Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> out = new java.util.HashMap<>();
        for (Object[] row : jpa.sumOnHandInternalByProductIds(
                companyId.getId(), productIds, com.bradox.erp.inventory.domain.core.valueobject.LocationType.INTERNAL)) {
            out.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return out;
    }

    @Override
    public BigDecimal sumOnHandByWarehouse(CompanyId companyId, ProductId productId, WarehouseId warehouseId) {
        BigDecimal sum = jpa.sumOnHandByWarehouse(
                companyId.getId(), productId.getId(), warehouseId.getId(), LocationType.INTERNAL);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public Map<UUID, BigDecimal> sumOnHandGroupedByWarehouse(CompanyId companyId, WarehouseId warehouseId) {
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (Object[] row : jpa.sumOnHandGroupedByWarehouse(
                companyId.getId(), warehouseId.getId(), LocationType.INTERNAL)) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            UUID productId = (UUID) row[0];
            BigDecimal qty = row[1] instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            out.put(productId, qty);
        }
        return out;
    }

    @Override
    public List<StockQuant> findByProduct(CompanyId companyId, ProductId productId) {
        return jpa.findByProduct(companyId.getId(), productId.getId()).stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public List<StockQuant> findByLocation(CompanyId companyId, StockLocationId locationId) {
        return jpa.findByLocation(companyId.getId(), locationId.getId()).stream()
                .map(mapper::entityToDomain)
                .toList();
    }
}
