package com.bradox.erp.inventory.application.rest;

import com.bradox.erp.inventory.service.domain.dto.ProductPackagingCommand;
import com.bradox.erp.inventory.service.domain.dto.ProductPackagingResponse;
import com.bradox.erp.inventory.service.domain.dto.PackStockCommand;
import com.bradox.erp.inventory.service.domain.ports.input.ProductPackagingApplicationService;
import com.bradox.erp.platform.security.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/inventory/products/{productId}/packagings", produces = "application/json")
public class ProductPackagingController {

    private final ProductPackagingApplicationService service;

    public ProductPackagingController(ProductPackagingApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("inventory.product.read")
    public ResponseEntity<List<ProductPackagingResponse>> list(@PathVariable UUID productId) {
        return ResponseEntity.ok(service.listByProduct(productId));
    }

    @PostMapping
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductPackagingResponse> create(@PathVariable UUID productId,
                                                           @Valid @RequestBody ProductPackagingCommand command) {
        return ResponseEntity.ok(service.create(productId, command));
    }

    @PutMapping("/{packagingId}")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductPackagingResponse> update(@PathVariable UUID productId,
                                                           @PathVariable UUID packagingId,
                                                           @Valid @RequestBody ProductPackagingCommand command) {
        return ResponseEntity.ok(service.update(productId, packagingId, command));
    }

    @DeleteMapping("/{packagingId}")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<Void> delete(@PathVariable UUID productId,
                                       @PathVariable UUID packagingId) {
        service.delete(productId, packagingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{packagingId}/pack")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductPackagingResponse> pack(@PathVariable UUID productId,
                                                         @PathVariable UUID packagingId,
                                                         @Valid @RequestBody PackStockCommand command) {
        return ResponseEntity.ok(service.pack(productId, packagingId, command));
    }

    @PostMapping("/{packagingId}/unpack")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductPackagingResponse> unpack(@PathVariable UUID productId,
                                                           @PathVariable UUID packagingId,
                                                           @Valid @RequestBody PackStockCommand command) {
        return ResponseEntity.ok(service.unpack(productId, packagingId, command));
    }
}
