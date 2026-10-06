package com.bradox.erp.inventory.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.entity.Warehouse;
import com.bradox.erp.inventory.domain.core.exception.InventoryDomainException;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.service.domain.dto.InventoryAdjustmentCommand;
import com.bradox.erp.inventory.service.domain.dto.OpeningStockCommand;
import com.bradox.erp.inventory.service.domain.dto.OpeningStockReport;
import com.bradox.erp.inventory.service.domain.ports.input.OpeningStockApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.WarehouseRepository;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Validated
class OpeningStockApplicationServiceImpl implements OpeningStockApplicationService {

    private static final String DEFAULT_WAREHOUSE_CODE = "WH";

    private final StockPickingApplicationService pickingService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    OpeningStockApplicationServiceImpl(StockPickingApplicationService pickingService,
                                       ProductRepository productRepository,
                                       WarehouseRepository warehouseRepository,
                                       ObjectProvider<CompanyContext> companyContextProvider) {
        this.pickingService = pickingService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    public OpeningStockReport importOpeningStock(OpeningStockCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        UUID locationId = command.getLocationId() != null
                ? command.getLocationId()
                : resolveDefaultStockLocation(companyId);

        OpeningStockReport report = new OpeningStockReport();
        int row = 0;
        for (OpeningStockCommand.Line line : command.getLines()) {
            row++;
            try {
                applyLine(companyId, locationId, line, report, row);
            } catch (RuntimeException ex) {
                report.error("Row " + row + ": " + ex.getMessage());
            }
        }
        return report;
    }

    private void applyLine(CompanyId companyId, UUID locationId, OpeningStockCommand.Line line,
                           OpeningStockReport report, int row) {
        if (line.getQuantity() == null || line.getQuantity().signum() < 0) {
            report.error("Row " + row + ": quantity must be zero or positive");
            return;
        }
        Optional<Product> product = resolveProduct(companyId, line);
        if (product.isEmpty()) {
            report.error("Row " + row + ": unknown product"
                    + (line.getSku() != null && !line.getSku().isBlank() ? " sku=" + line.getSku() : ""));
            return;
        }
        Product p = product.get();
        if (p.isPackagedVariant()) {
            report.error("Row " + row + ": packaged variant " + p.getSku()
                    + " cannot be adjusted directly (use Pack / Unpack)");
            return;
        }

        InventoryAdjustmentCommand adj = new InventoryAdjustmentCommand();
        adj.setCompanyId(companyId.getId());
        adj.setProductId(p.getId().getId());
        adj.setLocationId(locationId);
        adj.setTargetQuantity(line.getQuantity());
        adj.setOpeningBalance(true);
        adj.setReason(InventoryAdjustmentCommand.OPENING_STOCK_REASON);
        if (line.getUnitCost() != null && line.getUnitCost().signum() > 0) {
            adj.setUnitCost(line.getUnitCost());
        }

        try {
            pickingService.adjustInventory(adj);
            report.incrementApplied();
        } catch (InventoryDomainException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            if (msg.contains("no-op") || msg.contains("already equals target")) {
                report.incrementSkipped();
                report.warning("Row " + row + " (" + p.getSku() + "): already at target quantity");
            } else {
                report.error("Row " + row + " (" + p.getSku() + "): " + msg);
            }
        }
    }

    private Optional<Product> resolveProduct(CompanyId companyId, OpeningStockCommand.Line line) {
        if (line.getProductId() != null) {
            return productRepository.findById(new ProductId(line.getProductId()));
        }
        if (line.getSku() != null && !line.getSku().isBlank()) {
            return productRepository.findActiveByCompanyIdAndSku(companyId, line.getSku().trim());
        }
        return Optional.empty();
    }

    private UUID resolveDefaultStockLocation(CompanyId companyId) {
        List<Warehouse> warehouses = warehouseRepository.findByCompany(companyId, false);
        Warehouse preferred = warehouses.stream()
                .filter(w -> DEFAULT_WAREHOUSE_CODE.equalsIgnoreCase(w.getCode()))
                .findFirst()
                .or(() -> warehouses.stream().findFirst())
                .orElseThrow(() -> new InventoryDomainException(
                        "No warehouse for company " + companyId.getId()
                                + " — create a warehouse before importing opening stock"));
        if (preferred.getStockLocationId() == null) {
            throw new InventoryDomainException(
                    "Warehouse " + preferred.getCode() + " has no stock location");
        }
        return preferred.getStockLocationId().getId();
    }

    private CompanyId resolveCompany(UUID explicit) {
        if (explicit != null) {
            return new CompanyId(explicit);
        }
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany().orElseThrow(() ->
                    new InventoryDomainException("Company id is required"));
        }
        throw new InventoryDomainException("Company id is required");
    }
}
