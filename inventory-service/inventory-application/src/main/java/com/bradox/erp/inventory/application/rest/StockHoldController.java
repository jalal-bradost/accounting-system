package com.bradox.erp.inventory.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import com.bradox.erp.inventory.service.domain.StockHoldAvailabilityHub;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.HoldLine;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.SyncHoldsCommand;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.WarehouseAvailabilityRow;
import com.bradox.erp.inventory.service.domain.ports.input.StockHoldApplicationService;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class StockHoldController {

    private final StockHoldApplicationService service;
    private final StockHoldAvailabilityHub hub;

    public StockHoldController(StockHoldApplicationService service, StockHoldAvailabilityHub hub) {
        this.service = service;
        this.hub = hub;
    }

    public record SyncHoldsRequest(
            UUID cartSessionId,
            UUID warehouseId,
            UUID holderId,
            List<HoldLineInput> lines) {}

    public record HoldLineInput(UUID productId, BigDecimal quantity) {}

    public record HeartbeatRequest(UUID cartSessionId) {}

    public record ReleaseRequest(UUID cartSessionId) {}

    @PostMapping("/stock-holds/sync")
    @RequiresPermission(value = {"salespeople.self.write", "salespeople.write"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<Map<String, Object>> syncCart(@CurrentCompany CompanyId companyId,
                                                        @RequestBody SyncHoldsRequest body) {
        if (body == null || body.cartSessionId() == null || body.warehouseId() == null
                || body.holderId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cartSessionId, warehouseId, holderId required"));
        }
        List<HoldLine> lines = body.lines() == null ? List.of() : body.lines().stream()
                .filter(l -> l != null && l.productId() != null && l.quantity() != null)
                .map(l -> new HoldLine(l.productId(), l.quantity()))
                .toList();
        service.syncHolds(companyId, new SyncHoldsCommand(
                StockHoldOwnerType.CART, body.cartSessionId(), body.warehouseId(),
                body.holderId(), lines));
        return ResponseEntity.ok(Map.of("ok", true, "ownerType", "CART", "ownerId", body.cartSessionId()));
    }

    @PostMapping("/stock-holds/heartbeat")
    @RequiresPermission(value = {"salespeople.self.write", "salespeople.write"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<Map<String, Object>> heartbeat(@CurrentCompany CompanyId companyId,
                                                         @RequestBody HeartbeatRequest body) {
        if (body == null || body.cartSessionId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cartSessionId required"));
        }
        service.heartbeat(companyId, StockHoldOwnerType.CART, body.cartSessionId(),
                StockHoldApplicationService.CART_TTL);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/stock-holds/release")
    @RequiresPermission(value = {"salespeople.self.write", "salespeople.write"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<Map<String, Object>> release(@CurrentCompany CompanyId companyId,
                                                       @RequestBody ReleaseRequest body) {
        if (body == null || body.cartSessionId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cartSessionId required"));
        }
        service.releaseOwner(companyId, StockHoldOwnerType.CART, body.cartSessionId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/inventory/warehouses/{warehouseId}/available")
    @RequiresPermission(value = {
            "inventory.valuation.read", "inventory.product.read",
            "salespeople.self.read", "salespeople.read"
    }, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<List<WarehouseAvailabilityRow>> warehouseAvailable(
            @CurrentCompany CompanyId companyId,
            @PathVariable UUID warehouseId,
            @RequestParam(required = false) String excludeOwnerType,
            @RequestParam(required = false) UUID excludeOwnerId) {
        StockHoldOwnerType excludeType = null;
        if (excludeOwnerType != null && !excludeOwnerType.isBlank()) {
            excludeType = StockHoldOwnerType.valueOf(excludeOwnerType.trim().toUpperCase());
        }
        return ResponseEntity.ok(service.availableByWarehouse(companyId, warehouseId, excludeType, excludeOwnerId));
    }

    @GetMapping(value = "/stock-holds/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiresPermission(value = {"salespeople.self.read", "salespeople.read"}, op = RequiresPermission.LogicalOp.OR)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SseEmitter stream(@CurrentCompany CompanyId companyId,
                             @RequestParam UUID warehouseId) {
        return hub.subscribe(companyId.getId(), warehouseId);
    }
}
