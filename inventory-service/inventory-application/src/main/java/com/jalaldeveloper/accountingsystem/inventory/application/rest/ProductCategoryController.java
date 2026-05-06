package com.jalaldeveloper.accountingsystem.inventory.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductApplicationService;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/inventory/product-categories", produces = "application/json")
public class ProductCategoryController {

    private final ProductApplicationService service;

    public ProductCategoryController(ProductApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductCategoryResponse> create(@Valid @RequestBody ProductCategoryCommand cmd) {
        return ResponseEntity.ok(service.createCategory(cmd));
    }

    @PutMapping("/{id}")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductCategoryResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody ProductCategoryCommand cmd) {
        return ResponseEntity.ok(service.updateCategory(id, cmd));
    }

    @GetMapping
    @RequiresPermission("inventory.product.read")
    public ResponseEntity<List<ProductCategoryResponse>> list(@CurrentCompany CompanyId companyId,
                                                               @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(service.listCategories(companyId, includeArchived));
    }
}
