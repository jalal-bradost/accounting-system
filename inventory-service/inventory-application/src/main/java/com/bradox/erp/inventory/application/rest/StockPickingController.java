package com.bradox.erp.inventory.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.valueobject.PickingState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingType;
import com.bradox.erp.inventory.service.domain.dto.CreateStockPickingCommand;
import com.bradox.erp.inventory.service.domain.dto.InventoryAdjustmentCommand;
import com.bradox.erp.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.erp.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.erp.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/inventory/pickings", produces = "application/json")
public class StockPickingController {

    private final StockPickingApplicationService service;

    public StockPickingController(StockPickingApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<StockPickingResponse> create(@Valid @RequestBody CreateStockPickingCommand cmd) {
        return ResponseEntity.ok(service.createPicking(cmd));
    }

    @GetMapping("/{id}")
    @RequiresPermission("inventory.picking.read")
    public ResponseEntity<StockPickingResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPicking(id));
    }

    @PostMapping("/{id}/confirm")
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<StockPickingResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(service.confirmPicking(id));
    }

    @PostMapping("/{id}/assign")
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<StockPickingResponse> assign(@PathVariable UUID id) {
        return ResponseEntity.ok(service.assignPicking(id));
    }

    @PostMapping("/{id}/validate")
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<StockPickingResponse> validate(@PathVariable UUID id,
                                                          @RequestBody(required = false) ValidatePickingCommand cmd) {
        ValidatePickingCommand effective = cmd != null ? cmd : new ValidatePickingCommand();
        return ResponseEntity.ok(service.validatePicking(id, effective));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<StockPickingResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelPicking(id));
    }

    @PostMapping("/{id}/return")
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<StockPickingResponse> doReturn(@PathVariable UUID id) {
        return ResponseEntity.ok(service.returnPicking(id));
    }

    @PostMapping("/adjust")
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<StockPickingResponse> adjust(@Valid @RequestBody InventoryAdjustmentCommand cmd) {
        return ResponseEntity.ok(service.adjustInventory(cmd));
    }

    @GetMapping
    @RequiresPermission("inventory.picking.read")
    public ResponseEntity<PageResponse<StockPickingResponse>> list(@CurrentCompany CompanyId companyId,
                                                                    @RequestParam(required = false) PickingType pickingType,
                                                                    @RequestParam(required = false) PickingState state,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "50") int size) {
        var result = service.searchPickings(companyId, pickingType, state, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }
}
