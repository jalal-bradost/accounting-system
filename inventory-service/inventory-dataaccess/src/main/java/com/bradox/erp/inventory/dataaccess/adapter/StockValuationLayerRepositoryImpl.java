package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.Money;
import com.bradox.erp.inventory.dataaccess.entity.StockValuationLayerEntity;
import com.bradox.erp.inventory.dataaccess.mapper.StockValuationLayerDataAccessMapper;
import com.bradox.erp.inventory.dataaccess.repository.StockValuationLayerJpaRepository;
import com.bradox.erp.inventory.domain.core.entity.StockValuationLayer;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.ValuationLayerId;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockValuationLayerRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    public Map<UUID, BigDecimal> sumOnHandValueByProductIds(CompanyId companyId, Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (Object[] row : jpa.sumOnHandValueByProductIds(companyId.getId(), productIds)) {
            out.put((UUID) row[0], row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }
        return out;
    }

    @Override
    public List<StockValuationLayer> findByProduct(CompanyId companyId, ProductId productId) {
        return jpa.findByProduct(companyId.getId(), productId.getId()).stream()
                .map(mapper::entityToDomain)
                .toList();
    }
}
