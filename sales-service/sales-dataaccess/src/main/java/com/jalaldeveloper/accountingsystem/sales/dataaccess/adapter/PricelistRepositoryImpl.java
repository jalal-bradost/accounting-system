package com.jalaldeveloper.accountingsystem.sales.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.sales.dataaccess.mapper.SalesOrderDataAccessMapper;
import com.jalaldeveloper.accountingsystem.sales.dataaccess.repository.SalPricelistJpaRepository;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.Pricelist;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.repository.PricelistRepository;
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
