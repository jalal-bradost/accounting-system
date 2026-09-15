package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductPackaging;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockLocation;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UnitOfMeasure;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductPackagingId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateStockPickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.PackStockCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductPackagingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductPackagingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockMoveCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValidatePickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductPackagingApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductPackagingRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockQuantRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.UomRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository locationRepository;
    private final StockQuantRepository quantRepository;
    private final StockPickingApplicationService stockPickingApplicationService;

    ProductPackagingApplicationServiceImpl(ProductPackagingRepository packagingRepository,
                                           ProductRepository productRepository,
                                           UomRepository uomRepository,
                                           WarehouseRepository warehouseRepository,
                                           StockLocationRepository locationRepository,
                                           StockQuantRepository quantRepository,
                                           StockPickingApplicationService stockPickingApplicationService) {
        this.packagingRepository = packagingRepository;
        this.productRepository = productRepository;
        this.uomRepository = uomRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.quantRepository = quantRepository;
        this.stockPickingApplicationService = stockPickingApplicationService;
    }

    @Override
    @Transactional
    public List<ProductPackagingResponse> listByProduct(UUID productId) {
        ensureProduct(productId);
        return packagingRepository.findByProductId(new ProductId(productId)).stream()
                .map(this::ensurePackagedProduct)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<ProductPackagingResponse> listByProductIds(Collection<UUID> productIds) {
        return packagingRepository.findByProductIds(productIds).stream()
                .map(this::ensurePackagedProduct)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductPackagingResponse create(UUID productId, ProductPackagingCommand command) {
        Product product = ensureProduct(productId);
        if (product.isPackagedVariant()) {
            throw new InventoryDomainException(
                    "error.inventory.cannotDefinePackagingOnPackagedProduct",
                    null,
                    "Cannot define packaging on a packaged product");
        }
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
        packaging.setPurchasePrice(resolvePurchasePrice(product, command.getQty(), command.getPurchasePrice()));
        packaging.setListPrice(resolveListPrice(product, command.getQty(), command.getListPrice()));
        packaging.setBarcode(barcode);
        packaging.setSku(command.getSku());
        packaging.setActive(command.getActive() == null || command.getActive());
        packaging.setBase(false);
        Instant now = Instant.now();
        packaging.setCreatedAt(now);
        packaging.setUpdatedAt(now);
        packaging.validate();
        ProductPackaging saved = packagingRepository.save(packaging);
        return toResponse(ensurePackagedProduct(saved));
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
        packaging.setPurchasePrice(resolvePurchasePrice(product, packaging.getQty(), command.getPurchasePrice()));
        packaging.setListPrice(resolveListPrice(product, packaging.getQty(), command.getListPrice()));
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
        ProductPackaging saved = packagingRepository.save(packaging);
        ProductPackaging withProduct = ensurePackagedProduct(saved);
        syncPackagedProduct(withProduct);
        return toResponse(withProduct);
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
        if (packaging.getPackagedProductId() != null
                && productRepository.hasStockActivity(packaging.getPackagedProductId())) {
            throw new InventoryDomainException(
                    "error.inventory.cannotDeletePackagingWithStock",
                    null,
                    "Cannot delete packaging that still has packed stock");
        }
        packagingRepository.deleteById(packaging.getId());
        if (packaging.getPackagedProductId() != null) {
            productRepository.deleteById(packaging.getPackagedProductId());
        }
    }

    @Override
    @Transactional
    public Optional<ProductPackaging> findActiveByBarcode(CompanyId companyId, String barcode) {
        return packagingRepository.findActiveByCompanyIdAndBarcode(companyId, barcode)
                .map(this::ensurePackagedProduct);
    }

    @Override
    @Transactional
    public ProductPackagingResponse get(UUID packagingId) {
        ProductPackaging packaging = packagingRepository.findById(new ProductPackagingId(packagingId))
                .orElseThrow(() -> new InventoryDomainException(
                        "error.inventory.packagingNotFound",
                        new Object[]{packagingId},
                        "Packaging not found: " + packagingId));
        return toResponse(ensurePackagedProduct(packaging));
    }

    @Override
    @Transactional
    public ProductPackagingResponse pack(UUID productId, UUID packagingId, PackStockCommand command) {
        return toResponse(convertStock(productId, packagingId, command, true));
    }

    @Override
    @Transactional
    public ProductPackagingResponse unpack(UUID productId, UUID packagingId, PackStockCommand command) {
        return toResponse(convertStock(productId, packagingId, command, false));
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
        boolean parentMatch = packaging.getProductId().getId().equals(productId);
        boolean childMatch = packaging.getPackagedProductId() != null
                && packaging.getPackagedProductId().getId().equals(productId);
        if (!parentMatch && !childMatch) {
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
        UUID excludeProductId = null;
        if (excludePackagingId != null) {
            Optional<ProductPackaging> current = packagingRepository.findById(new ProductPackagingId(excludePackagingId));
            if (current.isPresent()) {
                ProductPackaging packaging = current.get();
                if (packaging.getPackagedProductId() != null) {
                    excludeProductId = packaging.getPackagedProductId().getId();
                }
                if (packaging.isBase()) {
                    Optional<Product> product = productRepository.findById(packaging.getProductId());
                    if (product.isPresent()
                            && barcode.equalsIgnoreCase(ProductPackaging.normalizeBarcode(product.get().getBarcode()))) {
                        excludeProductId = packaging.getProductId().getId();
                    }
                }
            }
        }
        if (productRepository.existsByCompanyIdAndBarcodeExcludingId(companyId, barcode, excludeProductId)) {
            throw new InventoryDomainException(
                    "error.inventory.barcodeDuplicate",
                    new Object[]{barcode},
                    "Barcode already in use: " + barcode);
        }
    }

    private Money resolvePurchasePrice(Product product, BigDecimal qty, BigDecimal requested) {
        if (requested != null) {
            return new Money(requested);
        }
        BigDecimal base = product.getStandardCost() != null ? product.getStandardCost().getAmount() : BigDecimal.ZERO;
        return new Money(base.multiply(qty).setScale(4, RoundingMode.HALF_UP));
    }

    private Money resolveListPrice(Product product, BigDecimal qty, BigDecimal requested) {
        if (requested != null) {
            return new Money(requested);
        }
        BigDecimal base = product.getListPrice() != null ? product.getListPrice().getAmount() : BigDecimal.ZERO;
        return new Money(base.multiply(qty).setScale(4, RoundingMode.HALF_UP));
    }

    private ProductPackaging convertStock(UUID productId, UUID packagingId, PackStockCommand command, boolean packing) {
        ProductPackaging packaging = ensurePackagedProduct(loadForProduct(productId, packagingId));
        Product parent = ensureProduct(packaging.getProductId().getId());
        if (packaging.isBase()) {
            throw new InventoryDomainException(
                    "error.inventory.cannotPackBasePackaging",
                    null,
                    "Cannot pack or unpack the base unit");
        }
        if (packaging.getPackagedProductId() == null) {
            throw new InventoryDomainException(
                    "error.inventory.packagedProductMissing",
                    null,
                    "Packaged product is missing for this packaging");
        }
        Product packed = productRepository.findById(packaging.getPackagedProductId())
                .orElseThrow(() -> new InventoryDomainException(
                        "error.inventory.packagedProductMissing",
                        null,
                        "Packaged product is missing for this packaging"));
        StockLocation location = resolveStockLocation(parent.getCompanyId(), command.getLocationId());
        BigDecimal packs = command.getQuantity();
        BigDecimal baseQty = ProductPackaging.toBaseQty(packs, packaging.getQty());
        Product consume = packing ? parent : packed;
        Product produce = packing ? packed : parent;
        BigDecimal consumeQty = packing ? baseQty : packs;
        BigDecimal produceQty = packing ? packs : baseQty;
        BigDecimal available = quantRepository.findByProductLocation(
                        parent.getCompanyId(), consume.getId(), location.getId())
                .map(q -> q.getQuantity())
                .orElse(BigDecimal.ZERO);
        if (available.compareTo(consumeQty) < 0) {
            throw new InventoryDomainException(
                    packing ? "error.inventory.insufficientBaseStockToPack" : "error.inventory.insufficientPackStockToUnpack",
                    new Object[]{consumeQty, available},
                    "Not enough stock to convert (need " + consumeQty + ", have " + available + ")");
        }
        BigDecimal consumeCost = consume.getStandardCost() != null ? consume.getStandardCost().getAmount() : BigDecimal.ZERO;
        BigDecimal produceCost = produceQty.signum() == 0
                ? BigDecimal.ZERO
                : consumeCost.multiply(consumeQty).divide(produceQty, 4, RoundingMode.HALF_UP);
        postConversionMove(parent.getCompanyId(), location, consume, consumeQty, consumeCost, false,
                packing ? "PACK-OUT" : "UNPACK-OUT");
        postConversionMove(parent.getCompanyId(), location, produce, produceQty, produceCost, true,
                packing ? "PACK-IN" : "UNPACK-IN");
        return packaging;
    }

    private void postConversionMove(CompanyId companyId,
                                    StockLocation location,
                                    Product product,
                                    BigDecimal qty,
                                    BigDecimal unitCost,
                                    boolean incoming,
                                    String reference) {
        StockLocation lossLocation = locationRepository.findByCompany(companyId, true).stream()
                .filter(l -> l.getLocationType() == LocationType.INVENTORY_LOSS)
                .findFirst()
                .orElseThrow(() -> new InventoryDomainException(
                        "error.inventory.noInventoryLossLocation",
                        null,
                        "No INVENTORY_LOSS location for company"));
        Warehouse warehouse = resolveWarehouse(companyId, location);
        CreateStockPickingCommand pickingCmd = new CreateStockPickingCommand();
        pickingCmd.setCompanyId(companyId.getId());
        pickingCmd.setWarehouseId(warehouse.getId().getId());
        pickingCmd.setPickingType(incoming ? PickingType.INCOMING : PickingType.OUTGOING);
        pickingCmd.setReference(reference);
        pickingCmd.setOrigin("PACKAGING");
        pickingCmd.setSourceLocationId(incoming ? lossLocation.getId().getId() : location.getId().getId());
        pickingCmd.setDestinationLocationId(incoming ? location.getId().getId() : lossLocation.getId().getId());
        StockMoveCommand move = new StockMoveCommand();
        move.setProductId(product.getId().getId());
        move.setUomId(product.getUomId().getId());
        move.setDemandQuantity(qty);
        move.setUnitCost(unitCost);
        pickingCmd.setMoves(List.of(move));
        var created = stockPickingApplicationService.createPicking(pickingCmd);
        stockPickingApplicationService.confirmPicking(created.getId());
        ValidatePickingCommand validate = new ValidatePickingCommand();
        validate.setCreateBackorder(false);
        stockPickingApplicationService.validatePicking(created.getId(), validate);
    }

    private Warehouse resolveWarehouse(CompanyId companyId, StockLocation location) {
        if (location.getWarehouseId() != null) {
            return warehouseRepository.findById(location.getWarehouseId())
                    .orElseThrow(() -> new InventoryDomainException(
                            "error.inventory.warehouseRequired",
                            null,
                            "A warehouse with a stock location is required"));
        }
        return warehouseRepository.findByCompany(companyId, false).stream()
                .filter(w -> w.getStockLocationId() != null)
                .findFirst()
                .orElseThrow(() -> new InventoryDomainException(
                        "error.inventory.warehouseRequired",
                        null,
                        "A warehouse with a stock location is required"));
    }

    private StockLocation resolveStockLocation(CompanyId companyId, UUID locationId) {
        if (locationId != null) {
            StockLocation location = locationRepository.findById(new StockLocationId(locationId))
                    .orElseThrow(() -> new InventoryDomainException("Location not found: " + locationId));
            if (!location.isInternal()) {
                throw new InventoryDomainException(
                        "error.inventory.adjustmentsOnlyInternalLocations",
                        null,
                        "Adjustments only allowed at INTERNAL locations");
            }
            return location;
        }
        return warehouseRepository.findByCompany(companyId, false).stream()
                .map(Warehouse::getStockLocationId)
                .filter(id -> id != null)
                .map(id -> locationRepository.findById(id).orElse(null))
                .filter(loc -> loc != null && loc.isInternal())
                .findFirst()
                .orElseThrow(() -> new InventoryDomainException(
                        "error.inventory.warehouseRequired",
                        null,
                        "A warehouse with a stock location is required"));
    }

    private ProductPackaging ensurePackagedProduct(ProductPackaging packaging) {
        if (packaging.isBase() || packaging.getPackagedProductId() != null) {
            return packaging;
        }
        Product parent = ensureProduct(packaging.getProductId().getId());
        String sku = uniquePackSku(parent);
        String childBarcode = packaging.getBarcode();
        if (childBarcode != null
                && productRepository.existsByCompanyIdAndBarcodeExcludingId(parent.getCompanyId(), childBarcode, null)) {
            childBarcode = null;
        }
        Product child = Product.builder()
                .id(new ProductId(UUID.randomUUID()))
                .companyId(parent.getCompanyId())
                .sku(sku)
                .name(parent.getName() + " – " + packaging.getName())
                .barcode(childBarcode)
                .description(parent.getDescription())
                .productType(parent.getProductType() == ProductType.SERVICE ? ProductType.STOCKABLE : parent.getProductType())
                .categoryId(parent.getCategoryId())
                .uomId(parent.getUomId())
                .purchaseUomId(parent.getPurchaseUomId())
                .standardCost(packaging.getPurchasePrice())
                .listPrice(packaging.getListPrice())
                .purchaseOk(parent.isPurchaseOk())
                .saleOk(parent.isSaleOk())
                .parentProductId(parent.getId())
                .sourcePackagingId(packaging.getId())
                .build();
        child.validate();
        Product saved = productRepository.save(child);
        packaging.setPackagedProductId(saved.getId());
        return packagingRepository.save(packaging);
    }

    private String uniquePackSku(Product parent) {
        String base = (parent.getSku() == null ? "P" : parent.getSku().trim()) + "-P";
        for (int i = 1; i < 1000; i++) {
            String sku = base + i;
            if (productRepository.findActiveByCompanyIdAndSku(parent.getCompanyId(), sku).isEmpty()) {
                return sku;
            }
        }
        return base + UUID.randomUUID().toString().substring(0, 8);
    }

    private void syncPackagedProduct(ProductPackaging packaging) {
        if (packaging.isBase() || packaging.getPackagedProductId() == null) {
            return;
        }
        Product parent = ensureProduct(packaging.getProductId().getId());
        Product child = productRepository.findByIdIncludingArchived(packaging.getPackagedProductId()).orElse(null);
        if (child == null) {
            return;
        }
        child.rename(parent.getName() + " – " + packaging.getName());
        child.changeBarcode(packaging.getBarcode());
        child.changeStandardCost(packaging.getPurchasePrice() != null ? packaging.getPurchasePrice() : Money.ZERO);
        child.changeListPrice(packaging.getListPrice() != null ? packaging.getListPrice() : Money.ZERO);
        if (!packaging.isActive() && child.isActive()) {
            child.archive("system");
        }
        productRepository.save(child);
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
        r.setPackagedProductId(p.getPackagedProductId() != null ? p.getPackagedProductId().getId() : null);
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
