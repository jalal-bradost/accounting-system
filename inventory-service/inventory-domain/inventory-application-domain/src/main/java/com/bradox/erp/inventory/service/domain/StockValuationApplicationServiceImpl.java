package com.bradox.erp.inventory.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.StockLocationId;
import com.bradox.erp.inventory.service.domain.dto.StockQuantResponse;
import com.bradox.erp.inventory.service.domain.dto.ValuationLayerResponse;
import com.bradox.erp.inventory.service.domain.mapper.InventoryDataMapper;
import com.bradox.erp.inventory.service.domain.ports.input.StockValuationApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.ProductMovementTotalsPort;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockQuantRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockValuationLayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class StockValuationApplicationServiceImpl implements StockValuationApplicationService {

    private final StockQuantRepository quantRepository;
    private final StockValuationLayerRepository layerRepository;
    private final InventoryDataMapper mapper;
    private final ProductMovementTotalsPort movementTotalsPort;

    StockValuationApplicationServiceImpl(StockQuantRepository quantRepository,
                                         StockValuationLayerRepository layerRepository,
                                         InventoryDataMapper mapper,
                                         ProductMovementTotalsPort movementTotalsPort) {
        this.quantRepository = quantRepository;
        this.layerRepository = layerRepository;
        this.mapper = mapper;
        this.movementTotalsPort = movementTotalsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> movementTotals(CompanyId companyId, UUID productId) {
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("sold", movementTotalsPort.soldQuantity(companyId.getId(), productId));
        totals.put("purchased", movementTotalsPort.purchasedQuantity(companyId.getId(), productId));
        return totals;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockQuantResponse> onHandByProduct(CompanyId companyId, UUID productId) {
        return quantRepository.findByProduct(companyId, new ProductId(productId)).stream()
                .map(mapper::quantToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockQuantResponse> onHandByLocation(CompanyId companyId, UUID locationId) {
        return quantRepository.findByLocation(companyId, new StockLocationId(locationId)).stream()
                .map(mapper::quantToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalOnHand(CompanyId companyId, UUID productId) {
        return quantRepository.sumOnHandInternal(companyId, new ProductId(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalOnHandForWarehouse(CompanyId companyId, UUID productId, UUID warehouseId) {
        return quantRepository.sumOnHandByWarehouse(companyId, new ProductId(productId), new WarehouseId(warehouseId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> onHandByWarehouse(CompanyId companyId, UUID warehouseId) {
        return quantRepository.sumOnHandGroupedByWarehouse(companyId, new WarehouseId(warehouseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValuationLayerResponse> layersByProduct(CompanyId companyId, UUID productId) {
        return layerRepository.findByProduct(companyId, new ProductId(productId)).stream()
                .map(mapper::layerToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal valuationOf(CompanyId companyId, UUID productId) {
        return layerRepository.sumOnHandValue(companyId, new ProductId(productId)).getAmount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> bulkValuation(CompanyId companyId, Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, BigDecimal> onHand = quantRepository.sumOnHandInternalByProductIds(companyId, productIds);
        Map<UUID, BigDecimal> values = layerRepository.sumOnHandValueByProductIds(companyId, productIds);
        List<Map<String, Object>> rows = new ArrayList<>(productIds.size());
        for (UUID productId : productIds) {
            Map<String, Object> row = new HashMap<>();
            row.put("productId", productId);
            row.put("totalOnHand", onHand.getOrDefault(productId, BigDecimal.ZERO));
            row.put("valuation", values.getOrDefault(productId, BigDecimal.ZERO));
            rows.add(row);
        }
        return rows;
    }
}
