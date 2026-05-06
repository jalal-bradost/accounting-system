package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockQuantEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.StockQuantDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockQuantJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockQuant;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockQuantRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
