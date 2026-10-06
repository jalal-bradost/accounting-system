package com.bradox.erp.purchase.service.domain;

import com.bradox.erp.accounting.service.domain.CurrencyMath;
import com.bradox.erp.accounting.service.domain.JournalEntryTiming;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.partnerstatement.PartnerStatementLineResponse;
import com.bradox.erp.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.bradox.erp.domain.settlement.OrderSettlement;
import com.bradox.erp.domain.settlement.OrderSettlementCalculator;
import com.bradox.erp.domain.settlement.SettlementDoc;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.DiscountMath;
import com.bradox.erp.domain.valueobject.DiscountType;
import com.bradox.erp.domain.valueobject.MonetaryScale;
import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.entity.ProductCategory;
import com.bradox.erp.inventory.domain.core.entity.ProductPackaging;
import com.bradox.erp.inventory.domain.core.entity.StockLocation;
import com.bradox.erp.inventory.domain.core.entity.Warehouse;
import com.bradox.erp.inventory.domain.core.valueobject.LocationType;
import com.bradox.erp.inventory.domain.core.valueobject.MoveState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingType;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductPackagingId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductType;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;
import com.bradox.erp.inventory.service.domain.dto.CreateStockPickingCommand;
import com.bradox.erp.inventory.service.domain.dto.ReturnPickingCommand;
import com.bradox.erp.inventory.service.domain.dto.StockMoveCommand;
import com.bradox.erp.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.erp.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.erp.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.StockMovePurchaseQueryPort;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductPackagingRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.WarehouseRepository;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.platform.document.DocumentSequenceService;
import com.bradox.erp.platform.settings.CompanyDocumentPolicyService;
import com.bradox.erp.platform.web.CompanyContext;
import com.bradox.erp.purchase.domain.core.*;
import com.bradox.erp.purchase.service.domain.FiscalTaxSnapshot;
import com.bradox.erp.purchase.service.domain.PurchaseTaxEngine;
import com.bradox.erp.purchase.service.domain.dto.*;
import com.bradox.erp.purchase.service.domain.event.VendorBillPostedEvent;
import com.bradox.erp.purchase.service.domain.event.VendorPaymentRegisteredEvent;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.bradox.erp.purchase.domain.core.entity.FiscalTax;
import com.bradox.erp.purchase.domain.core.entity.PurchaseOrder;
import com.bradox.erp.purchase.domain.core.entity.PurchaseOrderLine;
import com.bradox.erp.purchase.domain.core.entity.PurchaseOrderLineTax;
import com.bradox.erp.purchase.domain.core.entity.VendorBill;
import com.bradox.erp.purchase.domain.core.entity.VendorBillLine;
import com.bradox.erp.purchase.domain.core.entity.VendorBillLineTax;
import com.bradox.erp.purchase.domain.core.entity.VendorPayment;
import com.bradox.erp.purchase.service.domain.ports.output.repository.FiscalTaxRepository;
import com.bradox.erp.purchase.service.domain.ports.output.repository.PurchaseOrderRepository;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorBillRepository;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorPaymentRepository;
import com.bradox.erp.purchase.service.domain.ports.output.messaging.PurchaseEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class PurchaseApplicationServiceImpl implements PurchaseApplicationService {

    private static final String DEFAULT_AP_ACCOUNT_CODE = "430004";
    private static final String PURCHASE_DISCOUNT_ACCOUNT_CODE = "430007";
    private static final String PURCHASE_PRICE_VARIANCE_ACCOUNT_CODE = "430026";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final FiscalTaxRepository fiscalTaxRepository;
    private final VendorBillRepository vendorBillRepository;
    private final VendorPaymentRepository vendorPaymentRepository;
    private final VendorPaymentService vendorPaymentService;
    private final PartnerApplicationService partnerApplicationService;
    private final ProductRepository productRepository;
    private final ProductPackagingRepository productPackagingRepository;
    private final ProductCategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository stockLocationRepository;
    private final UomApplicationService uomApplicationService;
    private final StockPickingApplicationService stockPickingApplicationService;
    private final StockMovePurchaseQueryPort stockMovePurchaseQueryPort;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final AccountingReferenceLookupPort accountingReferenceLookupPort;
    private final com.bradox.erp.accounting.service.domain.PeriodPostingGuard periodPostingGuard;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final CurrencyConversionPort currencyConversionPort;
    private final PurchaseOrderQtyWriter purchaseOrderQtyWriter;
    private final DocumentSequenceService documentSequenceService;
    private final RecordActivityLogger activityLogger;
    private final CompanyDocumentPolicyService companyDocumentPolicyService;

    public PurchaseApplicationServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                          FiscalTaxRepository fiscalTaxRepository,
                                          VendorBillRepository vendorBillRepository,
                                          VendorPaymentRepository vendorPaymentRepository,
                                          VendorPaymentService vendorPaymentService,
                                          PartnerApplicationService partnerApplicationService,
                                          ProductRepository productRepository,
                                          ProductPackagingRepository productPackagingRepository,
                                          ProductCategoryRepository categoryRepository,
                                          WarehouseRepository warehouseRepository,
                                          StockLocationRepository stockLocationRepository,
                                          UomApplicationService uomApplicationService,
                                          StockPickingApplicationService stockPickingApplicationService,
                                          StockMovePurchaseQueryPort stockMovePurchaseQueryPort,
                                          JournalEntryApplicationService journalEntryApplicationService,
                                          AccountingReferenceLookupPort accountingReferenceLookupPort,
                                          com.bradox.erp.accounting.service.domain.PeriodPostingGuard periodPostingGuard,
                                          ObjectProvider<CompanyContext> companyContextProvider,
                                          PurchaseEventPublisher purchaseEventPublisher,
                                          CurrencyConversionPort currencyConversionPort,
                                          PurchaseOrderQtyWriter purchaseOrderQtyWriter,
                                          DocumentSequenceService documentSequenceService,
                                          RecordActivityLogger activityLogger,
                                          CompanyDocumentPolicyService companyDocumentPolicyService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.fiscalTaxRepository = fiscalTaxRepository;
        this.vendorBillRepository = vendorBillRepository;
        this.vendorPaymentRepository = vendorPaymentRepository;
        this.vendorPaymentService = vendorPaymentService;
        this.partnerApplicationService = partnerApplicationService;
        this.productRepository = productRepository;
        this.productPackagingRepository = productPackagingRepository;
        this.categoryRepository = categoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.uomApplicationService = uomApplicationService;
        this.stockPickingApplicationService = stockPickingApplicationService;
        this.stockMovePurchaseQueryPort = stockMovePurchaseQueryPort;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.accountingReferenceLookupPort = accountingReferenceLookupPort;
        this.periodPostingGuard = periodPostingGuard;
        this.companyContextProvider = companyContextProvider;
        this.purchaseEventPublisher = purchaseEventPublisher;
        this.currencyConversionPort = currencyConversionPort;
        this.purchaseOrderQtyWriter = purchaseOrderQtyWriter;
        this.documentSequenceService = documentSequenceService;
        this.activityLogger = activityLogger;
        this.companyDocumentPolicyService = companyDocumentPolicyService;
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) return fromCommand;
        return companyContextProvider.getObject().requireCompany().getId();
    }

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PartnerResponse vendor = partnerApplicationService.getPartner(command.getVendorPartnerId());
        if (!vendor.isVendor()) {
            throw new PurchaseDomainException(
                    "error.purchase.partnerNotVendor", null, "Partner is not a vendor");
        }
        if (!vendor.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException(
                    "error.purchase.vendorCompanyMismatch", null, "Vendor belongs to another company");
        }
        Instant now = Instant.now();
        PurchaseOrder o = new PurchaseOrder();
        o.setId(UUID.randomUUID());
        o.setCompanyId(companyId);
        o.setVendorPartnerId(command.getVendorPartnerId());
        o.setName(command.getName() != null && !command.getName().isBlank()
                ? command.getName()
                : documentSequenceService.next(companyId, "PO"));
        if (purchaseOrderRepository.findByCompanyIdAndName(companyId, o.getName()).isPresent()) {
            throw new PurchaseDomainException("error.purchase.orderNameExists", new Object[] { o.getName() }, "Purchase order name already exists: " + o.getName());
        }
        o.setState(PurchaseOrderState.DRAFT);
        o.setCurrencyCode(command.getCurrencyCode());
        o.setWarehouseId(command.getWarehouseId());
        o.setDestLocationId(command.getDestLocationId());
        o.setPaymentTermsId(command.getPaymentTermsId());
        o.setOrderDate(command.getOrderDate() != null ? command.getOrderDate() : LocalDate.now());
        o.setExpectedDate(command.getExpectedDate());
        o.setIncoterm(command.getIncoterm());
        o.setNotes(command.getNotes());
        o.setVendorReference(command.getVendorReference());
        applyOrderDiscountInput(o, command);
        o.setExchangeRateToCompany(resolveExchangeRate(
                companyId, command.getCurrencyCode(), o.getOrderDate(), command.getExchangeRateToCompany()));
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        int seq = 10;
        for (PurchaseOrderLineCommand lc : command.getLines()) {
            Product product = productRepository.findById(new ProductId(lc.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + lc.getProductId()));
            if (!product.isPurchaseOk()) {
                throw new PurchaseDomainException(
                        "error.purchase.productNotPurchasable",
                        new Object[] { lc.getProductId() },
                        "Product is not purchasable: " + lc.getProductId());
            }
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setId(UUID.randomUUID());
            line.setSequence(seq);
            line.setProductId(lc.getProductId());
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            line.setWarehouseId(lc.getWarehouseId());
            applyPackagingSnapshot(line, lc.getPackagingId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setQtyReceived(BigDecimal.ZERO);
            line.setQtyInvoiced(BigDecimal.ZERO);
            line.setUnitPrice(lc.getUnitPrice());
            applyLineDiscountInput(line, lc);
            line.setExpectedDate(lc.getExpectedDate());
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTax tax = fiscalTaxRepository.findById(taxId)
                        .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new PurchaseDomainException("error.purchase.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.PURCHASE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new PurchaseDomainException("error.purchase.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for purchase: " + taxId);
                }
                PurchaseOrderLineTax lt = new PurchaseOrderLineTax();
                lt.setId(UUID.randomUUID());
                lt.setTaxId(taxId);
                lt.setSequence(tseq);
                line.getTaxes().add(lt);
                tseq += 10;
            }
            o.getLines().add(line);
            seq += 10;
        }
        recalcTotals(o);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "Purchase Order created");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse updatePurchaseOrder(UUID id, CreatePurchaseOrderCommand command) {
        PurchaseOrder o = loadOrder(id);
        if (command.getRowVersion() != null && command.getRowVersion() != o.getRowVersion()) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Purchase order " + id + " was changed by someone else (version " + o.getRowVersion()
                            + ", client had " + command.getRowVersion() + ")");
        }
        PurchaseOrderRules.ensureCanUpdate(o.getState());
        if (o.isLocked()) {
            throw new PurchaseDomainException(
                    "error.purchase.orderLocked", null, "Purchase order is locked; unlock before amending");
        }
        if (o.getState() == PurchaseOrderState.CONFIRMED) {
            return amendConfirmedPurchaseOrder(o, command);
        }
        return replaceDraftOrSentPurchaseOrder(o, command);
    }

    private PurchaseOrderResponse replaceDraftOrSentPurchaseOrder(PurchaseOrder o, CreatePurchaseOrderCommand command) {
        UUID companyId = o.getCompanyId();
        PartnerResponse vendor = partnerApplicationService.getPartner(command.getVendorPartnerId());
        if (!vendor.isVendor()) {
            throw new PurchaseDomainException(
                    "error.purchase.partnerNotVendor", null, "Partner is not a vendor");
        }
        if (!vendor.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException(
                    "error.purchase.vendorCompanyMismatch", null, "Vendor belongs to another company");
        }
        Instant now = Instant.now();
        o.setVendorPartnerId(command.getVendorPartnerId());
        o.setCurrencyCode(command.getCurrencyCode());
        o.setWarehouseId(command.getWarehouseId());
        o.setDestLocationId(command.getDestLocationId());
        o.setPaymentTermsId(command.getPaymentTermsId());
        o.setOrderDate(command.getOrderDate() != null ? command.getOrderDate() : o.getOrderDate());
        o.setExpectedDate(command.getExpectedDate());
        o.setIncoterm(command.getIncoterm());
        o.setNotes(command.getNotes());
        o.setVendorReference(command.getVendorReference());
        applyOrderDiscountInput(o, command);
        o.setExchangeRateToCompany(resolveExchangeRate(
                companyId, command.getCurrencyCode(), o.getOrderDate(), command.getExchangeRateToCompany()));
        o.setUpdatedAt(now);
        o.getLines().clear();

        int seq = 10;
        for (PurchaseOrderLineCommand lc : command.getLines()) {
            Product product = productRepository.findById(new ProductId(lc.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + lc.getProductId()));
            if (!product.isPurchaseOk()) {
                throw new PurchaseDomainException(
                        "error.purchase.productNotPurchasable",
                        new Object[] { lc.getProductId() },
                        "Product is not purchasable: " + lc.getProductId());
            }
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setId(UUID.randomUUID());
            line.setSequence(seq);
            line.setProductId(lc.getProductId());
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            line.setWarehouseId(lc.getWarehouseId());
            applyPackagingSnapshot(line, lc.getPackagingId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setQtyReceived(BigDecimal.ZERO);
            line.setQtyInvoiced(BigDecimal.ZERO);
            line.setUnitPrice(lc.getUnitPrice());
            applyLineDiscountInput(line, lc);
            line.setExpectedDate(lc.getExpectedDate());
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTax tax = fiscalTaxRepository.findById(taxId)
                        .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new PurchaseDomainException("error.purchase.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.PURCHASE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new PurchaseDomainException("error.purchase.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for purchase: " + taxId);
                }
                PurchaseOrderLineTax lt = new PurchaseOrderLineTax();
                lt.setId(UUID.randomUUID());
                lt.setTaxId(taxId);
                lt.setSequence(tseq);
                line.getTaxes().add(lt);
                tseq += 10;
            }
            o.getLines().add(line);
            seq += 10;
        }
        recalcTotals(o);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        // Re-read so the response carries the version after this save (the client sends it back).
        return toResponse(loadOrder(saved.getId()));
    }

    private PurchaseOrderResponse amendConfirmedPurchaseOrder(PurchaseOrder o, CreatePurchaseOrderCommand command) {
        Map<UUID, BigDecimal> qtyOrderedBefore = o.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, PurchaseOrderLine::getQtyOrdered, (a, b) -> a, LinkedHashMap::new));
        BigDecimal untaxedBefore = o.getAmountUntaxed() != null ? o.getAmountUntaxed() : BigDecimal.ZERO;
        UUID companyId = o.getCompanyId();
        Map<UUID, String> lineTermsBefore = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            lineTermsBefore.put(line.getId(), lineTermsSignature(line));
        }
        boolean anyReceived = o.getLines().stream().anyMatch(l -> nz(l.getQtyReceived()).signum() > 0);
        boolean anyBilled = o.getLines().stream().anyMatch(l -> nz(l.getQtyInvoiced()).signum() > 0);
        boolean hasPostedDocs = anyReceived || vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                .anyMatch(b -> b.getState() == VendorBillState.POSTED);
        if (anyBilled && orderDiscountChanged(o, command)) {
            throw new PurchaseDomainException(
                    "error.purchase.orderDiscountLockedAfterBilling", null,
                    "The order discount cannot change after billing; credit the bill first");
        }
        if (hasPostedDocs) {
            if (command.getVendorPartnerId() != null && !command.getVendorPartnerId().equals(o.getVendorPartnerId())) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotChangeVendorAfterDocuments", null,
                        "Cannot change vendor after goods were received or bills posted");
            }
            if (command.getCurrencyCode() != null && !command.getCurrencyCode().equalsIgnoreCase(o.getCurrencyCode())) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotChangeCurrencyAfterDocuments", null,
                        "Cannot change currency after goods were received or bills posted");
            }
            if (command.getWarehouseId() != null && o.getWarehouseId() != null
                    && !command.getWarehouseId().equals(o.getWarehouseId())) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotChangeWarehouseAfterDocuments", null,
                        "Cannot change warehouse after goods were received or bills posted");
            }
        } else {
            PartnerResponse vendor = partnerApplicationService.getPartner(command.getVendorPartnerId());
            if (!vendor.isVendor()) {
                throw new PurchaseDomainException(
                        "error.purchase.partnerNotVendor", null, "Partner is not a vendor");
            }
            if (!vendor.getCompanyId().equals(companyId)) {
                throw new PurchaseDomainException(
                        "error.purchase.vendorCompanyMismatch", null, "Vendor belongs to another company");
            }
            o.setVendorPartnerId(command.getVendorPartnerId());
            boolean currencyChanged = command.getCurrencyCode() != null
                    && !command.getCurrencyCode().equalsIgnoreCase(o.getCurrencyCode());
            o.setCurrencyCode(command.getCurrencyCode());
            o.setWarehouseId(command.getWarehouseId());
            o.setDestLocationId(command.getDestLocationId());
            if (currencyChanged && command.getExchangeRateToCompany() == null) {
                // Nothing was received or billed yet: the rate follows the new currency, not the old one.
                o.setExchangeRateToCompany(resolveExchangeRate(
                        companyId, o.getCurrencyCode(), command.getOrderDate() != null ? command.getOrderDate() : o.getOrderDate(), null));
            }
        }

        Instant now = Instant.now();
        o.setPaymentTermsId(command.getPaymentTermsId());
        o.setOrderDate(command.getOrderDate() != null ? command.getOrderDate() : o.getOrderDate());
        o.setExpectedDate(command.getExpectedDate());
        o.setIncoterm(command.getIncoterm());
        o.setNotes(command.getNotes());
        o.setVendorReference(command.getVendorReference());
        applyOrderDiscountInput(o, command);
        if (command.getExchangeRateToCompany() != null) {
            o.setExchangeRateToCompany(resolveExchangeRate(
                    companyId, o.getCurrencyCode(), o.getOrderDate(), command.getExchangeRateToCompany()));
        }
        o.setUpdatedAt(now);

        Map<UUID, PurchaseOrderLine> existingById = o.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, l -> l, (a, b) -> a, LinkedHashMap::new));
        Set<UUID> keptIds = new LinkedHashSet<>();
        List<PurchaseOrderLine> nextLines = new ArrayList<>();
        int seq = 10;
        for (PurchaseOrderLineCommand lc : command.getLines()) {
            Product product = productRepository.findById(new ProductId(lc.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + lc.getProductId()));
            if (!product.isPurchaseOk()) {
                throw new PurchaseDomainException(
                        "error.purchase.productNotPurchasable",
                        new Object[] { lc.getProductId() },
                        "Product is not purchasable: " + lc.getProductId());
            }
            PurchaseOrderLine line;
            if (lc.getId() != null && existingById.containsKey(lc.getId())) {
                line = existingById.get(lc.getId());
                if (!line.getProductId().equals(lc.getProductId())) {
                    throw new PurchaseDomainException(
                            "error.purchase.cannotChangeProductOnConfirmedLine", null,
                            "Cannot change product on a confirmed order line");
                }
                assertLineAmendmentAllowed(line, lc);
                keptIds.add(line.getId());
            } else {
                line = new PurchaseOrderLine();
                line.setId(UUID.randomUUID());
                line.setQtyReceived(BigDecimal.ZERO);
                line.setQtyInvoiced(BigDecimal.ZERO);
                line.setCreatedAt(now);
                line.setProductId(lc.getProductId());
            }
            line.setSequence(seq);
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            line.setWarehouseId(lc.getWarehouseId());
            applyPackagingSnapshot(line, lc.getPackagingId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setUnitPrice(lc.getUnitPrice());
            applyLineDiscountInput(line, lc);
            line.setExpectedDate(lc.getExpectedDate());
            line.setUpdatedAt(now);
            line.getTaxes().clear();
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTax tax = fiscalTaxRepository.findById(taxId)
                        .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new PurchaseDomainException("error.purchase.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.PURCHASE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new PurchaseDomainException("error.purchase.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for purchase: " + taxId);
                }
                PurchaseOrderLineTax lt = new PurchaseOrderLineTax();
                lt.setId(UUID.randomUUID());
                lt.setTaxId(taxId);
                lt.setSequence(tseq);
                line.getTaxes().add(lt);
                tseq += 10;
            }
            nextLines.add(line);
            seq += 10;
        }
        for (PurchaseOrderLine existing : new ArrayList<>(o.getLines())) {
            if (keptIds.contains(existing.getId())) {
                continue;
            }
            if (existing.getQtyReceived().signum() > 0 || existing.getQtyInvoiced().signum() > 0) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotRemoveReceivedOrInvoicedLine", null,
                        "Cannot remove a line that has been received or invoiced");
            }
            o.getLines().remove(existing);
        }
        for (PurchaseOrderLine line : nextLines) {
            if (!o.getLines().contains(line)) {
                o.getLines().add(line);
            }
        }
        // Keep sequence order
        o.getLines().sort(Comparator.comparingInt(PurchaseOrderLine::getSequence));
        Map<UUID, String> lineTermsAfter = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            lineTermsAfter.put(line.getId(), lineTermsSignature(line));
        }
        if (!lineTermsAfter.equals(lineTermsBefore) && hasDraftBillDocuments(o.getId())) {
            throw new PurchaseDomainException(
                    "error.purchase.draftBillBlocksAmendment", null,
                    "A draft bill or credit note exists for this order; post or cancel it before changing lines");
        }
        recalcTotals(o);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        postPurchaseAmendmentTracking(saved, qtyOrderedBefore, untaxedBefore);
        return syncDocumentsAfterAmendment(saved);
    }

    /**
     * After a confirmed-order amendment: make the open receipts match what is still to be received
     * (ordered - received) at the current price. Quantities below received are refused before this
     * point, so no return is ever created here. Receipt validation and bills stay manual actions.
     */
    private PurchaseOrderResponse syncDocumentsAfterAmendment(PurchaseOrder saved) {
        if (saved.getWarehouseId() == null) {
            return toResponse(saved);
        }
        purchaseOrderQtyWriter.updateQtyReceivedJoiningCurrentTransaction(saved.getId());
        PurchaseOrder o = loadOrder(saved.getId());
        syncOpenReceipts(o, false);
        return getPurchaseOrder(o.getId());
    }

    /**
     * Open (not done, not cancelled) incoming pickings must carry exactly the remaining demand per
     * line at the current cost. When they already do nothing changes; otherwise they are cancelled
     * and replaced by one receipt, so an amendment never leaves two receipts for the same goods.
     *
     * @param trimOnly only remove open demand beyond what is left to receive (after a receipt was
     *                 validated some other way); never add demand that is not already open.
     */
    private void syncOpenReceipts(PurchaseOrder o, boolean trimOnly) {
        Map<UUID, StockMoveCommand> wanted = new LinkedHashMap<>();
        for (StockMoveCommand mc : buildRemainingIncomingMoves(o)) {
            wanted.put(mc.getPurchaseOrderLineId(), mc);
        }
        List<UUID> openPickings = new ArrayList<>();
        Map<UUID, BigDecimal> openQty = new LinkedHashMap<>();
        Map<UUID, BigDecimal> openCost = new LinkedHashMap<>();
        for (UUID pickingId : stockMovePurchaseQueryPort.findPickingIdsByPurchaseOrderId(o.getId())) {
            StockPickingResponse p = stockPickingApplicationService.getPicking(pickingId);
            if (p.getState() == PickingState.DONE || p.getState() == PickingState.CANCELLED) {
                continue;
            }
            openPickings.add(pickingId);
            for (StockPickingResponse.MoveResponse m : p.getMoves()) {
                if (m.getPurchaseOrderLineId() == null || m.getState() == MoveState.DONE
                        || m.getState() == MoveState.CANCELLED) {
                    continue;
                }
                openQty.merge(m.getPurchaseOrderLineId(), nz(m.getDemandQuantity()), BigDecimal::add);
                openCost.put(m.getPurchaseOrderLineId(), nz(m.getUnitCost()));
            }
        }
        Map<UUID, BigDecimal> cap = new LinkedHashMap<>();
        if (trimOnly) {
            boolean excess = false;
            for (Map.Entry<UUID, BigDecimal> e : openQty.entrySet()) {
                StockMoveCommand w = wanted.get(e.getKey());
                BigDecimal want = w != null ? nz(w.getDemandQuantity()) : BigDecimal.ZERO;
                if (e.getValue().compareTo(want) > 0) {
                    excess = true;
                }
                cap.put(e.getKey(), e.getValue().min(want));
            }
            if (!excess) {
                return;
            }
        } else {
            boolean same = wanted.keySet().equals(openQty.keySet());
            for (Map.Entry<UUID, StockMoveCommand> e : wanted.entrySet()) {
                if (!same) {
                    break;
                }
                BigDecimal q = nz(openQty.get(e.getKey()));
                BigDecimal cost = nz(openCost.get(e.getKey()));
                same = q.compareTo(nz(e.getValue().getDemandQuantity())) == 0
                        && cost.setScale(4, RoundingMode.HALF_UP).compareTo(
                                nz(e.getValue().getUnitCost()).setScale(4, RoundingMode.HALF_UP)) == 0;
            }
            if (same) {
                return;
            }
        }
        for (UUID pickingId : openPickings) {
            stockPickingApplicationService.cancelPicking(pickingId);
        }
        createReceiptPicking(o, trimOnly ? cap : null);
    }

    /** Cancels every receipt and return of the order that is not done or already cancelled. */
    private void cancelOpenPickings(UUID purchaseOrderId) {
        for (UUID pickingId : stockMovePurchaseQueryPort.findNonTerminalPickingIdsByPurchaseOrderId(purchaseOrderId)) {
            stockPickingApplicationService.cancelPicking(pickingId);
        }
    }

    private void createReceiptPickingForRemaining(PurchaseOrder o) {
        createReceiptPicking(o, null);
    }

    /** One receipt for the remaining quantities, or for {@code stockQtyByLine} (stock units) when given. */
    private void createReceiptPicking(PurchaseOrder o, Map<UUID, BigDecimal> stockQtyByLine) {
        List<StockMoveCommand> moves = new ArrayList<>();
        for (StockMoveCommand mc : buildRemainingIncomingMoves(o)) {
            BigDecimal qty = stockQtyByLine == null ? mc.getDemandQuantity()
                    : nz(stockQtyByLine.get(mc.getPurchaseOrderLineId())).min(mc.getDemandQuantity());
            if (qty.signum() > 0) {
                mc.setDemandQuantity(qty);
                moves.add(mc);
            }
        }
        if (moves.isEmpty()) {
            return;
        }
        UUID warehouseId = o.getWarehouseId();
        Warehouse wh = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new PurchaseDomainException("Warehouse not found: " + warehouseId));
        StockLocation supplier = findSupplierVirtual(o.getCompanyId());
        UUID destLoc = o.getDestLocationId() != null
                ? o.getDestLocationId()
                : wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (destLoc == null) {
            throw new PurchaseDomainException("error.purchase.destinationStockLocationUnresolved", null, "Destination stock location could not be resolved");
        }
        CreateStockPickingCommand cmd = new CreateStockPickingCommand();
        cmd.setCompanyId(o.getCompanyId());
        cmd.setWarehouseId(warehouseId);
        cmd.setPickingType(PickingType.INCOMING);
        cmd.setSourceLocationId(supplier.getId().getId());
        cmd.setDestinationLocationId(destLoc);
        cmd.setPartnerId(o.getVendorPartnerId());
        cmd.setOrigin(o.getName());
        cmd.setReference(o.getName());
        cmd.setPurchaseOrderId(o.getId());
        if (o.getOrderDate() != null) {
            cmd.setScheduledAt(o.getOrderDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        }
        cmd.setMoves(moves);
        stockPickingApplicationService.createPicking(cmd);
    }

    private List<StockMoveCommand> buildRemainingIncomingMoves(PurchaseOrder o) {
        BigDecimal rateToCompany = resolveExchangeRate(
                o.getCompanyId(), o.getCurrencyCode(), o.getOrderDate(), o.getExchangeRateToCompany());
        List<StockMoveCommand> moves = new ArrayList<>();
        for (PurchaseOrderLine line : o.getLines()) {
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + line.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                continue;
            }
            BigDecimal remaining = line.getQtyOrdered().subtract(line.getQtyReceived());
            if (remaining.signum() <= 0) {
                continue;
            }
            moves.add(buildPurchaseStockMove(line, product, remaining, rateToCompany));
        }
        return moves;
    }

    private StockMoveCommand buildPurchaseStockMove(PurchaseOrderLine line,
                                                    Product product,
                                                    BigDecimal qtyInOrderUom,
                                                    BigDecimal rateToCompany) {
        UUID stockUom = product.getUomId().getId();
        BigDecimal demandStockUom;
        BigDecimal oneInStockUom;
        if (line.getQtyPerPackage() != null && line.getQtyPerPackage().signum() > 0) {
            demandStockUom = ProductPackaging.toBaseQty(qtyInOrderUom, line.getQtyPerPackage());
            oneInStockUom = line.getQtyPerPackage();
        } else {
            demandStockUom = uomApplicationService.convert(line.getUomId(), stockUom, qtyInOrderUom);
            oneInStockUom = uomApplicationService.convert(line.getUomId(), stockUom, BigDecimal.ONE);
        }
        BigDecimal lineNetTotal = PurchaseOrderRules.lineNet(
                line.getQtyOrdered(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue());
        BigDecimal lineNetOne = line.getQtyOrdered().signum() > 0
                ? lineNetTotal.divide(line.getQtyOrdered(), 8, RoundingMode.HALF_UP)
                : lineNetTotal;
        BigDecimal unitCostDoc = oneInStockUom.signum() > 0
                ? lineNetOne.divide(oneInStockUom, 8, RoundingMode.HALF_UP)
                : lineNetOne;
        BigDecimal unitCost = CurrencyMath.convertAtRate(unitCostDoc, rateToCompany);

        StockMoveCommand mc = new StockMoveCommand();
        mc.setProductId(line.getProductId());
        mc.setUomId(stockUom);
        mc.setDemandQuantity(demandStockUom);
        mc.setUnitCost(unitCost);
        mc.setPurchaseOrderLineId(line.getId());
        return mc;
    }

    /**
     * After confirm: draft receipt pickings already exist. Receipt validation and
     * vendor bill creation are separate user actions.
     */
    private PurchaseOrderResponse finalizeAfterConfirm(PurchaseOrder saved) {
        return getPurchaseOrder(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderSummaryResponse> searchPurchaseOrders(UUID companyId,
                                                                   PurchaseOrderState state,
                                                                   UUID vendorPartnerId,
                                                                   String q,
                                                                   Pageable pageable) {
        UUID cid = companyIdOrDefault(companyId);
        String qNorm = q != null && !q.isBlank() ? q.trim() : "";
        var page = purchaseOrderRepository.search(cid, state, vendorPartnerId, qNorm, pageable);
        List<UUID> orderIds = page.getContent().stream().map(PurchaseOrder::getId).toList();
        List<VendorBill> allBills = vendorBillRepository.findByPurchaseOrderIdIn(orderIds);
        Map<UUID, List<VendorBill>> billsByOrder = new LinkedHashMap<>();
        for (VendorBill b : allBills) {
            if (b.getPurchaseOrderId() != null) {
                billsByOrder.computeIfAbsent(b.getPurchaseOrderId(), k -> new ArrayList<>()).add(b);
            }
        }
        List<UUID> billIds = allBills.stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .map(VendorBill::getId)
                .toList();
        Map<UUID, BigDecimal> settledByBill = vendorPaymentService.sumActiveAllocationsByBillIds(billIds);
        return page.map(o -> toSummary(o, billsByOrder.getOrDefault(o.getId(), List.of()), settledByBill));
    }

    private PurchaseOrderSummaryResponse toSummary(PurchaseOrder o) {
        return toSummary(o, null, null);
    }

    private PurchaseOrderSummaryResponse toSummary(
            PurchaseOrder o, List<VendorBill> bills, Map<UUID, BigDecimal> settledByBill) {
        PurchaseOrderSummaryResponse r = new PurchaseOrderSummaryResponse();
        r.setId(o.getId());
        r.setCompanyId(o.getCompanyId());
        r.setVendorPartnerId(o.getVendorPartnerId());
        r.setName(o.getName());
        r.setState(o.getState());
        r.setCurrencyCode(o.getCurrencyCode());
        r.setOrderDate(o.getOrderDate());
        r.setCreatedAt(o.getCreatedAt());
        if (bills != null && settledByBill != null) {
            applyPurchasePaymentStatus(r, o, bills, settledByBill);
        } else {
            applyPurchasePaymentStatus(r, o);
        }
        return r;
    }

    /**
     * List statuses: New / Unpaid / Partial Paid / Paid / To refund / Cancelled (+ amounts).
     * Total is net billed once posted bills/credit notes exist; otherwise the order total.
     * Paid is net of refunds; amountDue (balance) may be negative when a refund is owed.
     */
    private record OrderPaymentFields(
            String paymentStatus, BigDecimal amountTotal, BigDecimal amountPaid, BigDecimal amountDue) {}

    private void applyPurchasePaymentStatus(PurchaseOrderSummaryResponse r, PurchaseOrder o) {
        List<VendorBill> bills = vendorBillRepository.findByPurchaseOrderId(o.getId());
        List<UUID> billIds = bills.stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .map(VendorBill::getId)
                .toList();
        Map<UUID, BigDecimal> settledByBill = vendorPaymentService.sumActiveAllocationsByBillIds(billIds);
        applyPurchasePaymentStatus(r, o, bills, settledByBill);
    }

    private void applyPurchasePaymentStatus(
            PurchaseOrderSummaryResponse r,
            PurchaseOrder o,
            List<VendorBill> bills,
            Map<UUID, BigDecimal> settledByBill) {
        OrderPaymentFields f = computePurchasePaymentFields(o, bills, settledByBill);
        r.setPaymentStatus(f.paymentStatus());
        r.setAmountTotal(f.amountTotal());
        r.setAmountPaid(f.amountPaid());
        r.setAmountDue(f.amountDue());
    }

    private OrderPaymentFields computePurchasePaymentFields(PurchaseOrder o) {
        List<VendorBill> bills = vendorBillRepository.findByPurchaseOrderId(o.getId());
        List<UUID> billIds = bills.stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .map(VendorBill::getId)
                .toList();
        Map<UUID, BigDecimal> settledByBill = vendorPaymentService.sumActiveAllocationsByBillIds(billIds);
        return computePurchasePaymentFields(o, bills, settledByBill);
    }

    private OrderPaymentFields computePurchasePaymentFields(
            PurchaseOrder o, List<VendorBill> bills, Map<UUID, BigDecimal> settledByBill) {
        boolean cancelled = o.getState() == PurchaseOrderState.CANCELLED;
        List<SettlementDoc> docs = new ArrayList<>();
        if (!cancelled) {
            for (VendorBill bill : bills) {
                if (bill.getState() != VendorBillState.POSTED) {
                    continue;
                }
                VendorBillMoveType type = bill.getMoveType() != null ? bill.getMoveType() : VendorBillMoveType.BILL;
                if (type == VendorBillMoveType.DEBIT_NOTE) {
                    continue;
                }
                bill.getLines().size();
                for (VendorBillLine line : bill.getLines()) {
                    line.getTaxSnapshots().size();
                }
                BigDecimal total = billTotalDocumentCurrency(bill);
                BigDecimal settled = settledByBill.getOrDefault(bill.getId(), BigDecimal.ZERO)
                        .setScale(4, RoundingMode.HALF_UP);
                SettlementDoc.Kind kind = type == VendorBillMoveType.CREDIT_NOTE
                        ? SettlementDoc.Kind.CREDIT_NOTE
                        : SettlementDoc.Kind.INVOICE;
                docs.add(new SettlementDoc(kind, total, settled, bill.getDueDate()));
            }
        }
        OrderSettlement s = OrderSettlementCalculator.compute(
                cancelled,
                o.getAmountTotal(),
                docs,
                LocalDate.now());
        return new OrderPaymentFields(
                s.status().name(),
                s.total(),
                s.paidNet(),
                s.balance());
    }

    private boolean computeCanCreateVendorBill(PurchaseOrder po) {
        if (po.getState() != PurchaseOrderState.CONFIRMED) {
            return false;
        }
        boolean allowWithoutReceipt = companyDocumentPolicyService.allowBillWithoutReceipt(po.getCompanyId());
        Map<UUID, BigDecimal> draftAllocated = draftBillQtyByPoLine(po.getId());
        for (PurchaseOrderLine pol : po.getLines()) {
            Optional<Product> opt = productRepository.findById(new ProductId(pol.getProductId()));
            if (opt.isEmpty()) {
                continue;
            }
            if (billableQtyForLine(pol, opt.get(), draftAllocated, allowWithoutReceipt).signum() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean computeCanCreateReturn(PurchaseOrder po) {
        if (po.getState() != PurchaseOrderState.CONFIRMED) {
            return false;
        }
        return po.getLines().stream().anyMatch(pol -> {
            Optional<Product> opt = productRepository.findById(new ProductId(pol.getProductId()));
            if (opt.isEmpty() || opt.get().getProductType() == ProductType.SERVICE) {
                return false;
            }
            return pol.getQtyReceived().signum() > 0;
        }) && stockMovePurchaseQueryPort.findReturnableReceiptPickingId(po.getId()).isPresent();
    }

    /** Quantities already reserved on draft vendor bills (not yet posted to qtyInvoiced). */
    private Map<UUID, BigDecimal> draftBillQtyByPoLine(UUID purchaseOrderId) {
        Map<UUID, BigDecimal> allocated = new HashMap<>();
        for (VendorBill bill : vendorBillRepository.findByPurchaseOrderId(purchaseOrderId)) {
            if (bill.getState() != VendorBillState.DRAFT) {
                continue;
            }
            VendorBillMoveType type = bill.getMoveType() != null ? bill.getMoveType() : VendorBillMoveType.BILL;
            // Debit notes are amount adjustments and must not reserve billable qty.
            if (type != VendorBillMoveType.BILL) {
                continue;
            }
            bill.getLines().size();
            for (VendorBillLine line : bill.getLines()) {
                if (line.getPurchaseOrderLineId() != null) {
                    allocated.merge(line.getPurchaseOrderLineId(), line.getQty(), BigDecimal::add);
                }
            }
        }
        return allocated;
    }

    private Map<UUID, BigDecimal> draftCreditNoteQtyByPoLine(UUID purchaseOrderId) {
        return draftBillQtyByPoLine(purchaseOrderId, VendorBillMoveType.CREDIT_NOTE);
    }

    private Map<UUID, BigDecimal> draftBillQtyByPoLine(UUID purchaseOrderId, VendorBillMoveType moveType) {
        Map<UUID, BigDecimal> allocated = new HashMap<>();
        for (VendorBill bill : vendorBillRepository.findByPurchaseOrderId(purchaseOrderId)) {
            if (bill.getState() != VendorBillState.DRAFT) {
                continue;
            }
            VendorBillMoveType type = bill.getMoveType() != null ? bill.getMoveType() : VendorBillMoveType.BILL;
            if (type != moveType) {
                continue;
            }
            bill.getLines().size();
            for (VendorBillLine line : bill.getLines()) {
                if (line.getPurchaseOrderLineId() != null) {
                    allocated.merge(line.getPurchaseOrderLineId(), line.getQty(), BigDecimal::add);
                }
            }
        }
        return allocated;
    }

    private BigDecimal effectiveQtyInvoiced(PurchaseOrderLine pol, Map<UUID, BigDecimal> draftAllocated) {
        BigDecimal draft = draftAllocated.getOrDefault(pol.getId(), BigDecimal.ZERO);
        return pol.getQtyInvoiced().add(draft).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal billableQtyForLine(PurchaseOrderLine pol,
                                          Product product,
                                          Map<UUID, BigDecimal> draftAllocated,
                                          boolean allowWithoutReceipt) {
        BigDecimal invoiced = effectiveQtyInvoiced(pol, draftAllocated);
        if (product.getProductType() == ProductType.SERVICE || allowWithoutReceipt) {
            return pol.getQtyOrdered().subtract(invoiced).max(BigDecimal.ZERO);
        }
        return pol.getQtyReceived().subtract(invoiced).max(BigDecimal.ZERO);
    }

    private BigDecimal billableQtyForLine(PurchaseOrderLine pol,
                                          Product product,
                                          Map<UUID, BigDecimal> draftAllocated,
                                          UUID companyId) {
        return billableQtyForLine(
                pol, product, draftAllocated,
                companyDocumentPolicyService.allowBillWithoutReceipt(companyId));
    }

    /** Normalizes the order-level discount input, accepting the legacy percent-only payload. */
    private static void applyOrderDiscountInput(PurchaseOrder o, CreatePurchaseOrderCommand command) {
        if (command.getOrderDiscountType() != null) {
            o.setOrderDiscountType(command.getOrderDiscountType());
        }
        if (command.getOrderDiscountValue() != null) {
            o.setOrderDiscountValue(command.getOrderDiscountValue());
        } else if (command.getOrderDiscountPercent() != null) {
            // Legacy clients sent only a percent — keep PERCENT unless type was set above.
            if (command.getOrderDiscountType() == null) {
                o.setOrderDiscountType(DiscountType.PERCENT);
            }
            o.setOrderDiscountValue(command.getOrderDiscountPercent());
        }
    }

    private static void applyLineDiscountInput(PurchaseOrderLine line, PurchaseOrderLineCommand lc) {
        if (lc.getDiscountType() != null) {
            line.setDiscountType(lc.getDiscountType());
        }
        if (lc.getDiscountValue() != null) {
            line.setDiscountValue(lc.getDiscountValue());
        } else if (lc.getDiscountPercent() != null) {
            if (lc.getDiscountType() == null) {
                line.setDiscountType(DiscountType.PERCENT);
            }
            line.setDiscountValue(lc.getDiscountPercent());
        }
    }

    private void recalcTotals(PurchaseOrder o) {
        BigDecimal untaxed = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (PurchaseOrderLine line : o.getLines()) {
            List<FiscalTaxSnapshot> snaps = line.getTaxes().stream()
                    .map(lt -> fiscalTaxRepository.findById(lt.getTaxId()).orElseThrow())
                    .map(t -> new FiscalTaxSnapshot(t.getId(), t.getAmountType(), t.getAmount(), t.isPriceInclude()))
                    .toList();
            PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                    line.getQtyOrdered(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue(), snaps);
            line.setDiscountPercent(DiscountMath.effectivePercent(
                    line.getQtyOrdered().multiply(line.getUnitPrice()),
                    line.getDiscountType(),
                    line.getDiscountValue()));
            untaxed = untaxed.add(split.net());
            tax = tax.add(split.taxTotal());
        }
        BigDecimal orderDiscount = DiscountMath.discountAmount(
                untaxed, o.getOrderDiscountType(), o.getOrderDiscountValue());
        o.setOrderDiscountPercent(DiscountMath.effectivePercent(
                untaxed, o.getOrderDiscountType(), o.getOrderDiscountValue()));
        BigDecimal factor = untaxed.signum() > 0
                ? untaxed.subtract(orderDiscount).divide(untaxed, 12, RoundingMode.HALF_UP)
                : BigDecimal.ONE;
        untaxed = untaxed.subtract(orderDiscount);
        tax = tax.multiply(factor);
        o.setAmountUntaxed(untaxed.setScale(4, RoundingMode.HALF_UP));
        o.setAmountTax(tax.setScale(4, RoundingMode.HALF_UP));
        o.setAmountTotal(untaxed.add(tax).setScale(4, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrder(UUID id) {
        return toResponse(loadOrder(id));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse sendPurchaseOrder(UUID id) {
        PurchaseOrder o = loadOrder(id);
        PurchaseOrderRules.ensureCanSend(o.getState());
        o.setState(PurchaseOrderState.SENT);
        o.setSentAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "RFQ", "RFQ Sent", "Status");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse confirmPurchaseOrder(UUID id) {
        PurchaseOrder o = loadOrder(id);
        if (o.getState() == PurchaseOrderState.CONFIRMED) {
            return toResponse(o);
        }
        PurchaseOrderRules.ensureCanConfirm(o.getState());
        UUID warehouseId = o.getWarehouseId();
        if (warehouseId == null) {
            throw new PurchaseDomainException("error.purchase.warehouseIdRequiredToConfirm", null, "warehouseId is required to confirm a purchase order");
        }
        Warehouse wh = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new PurchaseDomainException("Warehouse not found: " + warehouseId));
        if (!wh.getCompanyId().getId().equals(o.getCompanyId())) {
            throw new PurchaseDomainException("error.purchase.warehouseCompanyMismatch", null, "Warehouse company mismatch");
        }
        StockLocation supplier = findSupplierVirtual(o.getCompanyId());
        UUID destLoc = o.getDestLocationId() != null
                ? o.getDestLocationId()
                : wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (destLoc == null) {
            throw new PurchaseDomainException("error.purchase.destinationStockLocationUnresolved", null, "Destination stock location could not be resolved");
        }

        BigDecimal rateToCompany = resolveExchangeRate(
                o.getCompanyId(), o.getCurrencyCode(), o.getOrderDate(), o.getExchangeRateToCompany());
        o.setExchangeRateToCompany(rateToCompany);

        List<StockMoveCommand> moves = buildRemainingIncomingMoves(o);

        if (!moves.isEmpty()) {
            CreateStockPickingCommand cmd = new CreateStockPickingCommand();
            cmd.setCompanyId(o.getCompanyId());
            cmd.setWarehouseId(warehouseId);
            cmd.setPickingType(PickingType.INCOMING);
            cmd.setSourceLocationId(supplier.getId().getId());
            cmd.setDestinationLocationId(destLoc);
            cmd.setPartnerId(o.getVendorPartnerId());
            cmd.setOrigin(o.getName());
            cmd.setReference(o.getName());
            cmd.setPurchaseOrderId(o.getId());
            if (o.getOrderDate() != null) {
                cmd.setScheduledAt(o.getOrderDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            }
            cmd.setMoves(moves);
            stockPickingApplicationService.createPicking(cmd);
            purchaseOrderRepository.flush();
        }

        o.setState(PurchaseOrderState.CONFIRMED);
        o.setConfirmedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "RFQ", "Purchase Order", "Status");
        return finalizeAfterConfirm(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(UUID id) {
        PurchaseOrder o = loadOrder(id);
        PurchaseOrderRules.ensureCanCancel(o.getState());
        if (o.isLocked()) {
            throw new PurchaseDomainException(
                    "error.purchase.orderLocked", null, "Purchase order is locked; unlock before cancelling");
        }
        if (o.getState() == PurchaseOrderState.CONFIRMED) {
            // Net quantities: fully returned goods and fully credited bills no longer block.
            if (o.getLines().stream().anyMatch(l -> nz(l.getQtyReceived()).signum() > 0)) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotCancelReceivedGoods", null,
                        "Cannot cancel: goods have been received; return them first");
            }
            if (o.getLines().stream().anyMatch(l -> nz(l.getQtyInvoiced()).signum() > 0)) {
                throw new PurchaseDomainException("error.purchase.cannotCancelPostedVendorBills", null, "Cannot cancel: posted vendor bills exist for this order");
            }
            cancelDraftBills(o.getId());
            cancelOpenPickings(o.getId());
        }
        o.setState(PurchaseOrderState.CANCELLED);
        o.setCancelledAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "Purchase Order", "Cancelled", "Status");
        return toResponse(saved);
    }


    // ---------------------------------------------------------------- guided corrections

    /** Corrections need a confirmed, unlocked order with no draft bill or credit note in the way. */
    private void assertCorrectable(PurchaseOrder o) {
        if (o.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException("error.purchase.correctionRequiresConfirmed", null,
                    "Only confirmed purchase orders can be corrected");
        }
        if (o.isLocked()) {
            throw new PurchaseDomainException(
                    "error.purchase.orderLocked", null, "Purchase order is locked; unlock before amending");
        }
        if (hasDraftBillDocuments(o.getId())) {
            throw new PurchaseDomainException(
                    "error.purchase.draftBillBlocksAmendment", null,
                    "A draft bill or credit note exists for this order; post or cancel it before changing lines");
        }
    }

    private Set<UUID> postedBillDocumentIds(UUID purchaseOrderId) {
        Set<UUID> ids = new java.util.HashSet<>();
        for (VendorBill b : vendorBillRepository.findByPurchaseOrderId(purchaseOrderId)) {
            if (b.getState() == VendorBillState.POSTED) {
                ids.add(b.getId());
            }
        }
        return ids;
    }

    /** The order afterwards plus every bill and credit note posted since {@code before}. */
    private PurchaseCorrectionResult correctionResult(UUID purchaseOrderId, Set<UUID> before) {
        PurchaseCorrectionResult result = new PurchaseCorrectionResult();
        result.setOrder(getPurchaseOrder(purchaseOrderId));
        List<VendorBill> created = new ArrayList<>();
        for (VendorBill b : vendorBillRepository.findByPurchaseOrderId(purchaseOrderId)) {
            if (b.getState() == VendorBillState.POSTED && !before.contains(b.getId())) {
                created.add(b);
            }
        }
        created.sort(Comparator.comparing(VendorBill::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        for (VendorBill b : created) {
            b.getLines().size();
            for (VendorBillLine l : b.getLines()) {
                l.getTaxSnapshots().size();
            }
            result.getDocuments().add(toBillResponse(b));
        }
        return result;
    }

    /** Bills the given quantity per order line at the order's current terms and posts it. */
    private void rebill(PurchaseOrder o, Map<UUID, BigDecimal> lineQuantities) {
        Map<UUID, BigDecimal> positive = new LinkedHashMap<>();
        lineQuantities.forEach((k, v) -> {
            if (nz(v).signum() > 0) {
                positive.put(k, v);
            }
        });
        if (positive.isEmpty()) {
            return;
        }
        CreateVendorBillFromPoCommand cmd = new CreateVendorBillFromPoCommand();
        cmd.setCompanyId(o.getCompanyId());
        cmd.setPurchaseOrderId(o.getId());
        cmd.setBillDate(LocalDate.now());
        cmd.setDueDate(LocalDate.now());
        cmd.setLineQuantities(positive);
        VendorBillResponse bill = createVendorBillFromPo(cmd);
        postVendorBill(bill.getId());
    }

    private List<PurchaseOrderLineTax> buildCorrectionLineTaxes(UUID companyId, List<UUID> taxIds) {
        List<PurchaseOrderLineTax> taxes = new ArrayList<>();
        int seq = 10;
        for (UUID taxId : taxIds) {
            FiscalTax tax = fiscalTaxRepository.findById(taxId)
                    .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
            if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                throw new PurchaseDomainException("error.purchase.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
            }
            if (tax.getScope() != FiscalTaxScope.PURCHASE && tax.getScope() != FiscalTaxScope.BOTH) {
                throw new PurchaseDomainException("error.purchase.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for purchase: " + taxId);
            }
            PurchaseOrderLineTax lt = new PurchaseOrderLineTax();
            lt.setId(UUID.randomUUID());
            lt.setTaxId(taxId);
            lt.setSequence(seq);
            taxes.add(lt);
            seq += 10;
        }
        return taxes;
    }

    private void saveCorrectedOrder(PurchaseOrder o, Map<UUID, BigDecimal> qtyOrderedBefore,
                                    BigDecimal untaxedBefore, String message, String reason) {
        o.setUpdatedAt(Instant.now());
        recalcTotals(o);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        postPurchaseAmendmentTracking(saved, qtyOrderedBefore, untaxedBefore);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                message + (reason != null && !reason.isBlank() ? ": " + reason.trim() : ""));
    }

    private static boolean lineTermsChange(PurchaseOrderLine pol, PurchaseCorrectionCommand.Line cl) {
        if (cl.getUnitPrice() != null && !sameAmount(cl.getUnitPrice(), pol.getUnitPrice())) {
            return true;
        }
        if (cl.getDiscountType() != null && cl.getDiscountType() != pol.getDiscountType()) {
            return true;
        }
        if (cl.getDiscountValue() != null && !sameAmount(cl.getDiscountValue(), pol.getDiscountValue())) {
            return true;
        }
        if (cl.getTaxIds() != null) {
            Set<UUID> current = new java.util.HashSet<>();
            for (PurchaseOrderLineTax t : pol.getTaxes()) {
                current.add(t.getTaxId());
            }
            return !current.equals(new java.util.HashSet<>(cl.getTaxIds()));
        }
        return false;
    }

    @Override
    @Transactional
    public PurchaseCorrectionResult changeTerms(UUID id, PurchaseCorrectionCommand command) {
        PurchaseOrder o = loadOrder(id);
        assertCorrectable(o);
        Set<UUID> before = postedBillDocumentIds(id);
        Map<UUID, PurchaseOrderLine> byId = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            byId.put(line.getId(), line);
        }
        boolean orderDiscountChanged = false;
        if (command.getOrderDiscountType() != null || command.getOrderDiscountValue() != null) {
            DiscountType t = command.getOrderDiscountType() != null ? command.getOrderDiscountType() : o.getOrderDiscountType();
            orderDiscountChanged = t != o.getOrderDiscountType()
                    || (command.getOrderDiscountValue() != null && !sameAmount(command.getOrderDiscountValue(), o.getOrderDiscountValue()));
        }
        // What is billed must be credited at the old terms and billed again at the new ones.
        Map<UUID, BigDecimal> affected = new LinkedHashMap<>();
        boolean anyChange = orderDiscountChanged;
        for (PurchaseCorrectionCommand.Line cl : command.getLines()) {
            PurchaseOrderLine pol = byId.get(cl.getPurchaseOrderLineId());
            if (pol == null) {
                throw new PurchaseDomainException("error.purchase.documentLineNotOnOrder", null,
                        "The document has a line that no longer exists on the purchase order");
            }
            if (!lineTermsChange(pol, cl)) {
                continue;
            }
            anyChange = true;
            if (nz(pol.getQtyInvoiced()).signum() > 0) {
                affected.put(pol.getId(), pol.getQtyInvoiced());
            }
        }
        if (!anyChange) {
            throw new PurchaseDomainException("error.purchase.correctionNothingToChange", null, "Nothing to change");
        }
        if (orderDiscountChanged) {
            for (PurchaseOrderLine pol : o.getLines()) {
                if (nz(pol.getQtyInvoiced()).signum() > 0) {
                    affected.put(pol.getId(), pol.getQtyInvoiced());
                }
            }
        }
        creditQuantities(o, affected);

        o = loadOrder(id);
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        Map<UUID, PurchaseCorrectionCommand.Line> changes = new LinkedHashMap<>();
        for (PurchaseCorrectionCommand.Line cl : command.getLines()) {
            changes.put(cl.getPurchaseOrderLineId(), cl);
        }
        for (PurchaseOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            PurchaseCorrectionCommand.Line cl = changes.get(line.getId());
            if (cl == null) {
                continue;
            }
            if (cl.getUnitPrice() != null) {
                line.setUnitPrice(cl.getUnitPrice());
            }
            if (cl.getDiscountType() != null) {
                line.setDiscountType(cl.getDiscountType());
            }
            if (cl.getDiscountValue() != null) {
                line.setDiscountValue(cl.getDiscountValue());
            }
            if (cl.getTaxIds() != null) {
                line.getTaxes().clear();
                line.getTaxes().addAll(buildCorrectionLineTaxes(o.getCompanyId(), cl.getTaxIds()));
            }
            line.setUpdatedAt(Instant.now());
        }
        if (orderDiscountChanged) {
            if (command.getOrderDiscountType() != null) {
                o.setOrderDiscountType(command.getOrderDiscountType());
            }
            if (command.getOrderDiscountValue() != null) {
                o.setOrderDiscountValue(command.getOrderDiscountValue());
            }
        }
        saveCorrectedOrder(o, qtyOrderedBefore, untaxedBefore,
                "Prices, discounts or taxes corrected after receipt or billing", command.getReason());
        if (o.getWarehouseId() != null) {
            // The part still to be received is valued at the new price.
            syncOpenReceipts(loadOrder(id), false);
        }
        rebill(loadOrder(id), affected);
        rematchVendorCredit(id, before);
        return correctionResult(id, before);
    }

    /**
     * Money already paid on the credited bills moves to the bills that replaced them: the credit is
     * released from the old bills and applied to the new ones, never more than was released.
     */
    private void rematchVendorCredit(UUID purchaseOrderId, Set<UUID> before) {
        List<VendorBill> bills = vendorBillRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .filter(b -> b.getMoveType() == null || b.getMoveType() == VendorBillMoveType.BILL)
                .sorted(Comparator.comparing(VendorBill::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        BigDecimal released = BigDecimal.ZERO;
        for (VendorBill b : bills) {
            if (before.contains(b.getId()) && vendorPaymentService.creditAvailableOn(b).signum() > 0) {
                released = released.add(vendorPaymentService.keepCredit(b.getId()));
            }
        }
        for (VendorBill b : bills) {
            if (released.signum() <= 0) {
                break;
            }
            if (!before.contains(b.getId())) {
                released = released.subtract(vendorPaymentService.applyCredit(b.getId(), released));
            }
        }
    }

    @Override
    @Transactional
    public PurchaseCorrectionResult reduceQuantities(UUID id, PurchaseCorrectionCommand command) {
        PurchaseOrder o = loadOrder(id);
        assertCorrectable(o);
        Set<UUID> before = postedBillDocumentIds(id);
        Map<UUID, PurchaseOrderLine> byId = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            byId.put(line.getId(), line);
        }
        Map<UUID, BigDecimal> target = new LinkedHashMap<>();
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        for (PurchaseCorrectionCommand.Line cl : command.getLines()) {
            PurchaseOrderLine pol = byId.get(cl.getPurchaseOrderLineId());
            if (pol == null || cl.getQty() == null) {
                throw new PurchaseDomainException("error.purchase.documentLineNotOnOrder", null,
                        "The document has a line that no longer exists on the purchase order");
            }
            BigDecimal t = cl.getQty();
            if (t.signum() < 0 || t.compareTo(nz(pol.getQtyOrdered())) > 0) {
                throw new PurchaseDomainException("error.purchase.reduceOnlyLowers", null,
                        "Reduce quantities can only lower a line; increase it by editing the order");
            }
            if (t.compareTo(nz(pol.getQtyReceived())) < 0) {
                throw new PurchaseDomainException("error.purchase.qtyBelowReceived",
                        new Object[] { MonetaryScale.toDisplayString(nz(pol.getQtyReceived())) },
                        "Quantity cannot be below the received quantity; return the goods first");
            }
            target.put(pol.getId(), t);
            BigDecimal excess = nz(pol.getQtyInvoiced()).subtract(t);
            if (excess.signum() > 0) {
                toCredit.put(pol.getId(), excess);
            }
        }
        creditQuantities(o, toCredit);

        o = loadOrder(id);
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        for (PurchaseOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            BigDecimal t = target.get(line.getId());
            if (t != null) {
                line.setQtyOrdered(t);
                line.setUpdatedAt(Instant.now());
            }
        }
        saveCorrectedOrder(o, qtyOrderedBefore, untaxedBefore, "Quantities reduced after billing", command.getReason());
        if (o.getWarehouseId() != null) {
            syncOpenReceipts(loadOrder(id), false);
        }
        return correctionResult(id, before);
    }

    @Override
    @Transactional
    public PurchaseCorrectionResult cancelWithDocuments(UUID id, PurchaseCorrectionCommand command) {
        PurchaseOrder o = loadOrder(id);
        if (o.getState() != PurchaseOrderState.CONFIRMED) {
            PurchaseCorrectionResult r = new PurchaseCorrectionResult();
            r.setOrder(cancelPurchaseOrder(id));
            return r;
        }
        assertCorrectable(o);
        Set<UUID> before = postedBillDocumentIds(id);
        String reason = command != null ? command.getReason() : null;
        // 1. Everything received goes back, refunded (credits the received and billed part).
        ReturnGoodsCommand ret = new ReturnGoodsCommand();
        for (PurchaseOrderLine line : o.getLines()) {
            if (nz(line.getQtyReceived()).signum() > 0) {
                ReturnGoodsCommand.Line rl = new ReturnGoodsCommand.Line();
                rl.setPurchaseOrderLineId(line.getId());
                rl.setQty(line.getQtyReceived());
                ret.getLines().add(rl);
            }
        }
        if (!ret.getLines().isEmpty()) {
            ret.setRefund(true);
            ret.setReason(reason);
            returnGoods(id, ret);
        }
        // 2. Whatever is still billed (billed before receipt, services) is credited.
        o = loadOrder(id);
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            if (nz(line.getQtyInvoiced()).signum() > 0) {
                toCredit.put(line.getId(), line.getQtyInvoiced());
            }
        }
        creditQuantities(o, toCredit);
        // 3. Nothing received or billed is left: cancel (open receipts and drafts go with it).
        cancelPurchaseOrder(id);
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, id,
                "Cancelled with its documents" + (reason != null && !reason.isBlank() ? ": " + reason.trim() : ""));
        return correctionResult(id, before);
    }

    @Override
    @Transactional
    public PurchaseCorrectionResult reassignVendor(UUID id, PurchaseCorrectionCommand command) {
        PurchaseOrder o = loadOrder(id);
        assertCorrectable(o);
        if (command.getVendorPartnerId() == null || command.getVendorPartnerId().equals(o.getVendorPartnerId())) {
            throw new PurchaseDomainException("error.purchase.correctionNothingToChange", null, "Nothing to change");
        }
        if (o.getLines().stream().anyMatch(l -> nz(l.getQtyReceived()).signum() > 0)) {
            throw new PurchaseDomainException("error.purchase.reassignAfterReceipt", null,
                    "Goods were already received from this vendor; return them first");
        }
        PartnerResponse vendor = partnerApplicationService.getPartner(command.getVendorPartnerId());
        if (!vendor.isVendor()) {
            throw new PurchaseDomainException("error.purchase.partnerNotVendor", null, "Partner is not a vendor");
        }
        if (!vendor.getCompanyId().equals(o.getCompanyId())) {
            throw new PurchaseDomainException("error.purchase.vendorCompanyMismatch", null, "Vendor belongs to another company");
        }
        Set<UUID> before = postedBillDocumentIds(id);
        Map<UUID, BigDecimal> billed = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            if (nz(line.getQtyInvoiced()).signum() > 0) {
                billed.put(line.getId(), line.getQtyInvoiced());
            }
        }
        creditQuantities(o, billed);

        o = loadOrder(id);
        UUID previous = o.getVendorPartnerId();
        o.setVendorPartnerId(vendor.getId());
        if (vendor.getPaymentTermsId() != null) {
            o.setPaymentTermsId(vendor.getPaymentTermsId());
        }
        o.setUpdatedAt(Instant.now());
        purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, id,
                "Vendor reassigned (" + previous + " -> " + vendor.getDisplayName() + ")"
                        + (command.getReason() != null && !command.getReason().isBlank() ? ": " + command.getReason().trim() : ""));
        if (o.getWarehouseId() != null) {
            // Open receipts carry the partner: replace them so they come from the new vendor.
            cancelOpenPickings(id);
            createReceiptPickingForRemaining(loadOrder(id));
        }
        rebill(loadOrder(id), billed);
        return correctionResult(id, before);
    }

    /** Order-line quantity (order unit or packaging) expressed in the product's stock unit. */
    private BigDecimal toStockUomQty(PurchaseOrderLine line, Product product, BigDecimal qtyInOrderUom) {
        if (line.getQtyPerPackage() != null && line.getQtyPerPackage().signum() > 0) {
            return ProductPackaging.toBaseQty(qtyInOrderUom, line.getQtyPerPackage());
        }
        return uomApplicationService.convert(line.getUomId(), product.getUomId().getId(), qtyInOrderUom);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse returnGoods(UUID id, ReturnGoodsCommand command) {
        PurchaseOrder o = loadOrder(id);
        if (o.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException(
                    "error.purchase.returnRequiresConfirmed", null,
                    "Purchase order must be confirmed to create a return");
        }
        if (o.isLocked()) {
            throw new PurchaseDomainException(
                    "error.purchase.orderLocked", null, "Purchase order is locked; unlock before amending");
        }
        Map<UUID, PurchaseOrderLine> byId = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            byId.put(line.getId(), line);
        }
        // Requested quantities, checked against what is received and converted to stock units.
        Map<UUID, BigDecimal> wantedStockQty = new LinkedHashMap<>();
        for (ReturnGoodsCommand.Line rl : command.getLines()) {
            BigDecimal qty = nz(rl.getQty());
            if (qty.signum() <= 0) {
                continue;
            }
            PurchaseOrderLine pol = byId.get(rl.getPurchaseOrderLineId());
            if (pol == null) {
                throw new PurchaseDomainException("error.purchase.documentLineNotOnOrder", null,
                        "The document has a line that no longer exists on the purchase order");
            }
            Product product = productRepository.findById(new ProductId(pol.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + pol.getProductId()));
            if (product.getProductType() == ProductType.SERVICE || qty.compareTo(nz(pol.getQtyReceived())) > 0) {
                throw new PurchaseDomainException("error.purchase.returnExceedsReceived",
                        new Object[] { pol.getName() },
                        "Cannot return more than was received for '" + pol.getName() + "'");
            }
            wantedStockQty.merge(pol.getId(), toStockUomQty(pol, product, qty), BigDecimal::add);
        }
        if (wantedStockQty.isEmpty()) {
            throw new PurchaseDomainException("error.purchase.nothingToReturn", null, "No received stock available to return");
        }

        // Validated receipts, newest first, and what earlier returns already took back from each.
        List<StockPickingResponse> receipts = new ArrayList<>();
        for (UUID pid : stockMovePurchaseQueryPort.findPickingIdsByPurchaseOrderId(o.getId())) {
            StockPickingResponse p = stockPickingApplicationService.getPicking(pid);
            if (p.getState() == PickingState.DONE) {
                receipts.add(p);
            }
        }
        receipts.sort(Comparator.comparing(StockPickingResponse::getValidatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        Map<String, BigDecimal> alreadyReturned = new LinkedHashMap<>();
        for (UUID rid : stockMovePurchaseQueryPort.findReturnPickingIdsByPurchaseOrderId(o.getId())) {
            StockPickingResponse r = stockPickingApplicationService.getPicking(rid);
            if (r.getState() == PickingState.CANCELLED || r.getBackorderOf() == null) {
                continue;
            }
            for (StockPickingResponse.MoveResponse m : r.getMoves()) {
                if (m.getPurchaseOrderLineId() == null || m.getState() == MoveState.CANCELLED) {
                    continue;
                }
                BigDecimal q = m.getState() == MoveState.DONE ? nz(m.getPickedQuantity()) : nz(m.getDemandQuantity());
                alreadyReturned.merge(r.getBackorderOf() + "|" + m.getPurchaseOrderLineId(), q, BigDecimal::add);
            }
        }

        // Spread each line's quantity over the receipts it came in.
        Map<UUID, Map<UUID, BigDecimal>> planByReceipt = new LinkedHashMap<>();
        for (Map.Entry<UUID, BigDecimal> want : wantedStockQty.entrySet()) {
            BigDecimal rem = want.getValue();
            for (StockPickingResponse d : receipts) {
                BigDecimal returnedHere = nz(alreadyReturned.get(d.getId() + "|" + want.getKey()));
                for (StockPickingResponse.MoveResponse m : d.getMoves()) {
                    if (rem.signum() <= 0) {
                        break;
                    }
                    if (!want.getKey().equals(m.getPurchaseOrderLineId()) || m.getState() != MoveState.DONE) {
                        continue;
                    }
                    BigDecimal picked = nz(m.getPickedQuantity());
                    BigDecimal usedByEarlier = returnedHere.min(picked);
                    returnedHere = returnedHere.subtract(usedByEarlier);
                    BigDecimal take = rem.min(picked.subtract(usedByEarlier));
                    if (take.signum() <= 0) {
                        continue;
                    }
                    planByReceipt.computeIfAbsent(d.getId(), k -> new LinkedHashMap<>()).merge(m.getId(), take, BigDecimal::add);
                    rem = rem.subtract(take);
                }
                if (rem.signum() <= 0) {
                    break;
                }
            }
            if (rem.signum() > 0) {
                PurchaseOrderLine pol = byId.get(want.getKey());
                throw new PurchaseDomainException("error.purchase.returnExceedsReceived",
                        new Object[] { pol.getName() },
                        "Cannot return more than was received for '" + pol.getName() + "'");
            }
        }

        for (Map.Entry<UUID, Map<UUID, BigDecimal>> plan : planByReceipt.entrySet()) {
            // A move missing from the quantity map is returned in full, so every other move of the
            // receipt gets an explicit zero: only the requested lines go back.
            Map<UUID, BigDecimal> quantities = new LinkedHashMap<>();
            for (StockPickingResponse d : receipts) {
                if (d.getId().equals(plan.getKey())) {
                    for (StockPickingResponse.MoveResponse m : d.getMoves()) {
                        quantities.put(m.getId(), BigDecimal.ZERO);
                    }
                }
            }
            quantities.putAll(plan.getValue());
            ReturnPickingCommand rc = new ReturnPickingCommand();
            rc.setMoveQuantities(quantities);
            rc.setToRefund(command.isRefund());
            StockPickingResponse ret = stockPickingApplicationService.returnPicking(plan.getKey(), rc);
            stockPickingApplicationService.validatePicking(ret.getId(), null);
        }
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, o.getId(),
                (command.isRefund() ? "Goods returned to the vendor and refunded" : "Goods returned for replacement")
                        + (command.getReason() != null && !command.getReason().isBlank()
                                ? ": " + command.getReason().trim() : ""));
        return getPurchaseOrder(id);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse closeRemainingQuantities(UUID id, String reason) {
        PurchaseOrder o = loadOrder(id);
        if (o.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException("error.purchase.closeRemainingRequiresConfirmed", null,
                    "Only confirmed orders can be closed short");
        }
        if (o.isLocked()) {
            throw new PurchaseDomainException(
                    "error.purchase.orderLocked", null, "Purchase order is locked; unlock before amending");
        }
        if (hasDraftBillDocuments(o.getId())) {
            throw new PurchaseDomainException(
                    "error.purchase.draftBillBlocksAmendment", null,
                    "A draft bill or credit note exists for this order; post or cancel it before changing lines");
        }
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        Instant now = Instant.now();
        for (PurchaseOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + line.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                continue;
            }
            BigDecimal received = nz(line.getQtyReceived());
            if (nz(line.getQtyInvoiced()).compareTo(received) > 0) {
                throw new PurchaseDomainException("error.purchase.closeRemainingBilledUnreceived", null,
                        "Some quantity is billed but not received; create a credit note for it first");
            }
            if (nz(line.getQtyOrdered()).compareTo(received) != 0) {
                line.setQtyOrdered(received);
                line.setUpdatedAt(now);
            }
        }
        cancelOpenPickings(o.getId());
        o.setUpdatedAt(now);
        recalcTotals(o);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        postPurchaseAmendmentTracking(saved, qtyOrderedBefore, untaxedBefore);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "Remaining quantities closed" + (reason != null && !reason.isBlank() ? ": " + reason.trim() : ""));
        return getPurchaseOrder(saved.getId());
    }

    /**
     * Before posting a bill or credit note of an order: refuse a document that no longer fits it,
     * i.e. billing beyond the ordered quantity or crediting beyond the billed quantity.
     */
    private void assertBillFitsOrder(VendorBill bill) {
        if (bill.getPurchaseOrderId() == null || bill.getMoveType() == VendorBillMoveType.DEBIT_NOTE) {
            return;
        }
        PurchaseOrder po = loadOrder(bill.getPurchaseOrderId());
        boolean creditNote = bill.getMoveType() == VendorBillMoveType.CREDIT_NOTE;
        Map<UUID, BigDecimal> qtyByLine = new LinkedHashMap<>();
        for (VendorBillLine line : bill.getLines()) {
            if (line.getPurchaseOrderLineId() != null) {
                qtyByLine.merge(line.getPurchaseOrderLineId(), nz(line.getQty()), BigDecimal::add);
            }
        }
        for (PurchaseOrderLine pol : po.getLines()) {
            BigDecimal qty = qtyByLine.get(pol.getId());
            if (qty == null) {
                continue;
            }
            BigDecimal billed = nz(pol.getQtyInvoiced());
            if (creditNote) {
                if (qty.compareTo(billed) > 0) {
                    throw new PurchaseDomainException("error.purchase.creditExceedsBilled",
                            new Object[] { pol.getName() },
                            "Cannot credit more than was billed for '" + pol.getName() + "'");
                }
            } else if (billed.add(qty).compareTo(nz(pol.getQtyOrdered())) > 0) {
                throw new PurchaseDomainException("error.purchase.billExceedsOrdered",
                        new Object[] { pol.getName() },
                        "Cannot bill more than ordered for '" + pol.getName() + "'");
            }
        }
    }

    private void cancelDraftBills(UUID purchaseOrderId) {
        for (VendorBill bill : vendorBillRepository.findByPurchaseOrderId(purchaseOrderId)) {
            if (bill.getState() == VendorBillState.DRAFT) {
                cancelVendorBill(bill.getId());
            }
        }
    }

    private boolean hasDraftBillDocuments(UUID purchaseOrderId) {
        return vendorBillRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .anyMatch(b -> b.getState() == VendorBillState.DRAFT);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) == 0;
    }

    /** Commercial terms of a line (qty, price, discount, taxes) used to detect edits. */
    private static String lineTermsSignature(PurchaseOrderLine line) {
        List<String> taxIds = new ArrayList<>();
        for (PurchaseOrderLineTax t : line.getTaxes()) {
            taxIds.add(String.valueOf(t.getTaxId()));
        }
        java.util.Collections.sort(taxIds);
        return nz(line.getQtyOrdered()).stripTrailingZeros().toPlainString()
                + "|" + nz(line.getUnitPrice()).stripTrailingZeros().toPlainString()
                + "|" + line.getDiscountType()
                + "|" + nz(line.getDiscountValue()).stripTrailingZeros().toPlainString()
                + "|" + String.join(",", taxIds);
    }

    private static boolean orderDiscountChanged(PurchaseOrder o, CreatePurchaseOrderCommand command) {
        PurchaseOrder probe = new PurchaseOrder();
        probe.setOrderDiscountType(o.getOrderDiscountType());
        probe.setOrderDiscountValue(o.getOrderDiscountValue());
        applyOrderDiscountInput(probe, command);
        return probe.getOrderDiscountType() != o.getOrderDiscountType()
                || !sameAmount(probe.getOrderDiscountValue(), o.getOrderDiscountValue());
    }

    /**
     * Rules for editing a line of a confirmed order: quantity never below what was received or
     * billed, and price, discount and taxes are fixed once anything is received or billed (the
     * received goods were valued at the agreed price; change it with the guided correction).
     */
    private static void assertLineAmendmentAllowed(PurchaseOrderLine line, PurchaseOrderLineCommand lc) {
        BigDecimal newQty = nz(lc.getQtyOrdered());
        BigDecimal received = nz(line.getQtyReceived());
        BigDecimal billed = nz(line.getQtyInvoiced());
        if (newQty.compareTo(received) < 0) {
            throw new PurchaseDomainException(
                    "error.purchase.qtyBelowReceived",
                    new Object[] { MonetaryScale.toDisplayString(received) },
                    "Quantity cannot be below the received quantity (" + received.toPlainString()
                            + "); return the goods first");
        }
        if (newQty.compareTo(billed) < 0) {
            throw new PurchaseDomainException(
                    "error.purchase.qtyBelowBilled",
                    new Object[] { MonetaryScale.toDisplayString(billed) },
                    "Quantity cannot be below the billed quantity (" + billed.toPlainString()
                            + "); create a credit note first");
        }
        if (received.signum() <= 0 && billed.signum() <= 0) {
            return;
        }
        boolean priceChanged = lc.getUnitPrice() != null && !sameAmount(lc.getUnitPrice(), line.getUnitPrice());
        PurchaseOrderLine probe = new PurchaseOrderLine();
        probe.setDiscountType(line.getDiscountType());
        probe.setDiscountValue(line.getDiscountValue());
        applyLineDiscountInput(probe, lc);
        boolean discountChanged = probe.getDiscountType() != line.getDiscountType()
                || !sameAmount(probe.getDiscountValue(), line.getDiscountValue());
        Set<UUID> taxesBefore = new HashSet<>();
        for (PurchaseOrderLineTax t : line.getTaxes()) {
            taxesBefore.add(t.getTaxId());
        }
        boolean taxesChanged = !taxesBefore.equals(new HashSet<>(lc.getTaxIds()));
        if (priceChanged || discountChanged || taxesChanged) {
            throw new PurchaseDomainException(
                    "error.purchase.receivedLineTermsLocked", null,
                    "Price, discount and taxes are locked once goods are received or billed; use Correct order");
        }
    }

    @Override
    @Transactional
    public PurchaseOrderResponse lockPurchaseOrder(UUID id) {
        PurchaseOrder o = loadOrder(id);
        if (o.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException(
                    "error.purchase.lockRequiresConfirmed", null, "Only confirmed purchase orders can be locked");
        }
        if (o.isLocked()) {
            return toResponse(o);
        }
        o.setLocked(true);
        o.setUpdatedAt(Instant.now());
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "Purchase order locked");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse unlockPurchaseOrder(UUID id) {
        PurchaseOrder o = loadOrder(id);
        if (!o.isLocked()) {
            return toResponse(o);
        }
        o.setLocked(false);
        o.setUpdatedAt(Instant.now());
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "Purchase order unlocked");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StockPickingResponse createReturnFromPurchaseOrder(UUID purchaseOrderId) {
        return createReturnFromPurchaseOrder(purchaseOrderId, new CreatePurchaseReturnCommand());
    }

    @Override
    @Transactional
    public StockPickingResponse createReturnFromPurchaseOrder(UUID purchaseOrderId, CreatePurchaseReturnCommand command) {
        PurchaseOrder o = loadOrder(purchaseOrderId);
        if (o.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException(
                    "error.purchase.returnRequiresConfirmed", null,
                    "Purchase order must be confirmed to create a return");
        }
        if (!computeCanCreateReturn(o)) {
            throw new PurchaseDomainException(
                    "error.purchase.nothingToReturn", null,
                    "No received stock available to return");
        }
        UUID receiptId = stockMovePurchaseQueryPort.findReturnableReceiptPickingId(o.getId())
                .orElseThrow(() -> new PurchaseDomainException(
                        "error.purchase.nothingToReturn", null,
                        "No received stock available to return"));
        CreatePurchaseReturnCommand cmd = command != null ? command : new CreatePurchaseReturnCommand();
        ReturnPickingCommand returnCmd = new ReturnPickingCommand();
        returnCmd.setMoveQuantities(cmd.getMoveQuantities());
        returnCmd.setToRefund(cmd.isToRefund());
        return stockPickingApplicationService.returnPicking(receiptId, returnCmd);
    }

    @Override
    @Transactional
    public StockPickingResponse validateReceiptPicking(UUID pickingId, ValidatePickingCommand command) {
        // validatePicking() already refreshes qty_received via PurchaseReceiveSyncPort.
        return stockPickingApplicationService.validatePicking(pickingId,
                command != null ? command : new ValidatePickingCommand());
    }

    @Override
    public void syncPurchaseOrderLineQtyReceivedFromStockMoves(UUID purchaseOrderId, UUID pickingId) {
        Map<UUID, BigDecimal> receivedBefore = new LinkedHashMap<>();
        purchaseOrderRepository.findById(purchaseOrderId).ifPresent(before -> before.getLines()
                .forEach(l -> receivedBefore.put(l.getId(), nz(l.getQtyReceived()))));
        PurchaseOrder o = purchaseOrderQtyWriter.updateQtyReceived(purchaseOrderId);
        if (o == null || pickingId == null) {
            return;
        }
        // Runs inside the picking validation transaction: refusing here rolls the whole validation
        // back, so goods can never be received beyond the ordered quantity.
        for (PurchaseOrderLine line : o.getLines()) {
            if (nz(line.getQtyReceived()).compareTo(nz(line.getQtyOrdered())) > 0) {
                throw new PurchaseDomainException("error.purchase.receivedExceedsOrdered",
                        new Object[] { line.getName() },
                        "Cannot receive more than ordered for '" + line.getName() + "'");
            }
        }
        StockPickingResponse picking = stockPickingApplicationService.getPicking(pickingId);
        if (picking.getPickingType() == PickingType.INCOMING) {
            if (o.getWarehouseId() != null) {
                syncOpenReceipts(loadOrder(o.getId()), true);
            }
            return;
        }
        Map<UUID, BigDecimal> returned = new LinkedHashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            BigDecimal delta = nz(receivedBefore.get(line.getId())).subtract(nz(line.getQtyReceived()));
            if (delta.signum() > 0) {
                returned.put(line.getId(), delta);
            }
        }
        if (returned.isEmpty()) {
            return;
        }
        if (picking.isToRefund()) {
            applyRefundReturn(o.getId(), returned);
        } else {
            applyReplaceReturn(o.getId());
        }
    }

    /**
     * Goods went back to the vendor and the vendor refunds: they are no longer ordered, and the
     * billed part of them is credited and posted now, in the same transaction as the return.
     */
    private void applyRefundReturn(UUID purchaseOrderId, Map<UUID, BigDecimal> returned) {
        PurchaseOrder o = loadOrder(purchaseOrderId);
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        Instant now = Instant.now();
        for (PurchaseOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            BigDecimal back = returned.get(line.getId());
            if (back == null) {
                continue;
            }
            line.setQtyOrdered(nz(line.getQtyOrdered()).subtract(back).max(nz(line.getQtyReceived())));
            line.setUpdatedAt(now);
        }
        o.setUpdatedAt(now);
        recalcTotals(o);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        postPurchaseAmendmentTracking(saved, qtyOrderedBefore, untaxedBefore);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "Returned goods refunded: ordered quantity reduced");
        creditReturnedGoods(loadOrder(purchaseOrderId), returned);
    }

    /** Goods went back to be replaced: the order is unchanged and a receipt is created for them. */
    private void applyReplaceReturn(UUID purchaseOrderId) {
        PurchaseOrder o = loadOrder(purchaseOrderId);
        if (o.getWarehouseId() != null) {
            syncOpenReceipts(o, false);
        }
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, o.getId(),
                "Returned goods to be replaced: re-receipt created");
    }

    /**
     * Credits and posts what is billed but no longer received, capped at what was actually returned
     * (a bill posted before receipt keeps covering goods that have not arrived yet).
     */
    private void creditReturnedGoods(PurchaseOrder o, Map<UUID, BigDecimal> returned) {
        Map<UUID, BigDecimal> draftCn = draftCreditNoteQtyByPoLine(o.getId());
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        for (PurchaseOrderLine pol : o.getLines()) {
            BigDecimal qty = creditNoteableQtyForPoLine(pol, draftCn).min(nz(returned.get(pol.getId())));
            if (qty.signum() > 0) {
                toCredit.put(pol.getId(), qty);
            }
        }
        creditQuantities(o, toCredit);
    }

    /**
     * Creates and posts vendor credit notes for the given quantity per order line, spread over the
     * order's posted bills newest first, at each bill line's original price, discount and tax and
     * never beyond what a bill line still has uncredited. Returns the posted credit notes.
     */
    private List<VendorBillResponse> creditQuantities(PurchaseOrder o, Map<UUID, BigDecimal> quantities) {
        List<VendorBillResponse> posted = new ArrayList<>();
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        quantities.forEach((k, v) -> {
            if (nz(v).signum() > 0) {
                toCredit.put(k, v);
            }
        });
        if (toCredit.isEmpty()) {
            return posted;
        }
        List<VendorBill> bills = vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .filter(b -> b.getMoveType() == null || b.getMoveType() == VendorBillMoveType.BILL)
                .sorted(Comparator.comparing(VendorBill::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(VendorBill::getId).reversed())
                .toList();
        for (VendorBill bill : bills) {
            bill.getLines().size();
            for (VendorBillLine l : bill.getLines()) {
                l.getTaxSnapshots().size();
            }
            Map<UUID, BigDecimal> credited = creditedQtyBySourceBillLine(bill);
            List<CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand> lines = new ArrayList<>();
            for (Map.Entry<UUID, BigDecimal> e : toCredit.entrySet()) {
                for (VendorBillLine billLine : bill.getLines()) {
                    if (e.getValue().signum() <= 0) {
                        break;
                    }
                    if (!e.getKey().equals(billLine.getPurchaseOrderLineId())) {
                        continue;
                    }
                    BigDecimal available = nz(billLine.getQty()).subtract(credited.getOrDefault(billLine.getId(), BigDecimal.ZERO))
                            .max(BigDecimal.ZERO);
                    BigDecimal take = e.getValue().min(available);
                    if (take.signum() <= 0) {
                        continue;
                    }
                    CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand lc =
                            new CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand();
                    lc.setBillLineId(billLine.getId());
                    lc.setQty(take);
                    lines.add(lc);
                    credited.merge(billLine.getId(), take, BigDecimal::add);
                    e.setValue(e.getValue().subtract(take));
                }
            }
            if (lines.isEmpty()) {
                continue;
            }
            CreateCreditNoteFromVendorBillCommand cmd = new CreateCreditNoteFromVendorBillCommand();
            cmd.setCompanyId(o.getCompanyId());
            cmd.setBillDate(LocalDate.now());
            cmd.setReference(o.getName() != null ? "CN/" + o.getName() : null);
            cmd.setLines(lines);
            VendorBillResponse cn = createCreditNoteFromVendorBill(bill.getId(), cmd);
            posted.add(postVendorBill(cn.getId()));
        }
        if (toCredit.values().stream().anyMatch(q -> q.signum() > 0)) {
            throw new PurchaseDomainException("error.purchase.cannotCreditReturn", null,
                    "The returned goods could not be fully credited against this order's bills");
        }
        return posted;
    }

    private BigDecimal creditNoteableQtyForPoLine(PurchaseOrderLine pol, Map<UUID, BigDecimal> draftCreditNotes) {
        BigDecimal draft = draftCreditNotes.getOrDefault(pol.getId(), BigDecimal.ZERO);
        Optional<Product> product = productRepository.findById(new ProductId(pol.getProductId()));
        BigDecimal baseline;
        if (product.isPresent() && product.get().getProductType() == ProductType.SERVICE) {
            baseline = pol.getQtyOrdered();
        } else {
            baseline = pol.getQtyReceived();
        }
        return pol.getQtyInvoiced().subtract(baseline).subtract(draft).max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public VendorBillResponse updateVendorBill(UUID id, UpdateVendorBillCommand command) {
        VendorBill bill = vendorBillRepository.findById(id)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found: " + id));
        if (bill.getState() != VendorBillState.DRAFT) {
            throw new PurchaseDomainException(
                    "error.purchase.onlyDraftBillEditable", null, "Only draft vendor bills can be updated");
        }
        periodPostingGuard.assertDatePostable(bill.getCompanyId(), command.getBillDate());
        bill.getLines().size();
        for (VendorBillLine line : bill.getLines()) {
            line.getTaxSnapshots().size();
        }

        Instant now = Instant.now();
        bill.setBillDate(command.getBillDate());
        bill.setDueDate(command.getDueDate());
        bill.setReference(command.getReference());
        bill.setOrderDiscountAmount(command.getOrderDiscountAmount() != null
                ? command.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO);
        bill.setUpdatedAt(now);

        Map<UUID, VendorBillLine> linesById = bill.getLines().stream()
                .collect(Collectors.toMap(VendorBillLine::getId, l -> l, (a, b) -> a, LinkedHashMap::new));

        VendorBillMoveType moveType = bill.getMoveType() != null ? bill.getMoveType() : VendorBillMoveType.BILL;
        PurchaseOrder po = bill.getPurchaseOrderId() != null ? loadOrder(bill.getPurchaseOrderId()) : null;
        Map<UUID, BigDecimal> otherDraftAllocated = new HashMap<>();
        // Debit notes are amount adjustments — do not cap against PO billable qty.
        if (po != null && moveType == VendorBillMoveType.BILL) {
            otherDraftAllocated = draftBillOrDebitQtyByPoLineExcludingBill(po.getId(), bill.getId());
        }
        Map<UUID, BigDecimal> alreadyCredited = Map.of();
        VendorBill sourceBill = null;
        if (moveType == VendorBillMoveType.CREDIT_NOTE && bill.getReversedBillId() != null) {
            sourceBill = vendorBillRepository.findById(bill.getReversedBillId()).orElse(null);
            if (sourceBill != null) {
                sourceBill.getLines().size();
                alreadyCredited = creditedQtyBySourceBillLineExcluding(sourceBill, bill.getId());
            }
        }

        for (UpdateVendorBillCommand.UpdateVendorBillLineCommand lc : command.getLines()) {
            VendorBillLine line = linesById.get(lc.getLineId());
            if (line == null) {
                throw new PurchaseDomainException(
                        "error.purchase.vendorBillLineNotFound", null,
                        "Vendor bill line not found: " + lc.getLineId());
            }
            BigDecimal newQty = lc.getQty().setScale(4, RoundingMode.HALF_UP);
            if (line.getPurchaseOrderLineId() != null && po != null && moveType == VendorBillMoveType.BILL) {
                PurchaseOrderLine pol = po.getLines().stream()
                        .filter(l -> l.getId().equals(line.getPurchaseOrderLineId()))
                        .findFirst()
                        .orElseThrow(() -> new PurchaseDomainException(
                                "error.purchase.poLineNotFound", null, "Purchase order line not found"));
                Product product = productRepository.findById(new ProductId(pol.getProductId()))
                        .orElseThrow(() -> new PurchaseDomainException("Product not found: " + pol.getProductId()));
                BigDecimal maxQty = billableQtyForLine(pol, product, otherDraftAllocated, bill.getCompanyId());
                if (newQty.compareTo(maxQty) > 0) {
                    throw new PurchaseDomainException(
                            "Bill qty " + MonetaryScale.toDisplayString(newQty) + " exceeds billable qty " + MonetaryScale.toDisplayString(maxQty)
                                    + " on PO line " + (pol.getName() != null ? pol.getName() : pol.getId()));
                }
            }
            if (moveType == VendorBillMoveType.CREDIT_NOTE && sourceBill != null) {
                UUID sourceLineId = matchSourceBillLineId(sourceBill, line);
                if (sourceLineId != null) {
                    VendorBillLine srcLine = sourceBill.getLines().stream()
                            .filter(l -> l.getId().equals(sourceLineId))
                            .findFirst()
                            .orElse(null);
                    if (srcLine != null) {
                        BigDecimal prior = alreadyCredited.getOrDefault(sourceLineId, BigDecimal.ZERO);
                        // Exclude this CN's old qty from prior (creditedQty excludes this bill already).
                        BigDecimal remaining = srcLine.getQty().subtract(prior).setScale(4, RoundingMode.HALF_UP);
                        if (newQty.compareTo(remaining) > 0) {
                            throw new PurchaseDomainException(
                                    "Credit qty " + MonetaryScale.toDisplayString(newQty) + " exceeds remaining creditable qty " + MonetaryScale.toDisplayString(remaining)
                                            + " on bill line " + srcLine.getName());
                        }
                    }
                }
            }
            line.setQty(newQty);
            line.setUnitPrice(lc.getUnitPrice().setScale(4, RoundingMode.HALF_UP));
            if (lc.getDiscountType() != null) {
                line.setDiscountType(lc.getDiscountType());
            }
            if (lc.getDiscountValue() != null) {
                line.setDiscountValue(lc.getDiscountValue());
            }
            line.setDiscountPercent(DiscountMath.effectivePercent(
                    line.getQty().multiply(line.getUnitPrice()), line.getDiscountType(), line.getDiscountValue()));
            recomputeBillLineTaxSnapshots(line);
            line.setUpdatedAt(now);
        }

        VendorBill saved = vendorBillRepository.save(bill);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, saved.getId(),
                "Vendor bill updated");
        return toBillResponse(saved);
    }

    @Override
    @Transactional
    public VendorBillResponse cancelVendorBill(UUID id) {
        VendorBill bill = vendorBillRepository.findById(id)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found: " + id));
        if (bill.getState() != VendorBillState.DRAFT) {
            throw new PurchaseDomainException(
                    "error.purchase.onlyDraftBillCancellable", null, "Only draft vendor bills can be cancelled");
        }
        bill.setState(VendorBillState.CANCELLED);
        bill.setUpdatedAt(Instant.now());
        VendorBill saved = vendorBillRepository.save(bill);
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, saved.getId(),
                "Draft", "Cancelled", "Status");
        return toBillResponse(saved);
    }

    private void recomputeBillLineTaxSnapshots(VendorBillLine line) {
        List<UUID> taxIds = line.getTaxSnapshots().stream()
                .map(VendorBillLineTax::getTaxId)
                .distinct()
                .toList();
        line.getTaxSnapshots().clear();
        if (taxIds.isEmpty()) {
            return;
        }
        List<FiscalTaxSnapshot> snaps = new ArrayList<>();
        for (UUID taxId : taxIds) {
            FiscalTax t = fiscalTaxRepository.findById(taxId)
                    .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
            snaps.add(new FiscalTaxSnapshot(t.getId(), t.getAmountType(), t.getAmount(), t.isPriceInclude()));
        }
        PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                line.getQty(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue(), snaps);
        for (Map.Entry<UUID, BigDecimal> e : split.taxAmountById().entrySet()) {
            FiscalTax t = fiscalTaxRepository.findById(e.getKey()).orElseThrow();
            VendorBillLineTax ts = new VendorBillLineTax();
            ts.setId(UUID.randomUUID());
            ts.setTaxId(t.getId());
            ts.setTaxName(t.getName());
            ts.setTaxBase(split.net());
            ts.setTaxAmount(e.getValue());
            ts.setAccountId(t.getAccountId());
            line.getTaxSnapshots().add(ts);
        }
    }

    private Map<UUID, BigDecimal> draftBillOrDebitQtyByPoLineExcludingBill(UUID purchaseOrderId, UUID excludeBillId) {
        Map<UUID, BigDecimal> allocated = new HashMap<>();
        for (VendorBill bill : vendorBillRepository.findByPurchaseOrderId(purchaseOrderId)) {
            if (bill.getState() != VendorBillState.DRAFT) {
                continue;
            }
            if (bill.getId().equals(excludeBillId)) {
                continue;
            }
            VendorBillMoveType type = bill.getMoveType() != null ? bill.getMoveType() : VendorBillMoveType.BILL;
            if (type != VendorBillMoveType.BILL) {
                continue;
            }
            bill.getLines().size();
            for (VendorBillLine line : bill.getLines()) {
                if (line.getPurchaseOrderLineId() != null) {
                    allocated.merge(line.getPurchaseOrderLineId(), line.getQty(), BigDecimal::add);
                }
            }
        }
        return allocated;
    }

    private Map<UUID, BigDecimal> creditedQtyBySourceBillLineExcluding(VendorBill source, UUID excludeCnId) {
        Map<UUID, BigDecimal> credited = new LinkedHashMap<>();
        for (VendorBill cn : vendorBillRepository.findByReversedBillId(source.getId())) {
            if (cn.getState() == VendorBillState.CANCELLED) {
                continue;
            }
            if (cn.getMoveType() != VendorBillMoveType.CREDIT_NOTE) {
                continue;
            }
            if (cn.getId().equals(excludeCnId)) {
                continue;
            }
            cn.getLines().size();
            for (VendorBillLine cnLine : cn.getLines()) {
                UUID sourceLineId = matchSourceBillLineId(source, cnLine);
                if (sourceLineId != null) {
                    credited.merge(sourceLineId, cnLine.getQty(), BigDecimal::add);
                }
            }
        }
        return credited;
    }

    @Override
    @Transactional
    public VendorBillResponse createVendorBillFromPo(CreateVendorBillFromPoCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PurchaseOrder po = loadOrder(command.getPurchaseOrderId());
        if (!po.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("error.purchase.orderCompanyMismatch", null, "Purchase order company mismatch");
        }
        if (po.getState() == PurchaseOrderState.CANCELLED) {
            throw new PurchaseDomainException("error.purchase.cannotBillCancelledOrder", null, "Cannot bill a cancelled purchase order");
        }
        if (po.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException("error.purchase.orderMustBeConfirmedBeforeBilling", null, "Purchase order must be confirmed before billing");
        }
        periodPostingGuard.assertDatePostable(companyId, command.getBillDate());
        PurchaseOrder synced = purchaseOrderQtyWriter.updateQtyReceived(po.getId());
        if (synced != null) {
            po = synced;
        }
        Instant now = Instant.now();
        VendorBill bill = new VendorBill();
        bill.setId(UUID.randomUUID());
        bill.setCompanyId(companyId);
        bill.setVendorPartnerId(po.getVendorPartnerId());
        bill.setPurchaseOrderId(po.getId());
        bill.setBillDate(command.getBillDate());
        bill.setDueDate(command.getDueDate());
        bill.setReference(command.getReference() != null && !command.getReference().isBlank()
                ? command.getReference()
                : documentSequenceService.next(companyId, "BILL"));
        bill.setCurrencyCode(po.getCurrencyCode());
        bill.setState(VendorBillState.DRAFT);
        bill.setMoveType(VendorBillMoveType.BILL);
        bill.setExchangeRateToCompany(resolveExchangeRate(
                po.getCompanyId(), po.getCurrencyCode(), command.getBillDate(), po.getExchangeRateToCompany()));
        bill.setCreatedAt(now);
        bill.setUpdatedAt(now);

        Map<UUID, BigDecimal> draftAllocated = draftBillQtyByPoLine(po.getId());
        BigDecimal orderSubtotal = orderSubtotalBeforeOrderDiscount(po);
        BigDecimal orderDiscountAmount = DiscountMath.discountAmount(
                orderSubtotal, po.getOrderDiscountType(), po.getOrderDiscountValue());
        int seq = 10;
        BigDecimal billedSubtotal = BigDecimal.ZERO;
        for (PurchaseOrderLine pol : po.getLines()) {
            Product product = productRepository.findById(new ProductId(pol.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + pol.getProductId()));
            BigDecimal qty = command.getLineQuantities() != null
                    ? nz(command.getLineQuantities().get(pol.getId()))
                            .min(nz(pol.getQtyOrdered()).subtract(effectiveQtyInvoiced(pol, draftAllocated)).max(BigDecimal.ZERO))
                    : billableQtyForLine(pol, product, draftAllocated, companyId);
            if (qty.signum() <= 0) {
                continue;
            }
            VendorBillLine vbl = new VendorBillLine();
            vbl.setId(UUID.randomUUID());
            vbl.setSequence(seq);
            vbl.setPurchaseOrderLineId(pol.getId());
            vbl.setProductId(pol.getProductId());
            vbl.setName(pol.getName());
            vbl.setUomId(pol.getUomId());
            vbl.setQty(qty);
            vbl.setUnitPrice(pol.getUnitPrice());
            applyBilledLineDiscountOnly(vbl, pol, qty);
            vbl.setAccountId(product.getProductType() == ProductType.SERVICE
                    ? resolveExpenseAccount(product)
                    : resolveStockInputAccount(product));
            vbl.setCreatedAt(now);
            vbl.setUpdatedAt(now);
            addBillTaxSnapshots(vbl, pol, now);
            bill.getLines().add(vbl);
            billedSubtotal = billedSubtotal.add(billLineNet(vbl));
            seq += 10;
        }
        if (bill.getLines().isEmpty()) {
            throw new PurchaseDomainException(
                    "No billable quantity: for stockable/consumable lines, receive goods first (qty received > qty "
                            + "invoiced); for service lines, bill from ordered quantity. "
                            + "If a draft bill already exists, post or remove it first.");
        }
        // Keep the order-level discount on the bill header (not folded into product lines).
        BigDecimal allocatedOrderDisc = BigDecimal.ZERO;
        if (orderSubtotal.signum() > 0 && orderDiscountAmount.signum() > 0 && billedSubtotal.signum() > 0) {
            allocatedOrderDisc = orderDiscountAmount.multiply(billedSubtotal)
                    .divide(orderSubtotal, 4, RoundingMode.HALF_UP)
                    .min(billedSubtotal);
        }
        bill.setOrderDiscountAmount(allocatedOrderDisc);
        // Scale tax snapshots so document tax matches the PO factor after order discount.
        if (billedSubtotal.signum() > 0 && allocatedOrderDisc.signum() > 0) {
            BigDecimal factor = billedSubtotal.subtract(allocatedOrderDisc)
                    .max(BigDecimal.ZERO)
                    .divide(billedSubtotal, 12, RoundingMode.HALF_UP);
            for (VendorBillLine line : bill.getLines()) {
                for (VendorBillLineTax ts : line.getTaxSnapshots()) {
                    ts.setTaxAmount(ts.getTaxAmount().multiply(factor).setScale(4, RoundingMode.HALF_UP));
                    ts.setTaxBase(ts.getTaxBase().multiply(factor).setScale(4, RoundingMode.HALF_UP));
                }
            }
        }
        VendorBill savedBill = vendorBillRepository.save(bill);
        activityLogger.log(savedBill.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, savedBill.getId(),
                "Vendor Bill created");
        if (savedBill.getPurchaseOrderId() != null) {
            activityLogger.log(savedBill.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER,
                    savedBill.getPurchaseOrderId(),
                    "Vendor bill created: " + (savedBill.getReference() != null ? savedBill.getReference() : savedBill.getId()));
        }
        return toBillResponse(savedBill);
    }

    private void addBillTaxSnapshots(VendorBillLine vbl, PurchaseOrderLine pol, Instant now) {
        List<FiscalTaxSnapshot> snaps = pol.getTaxes().stream()
                .map(lt -> fiscalTaxRepository.findById(lt.getTaxId()).orElseThrow())
                .map(t -> new FiscalTaxSnapshot(t.getId(), t.getAmountType(), t.getAmount(), t.isPriceInclude()))
                .toList();
        PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                vbl.getQty(), vbl.getUnitPrice(), vbl.getDiscountType(), vbl.getDiscountValue(), snaps);
        for (Map.Entry<UUID, BigDecimal> e : split.taxAmountById().entrySet()) {
            FiscalTax t = fiscalTaxRepository.findById(e.getKey()).orElseThrow();
            VendorBillLineTax ts = new VendorBillLineTax();
            ts.setId(UUID.randomUUID());
            ts.setTaxId(t.getId());
            ts.setTaxName(t.getName());
            ts.setTaxBase(split.net());
            ts.setTaxAmount(e.getValue());
            ts.setAccountId(t.getAccountId());
            vbl.getTaxSnapshots().add(ts);
        }
    }

    /** Order subtotal after line discounts but before the order-level discount. */
    private BigDecimal orderSubtotalBeforeOrderDiscount(PurchaseOrder po) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PurchaseOrderLine pol : po.getLines()) {
            subtotal = subtotal.add(PurchaseOrderRules.lineNet(
                    pol.getQtyOrdered(), pol.getUnitPrice(), pol.getDiscountType(), pol.getDiscountValue()));
        }
        return subtotal;
    }

    /**
     * Carries only the purchase-order LINE discount (prorated by billed qty). Order-level discount
     * is stored on {@link VendorBill#getOrderDiscountAmount()} so product lines stay clean.
     */
    private void applyBilledLineDiscountOnly(VendorBillLine vbl,
                                             PurchaseOrderLine pol,
                                             BigDecimal billedQty) {
        BigDecimal billedGross = billedQty.multiply(pol.getUnitPrice());
        BigDecimal orderedGross = pol.getQtyOrdered().multiply(pol.getUnitPrice());
        BigDecimal orderedNet = PurchaseOrderRules.lineNet(
                pol.getQtyOrdered(), pol.getUnitPrice(), pol.getDiscountType(), pol.getDiscountValue());
        BigDecimal lineDiscOnOrder = orderedGross.subtract(orderedNet).max(BigDecimal.ZERO);
        BigDecimal amount = orderedGross.signum() > 0
                ? lineDiscOnOrder.multiply(billedGross).divide(orderedGross, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vbl.setDiscountType(DiscountType.FIXED);
        vbl.setDiscountValue(amount);
        vbl.setDiscountPercent(DiscountMath.effectivePercent(billedGross, DiscountType.FIXED, amount));
    }

    private static BigDecimal billLineGross(VendorBillLine line) {
        if (line.getQty() == null || line.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return line.getQty().multiply(line.getUnitPrice()).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal billLineDiscount(VendorBillLine line) {
        return DiscountMath.discountAmount(billLineGross(line), line.getDiscountType(), line.getDiscountValue())
                .setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal billLineNet(VendorBillLine line) {
        return PurchaseOrderRules
                .lineNet(line.getQty(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue())
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal billTotalDocumentCurrency(VendorBill bill) {
        BigDecimal total = BigDecimal.ZERO;
        for (VendorBillLine line : bill.getLines()) {
            total = total.add(billLineNet(line));
            for (VendorBillLineTax ts : line.getTaxSnapshots()) {
                total = total.add(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }
        BigDecimal orderDisc = bill.getOrderDiscountAmount() != null
                ? bill.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        return total.subtract(orderDisc).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPostedPaymentsForBill(UUID billId, String billCurrency) {
        return vendorPaymentService.sumActiveAllocationsByBillIds(List.of(billId))
                .getOrDefault(billId, BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private UUID resolveStockInputAccount(Product product) {
        ProductCategory cat = product.getCategoryId() != null
                ? categoryRepository.findById(product.getCategoryId()).orElse(null)
                : null;
        if (cat != null && cat.getStockInputAccountId() != null) {
            return cat.getStockInputAccountId();
        }
        throw new PurchaseDomainException("error.purchase.categoryNoStockInputAccount", null, "Product category has no stock input account");
    }

    private UUID resolveExpenseAccount(Product product) {
        ProductCategory cat = product.getCategoryId() != null
                ? categoryRepository.findById(product.getCategoryId()).orElse(null)
                : null;
        if (cat != null && cat.getCogsAccountId() != null) {
            return cat.getCogsAccountId();
        }
        return resolveStockInputAccount(product);
    }

    @Override
    @Transactional
    public VendorBillResponse createCreditNoteFromVendorBill(UUID billId, CreateCreditNoteFromVendorBillCommand command) {
        VendorBill source = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found: " + billId));
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        if (!source.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("error.purchase.billCompanyMismatch", null, "Bill company mismatch");
        }
        if (source.getState() != VendorBillState.POSTED) {
            throw new PurchaseDomainException("error.purchase.onlyPostedBillCanBeCredited", null, "Only posted vendor bills can be credited");
        }
        if (source.getMoveType() == VendorBillMoveType.CREDIT_NOTE) {
            throw new PurchaseDomainException("error.purchase.cannotCreditNoteFromCreditNote", null, "Cannot create a credit note from another credit note");
        }
        periodPostingGuard.assertDatePostable(companyId, command.getBillDate());
        source.getLines().size();
        for (VendorBillLine line : source.getLines()) {
            line.getTaxSnapshots().size();
        }

        Map<UUID, BigDecimal> qtyByLineId = new LinkedHashMap<>();
        if (command.getLines() == null || command.getLines().isEmpty()) {
            for (VendorBillLine line : source.getLines()) {
                qtyByLineId.put(line.getId(), line.getQty());
            }
        } else {
            for (CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand lc : command.getLines()) {
                qtyByLineId.merge(lc.getBillLineId(), lc.getQty(), BigDecimal::add);
            }
        }

        Map<UUID, BigDecimal> alreadyCredited = creditedQtyBySourceBillLine(source);
        for (VendorBillLine srcLine : source.getLines()) {
            BigDecimal qty = qtyByLineId.get(srcLine.getId());
            if (qty == null || qty.signum() <= 0) {
                continue;
            }
            BigDecimal prior = alreadyCredited.getOrDefault(srcLine.getId(), BigDecimal.ZERO);
            BigDecimal remaining = srcLine.getQty().subtract(prior).setScale(4, RoundingMode.HALF_UP);
            if (qty.compareTo(remaining) > 0) {
                throw new PurchaseDomainException(
                        "Credit qty " + MonetaryScale.toDisplayString(qty) + " exceeds remaining creditable qty " + MonetaryScale.toDisplayString(remaining)
                                + " on bill line " + srcLine.getName()
                                + " (already credited " + MonetaryScale.toDisplayString(prior) + " of " + MonetaryScale.toDisplayString(srcLine.getQty()) + ")");
            }
        }

        Instant now = Instant.now();
        VendorBill cn = new VendorBill();
        cn.setId(UUID.randomUUID());
        cn.setCompanyId(companyId);
        cn.setVendorPartnerId(source.getVendorPartnerId());
        cn.setPurchaseOrderId(source.getPurchaseOrderId());
        cn.setBillDate(command.getBillDate());
        cn.setDueDate(command.getDueDate());
        cn.setReference(command.getReference() != null ? command.getReference()
                : "CN/" + (source.getReference() != null ? source.getReference() : source.getId()));
        cn.setCurrencyCode(source.getCurrencyCode());
        cn.setState(VendorBillState.DRAFT);
        cn.setMoveType(VendorBillMoveType.CREDIT_NOTE);
        cn.setReversedBillId(source.getId());
        cn.setExchangeRateToCompany(source.getExchangeRateToCompany());
        cn.setCreatedAt(now);
        cn.setUpdatedAt(now);
        cn.setRowVersion(0L);

        int seq = 0;
        for (VendorBillLine srcLine : source.getLines()) {
            BigDecimal qty = qtyByLineId.get(srcLine.getId());
            if (qty == null || qty.signum() <= 0) {
                continue;
            }
            BigDecimal ratio = qty.divide(srcLine.getQty(), 8, RoundingMode.HALF_UP);
            VendorBillLine line = new VendorBillLine();
            line.setId(UUID.randomUUID());
            line.setSequence(++seq);
            line.setPurchaseOrderLineId(srcLine.getPurchaseOrderLineId());
            line.setProductId(srcLine.getProductId());
            line.setName(srcLine.getName());
            line.setUomId(srcLine.getUomId());
            line.setQty(qty.setScale(4, RoundingMode.HALF_UP));
            line.setUnitPrice(srcLine.getUnitPrice());
            line.setDiscountType(srcLine.getDiscountType());
            // A fixed discount covers the whole source line, so scale it to the credited quantity.
            line.setDiscountValue(srcLine.getDiscountType() == DiscountType.FIXED
                    ? srcLine.getDiscountValue().multiply(ratio).setScale(4, RoundingMode.HALF_UP)
                    : srcLine.getDiscountValue());
            line.setDiscountPercent(DiscountMath.effectivePercent(
                    line.getQty().multiply(line.getUnitPrice()), line.getDiscountType(), line.getDiscountValue()));
            line.setAccountId(srcLine.getAccountId());
            line.setPriceVariance(nz(srcLine.getPriceVariance()).multiply(ratio).setScale(4, RoundingMode.HALF_UP));
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            for (VendorBillLineTax tax : srcLine.getTaxSnapshots()) {
                VendorBillLineTax ts = new VendorBillLineTax();
                ts.setId(UUID.randomUUID());
                ts.setTaxId(tax.getTaxId());
                ts.setTaxName(tax.getTaxName());
                ts.setTaxBase(tax.getTaxBase().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                ts.setTaxAmount(tax.getTaxAmount().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                ts.setAccountId(tax.getAccountId());
                line.getTaxSnapshots().add(ts);
            }
            cn.getLines().add(line);
        }
        if (cn.getLines().isEmpty()) {
            throw new PurchaseDomainException("error.purchase.creditNoteNoLines", null, "Credit note has no lines");
        }
        // Prorate the source bill's order-level discount onto the credit note.
        BigDecimal sourceSubtotal = BigDecimal.ZERO;
        for (VendorBillLine srcLine : source.getLines()) {
            sourceSubtotal = sourceSubtotal.add(billLineNet(srcLine));
        }
        BigDecimal cnSubtotal = BigDecimal.ZERO;
        for (VendorBillLine cnLine : cn.getLines()) {
            cnSubtotal = cnSubtotal.add(billLineNet(cnLine));
        }
        BigDecimal sourceOrderDisc = source.getOrderDiscountAmount() != null
                ? source.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (sourceSubtotal.signum() > 0 && sourceOrderDisc.signum() > 0 && cnSubtotal.signum() > 0) {
            cn.setOrderDiscountAmount(sourceOrderDisc.multiply(cnSubtotal)
                    .divide(sourceSubtotal, 4, RoundingMode.HALF_UP)
                    .min(cnSubtotal));
        }
        return toBillResponse(vendorBillRepository.save(cn));
    }

    @Override
    @Transactional
    public VendorBillResponse createDebitNoteFromVendorBill(UUID billId, CreateDebitNoteFromVendorBillCommand command) {
        VendorBill source = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found: " + billId));
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        if (!source.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("error.purchase.billCompanyMismatch", null, "Bill company mismatch");
        }
        if (source.getState() != VendorBillState.POSTED) {
            throw new PurchaseDomainException(
                    "error.purchase.onlyPostedBillCanBeDebited", null, "Only posted vendor bills can be debit-noted");
        }
        VendorBillMoveType sourceType = source.getMoveType() != null ? source.getMoveType() : VendorBillMoveType.BILL;
        if (sourceType != VendorBillMoveType.BILL) {
            throw new PurchaseDomainException(
                    "error.purchase.debitNoteOnlyFromBill", null, "Debit notes can only be created from a posted vendor bill");
        }
        periodPostingGuard.assertDatePostable(companyId, command.getBillDate());
        source.getLines().size();
        for (VendorBillLine line : source.getLines()) {
            line.getTaxSnapshots().size();
        }

        Map<UUID, BigDecimal> qtyByLineId = new LinkedHashMap<>();
        Map<UUID, BigDecimal> unitPriceOverride = new HashMap<>();
        if (command.getLines() == null || command.getLines().isEmpty()) {
            for (VendorBillLine line : source.getLines()) {
                qtyByLineId.put(line.getId(), line.getQty());
            }
        } else {
            for (CreateDebitNoteFromVendorBillCommand.DebitNoteLineQtyCommand lc : command.getLines()) {
                qtyByLineId.merge(lc.getBillLineId(), lc.getQty(), BigDecimal::add);
                if (lc.getUnitPrice() != null) {
                    unitPriceOverride.put(lc.getBillLineId(), lc.getUnitPrice());
                }
            }
        }

        Instant now = Instant.now();
        VendorBill dn = new VendorBill();
        dn.setId(UUID.randomUUID());
        dn.setCompanyId(companyId);
        dn.setVendorPartnerId(source.getVendorPartnerId());
        dn.setPurchaseOrderId(source.getPurchaseOrderId());
        dn.setBillDate(command.getBillDate());
        dn.setDueDate(command.getDueDate());
        dn.setReference(command.getReference() != null ? command.getReference()
                : "DN/" + (source.getReference() != null ? source.getReference() : source.getId()));
        dn.setCurrencyCode(source.getCurrencyCode());
        dn.setState(VendorBillState.DRAFT);
        dn.setMoveType(VendorBillMoveType.DEBIT_NOTE);
        dn.setReversedBillId(source.getId());
        dn.setExchangeRateToCompany(source.getExchangeRateToCompany());
        dn.setCreatedAt(now);
        dn.setUpdatedAt(now);
        dn.setRowVersion(0L);

        int seq = 0;
        for (VendorBillLine srcLine : source.getLines()) {
            BigDecimal qty = qtyByLineId.get(srcLine.getId());
            if (qty == null || qty.signum() <= 0) {
                continue;
            }
            BigDecimal ratio = qty.divide(srcLine.getQty(), 8, RoundingMode.HALF_UP);
            VendorBillLine line = new VendorBillLine();
            line.setId(UUID.randomUUID());
            line.setSequence(++seq);
            // Keep product context but do not link PO qty — debit notes are amount adjustments.
            line.setPurchaseOrderLineId(null);
            line.setProductId(srcLine.getProductId());
            line.setName(srcLine.getName());
            line.setUomId(srcLine.getUomId());
            line.setQty(qty.setScale(4, RoundingMode.HALF_UP));
            BigDecimal unitPrice = unitPriceOverride.getOrDefault(srcLine.getId(), srcLine.getUnitPrice());
            line.setUnitPrice(unitPrice);
            line.setDiscountType(srcLine.getDiscountType());
            line.setDiscountValue(srcLine.getDiscountType() == DiscountType.FIXED
                    ? srcLine.getDiscountValue().multiply(ratio).setScale(4, RoundingMode.HALF_UP)
                    : srcLine.getDiscountValue());
            line.setDiscountPercent(DiscountMath.effectivePercent(
                    line.getQty().multiply(line.getUnitPrice()), line.getDiscountType(), line.getDiscountValue()));
            line.setAccountId(srcLine.getAccountId());
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            boolean priceOverride = unitPriceOverride.containsKey(srcLine.getId())
                    || unitPrice.compareTo(srcLine.getUnitPrice()) != 0;
            if (priceOverride) {
                for (VendorBillLineTax tax : srcLine.getTaxSnapshots()) {
                    VendorBillLineTax placeholder = new VendorBillLineTax();
                    placeholder.setTaxId(tax.getTaxId());
                    line.getTaxSnapshots().add(placeholder);
                }
                recomputeBillLineTaxSnapshots(line);
            } else {
                for (VendorBillLineTax tax : srcLine.getTaxSnapshots()) {
                    VendorBillLineTax ts = new VendorBillLineTax();
                    ts.setId(UUID.randomUUID());
                    ts.setTaxId(tax.getTaxId());
                    ts.setTaxName(tax.getTaxName());
                    ts.setTaxBase(tax.getTaxBase().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                    ts.setTaxAmount(tax.getTaxAmount().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                    ts.setAccountId(tax.getAccountId());
                    line.getTaxSnapshots().add(ts);
                }
            }
            dn.getLines().add(line);
        }
        if (dn.getLines().isEmpty()) {
            throw new PurchaseDomainException("error.purchase.debitNoteNoLines", null, "Debit note has no lines");
        }
        BigDecimal sourceSubtotal = BigDecimal.ZERO;
        for (VendorBillLine srcLine : source.getLines()) {
            sourceSubtotal = sourceSubtotal.add(billLineNet(srcLine));
        }
        BigDecimal dnSubtotal = BigDecimal.ZERO;
        for (VendorBillLine dnLine : dn.getLines()) {
            dnSubtotal = dnSubtotal.add(billLineNet(dnLine));
        }
        BigDecimal sourceOrderDisc = source.getOrderDiscountAmount() != null
                ? source.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (sourceSubtotal.signum() > 0 && sourceOrderDisc.signum() > 0 && dnSubtotal.signum() > 0) {
            dn.setOrderDiscountAmount(sourceOrderDisc.multiply(dnSubtotal)
                    .divide(sourceSubtotal, 4, RoundingMode.HALF_UP)
                    .min(dnSubtotal));
        }
        return toBillResponse(vendorBillRepository.save(dn));
    }

    /**
     * What goods of an order were received at, so a bill that pays a different price books the
     * difference as price variance instead of leaving it on Stock Input (GR/IR).
     */
    private final class PriceVarianceContext {
        private final Map<UUID, PurchaseOrderLine> lines = new LinkedHashMap<>();
        private final Map<UUID, BigDecimal> avgStockCost = new LinkedHashMap<>();
        private final Map<UUID, BigDecimal> billedSoFar = new LinkedHashMap<>();

        PriceVarianceContext(PurchaseOrder po) {
            Map<UUID, BigDecimal[]> acc = new LinkedHashMap<>();
            for (PurchaseOrderLine l : po.getLines()) {
                lines.put(l.getId(), l);
                billedSoFar.put(l.getId(), nz(l.getQtyInvoiced()));
            }
            for (UUID pid : stockMovePurchaseQueryPort.findPickingIdsByPurchaseOrderId(po.getId())) {
                StockPickingResponse p = stockPickingApplicationService.getPicking(pid);
                if (p.getState() != PickingState.DONE || p.getPickingType() != PickingType.INCOMING) {
                    continue;
                }
                for (StockPickingResponse.MoveResponse m : p.getMoves()) {
                    if (m.getPurchaseOrderLineId() == null || m.getState() != MoveState.DONE) {
                        continue;
                    }
                    BigDecimal q = nz(m.getPickedQuantity());
                    BigDecimal[] a = acc.computeIfAbsent(m.getPurchaseOrderLineId(),
                            k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
                    a[0] = a[0].add(q.multiply(nz(m.getUnitCost())));
                    a[1] = a[1].add(q);
                }
            }
            acc.forEach((k, a) -> {
                if (a[1].signum() > 0) {
                    avgStockCost.put(k, a[0].divide(a[1], 8, RoundingMode.HALF_UP));
                }
            });
        }

        /** Bill net (company currency) minus the receipt cost of the received, not yet billed part. */
        BigDecimal varianceFor(VendorBillLine line, BigDecimal netComp) {
            PurchaseOrderLine pol = line.getPurchaseOrderLineId() != null ? lines.get(line.getPurchaseOrderLineId()) : null;
            if (pol == null || nz(line.getQty()).signum() <= 0) {
                return BigDecimal.ZERO;
            }
            Product product = productRepository.findById(new ProductId(pol.getProductId())).orElse(null);
            if (product == null || product.getProductType() == ProductType.SERVICE) {
                return BigDecimal.ZERO;
            }
            BigDecimal before = billedSoFar.get(pol.getId());
            billedSoFar.put(pol.getId(), before.add(line.getQty()));
            BigDecimal covered = line.getQty().min(nz(pol.getQtyReceived()).subtract(before).max(BigDecimal.ZERO));
            BigDecimal avg = avgStockCost.get(pol.getId());
            if (covered.signum() <= 0 || avg == null) {
                return BigDecimal.ZERO;
            }
            BigDecimal perOrderUnit = avg.multiply(toStockUomQty(pol, product, BigDecimal.ONE));
            BigDecimal billed = netComp.multiply(covered).divide(line.getQty(), 8, RoundingMode.HALF_UP);
            BigDecimal variance = billed.subtract(perOrderUnit.multiply(covered)).setScale(4, RoundingMode.HALF_UP);
            return variance.abs().compareTo(new BigDecimal("0.01")) < 0 ? BigDecimal.ZERO : variance;
        }
    }

    private PriceVarianceContext priceVarianceContext(VendorBill bill) {
        if (bill.getPurchaseOrderId() == null || bill.getMoveType() == VendorBillMoveType.DEBIT_NOTE
                || bill.isOpeningBalance()) {
            return null;
        }
        return new PriceVarianceContext(loadOrder(bill.getPurchaseOrderId()));
    }

    @Override
    @Transactional
    public VendorBillResponse postVendorBill(UUID billId) {
        VendorBill bill = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found: " + billId));
        if (bill.getState() == VendorBillState.POSTED) {
            return toBillResponse(bill);
        }
        if (bill.getState() == VendorBillState.CANCELLED) {
            throw new PurchaseDomainException("error.purchase.cannotPostCancelledBill", null, "Cannot post a cancelled bill");
        }
        assertBillFitsOrder(bill);
        PartnerResponse vendor = partnerApplicationService.getPartner(bill.getVendorPartnerId());
        UUID payableAccount = vendor.getPayableAccountId() != null
                ? vendor.getPayableAccountId()
                : accountingReferenceLookupPort.resolveAccountIdByCode(bill.getCompanyId(), DEFAULT_AP_ACCOUNT_CODE);

        UUID purchaseJournalId = accountingReferenceLookupPort.resolveJournalIdByType(
                bill.getCompanyId(), JournalType.PURCHASE);

        BigDecimal rate = resolveExchangeRate(
                bill.getCompanyId(), bill.getCurrencyCode(), bill.getBillDate(), bill.getExchangeRateToCompany());
        bill.setExchangeRateToCompany(rate);
        boolean creditNote = bill.getMoveType() == VendorBillMoveType.CREDIT_NOTE;
        List<JournalItemCommand> items = new ArrayList<>();
        BigDecimal apCreditCompany = BigDecimal.ZERO;
        BigDecimal apDocTotal = BigDecimal.ZERO;
        UUID purchaseDiscountAccount = accountingReferenceLookupPort
                .resolveAccountIdByCode(bill.getCompanyId(), PURCHASE_DISCOUNT_ACCOUNT_CODE);
        PriceVarianceContext varianceCtx = creditNote ? null : priceVarianceContext(bill);
        UUID varianceAccount = null;

        for (VendorBillLine line : bill.getLines()) {
            BigDecimal grossDoc = billLineGross(line);
            BigDecimal grossComp = PurchaseTaxEngine.convertAtRate(grossDoc, rate);
            BigDecimal discDoc = billLineDiscount(line);
            BigDecimal discComp = PurchaseTaxEngine.convertAtRate(discDoc, rate);
            BigDecimal lineNetDoc = billLineNet(line);
            BigDecimal netComp = PurchaseTaxEngine.convertAtRate(lineNetDoc, rate);
            if (varianceCtx != null) {
                line.setPriceVariance(varianceCtx.varianceFor(line, netComp));
            }
            if (line.getPriceVariance().signum() != 0) {
                if (varianceAccount == null) {
                    varianceAccount = accountingReferenceLookupPort
                            .resolveAccountIdByCode(bill.getCompanyId(), PURCHASE_PRICE_VARIANCE_ACCOUNT_CODE);
                }
                // A bill pays more (or less) than the goods were received at: the difference leaves
                // Stock Input so it clears, and sits on the variance account. A credit note reverses it.
                BigDecimal move = creditNote ? line.getPriceVariance().negate() : line.getPriceVariance();
                BigDecimal amt = move.abs();
                items.add(new JournalItemCommand(line.getAccountId(), "Price variance: " + line.getName(),
                        move.signum() < 0 ? amt : BigDecimal.ZERO, move.signum() > 0 ? amt : BigDecimal.ZERO,
                        null, null));
                items.add(new JournalItemCommand(varianceAccount, "Price variance: " + line.getName(),
                        move.signum() > 0 ? amt : BigDecimal.ZERO, move.signum() < 0 ? amt : BigDecimal.ZERO,
                        null, null));
            }

            if (grossComp.signum() > 0) {
                if (creditNote) {
                    items.add(new JournalItemCommand(line.getAccountId(), line.getName(), BigDecimal.ZERO, grossComp,
                            bill.getCurrencyCode(), grossDoc.negate(), null));
                } else {
                    items.add(new JournalItemCommand(line.getAccountId(), line.getName(), grossComp, BigDecimal.ZERO,
                            bill.getCurrencyCode(), grossDoc, null));
                }
            }
            if (discComp.signum() > 0) {
                if (creditNote) {
                    items.add(new JournalItemCommand(purchaseDiscountAccount, "Discount: " + line.getName(), discComp, BigDecimal.ZERO,
                            bill.getCurrencyCode(), discDoc, null));
                } else {
                    items.add(new JournalItemCommand(purchaseDiscountAccount, "Discount: " + line.getName(), BigDecimal.ZERO, discComp,
                            bill.getCurrencyCode(), discDoc.negate(), null));
                }
            }
            if (netComp.signum() > 0) {
                apCreditCompany = apCreditCompany.add(netComp);
                apDocTotal = apDocTotal.add(lineNetDoc);
            }
            for (VendorBillLineTax ts : line.getTaxSnapshots()) {
                BigDecimal taxDoc = ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP);
                BigDecimal taxComp = PurchaseTaxEngine.convertAtRate(taxDoc, rate);
                if (taxComp.signum() <= 0) {
                    continue;
                }
                if (creditNote) {
                    items.add(new JournalItemCommand(ts.getAccountId(), ts.getTaxName(), BigDecimal.ZERO, taxComp,
                            bill.getCurrencyCode(), taxDoc.negate(), null));
                } else {
                    items.add(new JournalItemCommand(ts.getAccountId(), ts.getTaxName(), taxComp, BigDecimal.ZERO,
                            bill.getCurrencyCode(), taxDoc, null));
                }
                apCreditCompany = apCreditCompany.add(taxComp);
                apDocTotal = apDocTotal.add(taxDoc);
            }
        }
        BigDecimal orderDiscDoc = bill.getOrderDiscountAmount() != null
                ? bill.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (orderDiscDoc.signum() > 0) {
            BigDecimal untaxedDoc = BigDecimal.ZERO;
            for (VendorBillLine line : bill.getLines()) {
                untaxedDoc = untaxedDoc.add(billLineNet(line));
            }
            BigDecimal discDoc = orderDiscDoc.min(untaxedDoc);
            BigDecimal discComp = PurchaseTaxEngine.convertAtRate(discDoc, rate);
            if (discComp.signum() > 0) {
                if (creditNote) {
                    items.add(new JournalItemCommand(purchaseDiscountAccount, "Order discount", discComp, BigDecimal.ZERO,
                            bill.getCurrencyCode(), discDoc, null));
                } else {
                    items.add(new JournalItemCommand(purchaseDiscountAccount, "Order discount", BigDecimal.ZERO, discComp,
                            bill.getCurrencyCode(), discDoc.negate(), null));
                }
                apCreditCompany = apCreditCompany.subtract(discComp).max(BigDecimal.ZERO);
                apDocTotal = apDocTotal.subtract(discDoc).max(BigDecimal.ZERO);
            }
        }
        if (apCreditCompany.signum() > 0) {
            if (creditNote) {
                items.add(new JournalItemCommand(payableAccount, "Accounts payable", apCreditCompany, BigDecimal.ZERO,
                        bill.getCurrencyCode(), apDocTotal, bill.getVendorPartnerId()));
            } else {
                items.add(new JournalItemCommand(payableAccount, "Accounts payable", BigDecimal.ZERO, apCreditCompany,
                        bill.getCurrencyCode(), apDocTotal.negate(), bill.getVendorPartnerId()));
            }
        }

        if (!items.isEmpty()) {
            CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                    bill.getCompanyId(),
                    purchaseJournalId,
                    "",
                    JournalEntryTiming.ofBusinessDate(bill.getBillDate()),
                    bill.getCurrencyCode(),
                    bill.getVendorPartnerId(),
                    items);
            CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(jcmd);
            journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());
            bill.setJournalEntryId(created.getJournalEntryId());
        } else {
            bill.setJournalEntryId(null);
        }

        bill.setState(VendorBillState.POSTED);
        bill.setUpdatedAt(Instant.now());
        vendorBillRepository.save(bill);

        // Debit notes are amount adjustments (Odoo-style); they must not change PO qty invoiced.
        if (bill.getPurchaseOrderId() != null && bill.getMoveType() != VendorBillMoveType.DEBIT_NOTE) {
            List<PurchaseOrderQtyWriter.PostedBillLineQty> qtyLines = bill.getLines().stream()
                    .map(l -> new PurchaseOrderQtyWriter.PostedBillLineQty(l.getPurchaseOrderLineId(), l.getQty()))
                    .toList();
            purchaseOrderQtyWriter.applyPostedBillQuantities(bill.getPurchaseOrderId(), creditNote, qtyLines);
        }
        purchaseEventPublisher.publishVendorBillPosted(new VendorBillPostedEvent(
                UUID.randomUUID(),
                Instant.now(),
                bill.getCompanyId(),
                bill.getId(),
                bill.getVendorPartnerId()));
        activityLogger.logFieldChange(bill.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, bill.getId(),
                "Draft", "Posted", "Status");
        if (bill.getPurchaseOrderId() != null) {
            activityLogger.log(bill.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, bill.getPurchaseOrderId(),
                    "Vendor bill posted: " + (bill.getReference() != null ? bill.getReference() : bill.getId()));
        }
        return toBillResponse(bill);
    }

    @Override
    @Transactional
    public VendorBillResponse createOpeningVendorBill(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                                      LocalDate date, LocalDate dueDate, String reference,
                                                      UUID openingJournalId, UUID openingEquityAccountId) {
        if (amount == null || amount.signum() <= 0) {
            throw new PurchaseDomainException("error.purchase.openingAmountPositive", null,
                    "Opening bill amount must be positive");
        }
        PartnerResponse vendor = partnerApplicationService.getPartner(partnerId);
        if (!vendor.isVendor()) {
            throw new PurchaseDomainException("error.purchase.partnerNotVendor", null, "Partner is not a vendor");
        }
        if (!companyId.equals(vendor.getCompanyId())) {
            throw new PurchaseDomainException("error.purchase.partnerCompanyMismatch", null, "Vendor belongs to another company");
        }
        periodPostingGuard.assertDatePostable(companyId, date);
        String currencyCode = currency != null ? currency.trim().toUpperCase() : null;
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new PurchaseDomainException("error.purchase.paymentCurrencyRequired", null, "Currency is required");
        }
        UUID unitUomId = resolveReferenceUnitUom(companyId);
        BigDecimal scaled = amount.setScale(4, RoundingMode.HALF_UP);
        BigDecimal rate = resolveExchangeRate(companyId, currencyCode, date, null);
        BigDecimal companyAmount = PurchaseTaxEngine.convertAtRate(scaled, rate);

        Instant now = Instant.now();
        VendorBill bill = new VendorBill();
        bill.setId(UUID.randomUUID());
        bill.setCompanyId(companyId);
        bill.setVendorPartnerId(partnerId);
        bill.setPurchaseOrderId(null);
        bill.setBillDate(date);
        bill.setDueDate(dueDate != null ? dueDate : date);
        bill.setReference(reference != null && !reference.isBlank()
                ? reference.trim()
                : documentSequenceService.next(companyId, "OB-BILL"));
        bill.setCurrencyCode(currencyCode);
        bill.setState(VendorBillState.DRAFT);
        bill.setMoveType(VendorBillMoveType.BILL);
        bill.setExchangeRateToCompany(rate);
        bill.setOrderDiscountAmount(BigDecimal.ZERO);
        bill.setOpeningBalance(true);
        bill.setCreatedAt(now);
        bill.setUpdatedAt(now);
        bill.setRowVersion(0L);

        VendorBillLine line = new VendorBillLine();
        line.setId(UUID.randomUUID());
        line.setSequence(10);
        line.setProductId(null);
        line.setName("Opening balance");
        line.setUomId(unitUomId);
        line.setQty(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        line.setUnitPrice(scaled);
        line.setDiscountType(DiscountType.PERCENT);
        line.setDiscountValue(BigDecimal.ZERO);
        line.setDiscountPercent(BigDecimal.ZERO);
        line.setAccountId(openingEquityAccountId);
        line.setCreatedAt(now);
        line.setUpdatedAt(now);
        bill.getLines().add(line);

        UUID payableAccount = vendor.getPayableAccountId() != null
                ? vendor.getPayableAccountId()
                : accountingReferenceLookupPort.resolveAccountIdByCode(companyId, DEFAULT_AP_ACCOUNT_CODE);

        List<JournalItemCommand> items = List.of(
                new JournalItemCommand(openingEquityAccountId, "Opening balance", companyAmount, BigDecimal.ZERO,
                        currencyCode, scaled, null),
                new JournalItemCommand(payableAccount, "Accounts payable", BigDecimal.ZERO, companyAmount,
                        currencyCode, scaled.negate(), partnerId));
        CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(
                new CreateJournalEntryCommand(companyId, openingJournalId, "",
                        JournalEntryTiming.ofBusinessDate(date), currencyCode, partnerId, items));
        journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());

        bill.setJournalEntryId(created.getJournalEntryId());
        bill.setState(VendorBillState.POSTED);
        bill.setUpdatedAt(Instant.now());
        VendorBill saved = vendorBillRepository.save(bill);
        vendorPaymentService.syncForBills(List.of(saved.getId()));
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, saved.getId(),
                "Opening balance bill posted");
        return toBillResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOpeningVendorBill(UUID billId) {
        VendorBill bill = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found: " + billId));
        if (!bill.isOpeningBalance()) {
            throw new PurchaseDomainException("error.purchase.notOpeningBill", null,
                    "Bill is not an opening balance document");
        }
        if (bill.getState() == VendorBillState.CANCELLED) {
            return;
        }
        if (bill.getState() != VendorBillState.POSTED) {
            throw new PurchaseDomainException("error.purchase.openingBillNotPosted", null,
                    "Only posted opening bills can be cancelled");
        }
        if (bill.getJournalEntryId() != null) {
            journalEntryApplicationService.reverseJournalEntry(
                    new com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand(
                            bill.getJournalEntryId(), "Opening balances replaced"));
        }
        bill.setState(VendorBillState.CANCELLED);
        bill.setUpdatedAt(Instant.now());
        vendorBillRepository.save(bill);
        vendorPaymentService.syncForBills(List.of(bill.getId()));
        activityLogger.log(bill.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, bill.getId(),
                "Opening balance bill cancelled");
    }

    @Override
    @Transactional
    public VendorPaymentResponse postOpeningVendorPayment(UUID companyId, UUID partnerId, BigDecimal amount,
                                                          String currency, LocalDate date, String reference,
                                                          UUID openingJournalId, UUID openingEquityAccountId) {
        VendorPayment payment = vendorPaymentService.postOpeningPayment(
                companyId, partnerId, amount, currency, date, reference, openingJournalId, openingEquityAccountId);
        return vendorPaymentService.get(payment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorBillResponse> listOpeningVendorBills(UUID companyId) {
        return vendorBillRepository.findOpeningBalanceByCompanyId(companyId).stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .map(this::toBillResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorPaymentResponse> listOpeningVendorPayments(UUID companyId) {
        return vendorPaymentRepository.findOpeningBalanceByCompanyId(companyId).stream()
                .filter(p -> p.getState() == VendorPaymentState.POSTED)
                .map(p -> vendorPaymentService.get(p.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveVendorAllocations(java.util.Collection<UUID> billIds,
                                              java.util.Collection<UUID> paymentIds) {
        return vendorPaymentService.hasActiveAllocations(billIds, paymentIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasVendorCreditNotes(java.util.Collection<UUID> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return false;
        }
        return vendorBillRepository.findByReversedBillIdIn(billIds).stream()
                .anyMatch(cn -> cn.getState() != VendorBillState.CANCELLED
                        && cn.getMoveType() == VendorBillMoveType.CREDIT_NOTE);
    }

    private UUID resolveReferenceUnitUom(UUID companyId) {
        for (var category : uomApplicationService.listUomCategories(new CompanyId(companyId), false)) {
            for (var uom : uomApplicationService.listUomsByCategory(category.getId(), false)) {
                if ("Unit".equalsIgnoreCase(uom.getName())) {
                    return uom.getId();
                }
            }
        }
        throw new PurchaseDomainException("error.purchase.unitUomMissing", null,
                "Company reference UoM \"Unit\" not found; run ERP bootstrap first.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorBillResponse> listCreditNotesForBill(UUID billId) {
        vendorBillRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found"));
        return vendorBillRepository.findByReversedBillId(billId).stream()
                .map(b -> {
                    b.getLines().size();
                    for (VendorBillLine line : b.getLines()) {
                        line.getTaxSnapshots().size();
                    }
                    return toBillResponse(b);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VendorBillResponse getVendorBill(UUID billId) {
        VendorBill bill = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found"));
        UUID cid = companyContextProvider.getObject().requireCompany().getId();
        if (!bill.getCompanyId().equals(cid)) {
            throw new PurchaseDomainException("error.purchase.vendorBillNotFound", null, "Vendor bill not found");
        }
        bill.getLines().size();
        for (VendorBillLine line : bill.getLines()) {
            line.getTaxSnapshots().size();
        }
        return toBillResponse(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorBillSummaryResponse> listVendorBills(UUID companyId) {
        UUID cid = companyIdOrDefault(companyId);
        return vendorBillRepository.findByCompanyIdOrderByBillDateDescCreatedAtDesc(cid).stream()
                .map(b -> {
                    b.getLines().size();
                    for (VendorBillLine line : b.getLines()) {
                        line.getTaxSnapshots().size();
                    }
                    return toBillSummaryResponse(b);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorBillSummaryResponse> searchVendorBills(UUID companyId, Pageable pageable) {
        UUID cid = companyIdOrDefault(companyId);
        return vendorBillRepository.searchByCompanyId(cid, pageable).map(b -> {
            b.getLines().size();
            for (VendorBillLine line : b.getLines()) {
                line.getTaxSnapshots().size();
            }
            return toBillSummaryResponse(b);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorPaymentResponse> searchVendorPayments(UUID companyId, Pageable pageable) {
        return vendorPaymentService.search(companyIdOrDefault(companyId), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorPaymentResponse> listVendorPayments(UUID companyId) {
        return vendorPaymentService.list(companyIdOrDefault(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public VendorPaymentResponse getVendorPayment(UUID paymentId) {
        return vendorPaymentService.get(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerStatementSectionResponse> payableStatement(UUID companyId,
                                                            UUID partnerId,
                                                            LocalDate from,
                                                            LocalDate to) {
        UUID cid = companyIdOrDefault(companyId);
        if (to.isBefore(from)) {
            throw new PurchaseDomainException("error.purchase.statementEndOnOrAfterStart", null, "Statement end date must be on or after start date");
        }
        PartnerResponse partner = partnerApplicationService.getPartner(partnerId);
        if (!partner.getCompanyId().equals(cid)) {
            throw new PurchaseDomainException("error.purchase.partnerCompanyMismatch", null, "Partner belongs to another company");
        }
        if (!partner.isVendor()) {
            throw new PurchaseDomainException(
                    "error.purchase.partnerNotVendor", null, "Partner is not a vendor");
        }
        return buildPayableSections(cid, partnerId, from, to);
    }

    private List<PartnerStatementSectionResponse> buildPayableSections(
            UUID cid, UUID partnerId, LocalDate from, LocalDate to) {
        List<VendorBill> bills = new ArrayList<>();
        bills.addAll(vendorBillRepository.findPostedByPartnerBefore(cid, partnerId, from));
        bills.addAll(vendorBillRepository.findPostedByPartnerBetween(cid, partnerId, from, to));
        for (VendorBill b : bills) {
            b.getLines().size();
            for (VendorBillLine line : b.getLines()) {
                line.getTaxSnapshots().size();
            }
        }
        List<VendorPayment> payments = new ArrayList<>();
        payments.addAll(vendorPaymentRepository.findPostedByPartnerBefore(cid, partnerId, from));
        payments.addAll(vendorPaymentRepository.findPostedByPartnerBetween(cid, partnerId, from, to));

        Set<String> currencies = new LinkedHashSet<>();
        for (VendorBill b : bills) {
            if (b.getState() == VendorBillState.POSTED && b.getCurrencyCode() != null) {
                currencies.add(b.getCurrencyCode().trim().toUpperCase());
            }
        }
        for (VendorPayment p : payments) {
            if (p.getState() == VendorPaymentState.POSTED && p.getCurrencyCode() != null) {
                currencies.add(p.getCurrencyCode().trim().toUpperCase());
            }
        }

        List<PartnerStatementSectionResponse> sections = new ArrayList<>();
        for (String currency : currencies) {
            sections.add(buildPayableSectionForCurrency(currency, from, to, bills, payments));
        }
        return sections;
    }

    private PartnerStatementSectionResponse buildPayableSectionForCurrency(
            String currency, LocalDate from, LocalDate to,
            List<VendorBill> bills, List<VendorPayment> payments) {
        BigDecimal opening = BigDecimal.ZERO;
        for (VendorBill b : bills) {
            if (b.getState() != VendorBillState.POSTED) {
                continue;
            }
            if (!currency.equalsIgnoreCase(b.getCurrencyCode())) {
                continue;
            }
            if (b.getBillDate().isBefore(from)) {
                BigDecimal total = billTotalDocumentCurrency(b);
                if (b.getMoveType() == VendorBillMoveType.CREDIT_NOTE) {
                    opening = opening.subtract(total);
                } else {
                    opening = opening.add(total);
                }
            }
        }
        for (VendorPayment p : payments) {
            if (p.getState() != VendorPaymentState.POSTED) {
                continue;
            }
            if (!currency.equalsIgnoreCase(p.getCurrencyCode())) {
                continue;
            }
            if (p.getPaymentDate().toLocalDate().isBefore(from)) {
                opening = opening.subtract(p.getAmount());
            }
        }
        opening = opening.setScale(4, RoundingMode.HALF_UP);

        record PayEvt(LocalDateTime d, Instant created, String idKey, VendorBill bill, VendorPayment pay) {}

        List<PayEvt> period = new ArrayList<>();
        for (VendorBill b : bills) {
            if (b.getState() != VendorBillState.POSTED) {
                continue;
            }
            if (!currency.equalsIgnoreCase(b.getCurrencyCode())) {
                continue;
            }
            if (!b.getBillDate().isBefore(from) && !b.getBillDate().isAfter(to)) {
                period.add(new PayEvt(b.getBillDate().atStartOfDay(), b.getCreatedAt(), "B:" + b.getId(), b, null));
            }
        }
        for (VendorPayment p : payments) {
            if (p.getState() != VendorPaymentState.POSTED) {
                continue;
            }
            if (!currency.equalsIgnoreCase(p.getCurrencyCode())) {
                continue;
            }
            if (!p.getPaymentDate().toLocalDate().isBefore(from) && !p.getPaymentDate().toLocalDate().isAfter(to)) {
                period.add(new PayEvt(p.getPaymentDate(), p.getCreatedAt(), "P:" + p.getId(), null, p));
            }
        }
        period.sort(Comparator
                .comparing((PayEvt e) -> e.d().toLocalDate())
                .thenComparing(PayEvt::created, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PayEvt::idKey));

        BigDecimal running = opening;
        List<PartnerStatementLineResponse> lines = new ArrayList<>();
        BigDecimal z = zeroMoney();
        for (PayEvt e : period) {
            PartnerStatementLineResponse row = new PartnerStatementLineResponse();
            row.setEntryDate(e.d());
            if (e.bill() != null) {
                VendorBill b = e.bill();
                BigDecimal amt = billTotalDocumentCurrency(b).setScale(4, RoundingMode.HALF_UP);
                boolean creditNote = b.getMoveType() == VendorBillMoveType.CREDIT_NOTE;
                row.setLineType(creditNote ? "VENDOR_CREDIT_NOTE" : "VENDOR_BILL");
                row.setReference(b.getReference() != null && !b.getReference().isBlank()
                        ? b.getReference()
                        : b.getId().toString());
                row.setCurrencyCode(b.getCurrencyCode());
                row.setVendorBillId(b.getId());
                row.setVendorPaymentId(null);
                row.setCustomerInvoiceId(null);
                row.setCustomerPaymentId(null);
                if (creditNote) {
                    row.setDebit(z);
                    row.setCredit(amt);
                    running = running.subtract(amt);
                } else {
                    row.setDebit(amt);
                    row.setCredit(z);
                    running = running.add(amt);
                }
            } else {
                VendorPayment p = e.pay();
                BigDecimal amt = p.getAmount().setScale(4, RoundingMode.HALF_UP);
                row.setLineType("VENDOR_PAYMENT");
                row.setReference(p.getReference() != null && !p.getReference().isBlank()
                        ? p.getReference()
                        : "Payment");
                row.setCurrencyCode(p.getCurrencyCode());
                row.setVendorBillId(null);
                row.setVendorPaymentId(p.getId());
                row.setCustomerInvoiceId(null);
                row.setCustomerPaymentId(null);
                row.setDebit(z);
                row.setCredit(amt);
                running = running.subtract(amt);
            }
            row.setBalance(running.setScale(4, RoundingMode.HALF_UP));
            lines.add(row);
        }

        PartnerStatementSectionResponse s = new PartnerStatementSectionResponse();
        s.setCurrencyCode(currency);
        s.setOpeningBalance(opening);
        s.setClosingBalance(running.setScale(4, RoundingMode.HALF_UP));
        s.setLines(lines);
        return s;
    }

    private static BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public VendorPaymentResponse registerVendorPayment(RegisterVendorPaymentCommand command) {
        VendorPaymentResponse response = vendorPaymentService.register(command);
        UUID primaryBillId = null;
        if (command.getVendorBillId() != null) {
            primaryBillId = command.getVendorBillId();
        } else if (command.getAllocations() != null && command.getAllocations().size() == 1) {
            primaryBillId = command.getAllocations().get(0).getBillId();
        }
        purchaseEventPublisher.publishVendorPaymentRegistered(new VendorPaymentRegisteredEvent(
                UUID.randomUUID(),
                Instant.now(),
                response.getCompanyId(),
                response.getId(),
                response.getVendorPartnerId(),
                primaryBillId));
        return response;
    }

    @Override
    @Transactional
    public VendorPaymentResponse allocateVendorPayment(UUID paymentId, AllocateVendorPaymentCommand command) {
        return vendorPaymentService.allocate(paymentId, command);
    }

    @Override
    @Transactional
    public VendorPaymentResponse deallocateVendorPayment(UUID allocationId) {
        return vendorPaymentService.deallocate(allocationId);
    }

    @Override
    @Transactional
    public VendorPaymentResponse reverseVendorPayment(UUID paymentId, String reason) {
        return vendorPaymentService.reverse(paymentId, reason);
    }

    @Override
    @Transactional
    public VendorPaymentResponse correctVendorPayment(UUID paymentId, CorrectVendorPaymentCommand command) {
        return vendorPaymentService.correctPayment(paymentId, command);
    }

    @Override
    @Transactional
    public VendorPaymentResponse refundVendorCredit(UUID billId, RefundVendorCreditCommand command) {
        return vendorPaymentService.refundCredit(billId, command);
    }

    @Override
    @Transactional
    public BigDecimal keepVendorCredit(UUID billId) {
        return vendorPaymentService.keepCredit(billId);
    }

    @Override
    @Transactional
    public BigDecimal applyVendorCredit(UUID billId) {
        return vendorPaymentService.applyCredit(billId);
    }

    @Override
    @Transactional
    public FiscalTaxResponse createFiscalTax(CreateFiscalTaxCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        FiscalTax t = new FiscalTax();
        t.setId(UUID.randomUUID());
        t.setCompanyId(companyId);
        t.setName(command.getName());
        t.setAmountType(command.getAmountType());
        t.setAmount(command.getAmount());
        t.setPriceInclude(command.isPriceInclude());
        t.setScope(command.getScope());
        t.setAccountId(command.getAccountId());
        t.setRefundAccountId(command.getRefundAccountId());
        t.setActive(true);
        return toTaxResponse(fiscalTaxRepository.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiscalTaxResponse> listFiscalTaxes(UUID companyId) {
        UUID cid = companyIdOrDefault(companyId);
        return fiscalTaxRepository.findByCompanyIdAndActive(cid, true).stream()
                .map(this::toTaxResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalTaxResponse getFiscalTax(UUID taxId) {
        FiscalTax t = fiscalTaxRepository.findById(taxId)
                .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
        return toTaxResponse(t);
    }

    private PurchaseOrder loadOrder(UUID id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new PurchaseDomainException("Purchase order not found: " + id));
    }

    private StockLocation findSupplierVirtual(UUID companyId) {
        return stockLocationRepository.findByCompany(new CompanyId(companyId), false).stream()
                .filter(l -> l.getLocationType() == LocationType.SUPPLIER)
                .filter(l -> "VIRT/SUPPLIERS".equalsIgnoreCase(l.getCode()))
                .findFirst()
                .orElseThrow(() -> new PurchaseDomainException("Virtual supplier location VIRT/SUPPLIERS not found"));
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder o) {
        PurchaseOrderResponse r = new PurchaseOrderResponse();
        r.setId(o.getId());
        r.setCompanyId(o.getCompanyId());
        r.setVendorPartnerId(o.getVendorPartnerId());
        r.setName(o.getName());
        r.setState(o.getState());
        r.setCurrencyCode(o.getCurrencyCode());
        r.setWarehouseId(o.getWarehouseId());
        r.setDestLocationId(o.getDestLocationId());
        r.setPaymentTermsId(o.getPaymentTermsId());
        r.setOrderDate(o.getOrderDate());
        r.setExpectedDate(o.getExpectedDate());
        r.setIncoterm(o.getIncoterm());
        r.setNotes(o.getNotes());
        r.setVendorReference(o.getVendorReference());
        r.setAmountUntaxed(o.getAmountUntaxed());
        r.setAmountTax(o.getAmountTax());
        r.setOrderDiscountType(o.getOrderDiscountType());
        r.setOrderDiscountValue(o.getOrderDiscountValue());
        r.setOrderDiscountPercent(o.getOrderDiscountPercent());
        OrderPaymentFields payment = computePurchasePaymentFields(o);
        r.setAmountTotal(payment.amountTotal());
        r.setPaymentStatus(payment.paymentStatus());
        r.setAmountPaid(payment.amountPaid());
        r.setAmountDue(payment.amountDue());
        r.setExchangeRateToCompany(o.getExchangeRateToCompany());
        r.setSentAt(o.getSentAt());
        r.setConfirmedAt(o.getConfirmedAt());
        r.setCancelledAt(o.getCancelledAt());
        r.setLocked(o.isLocked());
        r.setRowVersion(o.getRowVersion());
        r.setReceiptPickingIds(stockMovePurchaseQueryPort.findPickingIdsByPurchaseOrderId(o.getId()));
        r.setReturnPickingIds(stockMovePurchaseQueryPort.findReturnPickingIdsByPurchaseOrderId(o.getId()));
        r.setCanCreateVendorBill(computeCanCreateVendorBill(o));
        r.setCanCreateReturn(computeCanCreateReturn(o));
        r.setLines(o.getLines().stream().sorted(Comparator.comparingInt(PurchaseOrderLine::getSequence)).map(l -> {
            PurchaseOrderLineResponse lr = new PurchaseOrderLineResponse();
            lr.setId(l.getId());
            lr.setSequence(l.getSequence());
            lr.setProductId(l.getProductId());
            lr.setName(l.getName());
            lr.setUomId(l.getUomId());
            lr.setWarehouseId(l.getWarehouseId());
            lr.setPackagingId(l.getPackagingId());
            lr.setPackagingName(l.getPackagingName());
            lr.setQtyPerPackage(l.getQtyPerPackage());
            lr.setQtyOrdered(l.getQtyOrdered());
            lr.setQtyReceived(l.getQtyReceived());
            lr.setQtyInvoiced(l.getQtyInvoiced());
            lr.setUnitPrice(l.getUnitPrice());
            lr.setDiscountType(l.getDiscountType());
            lr.setDiscountValue(l.getDiscountValue());
            lr.setDiscountPercent(l.getDiscountPercent());
            lr.setExpectedDate(l.getExpectedDate());
            lr.setTaxIds(l.getTaxes().stream().sorted(Comparator.comparingInt(PurchaseOrderLineTax::getSequence))
                    .map(PurchaseOrderLineTax::getTaxId).collect(Collectors.toList()));
            productRepository.findById(new ProductId(l.getProductId()))
                    .ifPresent(p -> lr.setProductType(p.getProductType().name()));
            return lr;
        }).collect(Collectors.toList()));
        return r;
    }

    private VendorBillSummaryResponse toBillSummaryResponse(VendorBill b) {
        VendorBillSummaryResponse r = new VendorBillSummaryResponse();
        r.setId(b.getId());
        r.setCompanyId(b.getCompanyId());
        r.setVendorPartnerId(b.getVendorPartnerId());
        r.setPurchaseOrderId(b.getPurchaseOrderId());
        r.setBillDate(b.getBillDate());
        r.setDueDate(b.getDueDate());
        r.setReference(b.getReference());
        r.setCurrencyCode(b.getCurrencyCode());
        r.setState(b.getState());
        r.setMoveType(b.getMoveType() != null ? b.getMoveType() : VendorBillMoveType.BILL);
        r.setReversedBillId(b.getReversedBillId());
        r.setJournalEntryId(b.getJournalEntryId());
        r.setAmountTotal(billTotalDocumentCurrency(b));
        r.setCreatedAt(b.getCreatedAt());
        return r;
    }

    private VendorBillResponse toBillResponse(VendorBill b) {
        VendorBillResponse r = new VendorBillResponse();
        r.setId(b.getId());
        r.setCompanyId(b.getCompanyId());
        r.setVendorPartnerId(b.getVendorPartnerId());
        r.setPurchaseOrderId(b.getPurchaseOrderId());
        r.setBillDate(b.getBillDate());
        r.setDueDate(b.getDueDate());
        r.setReference(b.getReference());
        r.setCurrencyCode(b.getCurrencyCode());
        r.setState(b.getState());
        r.setMoveType(b.getMoveType() != null ? b.getMoveType() : VendorBillMoveType.BILL);
        r.setReversedBillId(b.getReversedBillId());
        r.setJournalEntryId(b.getJournalEntryId());
        r.setOrderDiscountAmount(b.getOrderDiscountAmount());
        if (b.getState() == VendorBillState.POSTED) {
            BigDecimal total = billTotalDocumentCurrency(b);
            BigDecimal paid = vendorPaymentService.sumActiveAllocationsByBillIds(List.of(b.getId()))
                    .getOrDefault(b.getId(), BigDecimal.ZERO);
            BigDecimal credited = BigDecimal.ZERO;
            if (b.getMoveType() != VendorBillMoveType.CREDIT_NOTE) {
                for (VendorBill cn : vendorBillRepository.findByReversedBillId(b.getId())) {
                    if (cn.getState() == VendorBillState.POSTED) {
                        cn.getLines().size();
                        for (VendorBillLine line : cn.getLines()) {
                            line.getTaxSnapshots().size();
                        }
                        credited = credited.add(billTotalDocumentCurrency(cn));
                    }
                }
            }
            r.setAmountTotal(total);
            r.setAmountPaid(paid);
            r.setAmountCredited(credited);
            r.setAmountResidual(total.subtract(paid).subtract(credited).max(BigDecimal.ZERO));
            if (b.getMoveType() == VendorBillMoveType.CREDIT_NOTE) {
                r.setAmountOverpaid(BigDecimal.ZERO);
                r.setAmountRefundable(vendorPaymentService.refundableOf(b));
            } else {
                r.setAmountOverpaid(vendorPaymentService.creditAvailableOn(b));
                r.setAmountRefundable(BigDecimal.ZERO);
            }
            r.setPaymentAllocations(vendorPaymentService.allocationResponsesForBill(b.getId()));
        } else {
            r.setAmountTotal(billTotalDocumentCurrency(b));
            r.setAmountPaid(BigDecimal.ZERO);
            r.setAmountCredited(BigDecimal.ZERO);
            r.setAmountResidual(BigDecimal.ZERO);
            r.setAmountOverpaid(BigDecimal.ZERO);
            r.setAmountRefundable(BigDecimal.ZERO);
        }
        r.setLines(b.getLines().stream().sorted(Comparator.comparingInt(VendorBillLine::getSequence)).map(l -> {
            VendorBillLineResponse lr = new VendorBillLineResponse();
            lr.setId(l.getId());
            lr.setSequence(l.getSequence());
            lr.setPurchaseOrderLineId(l.getPurchaseOrderLineId());
            lr.setProductId(l.getProductId());
            lr.setName(l.getName());
            lr.setUomId(l.getUomId());
            lr.setQty(l.getQty());
            lr.setUnitPrice(l.getUnitPrice());
            lr.setDiscountType(l.getDiscountType());
            lr.setDiscountValue(l.getDiscountValue());
            lr.setDiscountPercent(l.getDiscountPercent());
            lr.setAccountId(l.getAccountId());
            lr.setTaxes(l.getTaxSnapshots().stream().map(ts -> {
                VendorBillLineTaxResponse tr = new VendorBillLineTaxResponse();
                tr.setTaxId(ts.getTaxId());
                tr.setTaxName(ts.getTaxName());
                tr.setTaxBase(ts.getTaxBase());
                tr.setTaxAmount(ts.getTaxAmount());
                tr.setAccountId(ts.getAccountId());
                return tr;
            }).collect(Collectors.toList()));
            return lr;
        }).collect(Collectors.toList()));
        return r;
    }

    private Map<UUID, BigDecimal> creditedQtyBySourceBillLine(VendorBill source) {
        Map<UUID, BigDecimal> credited = new LinkedHashMap<>();
        for (VendorBill cn : vendorBillRepository.findByReversedBillId(source.getId())) {
            if (cn.getState() == VendorBillState.CANCELLED) {
                continue;
            }
            if (cn.getMoveType() != VendorBillMoveType.CREDIT_NOTE) {
                continue;
            }
            cn.getLines().size();
            for (VendorBillLine cnLine : cn.getLines()) {
                UUID sourceLineId = matchSourceBillLineId(source, cnLine);
                if (sourceLineId != null) {
                    credited.merge(sourceLineId, cnLine.getQty(), BigDecimal::add);
                }
            }
        }
        return credited;
    }

    private UUID matchSourceBillLineId(VendorBill source, VendorBillLine cnLine) {
        for (VendorBillLine src : source.getLines()) {
            if (java.util.Objects.equals(src.getPurchaseOrderLineId(), cnLine.getPurchaseOrderLineId())
                    && java.util.Objects.equals(src.getProductId(), cnLine.getProductId())
                    && java.util.Objects.equals(src.getName(), cnLine.getName())
                    && src.getUnitPrice().compareTo(cnLine.getUnitPrice()) == 0) {
                return src.getId();
            }
        }
        return null;
    }

    private FiscalTaxResponse toTaxResponse(FiscalTax t) {
        FiscalTaxResponse r = new FiscalTaxResponse();
        r.setId(t.getId());
        r.setCompanyId(t.getCompanyId());
        r.setName(t.getName());
        r.setAmountType(t.getAmountType());
        r.setAmount(t.getAmount());
        r.setPriceInclude(t.isPriceInclude());
        r.setScope(t.getScope());
        r.setAccountId(t.getAccountId());
        r.setRefundAccountId(t.getRefundAccountId());
        r.setActive(t.isActive());
        return r;
    }

    private void postPurchaseAmendmentTracking(
            PurchaseOrder saved,
            Map<UUID, BigDecimal> qtyOrderedBefore,
            BigDecimal untaxedBefore) {
        List<String> qtyBlocks = new ArrayList<>();
        for (PurchaseOrderLine line : saved.getLines()) {
            BigDecimal oldQty = qtyOrderedBefore.get(line.getId());
            if (oldQty == null) {
                continue;
            }
            BigDecimal newQty = line.getQtyOrdered() != null ? line.getQtyOrdered() : BigDecimal.ZERO;
            if (oldQty.compareTo(newQty) == 0) {
                continue;
            }
            String label = line.getName() != null && !line.getName().isBlank()
                    ? line.getName()
                    : line.getProductId().toString().substring(0, 8);
            Optional<Product> product = productRepository.findById(new ProductId(line.getProductId()));
            if (product.isPresent()) {
                label = "[" + product.get().getSku() + "] " + product.get().getName();
            }
            StringBuilder block = new StringBuilder("• ").append(label).append(":\n  Ordered Quantity: ")
                    .append(MonetaryScale.toDisplayString(oldQty))
                    .append(" → ")
                    .append(MonetaryScale.toDisplayString(newQty));
            if (line.getQtyInvoiced() != null && line.getQtyInvoiced().signum() > 0) {
                block.append("\n  Billed Quantity: ")
                        .append(MonetaryScale.toDisplayString(line.getQtyInvoiced()));
            }
            qtyBlocks.add(block.toString());
        }
        if (!qtyBlocks.isEmpty()) {
            activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                    "The ordered quantity has been updated.\n" + String.join("\n", qtyBlocks));
        }
        BigDecimal untaxedAfter = saved.getAmountUntaxed() != null ? saved.getAmountUntaxed() : BigDecimal.ZERO;
        if (untaxedBefore.compareTo(untaxedAfter) != 0) {
            activityLogger.logFieldChange(
                    saved.getCompanyId(),
                    RecordActivityLogger.MODEL_PURCHASE_ORDER,
                    saved.getId(),
                    MonetaryScale.toDisplayString(untaxedBefore),
                    MonetaryScale.toDisplayString(untaxedAfter),
                    "Subtotal");
        }
    }

    private BigDecimal resolveExchangeRate(
            UUID companyId, String currencyCode, LocalDate asOf, BigDecimal explicit) {
        if (explicit != null && explicit.signum() > 0) {
            String base = currencyConversionPort.baseCurrencyCode(companyId);
            if (explicit.compareTo(BigDecimal.ONE) != 0 || currencyCode.equalsIgnoreCase(base)) {
                return explicit.setScale(12, RoundingMode.HALF_UP);
            }
        }
        return currencyConversionPort.exchangeRateToCompany(companyId, currencyCode, asOf);
    }

    private void applyPackagingSnapshot(PurchaseOrderLine line, UUID packagingId) {
        if (packagingId == null) {
            line.setPackagingId(null);
            line.setPackagingName(null);
            line.setQtyPerPackage(null);
            return;
        }
        ProductPackaging packaging = productPackagingRepository.findById(new ProductPackagingId(packagingId))
                .orElseThrow(() -> new PurchaseDomainException(
                        "error.inventory.packagingNotFound",
                        new Object[]{packagingId},
                        "Packaging not found: " + packagingId));
        if (!packaging.isActive()) {
            throw new PurchaseDomainException(
                    "error.inventory.packagingInactive",
                    new Object[]{packaging.getName()},
                    "Packaging is inactive: " + packaging.getName());
        }
        boolean belongsToLine = packaging.getProductId().getId().equals(line.getProductId())
                || (packaging.getPackagedProductId() != null
                && packaging.getPackagedProductId().getId().equals(line.getProductId()));
        if (!belongsToLine) {
            throw new PurchaseDomainException(
                    "error.inventory.packagingProductMismatch",
                    null,
                    "Packaging does not belong to this product");
        }
        if (!packaging.isBase() && packaging.getPackagedProductId() != null) {
            line.setProductId(packaging.getPackagedProductId().getId());
            line.setPackagingId(packaging.getId().getId());
            line.setPackagingName(packaging.getName());
            line.setQtyPerPackage(null);
            return;
        }
        line.setPackagingId(packaging.getId().getId());
        line.setPackagingName(packaging.getName());
        line.setQtyPerPackage(packaging.getQty());
    }
}
