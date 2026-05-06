package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockValuationLayerEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.StockValuationLayerDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockValuationLayerJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationLayerId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockValuationLayerRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class StockValuationLayerRepositoryImpl implements StockValuationLayerRepository {

    private final StockValuationLayerJpaRepository jpa;
    private final StockValuationLayerDataAccessMapper mapper;

    public StockValuationLayerRepositoryImpl(StockValuationLayerJpaRepository jpa,
                                             StockValuationLayerDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public StockValuationLayer save(StockValuationLayer layer) {
        StockValuationLayerEntity existing = jpa.findById(layer.getId().getId()).orElse(null);
        StockValuationLayerEntity toSave = mapper.domainToEntity(layer, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public List<StockValuationLayer> saveAll(List<StockValuationLayer> layers) {
        List<StockValuationLayer> saved = new ArrayList<>(layers.size());
        for (StockValuationLayer l : layers) {
            saved.add(save(l));
        }
        return saved;
    }

    @Override
    public Optional<StockValuationLayer> findById(ValuationLayerId id) {
        return jpa.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public List<StockValuationLayer> findFifoCandidates(CompanyId companyId, ProductId productId) {
        return jpa.findFifoCandidates(companyId.getId(), productId.getId()).stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public Money sumOnHandValue(CompanyId companyId, ProductId productId) {
        BigDecimal sum = jpa.sumOnHandValue(companyId.getId(), productId.getId());
        return new Money(sum != null ? sum : BigDecimal.ZERO);
    }

    @Override
    public List<StockValuationLayer> findByProduct(CompanyId companyId, ProductId productId) {
        return jpa.findByProduct(companyId.getId(), productId.getId()).stream()
                .map(mapper::entityToDomain)
                .toList();
    }
}
