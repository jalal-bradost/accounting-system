package com.bradox.erp.sales.dataaccess.adapter;

import com.bradox.erp.sales.dataaccess.mapper.SalesOrderDataAccessMapper;
import com.bradox.erp.sales.dataaccess.repository.SalPricelistJpaRepository;
import com.bradox.erp.sales.domain.core.entity.Pricelist;
import com.bradox.erp.sales.service.domain.ports.output.repository.PricelistRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PricelistRepositoryImpl implements PricelistRepository {

    private final SalPricelistJpaRepository jpa;
    private final SalesOrderDataAccessMapper mapper;

    public PricelistRepositoryImpl(SalPricelistJpaRepository jpa, SalesOrderDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<Pricelist> findById(UUID id) {
        return jpa.findById(id).map(mapper::pricelistEntityToDomain);
    }

    @Override
    public Optional<Pricelist> findByIdWithItems(UUID id) {
        return jpa.findByIdWithItems(id).map(mapper::pricelistEntityToDomain);
    }
}
