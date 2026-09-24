package com.bradox.erp.inventory.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.service.domain.dto.StockQuantResponse;
import com.bradox.erp.inventory.service.domain.dto.ValuationLayerResponse;
import com.bradox.erp.inventory.service.domain.ports.input.StockValuationApplicationService;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/inventory", produces = "application/json")
public class StockValuationController {

    private final StockValuationApplicationService service;

    public StockValuationController(StockValuationApplicationService service) {
        this.service = service;
    }

    @GetMapping("/quants")
    @RequiresPermission("inventory.valuation.read")
    public ResponseEntity<List<StockQuantResponse>> quants(@CurrentCompany CompanyId companyId,
                                                            @RequestParam(required = false) UUID productId,
                                                            @RequestParam(required = false) UUID locationId) {
        if (productId == null && locationId == null) {
            throw new IllegalArgumentException("either productId or locationId is required");
        }
        if (productId != null) {
            return ResponseEntity.ok(service.onHandByProduct(companyId, productId));
        }
        return ResponseEntity.ok(service.onHandByLocation(companyId, locationId));
    }

    @GetMapping("/on-hand/{productId}")
    @RequiresPermission("inventory.valuation.read")
    public ResponseEntity<Map<String, Object>> onHand(@CurrentCompany CompanyId companyId,
                                                       @PathVariable UUID productId) {
        BigDecimal qty = service.totalOnHand(companyId, productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "totalOnHand", qty));
    }

    @GetMapping("/valuation-layers")
    @RequiresPermission("inventory.valuation.read")
    public ResponseEntity<List<ValuationLayerResponse>> layers(@CurrentCompany CompanyId companyId,
                                                                @RequestParam UUID productId) {
        return ResponseEntity.ok(service.layersByProduct(companyId, productId));
    }

    @GetMapping("/valuation/{productId}")
    @RequiresPermission("inventory.valuation.read")
    public ResponseEntity<Map<String, Object>> valuation(@CurrentCompany CompanyId companyId,
                                                          @PathVariable UUID productId) {
        BigDecimal value = service.valuationOf(companyId, productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "valuation", value));
    }
}
