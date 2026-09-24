package com.bradox.delin.inventory.service.domain;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.domain.valueobject.Money;
import com.bradox.delin.inventory.domain.core.entity.Product;
import com.bradox.delin.inventory.domain.core.entity.ProductCategory;
import com.bradox.delin.inventory.domain.core.entity.ProductPackaging;
import com.bradox.delin.inventory.domain.core.entity.UnitOfMeasure;
import com.bradox.delin.inventory.domain.core.exception.InventoryDomainException;
import com.bradox.delin.inventory.domain.core.valueobject.ProductCategoryId;
import com.bradox.delin.inventory.domain.core.valueobject.ProductId;
import com.bradox.delin.inventory.domain.core.valueobject.UomId;
import com.bradox.delin.inventory.service.domain.dto.CreateProductCommand;
import com.bradox.delin.inventory.service.domain.dto.ProductCategoryCommand;
import com.bradox.delin.inventory.service.domain.dto.ProductCategoryResponse;
import com.bradox.delin.inventory.service.domain.dto.ProductImageMeta;
import com.bradox.delin.inventory.service.domain.dto.ProductPackagingResponse;
import com.bradox.delin.inventory.service.domain.dto.ProductResponse;
import com.bradox.delin.inventory.service.domain.dto.UpdateProductCommand;
import com.bradox.delin.inventory.service.domain.mapper.InventoryDataMapper;
import com.bradox.delin.inventory.service.domain.ports.input.ProductApplicationService;
import com.bradox.delin.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import com.bradox.delin.inventory.service.domain.ports.output.repository.ProductPackagingRepository;
import com.bradox.delin.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.delin.inventory.service.domain.ports.output.repository.UomRepository;
import com.bradox.delin.inventory.service.domain.ports.output.storage.ProductImageStoragePort;
import com.bradox.delin.platform.audit.AuditLogPort;
import com.bradox.delin.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class ProductApplicationServiceImpl implements ProductApplicationService {

    private static final String MODEL_NAME = "inventory.product";
    private static final String CATEGORY_MODEL_NAME = "inventory.product.category";

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductPackagingRepository packagingRepository;
    private final UomRepository uomRepository;
    private final InventoryDataMapper mapper;
    private final ProductImageStoragePort imageStorage;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final AuditLogPort auditLogPort;

    ProductApplicationServiceImpl(ProductRepository productRepository,
                                  ProductCategoryRepository categoryRepository,
                                  ProductPackagingRepository packagingRepository,
                                  UomRepository uomRepository,
                                  InventoryDataMapper mapper,
                                  ProductImageStoragePort imageStorage,
                                  ObjectProvider<CompanyContext> companyContextProvider,
                                  AuditLogPort auditLogPort) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.packagingRepository = packagingRepository;
        this.uomRepository = uomRepository;
        this.mapper = mapper;
        this.imageStorage = imageStorage;
        this.companyContextProvider = companyContextProvider;
        this.auditLogPort = auditLogPort;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        ensureBarcodeUnique(companyId, Product.normalizeBarcode(command.getBarcode()), null);
        UUID id = UUID.randomUUID();
        Product product = mapper.createCommandToProduct(command, id, companyId);
        product.validate();
        Product saved = productRepository.save(product);
        ensureBasePackaging(saved);
        auditLogPort.recordBusinessEvent(companyId, MODEL_NAME, id,
                "Product created: " + saved.getSku(), null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse uploadProductImage(UUID productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InventoryDomainException("error.inventory.imageFileRequired", null, "Image file is required");
        }
        Product p = loadIncludingArchivedOrThrow(productId);
        productRepository.findImageMeta(productId).ifPresent(meta -> imageStorage.deleteIfPresent(meta.imageUrl()));
        ProductImageStoragePort.StoredImage stored;
        try {
            stored = imageStorage.store(
                    p.getCompanyId().getId(),
                    productId,
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream());
        } catch (IOException ex) {
            throw new InventoryDomainException("error.inventory.failedReadUploadedImage", null, "Failed to read uploaded image");
        }
        productRepository.updateImage(productId, stored.publicUrl(), stored.contentType());
        auditLogPort.recordBusinessEvent(p.getCompanyId(), MODEL_NAME, productId, "Product image updated", null);
        return getProduct(productId);
    }

    @Override
    @Transactional
    public ProductResponse deleteProductImage(UUID productId) {
        Product p = loadIncludingArchivedOrThrow(productId);
        productRepository.findImageMeta(productId).ifPresent(meta -> imageStorage.deleteIfPresent(meta.imageUrl()));
        productRepository.clearImage(productId);
        auditLogPort.recordBusinessEvent(p.getCompanyId(), MODEL_NAME, productId, "Product image removed", null);
        return getProduct(productId);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductCommand cmd) {
        Product p = loadOrThrow(productId);
        Map<String, Object> changes = new LinkedHashMap<>();
        if (cmd.getSku() != null && !cmd.getSku().equals(p.getSku())) {
            changes.put("sku", Map.of("old", p.getSku(), "new", cmd.getSku()));
            p.changeSku(cmd.getSku());
        }
        if (cmd.getName() != null && !cmd.getName().equals(p.getName())) {
            changes.put("name", Map.of("old", p.getName(), "new", cmd.getName()));
            p.rename(cmd.getName());
        }
        if (cmd.getBarcode() != null) {
            String normalized = Product.normalizeBarcode(cmd.getBarcode());
            ensureBarcodeUnique(p.getCompanyId(), normalized, productId);
            p.changeBarcode(normalized);
        }
        if (cmd.getDescription() != null) p.changeDescription(cmd.getDescription());
        if (cmd.getProductType() != null && cmd.getProductType() != p.getProductType()) {
            changes.put("productType", Map.of("old", p.getProductType(), "new", cmd.getProductType()));
            p.changeProductType(cmd.getProductType());
        }
        if (cmd.getCategoryId() != null && (p.getCategoryId() == null || !cmd.getCategoryId().equals(p.getCategoryId().getId()))) {
            changes.put("categoryId", Map.of("old", p.getCategoryId() != null ? p.getCategoryId().getId() : null,
                    "new", cmd.getCategoryId()));
            p.changeCategory(new ProductCategoryId(cmd.getCategoryId()));
        }
        if (cmd.getUomId() != null) p.changeUom(new UomId(cmd.getUomId()));
        if (cmd.getPurchaseUomId() != null) p.changePurchaseUom(new UomId(cmd.getPurchaseUomId()));
        if (cmd.getStandardCost() != null) p.changeStandardCost(new Money(cmd.getStandardCost()));
        if (cmd.getListPrice() != null) p.changeListPrice(new Money(cmd.getListPrice()));
        boolean saleOkChanged = cmd.getSaleOk() != null && cmd.getSaleOk() != p.isSaleOk();
        boolean purchaseOkChanged = cmd.getPurchaseOk() != null && cmd.getPurchaseOk() != p.isPurchaseOk();
        if (cmd.getPurchaseOk() != null) p.changePurchaseOk(cmd.getPurchaseOk());
        if (cmd.getSaleOk() != null) p.changeSaleOk(cmd.getSaleOk());
        if (Boolean.TRUE.equals(cmd.getValuationMethodOverrideReset())) {
            p.changeValuationMethodOverride(null);
        } else if (cmd.getValuationMethodOverride() != null) {
            p.changeValuationMethodOverride(cmd.getValuationMethodOverride());
        }
        p.validate();
        Product saved = productRepository.save(p);
        if (saleOkChanged || purchaseOkChanged) {
            syncSalePurchaseFlagsToPackagedChildren(saved);
        }
        if (!changes.isEmpty()) {
            auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, productId,
                    "Product updated", changes);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse archiveProduct(UUID productId) {
        Product p = loadOrThrow(productId);
        if (!p.isActive()) return toResponse(p);
        p.archive(currentUserDisplay());
        Product saved = productRepository.save(p);
        auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, productId,
                "Product archived", null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse unarchiveProduct(UUID productId) {
        Product p = loadIncludingArchivedOrThrow(productId);
        if (p.isActive()) return toResponse(p);
        p.unarchive();
        Product saved = productRepository.save(p);
        auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, productId,
                "Product unarchived", null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID productId) {
        Product p = loadIncludingArchivedOrThrow(productId);
        if (productRepository.hasStockActivity(p.getId())) {
            throw new InventoryDomainException(
                    "Cannot delete product \"" + p.getName() + "\" because it has stock activity "
                            + "(moves, valuation layers, or on-hand quantities). Archive it instead.");
        }
        productRepository.findImageMeta(productId).ifPresent(meta -> imageStorage.deleteIfPresent(meta.imageUrl()));
        productRepository.deleteById(p.getId());
        auditLogPort.recordBusinessEvent(p.getCompanyId(), MODEL_NAME, productId,
                "Product deleted: " + p.getSku(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        Product product = loadIncludingArchivedOrThrow(productId);
        ensureBasePackaging(product);
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(CompanyId companyId, String query, boolean includeArchived, Pageable pageable) {
        Page<ProductResponse> page = productRepository.search(companyId, query, includeArchived, pageable)
                .map(mapper::productToResponse);
        enrichWithImages(page.getContent());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchSaleableProducts(CompanyId companyId, String query, Pageable pageable) {
        Page<ProductResponse> page = productRepository.searchSaleable(companyId, query, pageable)
                .map(mapper::productToResponse);
        enrichWithImages(page.getContent());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductResponse> findSaleableByBarcodeOrSku(CompanyId companyId, String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String trimmed = code.trim();
        Optional<Product> found = productRepository.findActiveByCompanyIdAndBarcode(companyId, trimmed);
        if (found.isEmpty()) {
            found = productRepository.findActiveByCompanyIdAndSku(companyId, trimmed);
        }
        return found.filter(Product::isSaleOk).filter(this::isEffectivelySaleable).map(this::toResponse);
    }

    /** Packaging variants inherit sellability from their parent. */
    private boolean isEffectivelySaleable(Product product) {
        if (!product.isSaleOk()) {
            return false;
        }
        if (product.getParentProductId() == null) {
            return true;
        }
        return productRepository.findByIdIncludingArchived(product.getParentProductId())
                .map(parent -> parent.isActive() && parent.isSaleOk())
                .orElse(false);
    }

    /** Keep packaging child products aligned with the parent's sell/purchase flags. */
    private void syncSalePurchaseFlagsToPackagedChildren(Product parent) {
        for (Product child : productRepository.findByParentProductId(parent.getId())) {
            boolean dirty = false;
            if (child.isSaleOk() != parent.isSaleOk()) {
                child.changeSaleOk(parent.isSaleOk());
                dirty = true;
            }
            if (child.isPurchaseOk() != parent.isPurchaseOk()) {
                child.changePurchaseOk(parent.isPurchaseOk());
                dirty = true;
            }
            if (dirty) {
                productRepository.save(child);
            }
        }
    }

    private void ensureBarcodeUnique(CompanyId companyId, String barcode, UUID excludeProductId) {
        if (barcode == null) {
            return;
        }
        if (productRepository.existsByCompanyIdAndBarcodeExcludingId(companyId, barcode, excludeProductId)) {
            throw new InventoryDomainException(
                    "error.inventory.barcodeDuplicate",
                    new Object[]{barcode},
                    "Barcode already in use: " + barcode);
        }
        UUID excludePackagingId = null;
        if (excludeProductId != null) {
            Product existing = productRepository.findByIdIncludingArchived(new ProductId(excludeProductId)).orElse(null);
            if (existing != null && existing.getSourcePackagingId() != null) {
                excludePackagingId = existing.getSourcePackagingId().getId();
            }
        }
        if (packagingRepository.existsByCompanyIdAndBarcodeExcludingId(companyId, barcode, excludePackagingId)) {
            throw new InventoryDomainException(
                    "error.inventory.barcodeDuplicate",
                    new Object[]{barcode},
                    "Barcode already in use: " + barcode);
        }
    }

    private void ensureBasePackaging(Product product) {
        if (product.isPackagedVariant()) {
            return;
        }
        if (packagingRepository.findBaseByProductId(product.getId()).isPresent()) {
            return;
        }
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
        packagingRepository.save(base);
    }

    private ProductPackagingResponse toPackagingResponse(ProductPackaging p) {
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
        r.setPackagedProductId(p.getPackagedProductId() != null ? p.getPackagedProductId().getId() : null);
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    @Override
    @Transactional
    public ProductCategoryResponse createCategory(ProductCategoryCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        ProductCategory entity = mapper.categoryCommandToDomain(command, UUID.randomUUID(), companyId);
        entity.validate();
        ProductCategory saved = categoryRepository.save(entity);
        auditLogPort.recordBusinessEvent(companyId, CATEGORY_MODEL_NAME, saved.getId().getId(),
                "Product category created: " + saved.getName(), null);
        return mapper.categoryToResponse(saved);
    }

    @Override
    @Transactional
    public ProductCategoryResponse updateCategory(UUID categoryId, ProductCategoryCommand cmd) {
        ProductCategory c = categoryRepository.findById(new ProductCategoryId(categoryId))
                .orElseThrow(() -> new InventoryDomainException("Product category not found: " + categoryId));
        if (cmd.getName() != null) c.rename(cmd.getName());
        if (cmd.getValuationMethod() != null) c.changeValuationMethod(cmd.getValuationMethod());
        if (cmd.getParentId() != null) c.changeParent(new ProductCategoryId(cmd.getParentId()));
        c.changeAccounts(cmd.getStockValuationAccountId(), cmd.getStockInputAccountId(),
                cmd.getStockOutputAccountId(), cmd.getCogsAccountId());
        c.validate();
        return mapper.categoryToResponse(categoryRepository.save(c));
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId) {
        ProductCategoryId id = new ProductCategoryId(categoryId);
        ProductCategory c = categoryRepository.findByIdIncludingArchived(id)
                .orElseThrow(() -> new InventoryDomainException("Product category not found: " + categoryId));
        if (productRepository.existsByCategory(id)) {
            throw new InventoryDomainException(
                    "Cannot delete category \"" + c.getName() + "\" because products are assigned to it. "
                            + "Reassign or remove those products first.");
        }
        if (categoryRepository.hasChildren(id)) {
            throw new InventoryDomainException(
                    "Cannot delete category \"" + c.getName() + "\" because it has child categories. "
                            + "Delete or reparent them first.");
        }
        categoryRepository.deleteById(id);
        auditLogPort.recordBusinessEvent(c.getCompanyId(), CATEGORY_MODEL_NAME, categoryId,
                "Product category deleted: " + c.getName(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> listCategories(CompanyId companyId, boolean includeArchived) {
        return categoryRepository.findByCompany(companyId, includeArchived).stream()
                .map(mapper::categoryToResponse)
                .toList();
    }

    private Product loadOrThrow(UUID id) {
        return productRepository.findById(new ProductId(id))
                .orElseThrow(() -> new InventoryDomainException("Product not found: " + id));
    }

    private Product loadIncludingArchivedOrThrow(UUID id) {
        return productRepository.findByIdIncludingArchived(new ProductId(id))
                .orElseThrow(() -> new InventoryDomainException("Product not found: " + id));
    }

    private CompanyId resolveCompany(UUID explicit) {
        if (explicit != null) return new CompanyId(explicit);
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany().orElseThrow(() ->
                    new IllegalArgumentException("companyId required (header X-Company-Id, query param, or body)"));
        }
        throw new IllegalArgumentException("companyId required");
    }

    private String currentUserDisplay() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? "system" : ctx.currentUserDisplay();
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = mapper.productToResponse(product);
        productRepository.findImageMeta(product.getId().getId()).ifPresent(meta -> applyImage(response, meta));
        response.setPackagings(packagingRepository.findByProductId(product.getId()).stream()
                .map(this::toPackagingResponse)
                .toList());
        return response;
    }

    private void enrichWithImages(List<ProductResponse> items) {
        if (items.isEmpty()) return;
        Map<UUID, ProductImageMeta> meta = productRepository.findImageMetaByProductIds(
                items.stream().map(ProductResponse::getId).collect(Collectors.toList()));
        for (ProductResponse item : items) {
            ProductImageMeta image = meta.get(item.getId());
            if (image != null) applyImage(item, image);
        }
    }

    private static void applyImage(ProductResponse response, ProductImageMeta meta) {
        response.setImageUrl(meta.imageUrl());
        response.setImageContentType(meta.contentType());
    }
}
