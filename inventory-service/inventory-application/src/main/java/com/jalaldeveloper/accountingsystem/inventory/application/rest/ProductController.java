package com.jalaldeveloper.accountingsystem.inventory.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UpdateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductApplicationService;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/inventory/products", produces = "application/json")
public class ProductController {

    private final ProductApplicationService service;

    public ProductController(ProductApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductCommand cmd) {
        return ResponseEntity.ok(service.createProduct(cmd));
    }

    @GetMapping("/{id}")
    @RequiresPermission("inventory.product.read")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getProduct(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateProductCommand cmd) {
        return ResponseEntity.ok(service.updateProduct(id, cmd));
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("inventory.product.archive")
    public ResponseEntity<ProductResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.archiveProduct(id));
    }

    @PostMapping("/{id}/unarchive")
    @RequiresPermission("inventory.product.archive")
    public ResponseEntity<ProductResponse> unarchive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.unarchiveProduct(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductResponse> uploadImage(@PathVariable UUID id,
                                                       @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(service.uploadProductImage(id, file));
    }

    @DeleteMapping("/{id}/image")
    @RequiresPermission("inventory.product.write")
    public ResponseEntity<ProductResponse> deleteImage(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deleteProductImage(id));
    }

    @GetMapping
    @RequiresPermission("inventory.product.read")
    public ResponseEntity<PageResponse<ProductResponse>> list(@CurrentCompany CompanyId companyId,
                                                               @RequestParam(required = false) String q,
                                                               @RequestParam(defaultValue = "false") boolean includeArchived,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "50") int size) {
        var result = service.searchProducts(companyId, q, includeArchived, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }
}
