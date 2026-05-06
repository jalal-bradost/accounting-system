package com.jalaldeveloper.accountingsystem.inventory.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.UomApplicationService;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/inventory", produces = "application/json")
public class UomController {

    private final UomApplicationService service;

    public UomController(UomApplicationService service) {
        this.service = service;
    }

    @PostMapping("/uom-categories")
    @RequiresPermission("inventory.uom.write")
    public ResponseEntity<UomCategoryResponse> createCategory(@Valid @RequestBody UomCategoryCommand cmd) {
        return ResponseEntity.ok(service.createUomCategory(cmd));
    }

    @GetMapping("/uom-categories")
    @RequiresPermission("inventory.uom.read")
    public ResponseEntity<List<UomCategoryResponse>> listCategories(@CurrentCompany CompanyId companyId,
                                                                     @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(service.listUomCategories(companyId, includeArchived));
    }

    @PostMapping("/uoms")
    @RequiresPermission("inventory.uom.write")
    public ResponseEntity<UomResponse> createUom(@Valid @RequestBody UomCommand cmd) {
        return ResponseEntity.ok(service.createUom(cmd));
    }

    @GetMapping("/uoms/{id}")
    @RequiresPermission("inventory.uom.read")
    public ResponseEntity<UomResponse> getUom(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getUom(id));
    }

    @PutMapping("/uoms/{id}")
    @RequiresPermission("inventory.uom.write")
    public ResponseEntity<UomResponse> updateUom(@PathVariable UUID id,
                                                  @Valid @RequestBody UomCommand cmd) {
        return ResponseEntity.ok(service.updateUom(id, cmd));
    }

    @GetMapping("/uom-categories/{categoryId}/uoms")
    @RequiresPermission("inventory.uom.read")
    public ResponseEntity<List<UomResponse>> listByCategory(@PathVariable UUID categoryId,
                                                             @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(service.listUomsByCategory(categoryId, includeArchived));
    }

    @GetMapping("/uoms/convert")
    @RequiresPermission("inventory.uom.read")
    public ResponseEntity<Map<String, Object>> convert(@RequestParam UUID fromUomId,
                                                        @RequestParam UUID toUomId,
                                                        @RequestParam BigDecimal quantity) {
        BigDecimal converted = service.convert(fromUomId, toUomId, quantity);
        return ResponseEntity.ok(Map.of(
                "fromUomId", fromUomId,
                "toUomId", toUomId,
                "inputQuantity", quantity,
                "convertedQuantity", converted));
    }
}
