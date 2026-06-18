package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockQuantResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValuationLayerResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.mapper.InventoryDataMapper;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockValuationApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockQuantRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockValuationLayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
class StockValuationApplicationServiceImpl implements StockValuationApplicationService {

    private final StockQuantRepository quantRepository;
    private final StockValuationLayerRepository layerRepository;
    private final InventoryDataMapper mapper;

    StockValuationApplicationServiceImpl(StockQuantRepository quantRepository,
                                         StockValuationLayerRepository layerRepository,
                                         InventoryDataMapper mapper) {
        this.quantRepository = quantRepository;
        this.layerRepository = layerRepository;
        this.mapper = mapper;
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
}
