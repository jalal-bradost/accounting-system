package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UpdateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.mapper.InventoryDataMapper;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditLogPort;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class ProductApplicationServiceImpl implements ProductApplicationService {

    private static final String MODEL_NAME = "inventory.product";
    private static final String CATEGORY_MODEL_NAME = "inventory.product.category";

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final InventoryDataMapper mapper;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final AuditLogPort auditLogPort;

    ProductApplicationServiceImpl(ProductRepository productRepository,
                                  ProductCategoryRepository categoryRepository,
                                  InventoryDataMapper mapper,
                                  ObjectProvider<CompanyContext> companyContextProvider,
                                  AuditLogPort auditLogPort) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
        this.companyContextProvider = companyContextProvider;
        this.auditLogPort = auditLogPort;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        UUID id = UUID.randomUUID();
        Product product = mapper.createCommandToProduct(command, id, companyId);
        product.validate();
        Product saved = productRepository.save(product);
        auditLogPort.recordBusinessEvent(companyId, MODEL_NAME, id,
                "Product created: " + saved.getSku(), null);
        return mapper.productToResponse(saved);
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
        if (cmd.getBarcode() != null) p.changeBarcode(cmd.getBarcode());
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
        if (cmd.getPurchaseOk() != null) p.changePurchaseOk(cmd.getPurchaseOk());
        if (cmd.getSaleOk() != null) p.changeSaleOk(cmd.getSaleOk());
        if (Boolean.TRUE.equals(cmd.getValuationMethodOverrideReset())) {
            p.changeValuationMethodOverride(null);
        } else if (cmd.getValuationMethodOverride() != null) {
            p.changeValuationMethodOverride(cmd.getValuationMethodOverride());
        }
        p.validate();
        Product saved = productRepository.save(p);
        if (!changes.isEmpty()) {
            auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, productId,
                    "Product updated", changes);
        }
        return mapper.productToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse archiveProduct(UUID productId) {
        Product p = loadOrThrow(productId);
        if (!p.isActive()) return mapper.productToResponse(p);
        p.archive(currentUserDisplay());
        Product saved = productRepository.save(p);
        auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, productId,
                "Product archived", null);
        return mapper.productToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse unarchiveProduct(UUID productId) {
        Product p = loadIncludingArchivedOrThrow(productId);
        if (p.isActive()) return mapper.productToResponse(p);
        p.unarchive();
        Product saved = productRepository.save(p);
        auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, productId,
                "Product unarchived", null);
        return mapper.productToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        return mapper.productToResponse(loadIncludingArchivedOrThrow(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(CompanyId companyId, String query, boolean includeArchived, Pageable pageable) {
        return productRepository.search(companyId, query, includeArchived, pageable).map(mapper::productToResponse);
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
}
