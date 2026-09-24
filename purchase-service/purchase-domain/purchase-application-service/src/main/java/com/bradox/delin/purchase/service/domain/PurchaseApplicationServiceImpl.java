package com.bradox.delin.purchase.service.domain;

import com.bradox.delin.accounting.service.domain.CurrencyMath;
import com.bradox.delin.accounting.service.domain.JournalEntryTiming;
import com.bradox.delin.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.delin.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import com.bradox.delin.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.delin.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.delin.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.delin.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.delin.accounting.service.domain.create.JournalEntryResponse;
import com.bradox.delin.accounting.service.domain.create.JournalItemCommand;
import com.bradox.delin.accounting.service.domain.partnerstatement.PartnerStatementLineResponse;
import com.bradox.delin.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.bradox.delin.contacts.service.domain.dto.PartnerResponse;
import com.bradox.delin.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.delin.domain.core.ValueObject.JournalType;
import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.domain.valueobject.DiscountMath;
import com.bradox.delin.domain.valueobject.DiscountType;
import com.bradox.delin.inventory.domain.core.entity.Product;
import com.bradox.delin.inventory.domain.core.entity.ProductCategory;
import com.bradox.delin.inventory.domain.core.entity.ProductPackaging;
import com.bradox.delin.inventory.domain.core.entity.StockLocation;
import com.bradox.delin.inventory.domain.core.entity.Warehouse;
import com.bradox.delin.inventory.domain.core.valueobject.LocationType;
import com.bradox.delin.inventory.domain.core.valueobject.PickingType;
import com.bradox.delin.inventory.domain.core.valueobject.ProductId;
import com.bradox.delin.inventory.domain.core.valueobject.ProductPackagingId;
import com.bradox.delin.inventory.domain.core.valueobject.ProductType;
import com.bradox.delin.inventory.domain.core.valueobject.WarehouseId;
import com.bradox.delin.inventory.service.domain.dto.CreateStockPickingCommand;
import com.bradox.delin.inventory.service.domain.dto.ReturnPickingCommand;
import com.bradox.delin.inventory.service.domain.dto.StockMoveCommand;
import com.bradox.delin.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.delin.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.delin.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.delin.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.delin.inventory.service.domain.ports.output.StockMovePurchaseQueryPort;
import com.bradox.delin.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import com.bradox.delin.inventory.service.domain.ports.output.repository.ProductPackagingRepository;
import com.bradox.delin.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.delin.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.bradox.delin.inventory.service.domain.ports.output.repository.WarehouseRepository;
import com.bradox.delin.platform.activity.RecordActivityLogger;
import com.bradox.delin.platform.document.DocumentSequenceService;
import com.bradox.delin.platform.settings.CompanyDocumentPolicyService;
import com.bradox.delin.platform.web.CompanyContext;
import com.bradox.delin.purchase.domain.core.*;
import com.bradox.delin.purchase.service.domain.FiscalTaxSnapshot;
import com.bradox.delin.purchase.service.domain.PurchaseTaxEngine;
import com.bradox.delin.purchase.service.domain.dto.*;
import com.bradox.delin.purchase.service.domain.event.VendorBillPostedEvent;
import com.bradox.delin.purchase.service.domain.event.VendorPaymentRegisteredEvent;
import com.bradox.delin.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.bradox.delin.purchase.domain.core.entity.FiscalTax;
import com.bradox.delin.purchase.domain.core.entity.PurchaseOrder;
import com.bradox.delin.purchase.domain.core.entity.PurchaseOrderLine;
import com.bradox.delin.purchase.domain.core.entity.PurchaseOrderLineTax;
import com.bradox.delin.purchase.domain.core.entity.VendorBill;
import com.bradox.delin.purchase.domain.core.entity.VendorBillLine;
import com.bradox.delin.purchase.domain.core.entity.VendorBillLineTax;
import com.bradox.delin.purchase.domain.core.entity.VendorPayment;
import com.bradox.delin.purchase.service.domain.ports.output.repository.FiscalTaxRepository;
import com.bradox.delin.purchase.service.domain.ports.output.repository.PurchaseOrderRepository;
import com.bradox.delin.purchase.service.domain.ports.output.repository.VendorBillRepository;
import com.bradox.delin.purchase.service.domain.ports.output.repository.VendorPaymentRepository;
import com.bradox.delin.purchase.service.domain.ports.output.messaging.PurchaseEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class PurchaseApplicationServiceImpl implements PurchaseApplicationService {

    private static final String DEFAULT_AP_ACCOUNT_CODE = "430004";
    private static final String EXCHANGE_GAIN_ACCOUNT_CODE = "430014";
    private static final String EXCHANGE_LOSS_ACCOUNT_CODE = "430015";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final FiscalTaxRepository fiscalTaxRepository;
    private final VendorBillRepository vendorBillRepository;
    private final VendorPaymentRepository vendorPaymentRepository;
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
    private final ReconciliationApplicationService reconciliationApplicationService;
    private final com.bradox.delin.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort journalItemReconciliationPort;
    private final com.bradox.delin.accounting.service.domain.PeriodPostingGuard periodPostingGuard;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final CurrencyConversionPort currencyConversionPort;
    private final PurchaseOrderQtyWriter purchaseOrderQtyWriter;
    private final DocumentSequenceService documentSequenceService;
    private final RecordActivityLogger activityLogger;
    private final CompanyDocumentPolicyService companyDocumentPolicyService;
    private final TransactionTemplate afterCommitTx;

    public PurchaseApplicationServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                          FiscalTaxRepository fiscalTaxRepository,
                                          VendorBillRepository vendorBillRepository,
                                          VendorPaymentRepository vendorPaymentRepository,
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
                                          ReconciliationApplicationService reconciliationApplicationService,
                                          com.bradox.delin.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort journalItemReconciliationPort,
                                          com.bradox.delin.accounting.service.domain.PeriodPostingGuard periodPostingGuard,
                                          ObjectProvider<CompanyContext> companyContextProvider,
                                          PurchaseEventPublisher purchaseEventPublisher,
                                          CurrencyConversionPort currencyConversionPort,
                                          PurchaseOrderQtyWriter purchaseOrderQtyWriter,
                                          DocumentSequenceService documentSequenceService,
                                          RecordActivityLogger activityLogger,
                                          CompanyDocumentPolicyService companyDocumentPolicyService,
                                          PlatformTransactionManager transactionManager) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.fiscalTaxRepository = fiscalTaxRepository;
        this.vendorBillRepository = vendorBillRepository;
        this.vendorPaymentRepository = vendorPaymentRepository;
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
        this.reconciliationApplicationService = reconciliationApplicationService;
        this.journalItemReconciliationPort = journalItemReconciliationPort;
        this.periodPostingGuard = periodPostingGuard;
        this.companyContextProvider = companyContextProvider;
        this.purchaseEventPublisher = purchaseEventPublisher;
        this.currencyConversionPort = currencyConversionPort;
        this.purchaseOrderQtyWriter = purchaseOrderQtyWriter;
        this.documentSequenceService = documentSequenceService;
        this.activityLogger = activityLogger;
        this.companyDocumentPolicyService = companyDocumentPolicyService;
        this.afterCommitTx = new TransactionTemplate(transactionManager);
        this.afterCommitTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) return fromCommand;
        return companyContextProvider.getObject().requireCompany().getId();
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    afterCommitTx.executeWithoutResult(status -> action.run());
                }
            });
        } else {
            action.run();
        }
    }

    private UUID resolveLiquidityAccountForPaymentJournal(UUID companyId, UUID journalId) {
        JournalType journalType = accountingReferenceLookupPort.resolveJournalType(companyId, journalId);
        if (journalType != JournalType.CASH && journalType != JournalType.BANK) {
            throw new PurchaseDomainException("error.purchase.paymentJournalCashOrBank", null, "Payment journal must be cash or bank");
        }
        return accountingReferenceLookupPort.resolveLiquidityAccountIdForJournal(companyId, journalId);
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
        return toResponse(saved);
    }

    private PurchaseOrderResponse amendConfirmedPurchaseOrder(PurchaseOrder o, CreatePurchaseOrderCommand command) {
        Map<UUID, BigDecimal> qtyOrderedBefore = o.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, PurchaseOrderLine::getQtyOrdered, (a, b) -> a, LinkedHashMap::new));
        BigDecimal untaxedBefore = o.getAmountUntaxed() != null ? o.getAmountUntaxed() : BigDecimal.ZERO;
        UUID companyId = o.getCompanyId();
        boolean hasPostedDocs = vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                .anyMatch(b -> b.getState() == VendorBillState.POSTED);
        if (hasPostedDocs) {
            if (command.getVendorPartnerId() != null && !command.getVendorPartnerId().equals(o.getVendorPartnerId())) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotChangeVendorAfterBilling", null,
                        "Cannot change vendor after posted bills exist");
            }
            if (command.getCurrencyCode() != null && !command.getCurrencyCode().equalsIgnoreCase(o.getCurrencyCode())) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotChangeCurrencyAfterBilling", null,
                        "Cannot change currency after posted bills exist");
            }
            if (command.getWarehouseId() != null && o.getWarehouseId() != null
                    && !command.getWarehouseId().equals(o.getWarehouseId())) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotChangeWarehouseAfterBilling", null,
                        "Cannot change warehouse after posted bills exist");
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
            o.setCurrencyCode(command.getCurrencyCode());
            o.setWarehouseId(command.getWarehouseId());
            o.setDestLocationId(command.getDestLocationId());
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
        recalcTotals(o);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        postPurchaseAmendmentTracking(saved, qtyOrderedBefore, untaxedBefore);
        return syncDocumentsAfterAmendment(saved);
    }

    /**
     * After a confirmed-order amendment: create draft return/receipt pickings for
     * qty changes. Receipt/return validation and vendor bills stay manual actions.
     */
    private PurchaseOrderResponse syncDocumentsAfterAmendment(PurchaseOrder saved) {
        if (saved.getWarehouseId() == null) {
            return toResponse(saved);
        }
        purchaseOrderQtyWriter.updateQtyReceivedJoiningCurrentTransaction(saved.getId());
        PurchaseOrder o = loadOrder(saved.getId());

        createReturnPickingForExcess(o);
        o = loadOrder(o.getId());

        createReceiptPickingForRemaining(o);
        return getPurchaseOrder(o.getId());
    }

    private void createReceiptPickingForRemaining(PurchaseOrder o) {
        List<StockMoveCommand> moves = buildRemainingIncomingMoves(o);
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

    private void createReturnPickingForExcess(PurchaseOrder o) {
        List<StockMoveCommand> moves = buildExcessReturnMoves(o);
        if (moves.isEmpty()) {
            return;
        }
        UUID warehouseId = o.getWarehouseId();
        Warehouse wh = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new PurchaseDomainException("Warehouse not found: " + warehouseId));
        StockLocation supplier = findSupplierVirtual(o.getCompanyId());
        UUID sourceLoc = o.getDestLocationId() != null
                ? o.getDestLocationId()
                : wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (sourceLoc == null) {
            throw new PurchaseDomainException("error.purchase.destinationStockLocationUnresolved", null, "Destination stock location could not be resolved");
        }
        CreateStockPickingCommand cmd = new CreateStockPickingCommand();
        cmd.setCompanyId(o.getCompanyId());
        cmd.setWarehouseId(warehouseId);
        cmd.setPickingType(PickingType.OUTGOING);
        cmd.setSourceLocationId(sourceLoc);
        cmd.setDestinationLocationId(supplier.getId().getId());
        cmd.setPartnerId(o.getVendorPartnerId());
        cmd.setOrigin("RETURN OF " + o.getName());
        cmd.setReference(o.getName() != null ? o.getName() + "-RET" : null);
        cmd.setPurchaseOrderId(o.getId());
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

    private List<StockMoveCommand> buildExcessReturnMoves(PurchaseOrder o) {
        BigDecimal rateToCompany = resolveExchangeRate(
                o.getCompanyId(), o.getCurrencyCode(), o.getOrderDate(), o.getExchangeRateToCompany());
        List<StockMoveCommand> moves = new ArrayList<>();
        for (PurchaseOrderLine line : o.getLines()) {
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + line.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                continue;
            }
            BigDecimal excess = line.getQtyReceived().subtract(line.getQtyOrdered());
            if (excess.signum() <= 0) {
                continue;
            }
            moves.add(buildPurchaseStockMove(line, product, excess, rateToCompany));
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
        return purchaseOrderRepository.search(cid, state, vendorPartnerId, qNorm, pageable).map(this::toSummary);
    }

    private PurchaseOrderSummaryResponse toSummary(PurchaseOrder o) {
        PurchaseOrderSummaryResponse r = new PurchaseOrderSummaryResponse();
        r.setId(o.getId());
        r.setCompanyId(o.getCompanyId());
        r.setVendorPartnerId(o.getVendorPartnerId());
        r.setName(o.getName());
        r.setState(o.getState());
        r.setCurrencyCode(o.getCurrencyCode());
        r.setOrderDate(o.getOrderDate());
        r.setAmountTotal(o.getAmountTotal());
        r.setCreatedAt(o.getCreatedAt());
        applyPurchasePaymentStatus(r, o);
        return r;
    }

    /**
     * List statuses: New / Unpaid / Partial Paid / Paid / Cancelled (+ amounts).
     * New = unpaid with due date today or later (or no due date); Unpaid = overdue unpaid.
     */
    private record OrderPaymentFields(String paymentStatus, BigDecimal amountPaid, BigDecimal amountDue) {}

    private void applyPurchasePaymentStatus(PurchaseOrderSummaryResponse r, PurchaseOrder o) {
        OrderPaymentFields f = computePurchasePaymentFields(o);
        r.setPaymentStatus(f.paymentStatus());
        r.setAmountPaid(f.amountPaid());
        r.setAmountDue(f.amountDue());
    }

    private OrderPaymentFields computePurchasePaymentFields(PurchaseOrder o) {
        if (o.getState() == PurchaseOrderState.CANCELLED) {
            return new OrderPaymentFields("CANCELLED", BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<VendorBill> bills = vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                .filter(b -> b.getMoveType() == null || b.getMoveType() == VendorBillMoveType.BILL)
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .toList();
        if (bills.isEmpty()) {
            return new OrderPaymentFields("NEW", BigDecimal.ZERO,
                    o.getAmountTotal() != null ? o.getAmountTotal() : BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal credited = BigDecimal.ZERO;
        LocalDate earliestDue = null;
        for (VendorBill bill : bills) {
            bill.getLines().size();
            for (VendorBillLine line : bill.getLines()) {
                line.getTaxSnapshots().size();
            }
            String cur = bill.getCurrencyCode();
            total = total.add(billTotalDocumentCurrency(bill));
            paid = paid.add(sumPostedPaymentsForBill(bill.getId(), cur));
            credited = credited.add(sumPostedCreditNotesForBill(bill.getId(), cur));
            if (bill.getDueDate() != null && (earliestDue == null || bill.getDueDate().isBefore(earliestDue))) {
                earliestDue = bill.getDueDate();
            }
        }
        total = total.setScale(4, RoundingMode.HALF_UP);
        paid = paid.setScale(4, RoundingMode.HALF_UP);
        credited = credited.setScale(4, RoundingMode.HALF_UP);
        BigDecimal due = total.subtract(paid).subtract(credited).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        BigDecimal eps = new BigDecimal("0.005");
        String status;
        if (paid.compareTo(eps) <= 0) {
            boolean overdue = earliestDue != null && earliestDue.isBefore(LocalDate.now());
            status = overdue ? "UNPAID" : "NEW";
        } else if (paid.add(eps).compareTo(total.subtract(credited)) >= 0) {
            status = "PAID";
            due = BigDecimal.ZERO;
        } else {
            status = "PARTIAL_PAID";
        }
        return new OrderPaymentFields(status, paid, due);
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
            if (vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                    .anyMatch(b -> b.getState() == VendorBillState.POSTED)) {
                throw new PurchaseDomainException("error.purchase.cannotCancelPostedVendorBills", null, "Cannot cancel: posted vendor bills exist for this order");
            }
            for (UUID pickingId : stockMovePurchaseQueryPort.findNonTerminalPickingIdsByPurchaseOrderId(o.getId())) {
                stockPickingApplicationService.cancelPicking(pickingId);
            }
            if (stockMovePurchaseQueryPort.existsDonePickingForPurchaseOrder(o.getId())) {
                throw new PurchaseDomainException(
                        "error.purchase.cannotCancelReceivedGoods", null,
                        "Cannot cancel: goods have been received (return or cancel receipts first)");
            }
            if (stockMovePurchaseQueryPort.existsNonTerminalPickingForPurchaseOrder(o.getId())) {
                throw new PurchaseDomainException("error.purchase.cannotCancelOpenPickings", null, "Cannot cancel: open pickings exist (confirm/cancel pickings first)");
            }
        }
        o.setState(PurchaseOrderState.CANCELLED);
        o.setCancelledAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, saved.getId(),
                "Purchase Order", "Cancelled", "Status");
        return toResponse(saved);
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
        PurchaseOrder o = purchaseOrderQtyWriter.updateQtyReceived(purchaseOrderId);
        if (o != null) {
            UUID id = o.getId();
            boolean toRefund = stockMovePurchaseQueryPort.isPickingToRefund(pickingId);
            runAfterCommit(() -> {
                if (!toRefund) {
                    return;
                }
                PurchaseOrder fresh = purchaseOrderRepository.findById(id).orElse(null);
                if (fresh != null) {
                    tryAutoCreateDraftCreditNoteFromReturn(fresh);
                }
            });
        }
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

    private void tryAutoCreateDraftCreditNoteFromReturn(PurchaseOrder o) {
        Map<UUID, BigDecimal> draftCn = draftCreditNoteQtyByPoLine(o.getId());
        final Map<UUID, BigDecimal> initialDraftCn = draftCn;
        boolean hasCreditable = o.getLines().stream()
                .anyMatch(pol -> creditNoteableQtyForPoLine(pol, initialDraftCn).signum() > 0);
        if (!hasCreditable) {
            return;
        }
        List<VendorBill> postedBills = vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .filter(b -> b.getMoveType() == null || b.getMoveType() == VendorBillMoveType.BILL)
                .sorted(Comparator.comparing(VendorBill::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VendorBill::getBillDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (postedBills.isEmpty()) {
            return;
        }
        // Remaining credit-noteable qty per PO line after each CN (FIFO across bills).
        Map<UUID, BigDecimal> remaining = new HashMap<>();
        for (PurchaseOrderLine pol : o.getLines()) {
            BigDecimal qty = creditNoteableQtyForPoLine(pol, draftCn);
            if (qty.signum() > 0) {
                remaining.put(pol.getId(), qty);
            }
        }
        for (VendorBill sourceBill : postedBills) {
            if (remaining.values().stream().noneMatch(q -> q.signum() > 0)) {
                break;
            }
            VendorBillResponse cn = createVendorCreditNoteFromPurchaseOrder(o, sourceBill, remaining);
            if (cn != null) {
                // Leave as DRAFT — do not auto-post (Odoo-like purchase return correction).
                draftCn = draftCreditNoteQtyByPoLine(o.getId());
                remaining.clear();
                for (PurchaseOrderLine pol : o.getLines()) {
                    BigDecimal qty = creditNoteableQtyForPoLine(pol, draftCn);
                    if (qty.signum() > 0) {
                        remaining.put(pol.getId(), qty);
                    }
                }
            }
        }
    }

    private VendorBillResponse createVendorCreditNoteFromPurchaseOrder(PurchaseOrder po,
                                                                         VendorBill sourceBill,
                                                                         Map<UUID, BigDecimal> remainingByPoLine) {
        sourceBill.getLines().size();
        for (VendorBillLine line : sourceBill.getLines()) {
            line.getTaxSnapshots().size();
        }
        CreateCreditNoteFromVendorBillCommand cnCmd = new CreateCreditNoteFromVendorBillCommand();
        cnCmd.setCompanyId(po.getCompanyId());
        cnCmd.setBillDate(LocalDate.now());
        // PO names are already "PO/YYYY/NNNNN"; prefix only "CN/" → "CN/PO/YYYY/NNNNN".
        cnCmd.setReference(po.getName() != null ? "CN/" + po.getName() : "CN/PO/" + po.getId());
        List<CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand> cnLines = new ArrayList<>();
        Map<UUID, BigDecimal> creditedOnBill = creditedQtyBySourceBillLine(sourceBill);
        for (PurchaseOrderLine pol : po.getLines()) {
            BigDecimal remaining = remainingByPoLine.getOrDefault(pol.getId(), BigDecimal.ZERO);
            if (remaining.signum() <= 0) {
                continue;
            }
            for (VendorBillLine billLine : sourceBill.getLines()) {
                if (!pol.getId().equals(billLine.getPurchaseOrderLineId())) {
                    continue;
                }
                BigDecimal alreadyCredited = creditedOnBill.getOrDefault(billLine.getId(), BigDecimal.ZERO);
                BigDecimal availableOnBill = billLine.getQty().subtract(alreadyCredited).max(BigDecimal.ZERO);
                BigDecimal qty = remaining.min(availableOnBill);
                if (qty.signum() <= 0) {
                    continue;
                }
                CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand lc =
                        new CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand();
                lc.setBillLineId(billLine.getId());
                lc.setQty(qty);
                cnLines.add(lc);
                remaining = remaining.subtract(qty);
                remainingByPoLine.put(pol.getId(), remaining);
                break;
            }
        }
        if (cnLines.isEmpty()) {
            return null;
        }
        cnCmd.setLines(cnLines);
        return createCreditNoteFromVendorBill(sourceBill.getId(), cnCmd);
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
                            "Bill qty " + newQty + " exceeds billable qty " + maxQty
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
                                    "Credit qty " + newQty + " exceeds remaining creditable qty " + remaining
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
            BigDecimal qty = billableQtyForLine(pol, product, draftAllocated, companyId);
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
        return vendorPaymentRepository.findByVendorBillId(billId).stream()
                .filter(p -> p.getState() == VendorPaymentState.POSTED)
                .filter(p -> billCurrency.equalsIgnoreCase(p.getCurrencyCode()))
                .map(VendorPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPostedCreditNotesForBill(UUID sourceBillId, String billCurrency) {
        return vendorBillRepository.findByReversedBillId(sourceBillId).stream()
                .filter(cn -> cn.getState() == VendorBillState.POSTED)
                .filter(cn -> cn.getMoveType() == VendorBillMoveType.CREDIT_NOTE)
                .filter(cn -> billCurrency.equalsIgnoreCase(cn.getCurrencyCode()))
                .map(this::billTotalDocumentCurrency)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private void ensurePaymentWithinOutstanding(VendorBill bill, BigDecimal docAmt, String paymentCurrency) {
        bill.getLines().size();
        for (VendorBillLine line : bill.getLines()) {
            line.getTaxSnapshots().size();
        }
        String billCurrency = bill.getCurrencyCode();
        BigDecimal billTotal = billTotalDocumentCurrency(bill);
        BigDecimal paid = sumPostedPaymentsForBill(bill.getId(), billCurrency);
        BigDecimal outstanding;
        if (bill.getMoveType() == VendorBillMoveType.CREDIT_NOTE) {
            // Refunds against a credit note: outstanding = CN total - refunds already paid.
            outstanding = billTotal.subtract(paid).setScale(4, RoundingMode.HALF_UP);
            if (outstanding.signum() <= 0) {
                throw new PurchaseDomainException(
                        "error.purchase.creditNoteFullyRefunded", null, "Credit note is already fully refunded");
            }
        } else {
            BigDecimal credited = sumPostedCreditNotesForBill(bill.getId(), billCurrency);
            outstanding = billTotal.subtract(paid).subtract(credited).setScale(4, RoundingMode.HALF_UP);
            if (outstanding.signum() <= 0) {
                throw new PurchaseDomainException("error.purchase.vendorBillFullyPaid", null, "Vendor bill is already fully paid");
            }
        }
        if (!billCurrency.equalsIgnoreCase(paymentCurrency)) {
            return;
        }
        if (docAmt.compareTo(outstanding) > 0) {
            throw new PurchaseDomainException(
                    "Payment amount exceeds outstanding balance of " + outstanding.toPlainString()
                            + " " + billCurrency);
        }
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
                        "Credit qty " + qty + " exceeds remaining creditable qty " + remaining
                                + " on bill line " + srcLine.getName()
                                + " (already credited " + prior + " of " + srcLine.getQty() + ")");
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

        for (VendorBillLine line : bill.getLines()) {
            BigDecimal lineNetDoc = billLineNet(line);
            BigDecimal netComp = PurchaseTaxEngine.convertAtRate(lineNetDoc, rate);
            if (netComp.signum() > 0) {
                if (creditNote) {
                    items.add(new JournalItemCommand(line.getAccountId(), line.getName(), BigDecimal.ZERO, netComp,
                            bill.getCurrencyCode(), lineNetDoc.negate(), null));
                } else {
                    items.add(new JournalItemCommand(line.getAccountId(), line.getName(), netComp, BigDecimal.ZERO,
                            bill.getCurrencyCode(), lineNetDoc, null));
                }
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
        if (orderDiscDoc.signum() > 0 && apDocTotal.signum() > 0) {
            // Reduce expense side and AP by the order discount so the payable matches the PO total.
            BigDecimal untaxedDoc = BigDecimal.ZERO;
            for (VendorBillLine line : bill.getLines()) {
                untaxedDoc = untaxedDoc.add(billLineNet(line));
            }
            BigDecimal discDoc = orderDiscDoc.min(untaxedDoc);
            BigDecimal discComp = PurchaseTaxEngine.convertAtRate(discDoc, rate);
            if (discComp.signum() > 0 && !items.isEmpty()) {
                // Credit the first expense line (or debit for credit notes) to book the discount.
                JournalItemCommand first = items.get(0);
                if (creditNote) {
                    items.set(0, new JournalItemCommand(
                            first.getAccountId(), first.getLabel(),
                            first.getDebit().add(discComp), first.getCredit(),
                            first.getCurrencyCode(),
                            first.getAmountCurrency() != null ? first.getAmountCurrency().add(discDoc) : discDoc,
                            first.getPartnerId()));
                } else {
                    BigDecimal newDebit = first.getDebit().subtract(discComp).max(BigDecimal.ZERO);
                    BigDecimal newAmtCur = first.getAmountCurrency() != null
                            ? first.getAmountCurrency().subtract(discDoc)
                            : discDoc.negate();
                    items.set(0, new JournalItemCommand(
                            first.getAccountId(), first.getLabel(),
                            newDebit, first.getCredit(),
                            first.getCurrencyCode(), newAmtCur, first.getPartnerId()));
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
    public List<VendorPaymentResponse> listVendorPayments(UUID companyId) {
        UUID cid = companyIdOrDefault(companyId);
        return vendorPaymentRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(cid).stream()
                .map(this::toVendorPaymentListRow)
                .collect(Collectors.toList());
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
        List<VendorBill> bills = vendorBillRepository
                .findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(cid, partnerId);
        for (VendorBill b : bills) {
            b.getLines().size();
            for (VendorBillLine line : b.getLines()) {
                line.getTaxSnapshots().size();
            }
        }
        List<VendorPayment> payments = vendorPaymentRepository
                .findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(cid, partnerId);

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
        period.sort(Comparator.comparing(PayEvt::d)
                .thenComparing(PayEvt::created)
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
                row.setVendorBillId(p.getVendorBillId());
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
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        VendorBill bill = vendorBillRepository.findById(command.getVendorBillId())
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found"));
        if (!bill.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("error.purchase.billCompanyMismatch", null, "Bill company mismatch");
        }
        if (bill.getState() != VendorBillState.POSTED || bill.getJournalEntryId() == null) {
            throw new PurchaseDomainException("error.purchase.billMustBePostedBeforePayment", null, "Bill must be posted before payment");
        }
        boolean refund = bill.getMoveType() == VendorBillMoveType.CREDIT_NOTE;
        periodPostingGuard.assertDatePostable(companyId, command.getPaymentDate().toLocalDate());
        String paymentCurrency = command.getCurrencyCode() != null ? command.getCurrencyCode() : bill.getCurrencyCode();
        BigDecimal docAmt = command.getAmount().setScale(4, RoundingMode.HALF_UP);
        ensurePaymentWithinOutstanding(bill, docAmt, paymentCurrency);
        PartnerResponse vendor = partnerApplicationService.getPartner(bill.getVendorPartnerId());
        UUID payableAccount = vendor.getPayableAccountId() != null
                ? vendor.getPayableAccountId()
                : accountingReferenceLookupPort.resolveAccountIdByCode(companyId, DEFAULT_AP_ACCOUNT_CODE);

        JournalType paymentJournalType = accountingReferenceLookupPort
                .resolveJournalType(companyId, command.getBankJournalId());
        UUID liquidityAccountId = resolveLiquidityAccountForPaymentJournal(companyId, command.getBankJournalId());
        String liquidityLabel = paymentJournalType == JournalType.CASH
                ? (refund ? "Cash refund" : "Cash payment")
                : (refund ? "Bank refund" : "Bank payment");

        BigDecimal billRate = resolveExchangeRate(
                companyId, bill.getCurrencyCode(), bill.getBillDate(), bill.getExchangeRateToCompany());
        BigDecimal paymentRate = resolveExchangeRate(
                companyId, paymentCurrency, command.getPaymentDate().toLocalDate(), command.getExchangeRateToCompany());

        BigDecimal apClearComp = CurrencyMath.convertAtRate(docAmt, billRate);
        BigDecimal liquidityComp = CurrencyMath.convertAtRate(docAmt, paymentRate);
        BigDecimal fxDiff = refund
                ? liquidityComp.subtract(apClearComp).setScale(4, RoundingMode.HALF_UP)
                : apClearComp.subtract(liquidityComp).setScale(4, RoundingMode.HALF_UP);

        List<JournalItemCommand> items = new ArrayList<>();
        if (refund) {
            // Opposite of normal payment: debit liquidity, credit AP.
            items.add(new JournalItemCommand(liquidityAccountId, liquidityLabel, liquidityComp, BigDecimal.ZERO,
                    paymentCurrency, docAmt, null));
            items.add(new JournalItemCommand(payableAccount, "Refund " + bill.getReference(), BigDecimal.ZERO, apClearComp,
                    paymentCurrency, docAmt.negate(), bill.getVendorPartnerId()));
        } else {
            items.add(new JournalItemCommand(payableAccount, "Payment " + bill.getReference(), apClearComp, BigDecimal.ZERO,
                    paymentCurrency, docAmt, bill.getVendorPartnerId()));
            items.add(new JournalItemCommand(liquidityAccountId, liquidityLabel, BigDecimal.ZERO, liquidityComp,
                    paymentCurrency, docAmt.negate(), null));
        }
        appendVendorExchangeDifference(items, companyId, fxDiff);
        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                command.getBankJournalId(),
                "",
                    JournalEntryTiming.ensureTimed(command.getPaymentDate()),
                paymentCurrency,
                bill.getVendorPartnerId(),
                items);
        CreateJournalEntryResponse payEntry = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(payEntry.getJournalEntryId());

        JournalEntryResponse billEntry = journalEntryApplicationService.getJournalEntry(bill.getJournalEntryId());
        UUID billApItem;
        if (refund) {
            billApItem = billEntry.getItems().stream()
                    .filter(i -> payableAccount.equals(i.getAccountId()) && i.getDebit().compareTo(BigDecimal.ZERO) > 0)
                    .map(JournalEntryResponse.JournalItemResponse::getId)
                    .findFirst()
                    .orElseThrow(() -> new PurchaseDomainException("Could not find AP line on vendor credit note entry"));
        } else {
            billApItem = billEntry.getItems().stream()
                    .filter(i -> payableAccount.equals(i.getAccountId()) && i.getCredit().compareTo(BigDecimal.ZERO) > 0)
                    .map(JournalEntryResponse.JournalItemResponse::getId)
                    .findFirst()
                    .orElseThrow(() -> new PurchaseDomainException("Could not find AP line on vendor bill entry"));
        }
        JournalEntryResponse paymentEntry = journalEntryApplicationService.getJournalEntry(payEntry.getJournalEntryId());
        UUID payApItem;
        if (refund) {
            payApItem = paymentEntry.getItems().stream()
                    .filter(i -> payableAccount.equals(i.getAccountId()) && i.getCredit().compareTo(BigDecimal.ZERO) > 0)
                    .map(JournalEntryResponse.JournalItemResponse::getId)
                    .findFirst()
                    .orElseThrow(() -> new PurchaseDomainException("Could not find AP line on refund entry"));
        } else {
            payApItem = paymentEntry.getItems().stream()
                    .filter(i -> payableAccount.equals(i.getAccountId()) && i.getDebit().compareTo(BigDecimal.ZERO) > 0)
                    .map(JournalEntryResponse.JournalItemResponse::getId)
                    .findFirst()
                    .orElseThrow(() -> new PurchaseDomainException("Could not find AP line on payment entry"));
        }

        UUID reconciliationId = UUID.randomUUID();
        reconciliationApplicationService.reconcile(
                new ReconciliationApplicationService.ReconcileCommand(
                        List.of(billApItem, payApItem), reconciliationId));

        Instant now = Instant.now();
        VendorPayment p = new VendorPayment();
        p.setId(UUID.randomUUID());
        p.setCompanyId(companyId);
        p.setVendorPartnerId(bill.getVendorPartnerId());
        p.setVendorBillId(bill.getId());
        p.setPaymentDate(command.getPaymentDate());
        p.setBankJournalId(command.getBankJournalId());
        p.setAmount(docAmt);
        p.setCurrencyCode(paymentCurrency);
        p.setExchangeRateToCompany(paymentRate);
        p.setState(VendorPaymentState.POSTED);
        p.setPaymentKind(refund ? VendorPaymentKind.REFUND : VendorPaymentKind.PAYOUT);
        p.setJournalEntryId(payEntry.getJournalEntryId());
        String paymentReference = command.getReference();
        if (paymentReference == null || paymentReference.isBlank()) {
            paymentReference = paymentEntry.getSequenceNumber();
        }
        p.setReference(paymentReference);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        vendorPaymentRepository.save(p);

        String actionLabel = refund ? "Refund registered: " : "Payment registered: ";
        activityLogger.log(p.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_PAYMENT, p.getId(),
                actionLabel + docAmt.toPlainString() + " " + paymentCurrency);
        activityLogger.log(bill.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, bill.getId(),
                actionLabel + docAmt.toPlainString() + " " + paymentCurrency
                        + (p.getReference() != null ? " (" + p.getReference() + ")" : ""));
        if (bill.getPurchaseOrderId() != null) {
            activityLogger.log(bill.getCompanyId(), RecordActivityLogger.MODEL_PURCHASE_ORDER, bill.getPurchaseOrderId(),
                    actionLabel + "on bill " + (bill.getReference() != null ? bill.getReference() : bill.getId())
                            + ": " + docAmt.toPlainString() + " " + paymentCurrency);
        }

        VendorPaymentResponse r = new VendorPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(companyId);
        r.setVendorPartnerId(p.getVendorPartnerId());
        r.setVendorBillId(bill.getId());
        r.setPaymentDate(command.getPaymentDate());
        r.setBankJournalId(p.getBankJournalId());
        r.setAmount(docAmt);
        r.setCurrencyCode(paymentCurrency);
        r.setExchangeRateToCompany(paymentRate);
        r.setState(VendorPaymentState.POSTED);
        r.setPaymentKind(p.getPaymentKind());
        r.setJournalEntryId(payEntry.getJournalEntryId());
        r.setReconciliationId(reconciliationId);
        r.setReference(paymentReference);
        purchaseEventPublisher.publishVendorPaymentRegistered(new VendorPaymentRegisteredEvent(
                UUID.randomUUID(),
                Instant.now(),
                companyId,
                p.getId(),
                p.getVendorPartnerId(),
                p.getVendorBillId()));
        return r;
    }

    @Override
    @Transactional
    public VendorPaymentResponse reverseVendorPayment(UUID paymentId, String reason) {
        VendorPayment p = vendorPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor payment not found"));
        if (p.getState() != VendorPaymentState.POSTED) {
            throw new PurchaseDomainException("Only posted payments can be reversed");
        }
        if (p.getJournalEntryId() == null) {
            throw new PurchaseDomainException("Payment has no journal entry to reverse");
        }
        JournalEntryResponse paymentEntry = journalEntryApplicationService.getJournalEntry(p.getJournalEntryId());
        List<UUID> reconciliationIds = paymentEntry.getItems().stream()
                .map(JournalEntryResponse.JournalItemResponse::getReconciliationId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (!reconciliationIds.isEmpty()) {
            List<UUID> itemIds = journalItemReconciliationPort.findItemIdsByReconciliationIds(reconciliationIds);
            reconciliationApplicationService.unreconcile(
                    new ReconciliationApplicationService.UnreconcileCommand(itemIds));
        }
        var reverseResp = journalEntryApplicationService.reverseJournalEntry(
                new com.bradox.delin.accounting.service.domain.create.ReverseJournalEntryCommand(
                        p.getJournalEntryId(),
                        reason != null && !reason.isBlank() ? reason : "Payment reverse"));
        p.setState(VendorPaymentState.REVERSED);
        p.setReversalJournalEntryId(reverseResp.getReversalJournalEntryId());
        p.setUpdatedAt(Instant.now());
        vendorPaymentRepository.save(p);

        activityLogger.log(p.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_PAYMENT, p.getId(),
                "Payment reversed: " + (reason != null ? reason : ""));
        return toVendorPaymentListRow(p);
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
        r.setAmountTotal(o.getAmountTotal());
        r.setOrderDiscountType(o.getOrderDiscountType());
        r.setOrderDiscountValue(o.getOrderDiscountValue());
        r.setOrderDiscountPercent(o.getOrderDiscountPercent());
        OrderPaymentFields payment = computePurchasePaymentFields(o);
        r.setPaymentStatus(payment.paymentStatus());
        r.setAmountPaid(payment.amountPaid());
        r.setAmountDue(payment.amountDue());
        r.setExchangeRateToCompany(o.getExchangeRateToCompany());
        r.setSentAt(o.getSentAt());
        r.setConfirmedAt(o.getConfirmedAt());
        r.setCancelledAt(o.getCancelledAt());
        r.setLocked(o.isLocked());
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

    private VendorPaymentResponse toVendorPaymentListRow(VendorPayment p) {
        VendorPaymentResponse r = new VendorPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(p.getCompanyId());
        r.setVendorPartnerId(p.getVendorPartnerId());
        r.setVendorBillId(p.getVendorBillId());
        r.setPaymentDate(p.getPaymentDate());
        r.setBankJournalId(p.getBankJournalId());
        r.setAmount(p.getAmount());
        r.setCurrencyCode(p.getCurrencyCode());
        r.setExchangeRateToCompany(p.getExchangeRateToCompany());
        r.setState(p.getState());
        r.setPaymentKind(p.getPaymentKind() != null ? p.getPaymentKind() : VendorPaymentKind.PAYOUT);
        r.setJournalEntryId(p.getJournalEntryId());
        r.setReconciliationId(null);
        r.setReversalJournalEntryId(p.getReversalJournalEntryId());
        r.setReference(p.getReference());
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
                    .append(oldQty.stripTrailingZeros().toPlainString())
                    .append(" → ")
                    .append(newQty.stripTrailingZeros().toPlainString());
            if (line.getQtyInvoiced() != null && line.getQtyInvoiced().signum() > 0) {
                block.append("\n  Billed Quantity: ")
                        .append(line.getQtyInvoiced().stripTrailingZeros().toPlainString());
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
                    untaxedBefore.stripTrailingZeros().toPlainString(),
                    untaxedAfter.stripTrailingZeros().toPlainString(),
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

    private void appendVendorExchangeDifference(List<JournalItemCommand> items, UUID companyId, BigDecimal fxDiff) {
        if (fxDiff.signum() == 0) {
            return;
        }
        if (fxDiff.signum() > 0) {
            UUID gainAccount = accountingReferenceLookupPort.resolveAccountIdByCode(
                    companyId, EXCHANGE_GAIN_ACCOUNT_CODE);
            items.add(new JournalItemCommand(gainAccount, "Exchange gain", BigDecimal.ZERO, fxDiff,
                    null, null, null));
        } else {
            UUID lossAccount = accountingReferenceLookupPort.resolveAccountIdByCode(
                    companyId, EXCHANGE_LOSS_ACCOUNT_CODE);
            items.add(new JournalItemCommand(lossAccount, "Exchange loss", fxDiff.abs(), BigDecimal.ZERO,
                    null, null, null));
        }
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
