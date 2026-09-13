package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductPackaging;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UnitOfMeasure;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductPackagingId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductPackagingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductPackagingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductPackagingApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductPackagingRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.UomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Validated
class ProductPackagingApplicationServiceImpl implements ProductPackagingApplicationService {

    private final ProductPackagingRepository packagingRepository;
    private final ProductRepository productRepository;
    private final UomRepository uomRepository;

    ProductPackagingApplicationServiceImpl(ProductPackagingRepository packagingRepository,
                                           ProductRepository productRepository,
                                           UomRepository uomRepository) {
        this.packagingRepository = packagingRepository;
        this.productRepository = productRepository;
        this.uomRepository = uomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPackagingResponse> listByProduct(UUID productId) {
        ensureProduct(productId);
        return packagingRepository.findByProductId(new ProductId(productId)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPackagingResponse> listByProductIds(Collection<UUID> productIds) {
        return packagingRepository.findByProductIds(productIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductPackagingResponse create(UUID productId, ProductPackagingCommand command) {
        Product product = ensureProduct(productId);
        String name = command.getName().trim();
        ensureUniqueName(product.getCompanyId(), product.getId(), name, null);
        String barcode = ProductPackaging.normalizeBarcode(command.getBarcode());
        ensureBarcodeAvailable(product.getCompanyId(), barcode, null);

        ProductPackaging packaging = new ProductPackaging();
        packaging.setId(new ProductPackagingId(UUID.randomUUID()));
        packaging.setCompanyId(product.getCompanyId());
        packaging.setProductId(product.getId());
        packaging.setName(name);
        packaging.setQty(command.getQty());
        packaging.setPurchasePrice(new Money(command.getPurchasePrice()));
        packaging.setListPrice(new Money(command.getListPrice()));
        packaging.setBarcode(barcode);
        packaging.setSku(command.getSku());
        packaging.setActive(command.getActive() == null || command.getActive());
        packaging.setBase(false);
        Instant now = Instant.now();
        packaging.setCreatedAt(now);
        packaging.setUpdatedAt(now);
        packaging.validate();
        return toResponse(packagingRepository.save(packaging));
    }

    @Override
    @Transactional
    public ProductPackagingResponse update(UUID productId, UUID packagingId, ProductPackagingCommand command) {
        Product product = ensureProduct(productId);
        ProductPackaging packaging = loadForProduct(productId, packagingId);
        String name = command.getName().trim();
        ensureUniqueName(product.getCompanyId(), product.getId(), name, packagingId);
        String barcode = ProductPackaging.normalizeBarcode(command.getBarcode());
        ensureBarcodeAvailable(product.getCompanyId(), barcode, packagingId);

        packaging.setName(name);
        if (!packaging.isBase()) {
            packaging.setQty(command.getQty());
        } else if (command.getQty() != null && command.getQty().compareTo(java.math.BigDecimal.ONE) != 0) {
            throw new InventoryDomainException(
                    "error.inventory.basePackagingQtyFixed",
                    null,
                    "Base packaging quantity must remain 1");
        }
        packaging.setPurchasePrice(new Money(command.getPurchasePrice()));
        packaging.setListPrice(new Money(command.getListPrice()));
        packaging.setBarcode(barcode);
        packaging.setSku(command.getSku());
        if (command.getActive() != null) {
            if (packaging.isBase() && !command.getActive()) {
                throw new InventoryDomainException(
                        "error.inventory.cannotDeactivateBasePackaging",
                        null,
                        "Cannot deactivate base packaging");
            }
            packaging.setActive(command.getActive());
        }
        packaging.setUpdatedAt(Instant.now());
        packaging.validate();
        return toResponse(packagingRepository.save(packaging));
    }

    @Override
    @Transactional
    public void delete(UUID productId, UUID packagingId) {
        ProductPackaging packaging = loadForProduct(productId, packagingId);
        if (packaging.isBase()) {
            throw new InventoryDomainException(
                    "error.inventory.cannotDeleteBasePackaging",
                    null,
                    "Cannot delete base packaging");
        }
        packagingRepository.deleteById(packaging.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductPackaging> findActiveByBarcode(CompanyId companyId, String barcode) {
        return packagingRepository.findActiveByCompanyIdAndBarcode(companyId, barcode);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPackagingResponse get(UUID packagingId) {
        return packagingRepository.findById(new ProductPackagingId(packagingId))
                .map(this::toResponse)
                .orElseThrow(() -> new InventoryDomainException(
                        "error.inventory.packagingNotFound",
                        new Object[]{packagingId},
                        "Packaging not found: " + packagingId));
    }

    @Override
    @Transactional
    public ProductPackagingResponse ensureBasePackaging(UUID productId) {
        Product product = ensureProduct(productId);
        return packagingRepository.findBaseByProductId(product.getId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    String uomName = uomRepository.findById(product.getUomId())
                            .map(UnitOfMeasure::getName)
                            .orElse("Unit");
                    ProductPackaging base = ProductPackaging.createBase(
                            product.getCompanyId(),
                            product.getId(),
                            uomName,
                            product.getStandardCost(),
                            product.getListPrice(),
                            product.getBarcode());
                    return toResponse(packagingRepository.save(base));
                });
    }

    private Product ensureProduct(UUID productId) {
        return productRepository.findByIdIncludingArchived(new ProductId(productId))
                .orElseThrow(() -> new InventoryDomainException("Product not found: " + productId));
    }

    private ProductPackaging loadForProduct(UUID productId, UUID packagingId) {
        ProductPackaging packaging = packagingRepository.findById(new ProductPackagingId(packagingId))
                .orElseThrow(() -> new InventoryDomainException(
                        "error.inventory.packagingNotFound",
                        new Object[]{packagingId},
                        "Packaging not found: " + packagingId));
        if (!packaging.getProductId().getId().equals(productId)) {
            throw new InventoryDomainException(
                    "error.inventory.packagingProductMismatch",
                    null,
                    "Packaging does not belong to this product");
        }
        return packaging;
    }

    private void ensureUniqueName(CompanyId companyId, ProductId productId, String name, UUID excludeId) {
        if (packagingRepository.existsByProductIdAndNameExcludingId(companyId, productId, name, excludeId)) {
            throw new InventoryDomainException(
                    "error.inventory.packagingNameDuplicate",
                    new Object[]{name},
                    "Packaging name already exists: " + name);
        }
    }

    private void ensureBarcodeAvailable(CompanyId companyId, String barcode, UUID excludePackagingId) {
        if (barcode == null) return;
        if (packagingRepository.existsByCompanyIdAndBarcodeExcludingId(companyId, barcode, excludePackagingId)) {
            throw new InventoryDomainException(
                    "error.inventory.barcodeDuplicate",
                    new Object[]{barcode},
                    "Barcode already in use: " + barcode);
        }
        if (productRepository.existsByCompanyIdAndBarcodeExcludingId(companyId, barcode, null)) {
            // Allow same barcode as the product itself only when updating the product's base pack that was seeded with it.
            // Still block when another product owns it — excludeId null checks any product.
            boolean ownedBySameProductBase = false;
            if (excludePackagingId != null) {
                Optional<ProductPackaging> current = packagingRepository.findById(new ProductPackagingId(excludePackagingId));
                if (current.isPresent() && current.get().isBase()) {
                    Optional<Product> product = productRepository.findById(current.get().getProductId());
                    ownedBySameProductBase = product.isPresent()
                            && barcode.equalsIgnoreCase(ProductPackaging.normalizeBarcode(product.get().getBarcode()));
                }
            }
            if (!ownedBySameProductBase) {
                throw new InventoryDomainException(
                        "error.inventory.barcodeDuplicate",
                        new Object[]{barcode},
                        "Barcode already in use: " + barcode);
            }
        }
    }

    private ProductPackagingResponse toResponse(ProductPackaging p) {
        ProductPackagingResponse r = new ProductPackagingResponse();
        r.setId(p.getId().getId());
        r.setCompanyId(p.getCompanyId().getId());
        r.setProductId(p.getProductId().getId());
        r.setName(p.getName());
        r.setQty(p.getQty());
        r.setPurchasePrice(p.getPurchasePrice() != null ? p.getPurchasePrice().getAmount() : java.math.BigDecimal.ZERO);
        r.setListPrice(p.getListPrice() != null ? p.getListPrice().getAmount() : java.math.BigDecimal.ZERO);
        r.setBarcode(p.getBarcode());
        r.setSku(p.getSku());
        r.setActive(p.isActive());
        r.setBase(p.isBase());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
