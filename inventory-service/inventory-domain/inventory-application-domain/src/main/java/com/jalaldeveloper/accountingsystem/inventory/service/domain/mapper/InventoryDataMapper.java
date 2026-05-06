package com.jalaldeveloper.accountingsystem.inventory.service.domain.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockLocation;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockMove;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockPicking;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockQuant;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UnitOfMeasure;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UomCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockMoveId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockPickingId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockQuantId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateStockPickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockMoveCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockPickingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockQuantResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValuationLayerResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InventoryDataMapper {

    public Product createCommandToProduct(CreateProductCommand cmd, UUID id, CompanyId companyId) {
        Product.Builder b = Product.builder()
                .id(new ProductId(id))
                .companyId(companyId)
                .sku(cmd.getSku())
                .name(cmd.getName())
                .barcode(cmd.getBarcode())
                .description(cmd.getDescription())
                .productType(cmd.getProductType())
                .categoryId(cmd.getCategoryId() != null ? new ProductCategoryId(cmd.getCategoryId()) : null)
                .uomId(cmd.getUomId() != null ? new UomId(cmd.getUomId()) : null)
                .purchaseUomId(cmd.getPurchaseUomId() != null ? new UomId(cmd.getPurchaseUomId()) : null)
                .standardCost(cmd.getStandardCost() != null ? new Money(cmd.getStandardCost()) : null)
                .listPrice(cmd.getListPrice() != null ? new Money(cmd.getListPrice()) : null)
                .purchaseOk(cmd.getPurchaseOk() == null || cmd.getPurchaseOk())
                .saleOk(cmd.getSaleOk() == null || cmd.getSaleOk())
                .valuationMethodOverride(cmd.getValuationMethodOverride())
                .stockValuationAccountIdOverride(cmd.getStockValuationAccountIdOverride())
                .stockInputAccountIdOverride(cmd.getStockInputAccountIdOverride())
                .stockOutputAccountIdOverride(cmd.getStockOutputAccountIdOverride())
                .cogsAccountIdOverride(cmd.getCogsAccountIdOverride());
        return b.build();
    }

    public ProductResponse productToResponse(Product p) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId().getId());
        r.setCompanyId(p.getCompanyId().getId());
        r.setSku(p.getSku());
        r.setName(p.getName());
        r.setBarcode(p.getBarcode());
        r.setDescription(p.getDescription());
        r.setProductType(p.getProductType());
        r.setCategoryId(p.getCategoryId() != null ? p.getCategoryId().getId() : null);
        r.setUomId(p.getUomId() != null ? p.getUomId().getId() : null);
        r.setPurchaseUomId(p.getPurchaseUomId() != null ? p.getPurchaseUomId().getId() : null);
        r.setStandardCost(p.getStandardCost() != null ? p.getStandardCost().getAmount() : null);
        r.setListPrice(p.getListPrice() != null ? p.getListPrice().getAmount() : null);
        r.setPurchaseOk(p.isPurchaseOk());
        r.setSaleOk(p.isSaleOk());
        r.setValuationMethodOverride(p.getValuationMethodOverride());
        r.setStockValuationAccountIdOverride(p.getStockValuationAccountIdOverride());
        r.setStockInputAccountIdOverride(p.getStockInputAccountIdOverride());
        r.setStockOutputAccountIdOverride(p.getStockOutputAccountIdOverride());
        r.setCogsAccountIdOverride(p.getCogsAccountIdOverride());
        r.setActive(p.isActive());
        r.setArchivedAt(p.getArchivedAt());
        return r;
    }

    public ProductCategory categoryCommandToDomain(ProductCategoryCommand cmd, UUID id, CompanyId companyId) {
        return ProductCategory.builder()
                .id(new ProductCategoryId(id))
                .companyId(companyId)
                .name(cmd.getName())
                .parentId(cmd.getParentId() != null ? new ProductCategoryId(cmd.getParentId()) : null)
                .valuationMethod(cmd.getValuationMethod())
                .stockValuationAccountId(cmd.getStockValuationAccountId())
                .stockInputAccountId(cmd.getStockInputAccountId())
                .stockOutputAccountId(cmd.getStockOutputAccountId())
                .cogsAccountId(cmd.getCogsAccountId())
                .build();
    }

    public ProductCategoryResponse categoryToResponse(ProductCategory c) {
        ProductCategoryResponse r = new ProductCategoryResponse();
        r.setId(c.getId().getId());
        r.setCompanyId(c.getCompanyId().getId());
        r.setName(c.getName());
        r.setParentId(c.getParentId() != null ? c.getParentId().getId() : null);
        r.setValuationMethod(c.getValuationMethod());
        r.setStockValuationAccountId(c.getStockValuationAccountId());
        r.setStockInputAccountId(c.getStockInputAccountId());
        r.setStockOutputAccountId(c.getStockOutputAccountId());
        r.setCogsAccountId(c.getCogsAccountId());
        r.setActive(c.isActive());
        return r;
    }

    public UomCategory uomCategoryCommandToDomain(UomCategoryCommand cmd, UUID id, CompanyId companyId) {
        return UomCategory.builder()
                .id(new UomCategoryId(id))
                .companyId(companyId)
                .name(cmd.getName())
                .build();
    }

    public UomCategoryResponse uomCategoryToResponse(UomCategory c) {
        UomCategoryResponse r = new UomCategoryResponse();
        r.setId(c.getId().getId());
        r.setCompanyId(c.getCompanyId().getId());
        r.setName(c.getName());
        r.setActive(c.isActive());
        return r;
    }

    public UnitOfMeasure uomCommandToDomain(UomCommand cmd, UUID id, CompanyId companyId) {
        return UnitOfMeasure.builder()
                .id(new UomId(id))
                .companyId(companyId)
                .categoryId(new UomCategoryId(cmd.getCategoryId()))
                .name(cmd.getName())
                .uomType(cmd.getUomType())
                .factor(cmd.getFactor())
                .rounding(cmd.getRounding() != null ? cmd.getRounding() : 4)
                .build();
    }

    public UomResponse uomToResponse(UnitOfMeasure u) {
        UomResponse r = new UomResponse();
        r.setId(u.getId().getId());
        r.setCompanyId(u.getCompanyId().getId());
        r.setCategoryId(u.getCategoryId().getId());
        r.setName(u.getName());
        r.setUomType(u.getUomType());
        r.setFactor(u.getFactor());
        r.setRounding(u.getRounding());
        r.setActive(u.isActive());
        return r;
    }

    public Warehouse warehouseCommandToDomain(WarehouseCommand cmd, UUID id, CompanyId companyId) {
        return Warehouse.builder()
                .id(new WarehouseId(id))
                .companyId(companyId)
                .code(cmd.getCode())
                .name(cmd.getName())
                .build();
    }

    public WarehouseResponse warehouseToResponse(Warehouse w) {
        WarehouseResponse r = new WarehouseResponse();
        r.setId(w.getId().getId());
        r.setCompanyId(w.getCompanyId().getId());
        r.setCode(w.getCode());
        r.setName(w.getName());
        r.setStockLocationId(w.getStockLocationId() != null ? w.getStockLocationId().getId() : null);
        r.setInputLocationId(w.getInputLocationId() != null ? w.getInputLocationId().getId() : null);
        r.setOutputLocationId(w.getOutputLocationId() != null ? w.getOutputLocationId().getId() : null);
        r.setActive(w.isActive());
        return r;
    }

    public StockLocation locationCommandToDomain(StockLocationCommand cmd, UUID id, CompanyId companyId) {
        return StockLocation.builder()
                .id(new StockLocationId(id))
                .companyId(companyId)
                .code(cmd.getCode())
                .name(cmd.getName())
                .locationType(cmd.getLocationType())
                .parentId(cmd.getParentId() != null ? new StockLocationId(cmd.getParentId()) : null)
                .warehouseId(cmd.getWarehouseId() != null ? new WarehouseId(cmd.getWarehouseId()) : null)
                .allowNegativeStock(cmd.isAllowNegativeStock())
                .build();
    }

    public StockLocationResponse locationToResponse(StockLocation l) {
        StockLocationResponse r = new StockLocationResponse();
        r.setId(l.getId().getId());
        r.setCompanyId(l.getCompanyId().getId());
        r.setCode(l.getCode());
        r.setName(l.getName());
        r.setLocationType(l.getLocationType());
        r.setParentId(l.getParentId() != null ? l.getParentId().getId() : null);
        r.setWarehouseId(l.getWarehouseId() != null ? l.getWarehouseId().getId() : null);
        r.setAllowNegativeStock(l.isAllowNegativeStock());
        r.setActive(l.isActive());
        return r;
    }

    public StockPickingResponse pickingToResponse(StockPicking p) {
        StockPickingResponse r = new StockPickingResponse();
        r.setId(p.getId().getId());
        r.setCompanyId(p.getCompanyId().getId());
        r.setWarehouseId(p.getWarehouseId() != null ? p.getWarehouseId().getId() : null);
        r.setPickingType(p.getPickingType());
        r.setReference(p.getReference());
        r.setSourceLocationId(p.getSourceLocationId().getId());
        r.setDestinationLocationId(p.getDestinationLocationId().getId());
        r.setPartnerId(p.getPartnerId());
        r.setOrigin(p.getOrigin());
        r.setScheduledAt(p.getScheduledAt());
        r.setValidatedAt(p.getValidatedAt());
        r.setValidatedBy(p.getValidatedBy());
        r.setState(p.getState() != null ? p.getState() : PickingState.DRAFT);
        r.setBackorderOf(p.getBackorderOf() != null ? p.getBackorderOf().getId() : null);
        r.setPurchaseOrderId(p.getPurchaseOrderId());
        r.setSalesOrderId(p.getSalesOrderId());
        r.setMoves(p.getMoves().stream().map(this::moveToResponse).collect(Collectors.toList()));
        return r;
    }

    public StockPickingResponse.MoveResponse moveToResponse(StockMove m) {
        StockPickingResponse.MoveResponse r = new StockPickingResponse.MoveResponse();
        r.setId(m.getId() != null ? m.getId().getId() : null);
        r.setProductId(m.getProductId().getId());
        r.setUomId(m.getUomId().getId());
        r.setSourceLocationId(m.getSourceLocationId().getId());
        r.setDestinationLocationId(m.getDestinationLocationId().getId());
        r.setDemandQuantity(m.getDemandQuantity());
        r.setReservedQuantity(m.getReservedQuantity());
        r.setPickedQuantity(m.getPickedQuantity());
        r.setUnitCost(m.getUnitCost() != null ? m.getUnitCost().getAmount() : null);
        r.setState(m.getState());
        r.setPurchaseOrderLineId(m.getPurchaseOrderLineId());
        r.setSalesOrderLineId(m.getSalesOrderLineId());
        return r;
    }

    public StockMove moveCommandToDomain(StockMoveCommand cmd,
                                          UUID moveId,
                                          StockLocationId source,
                                          StockLocationId destination) {
        StockMove.Builder b = StockMove.builder()
                .id(new StockMoveId(moveId))
                .productId(new ProductId(cmd.getProductId()))
                .uomId(new UomId(cmd.getUomId()))
                .sourceLocationId(source)
                .destinationLocationId(destination)
                .demandQuantity(cmd.getDemandQuantity())
                .purchaseOrderLineId(cmd.getPurchaseOrderLineId())
                .salesOrderLineId(cmd.getSalesOrderLineId());
        if (cmd.getUnitCost() != null) {
            b.unitCost(new Money(cmd.getUnitCost()));
        }
        return b.build();
    }

    public StockPicking pickingCommandToDomain(CreateStockPickingCommand cmd,
                                                UUID pickingId,
                                                CompanyId companyId,
                                                List<StockMove> moves) {
        return StockPicking.builder()
                .id(new StockPickingId(pickingId))
                .companyId(companyId)
                .warehouseId(cmd.getWarehouseId() != null ? new WarehouseId(cmd.getWarehouseId()) : null)
                .pickingType(cmd.getPickingType())
                .reference(cmd.getReference())
                .sourceLocationId(new StockLocationId(cmd.getSourceLocationId()))
                .destinationLocationId(new StockLocationId(cmd.getDestinationLocationId()))
                .partnerId(cmd.getPartnerId())
                .origin(cmd.getOrigin())
                .scheduledAt(cmd.getScheduledAt())
                .purchaseOrderId(cmd.getPurchaseOrderId())
                .salesOrderId(cmd.getSalesOrderId())
                .moves(moves)
                .build();
    }

    public StockQuantResponse quantToResponse(StockQuant q) {
        StockQuantResponse r = new StockQuantResponse();
        r.setId(q.getId() != null ? q.getId().getId() : null);
        r.setCompanyId(q.getCompanyId().getId());
        r.setProductId(q.getProductId().getId());
        r.setLocationId(q.getLocationId().getId());
        r.setQuantity(q.getQuantity());
        r.setReservedQuantity(q.getReservedQuantity());
        r.setAvailableQuantity(q.getAvailable());
        r.setLastChangedAt(q.getLastChangedAt());
        return r;
    }

    public ValuationLayerResponse layerToResponse(StockValuationLayer l) {
        ValuationLayerResponse r = new ValuationLayerResponse();
        r.setId(l.getId() != null ? l.getId().getId() : null);
        r.setCompanyId(l.getCompanyId().getId());
        r.setProductId(l.getProductId().getId());
        r.setStockMoveId(l.getStockMoveId() != null ? l.getStockMoveId().getId() : null);
        r.setMethod(l.getMethod());
        r.setOccurredAt(l.getOccurredAt());
        r.setQuantity(l.getQuantity());
        r.setUnitCost(l.getUnitCost() != null ? l.getUnitCost().getAmount() : null);
        r.setValue(l.getValue() != null ? l.getValue().getAmount() : null);
        r.setRemainingQuantity(l.getRemainingQuantity());
        r.setRemainingValue(l.getRemainingValue() != null ? l.getRemainingValue().getAmount() : null);
        r.setJournalEntryId(l.getJournalEntryId());
        return r;
    }

    /** Suppresses unused-imports complaints. */
    @SuppressWarnings("unused")
    private void _unusedTypes(LocationType x, StockQuantId y) {}
}
