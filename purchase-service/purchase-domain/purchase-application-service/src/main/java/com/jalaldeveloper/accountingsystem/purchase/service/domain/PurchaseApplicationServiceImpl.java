package com.jalaldeveloper.accountingsystem.purchase.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.CurrencyMath;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementLineResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PartnerResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockLocation;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateStockPickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockMoveCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockPickingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValidatePickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.UomApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.StockMovePurchaseQueryPort;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.WarehouseRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.*;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.FiscalTaxSnapshot;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.PurchaseTaxEngine;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorBillPostedEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorPaymentRegisteredEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.FiscalTax;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrder;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrderLine;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrderLineTax;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBill;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBillLine;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBillLineTax;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorPayment;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.FiscalTaxRepository;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.PurchaseOrderRepository;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.VendorBillRepository;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.VendorPaymentRepository;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.messaging.PurchaseEventPublisher;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    private final ProductCategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository stockLocationRepository;
    private final UomApplicationService uomApplicationService;
    private final StockPickingApplicationService stockPickingApplicationService;
    private final StockMovePurchaseQueryPort stockMovePurchaseQueryPort;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final AccountingReferenceLookupPort accountingReferenceLookupPort;
    private final ReconciliationApplicationService reconciliationApplicationService;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final CurrencyConversionPort currencyConversionPort;
    private final PurchaseOrderQtyWriter purchaseOrderQtyWriter;

    public PurchaseApplicationServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                          FiscalTaxRepository fiscalTaxRepository,
                                          VendorBillRepository vendorBillRepository,
                                          VendorPaymentRepository vendorPaymentRepository,
                                          PartnerApplicationService partnerApplicationService,
                                          ProductRepository productRepository,
                                          ProductCategoryRepository categoryRepository,
                                          WarehouseRepository warehouseRepository,
                                          StockLocationRepository stockLocationRepository,
                                          UomApplicationService uomApplicationService,
                                          StockPickingApplicationService stockPickingApplicationService,
                                          StockMovePurchaseQueryPort stockMovePurchaseQueryPort,
                                          JournalEntryApplicationService journalEntryApplicationService,
                                          AccountingReferenceLookupPort accountingReferenceLookupPort,
                                          ReconciliationApplicationService reconciliationApplicationService,
                                          ObjectProvider<CompanyContext> companyContextProvider,
                                          PurchaseEventPublisher purchaseEventPublisher,
                                          CurrencyConversionPort currencyConversionPort,
                                          PurchaseOrderQtyWriter purchaseOrderQtyWriter) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.fiscalTaxRepository = fiscalTaxRepository;
        this.vendorBillRepository = vendorBillRepository;
        this.vendorPaymentRepository = vendorPaymentRepository;
        this.partnerApplicationService = partnerApplicationService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.uomApplicationService = uomApplicationService;
        this.stockPickingApplicationService = stockPickingApplicationService;
        this.stockMovePurchaseQueryPort = stockMovePurchaseQueryPort;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.accountingReferenceLookupPort = accountingReferenceLookupPort;
        this.reconciliationApplicationService = reconciliationApplicationService;
        this.companyContextProvider = companyContextProvider;
        this.purchaseEventPublisher = purchaseEventPublisher;
        this.currencyConversionPort = currencyConversionPort;
        this.purchaseOrderQtyWriter = purchaseOrderQtyWriter;
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) return fromCommand;
        return companyContextProvider.getObject().requireCompany().getId();
    }

    private UUID resolveLiquidityAccountForPaymentJournal(UUID companyId, UUID journalId) {
        JournalType journalType = accountingReferenceLookupPort.resolveJournalType(companyId, journalId);
        if (journalType != JournalType.CASH && journalType != JournalType.BANK) {
            throw new PurchaseDomainException("Payment journal must be cash or bank");
        }
        return accountingReferenceLookupPort.resolveLiquidityAccountIdForJournal(companyId, journalId);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PartnerResponse vendor = partnerApplicationService.getPartner(command.getVendorPartnerId());
        if (!vendor.isVendor()) {
            throw new PurchaseDomainException("Partner is not a vendor");
        }
        if (!vendor.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("Vendor belongs to another company");
        }
        Instant now = Instant.now();
        PurchaseOrder o = new PurchaseOrder();
        o.setId(UUID.randomUUID());
        o.setCompanyId(companyId);
        o.setVendorPartnerId(command.getVendorPartnerId());
        o.setName(command.getName() != null && !command.getName().isBlank()
                ? command.getName()
                : "PO-" + o.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        if (purchaseOrderRepository.findByCompanyIdAndName(companyId, o.getName()).isPresent()) {
            throw new PurchaseDomainException("Purchase order name already exists: " + o.getName());
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
        o.setExchangeRateToCompany(resolveExchangeRate(
                companyId, command.getCurrencyCode(), o.getOrderDate(), command.getExchangeRateToCompany()));
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        int seq = 10;
        for (PurchaseOrderLineCommand lc : command.getLines()) {
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setId(UUID.randomUUID());
            line.setSequence(seq);
            line.setProductId(lc.getProductId());
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            line.setWarehouseId(lc.getWarehouseId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setQtyReceived(BigDecimal.ZERO);
            line.setQtyInvoiced(BigDecimal.ZERO);
            line.setUnitPrice(lc.getUnitPrice());
            line.setDiscountPercent(lc.getDiscountPercent() != null ? lc.getDiscountPercent() : BigDecimal.ZERO);
            line.setExpectedDate(lc.getExpectedDate());
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTax tax = fiscalTaxRepository.findById(taxId)
                        .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new PurchaseDomainException("Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.PURCHASE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new PurchaseDomainException("Tax scope not valid for purchase: " + taxId);
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
        return toResponse(purchaseOrderRepository.save(o));
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
        return r;
    }

    private boolean computeCanCreateVendorBill(PurchaseOrder po) {
        if (po.getState() != PurchaseOrderState.CONFIRMED) {
            return false;
        }
        Map<UUID, BigDecimal> draftAllocated = draftBillQtyByPoLine(po.getId());
        for (PurchaseOrderLine pol : po.getLines()) {
            Optional<Product> opt = productRepository.findById(new ProductId(pol.getProductId()));
            if (opt.isEmpty()) {
                continue;
            }
            if (billableQtyForLine(pol, opt.get(), draftAllocated).signum() > 0) {
                return true;
            }
        }
        return false;
    }

    /** Quantities already reserved on draft vendor bills (not yet posted to qtyInvoiced). */
    private Map<UUID, BigDecimal> draftBillQtyByPoLine(UUID purchaseOrderId) {
        return draftBillQtyByPoLine(purchaseOrderId, VendorBillMoveType.BILL);
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
                                          Map<UUID, BigDecimal> draftAllocated) {
        BigDecimal invoiced = effectiveQtyInvoiced(pol, draftAllocated);
        if (product.getProductType() == ProductType.SERVICE) {
            return pol.getQtyOrdered().subtract(invoiced).max(BigDecimal.ZERO);
        }
        return pol.getQtyReceived().subtract(invoiced).max(BigDecimal.ZERO);
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
                    line.getQtyOrdered(), line.getUnitPrice(), line.getDiscountPercent(), snaps);
            untaxed = untaxed.add(split.net());
            tax = tax.add(split.taxTotal());
        }
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
        return toResponse(purchaseOrderRepository.save(o));
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
            throw new PurchaseDomainException("warehouseId is required to confirm a purchase order");
        }
        Warehouse wh = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new PurchaseDomainException("Warehouse not found: " + warehouseId));
        if (!wh.getCompanyId().getId().equals(o.getCompanyId())) {
            throw new PurchaseDomainException("Warehouse company mismatch");
        }
        StockLocation supplier = findSupplierVirtual(o.getCompanyId());
        UUID destLoc = o.getDestLocationId() != null
                ? o.getDestLocationId()
                : wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (destLoc == null) {
            throw new PurchaseDomainException("Destination stock location could not be resolved");
        }

        BigDecimal rateToCompany = resolveExchangeRate(
                o.getCompanyId(), o.getCurrencyCode(), o.getOrderDate(), o.getExchangeRateToCompany());
        o.setExchangeRateToCompany(rateToCompany);

        List<StockMoveCommand> moves = new ArrayList<>();
        for (PurchaseOrderLine line : o.getLines()) {
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + line.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                continue;
            }
            if (!product.isPurchaseOk()) {
                throw new PurchaseDomainException("Product is not purchasable: " + line.getProductId());
            }
            BigDecimal remaining = line.getQtyOrdered().subtract(line.getQtyReceived());
            if (remaining.signum() <= 0) {
                continue;
            }
            UUID stockUom = product.getUomId().getId();
            BigDecimal demandStockUom = uomApplicationService.convert(line.getUomId(), stockUom, remaining);
            BigDecimal oneInStockUom = uomApplicationService.convert(line.getUomId(), stockUom, BigDecimal.ONE);
            BigDecimal lineNetOne = PurchaseOrderRules.lineNet(BigDecimal.ONE, line.getUnitPrice(), line.getDiscountPercent());
            BigDecimal unitCostDoc = oneInStockUom.signum() > 0
                    ? lineNetOne.divide(oneInStockUom, 8, RoundingMode.HALF_UP)
                    : lineNetOne;
            // Stock valuation / COGS are kept in company currency (same as invoice GL amounts).
            BigDecimal unitCost = CurrencyMath.convertAtRate(unitCostDoc, rateToCompany);

            StockMoveCommand mc = new StockMoveCommand();
            mc.setProductId(line.getProductId());
            mc.setUomId(stockUom);
            mc.setDemandQuantity(demandStockUom);
            mc.setUnitCost(unitCost);
            mc.setPurchaseOrderLineId(line.getId());
            moves.add(mc);
        }

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
            cmd.setMoves(moves);
            stockPickingApplicationService.createPicking(cmd);
            purchaseOrderRepository.flush();
        }

        o.setState(PurchaseOrderState.CONFIRMED);
        o.setConfirmedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        purchaseOrderRepository.flush();
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(UUID id) {
        PurchaseOrder o = loadOrder(id);
        PurchaseOrderRules.ensureCanCancel(o.getState());
        if (o.getState() == PurchaseOrderState.CONFIRMED) {
            if (vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                    .anyMatch(b -> b.getState() == VendorBillState.POSTED)) {
                throw new PurchaseDomainException("Cannot cancel: posted vendor bills exist for this order");
            }
            if (stockMovePurchaseQueryPort.existsNonTerminalPickingForPurchaseOrder(o.getId())) {
                throw new PurchaseDomainException("Cannot cancel: open pickings exist (confirm/cancel pickings first)");
            }
        }
        o.setState(PurchaseOrderState.CANCELLED);
        o.setCancelledAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return toResponse(purchaseOrderRepository.save(o));
    }

    @Override
    @Transactional
    public StockPickingResponse validateReceiptPicking(UUID pickingId, ValidatePickingCommand command) {
        // validatePicking() already refreshes qty_received via PurchaseReceiveSyncPort.
        return stockPickingApplicationService.validatePicking(pickingId,
                command != null ? command : new ValidatePickingCommand());
    }

    @Override
    public void syncPurchaseOrderLineQtyReceivedFromStockMoves(UUID purchaseOrderId) {
        PurchaseOrder o = purchaseOrderQtyWriter.updateQtyReceived(purchaseOrderId);
        if (o != null) {
            tryAutoCreateAndPostCreditNoteFromReturn(o);
        }
    }

    private BigDecimal creditNoteableQtyForPoLine(PurchaseOrderLine pol, Map<UUID, BigDecimal> draftCreditNotes) {
        BigDecimal draft = draftCreditNotes.getOrDefault(pol.getId(), BigDecimal.ZERO);
        return pol.getQtyInvoiced().subtract(pol.getQtyReceived()).subtract(draft).max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private void tryAutoCreateAndPostCreditNoteFromReturn(PurchaseOrder o) {
        Map<UUID, BigDecimal> draftCn = draftCreditNoteQtyByPoLine(o.getId());
        boolean hasCreditable = o.getLines().stream()
                .anyMatch(pol -> creditNoteableQtyForPoLine(pol, draftCn).signum() > 0);
        if (!hasCreditable) {
            return;
        }
        List<VendorBill> postedBills = vendorBillRepository.findByPurchaseOrderId(o.getId()).stream()
                .filter(b -> b.getState() == VendorBillState.POSTED)
                .filter(b -> b.getMoveType() == null || b.getMoveType() == VendorBillMoveType.BILL)
                .toList();
        if (postedBills.size() != 1) {
            return;
        }
        VendorBillResponse cn = createVendorCreditNoteFromPurchaseOrder(o, postedBills.get(0), draftCn);
        postVendorBill(cn.getId());
    }

    private VendorBillResponse createVendorCreditNoteFromPurchaseOrder(PurchaseOrder po,
                                                                         VendorBill sourceBill,
                                                                         Map<UUID, BigDecimal> draftCn) {
        sourceBill.getLines().size();
        for (VendorBillLine line : sourceBill.getLines()) {
            line.getTaxSnapshots().size();
        }
        CreateCreditNoteFromVendorBillCommand cnCmd = new CreateCreditNoteFromVendorBillCommand();
        cnCmd.setCompanyId(po.getCompanyId());
        cnCmd.setBillDate(LocalDate.now());
        cnCmd.setReference("CN/PO/" + po.getName());
        List<CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand> cnLines = new ArrayList<>();
        for (PurchaseOrderLine pol : po.getLines()) {
            BigDecimal qty = creditNoteableQtyForPoLine(pol, draftCn);
            if (qty.signum() <= 0) {
                continue;
            }
            for (VendorBillLine billLine : sourceBill.getLines()) {
                if (pol.getId().equals(billLine.getPurchaseOrderLineId())) {
                    CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand lc =
                            new CreateCreditNoteFromVendorBillCommand.CreditNoteLineQtyCommand();
                    lc.setBillLineId(billLine.getId());
                    lc.setQty(qty);
                    cnLines.add(lc);
                    break;
                }
            }
        }
        if (cnLines.isEmpty()) {
            throw new PurchaseDomainException(
                    "No credit-note quantity: return received goods first so qty invoiced exceeds qty received");
        }
        cnCmd.setLines(cnLines);
        return createCreditNoteFromVendorBill(sourceBill.getId(), cnCmd);
    }

    @Override
    @Transactional
    public VendorBillResponse createVendorBillFromPo(CreateVendorBillFromPoCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PurchaseOrder po = loadOrder(command.getPurchaseOrderId());
        if (!po.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("Purchase order company mismatch");
        }
        if (po.getState() == PurchaseOrderState.CANCELLED) {
            throw new PurchaseDomainException("Cannot bill a cancelled purchase order");
        }
        if (po.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException("Purchase order must be confirmed before billing");
        }
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
        bill.setReference(command.getReference() != null ? command.getReference() : "BILL/" + bill.getId().toString().substring(0, 8));
        bill.setCurrencyCode(po.getCurrencyCode());
        bill.setState(VendorBillState.DRAFT);
        bill.setMoveType(VendorBillMoveType.BILL);
        bill.setExchangeRateToCompany(resolveExchangeRate(
                po.getCompanyId(), po.getCurrencyCode(), command.getBillDate(), po.getExchangeRateToCompany()));
        bill.setCreatedAt(now);
        bill.setUpdatedAt(now);

        Map<UUID, BigDecimal> draftAllocated = draftBillQtyByPoLine(po.getId());
        int seq = 10;
        for (PurchaseOrderLine pol : po.getLines()) {
            Product product = productRepository.findById(new ProductId(pol.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + pol.getProductId()));
            BigDecimal qty = billableQtyForLine(pol, product, draftAllocated);
            if (qty.signum() <= 0) {
                continue;
            }
            if (product.getProductType() == ProductType.SERVICE) {
                VendorBillLine vbl = new VendorBillLine();
                vbl.setId(UUID.randomUUID());
                vbl.setSequence(seq);
                vbl.setPurchaseOrderLineId(pol.getId());
                vbl.setProductId(pol.getProductId());
                vbl.setName(pol.getName());
                vbl.setUomId(pol.getUomId());
                vbl.setQty(qty);
                vbl.setUnitPrice(pol.getUnitPrice());
                vbl.setAccountId(resolveExpenseAccount(product));
                vbl.setCreatedAt(now);
                vbl.setUpdatedAt(now);
                addBillTaxSnapshots(vbl, pol, now);
                bill.getLines().add(vbl);
                seq += 10;
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
            vbl.setAccountId(resolveStockInputAccount(product));
            vbl.setCreatedAt(now);
            vbl.setUpdatedAt(now);
            addBillTaxSnapshots(vbl, pol, now);
            bill.getLines().add(vbl);
            seq += 10;
        }
        if (bill.getLines().isEmpty()) {
            throw new PurchaseDomainException(
                    "No billable quantity: for stockable/consumable lines, receive goods first (qty received > qty "
                            + "invoiced); for service lines, bill from ordered quantity. "
                            + "If a draft bill already exists, post or remove it first.");
        }
        return toBillResponse(vendorBillRepository.save(bill));
    }

    private void addBillTaxSnapshots(VendorBillLine vbl, PurchaseOrderLine pol, Instant now) {
        List<FiscalTaxSnapshot> snaps = pol.getTaxes().stream()
                .map(lt -> fiscalTaxRepository.findById(lt.getTaxId()).orElseThrow())
                .map(t -> new FiscalTaxSnapshot(t.getId(), t.getAmountType(), t.getAmount(), t.isPriceInclude()))
                .toList();
        PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                vbl.getQty(), vbl.getUnitPrice(), pol.getDiscountPercent(), snaps);
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

    private BigDecimal discountForBillLine(VendorBill bill, VendorBillLine line) {
        if (bill.getPurchaseOrderId() == null || line.getPurchaseOrderLineId() == null) {
            return BigDecimal.ZERO;
        }
        return purchaseOrderRepository.findById(bill.getPurchaseOrderId()).stream()
                .flatMap(po -> po.getLines().stream())
                .filter(l -> l.getId().equals(line.getPurchaseOrderLineId()))
                .map(PurchaseOrderLine::getDiscountPercent)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal billTotalDocumentCurrency(VendorBill bill) {
        BigDecimal total = BigDecimal.ZERO;
        for (VendorBillLine line : bill.getLines()) {
            BigDecimal disc = discountForBillLine(bill, line);
            BigDecimal lineNetDoc = PurchaseOrderRules.lineNet(line.getQty(), line.getUnitPrice(), disc)
                    .setScale(4, RoundingMode.HALF_UP);
            total = total.add(lineNetDoc);
            for (VendorBillLineTax ts : line.getTaxSnapshots()) {
                total = total.add(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }
        return total;
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
        BigDecimal credited = sumPostedCreditNotesForBill(bill.getId(), billCurrency);
        BigDecimal outstanding = billTotal.subtract(paid).subtract(credited).setScale(4, RoundingMode.HALF_UP);
        if (outstanding.signum() <= 0) {
            throw new PurchaseDomainException("Vendor bill is already fully paid");
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
        throw new PurchaseDomainException("Product category has no stock input account");
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
            throw new PurchaseDomainException("Bill company mismatch");
        }
        if (source.getState() != VendorBillState.POSTED) {
            throw new PurchaseDomainException("Only posted vendor bills can be credited");
        }
        if (source.getMoveType() == VendorBillMoveType.CREDIT_NOTE) {
            throw new PurchaseDomainException("Cannot create a credit note from another credit note");
        }
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
            throw new PurchaseDomainException("Credit note has no lines");
        }
        return toBillResponse(vendorBillRepository.save(cn));
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
            throw new PurchaseDomainException("Cannot post a cancelled bill");
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
            BigDecimal disc = discountForBillLine(bill, line);
            BigDecimal lineNetDoc = PurchaseOrderRules.lineNet(line.getQty(), line.getUnitPrice(), disc)
                    .setScale(4, RoundingMode.HALF_UP);
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
                    bill.getBillDate(),
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

        if (bill.getPurchaseOrderId() != null) {
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
            throw new PurchaseDomainException("Vendor bill not found");
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
                .map(this::toBillSummaryResponse)
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
            throw new PurchaseDomainException("Statement end date must be on or after start date");
        }
        PartnerResponse partner = partnerApplicationService.getPartner(partnerId);
        if (!partner.getCompanyId().equals(cid)) {
            throw new PurchaseDomainException("Partner belongs to another company");
        }
        if (!partner.isVendor()) {
            throw new PurchaseDomainException("Partner is not a vendor");
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
            if (p.getPaymentDate().isBefore(from)) {
                opening = opening.subtract(p.getAmount());
            }
        }
        opening = opening.setScale(4, RoundingMode.HALF_UP);

        record PayEvt(LocalDate d, Instant created, String idKey, VendorBill bill, VendorPayment pay) {}

        List<PayEvt> period = new ArrayList<>();
        for (VendorBill b : bills) {
            if (b.getState() != VendorBillState.POSTED) {
                continue;
            }
            if (!currency.equalsIgnoreCase(b.getCurrencyCode())) {
                continue;
            }
            if (!b.getBillDate().isBefore(from) && !b.getBillDate().isAfter(to)) {
                period.add(new PayEvt(b.getBillDate(), b.getCreatedAt(), "B:" + b.getId(), b, null));
            }
        }
        for (VendorPayment p : payments) {
            if (p.getState() != VendorPaymentState.POSTED) {
                continue;
            }
            if (!currency.equalsIgnoreCase(p.getCurrencyCode())) {
                continue;
            }
            if (!p.getPaymentDate().isBefore(from) && !p.getPaymentDate().isAfter(to)) {
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
            throw new PurchaseDomainException("Bill company mismatch");
        }
        if (bill.getState() != VendorBillState.POSTED || bill.getJournalEntryId() == null) {
            throw new PurchaseDomainException("Bill must be posted before payment");
        }
        if (bill.getMoveType() == VendorBillMoveType.CREDIT_NOTE) {
            throw new PurchaseDomainException("Cannot register payment against a credit note");
        }
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
        String liquidityLabel = paymentJournalType == JournalType.CASH ? "Cash payment" : "Bank payment";

        BigDecimal billRate = resolveExchangeRate(
                companyId, bill.getCurrencyCode(), bill.getBillDate(), bill.getExchangeRateToCompany());
        BigDecimal paymentRate = resolveExchangeRate(
                companyId, paymentCurrency, command.getPaymentDate(), command.getExchangeRateToCompany());

        BigDecimal apClearComp = CurrencyMath.convertAtRate(docAmt, billRate);
        BigDecimal liquidityComp = CurrencyMath.convertAtRate(docAmt, paymentRate);
        BigDecimal fxDiff = apClearComp.subtract(liquidityComp).setScale(4, RoundingMode.HALF_UP);

        List<JournalItemCommand> items = new ArrayList<>();
        items.add(new JournalItemCommand(payableAccount, "Payment " + bill.getReference(), apClearComp, BigDecimal.ZERO,
                paymentCurrency, docAmt, bill.getVendorPartnerId()));
        items.add(new JournalItemCommand(liquidityAccountId, liquidityLabel, BigDecimal.ZERO, liquidityComp,
                paymentCurrency, docAmt.negate(), null));
        appendVendorExchangeDifference(items, companyId, fxDiff);
        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                command.getBankJournalId(),
                "",
                command.getPaymentDate(),
                paymentCurrency,
                bill.getVendorPartnerId(),
                items);
        CreateJournalEntryResponse payEntry = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(payEntry.getJournalEntryId());

        JournalEntryResponse billEntry = journalEntryApplicationService.getJournalEntry(bill.getJournalEntryId());
        UUID billApItem = billEntry.getItems().stream()
                .filter(i -> payableAccount.equals(i.getAccountId()) && i.getCredit().compareTo(BigDecimal.ZERO) > 0)
                .map(JournalEntryResponse.JournalItemResponse::getId)
                .findFirst()
                .orElseThrow(() -> new PurchaseDomainException("Could not find AP line on vendor bill entry"));
        JournalEntryResponse paymentEntry = journalEntryApplicationService.getJournalEntry(payEntry.getJournalEntryId());
        UUID payApItem = paymentEntry.getItems().stream()
                .filter(i -> payableAccount.equals(i.getAccountId()) && i.getDebit().compareTo(BigDecimal.ZERO) > 0)
                .map(JournalEntryResponse.JournalItemResponse::getId)
                .findFirst()
                .orElseThrow(() -> new PurchaseDomainException("Could not find AP line on payment entry"));

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
        p.setJournalEntryId(payEntry.getJournalEntryId());
        p.setReference(command.getReference());
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        vendorPaymentRepository.save(p);

        VendorPaymentResponse r = new VendorPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(companyId);
        r.setVendorBillId(bill.getId());
        r.setPaymentDate(command.getPaymentDate());
        r.setAmount(docAmt);
        r.setCurrencyCode(paymentCurrency);
        r.setExchangeRateToCompany(paymentRate);
        r.setState(VendorPaymentState.POSTED);
        r.setJournalEntryId(payEntry.getJournalEntryId());
        r.setReconciliationId(reconciliationId);
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
        r.setExchangeRateToCompany(o.getExchangeRateToCompany());
        r.setSentAt(o.getSentAt());
        r.setConfirmedAt(o.getConfirmedAt());
        r.setCancelledAt(o.getCancelledAt());
        r.setReceiptPickingIds(stockMovePurchaseQueryPort.findPickingIdsByPurchaseOrderId(o.getId()));
        r.setCanCreateVendorBill(computeCanCreateVendorBill(o));
        r.setLines(o.getLines().stream().sorted(Comparator.comparingInt(PurchaseOrderLine::getSequence)).map(l -> {
            PurchaseOrderLineResponse lr = new PurchaseOrderLineResponse();
            lr.setId(l.getId());
            lr.setSequence(l.getSequence());
            lr.setProductId(l.getProductId());
            lr.setName(l.getName());
            lr.setUomId(l.getUomId());
            lr.setWarehouseId(l.getWarehouseId());
            lr.setQtyOrdered(l.getQtyOrdered());
            lr.setQtyReceived(l.getQtyReceived());
            lr.setQtyInvoiced(l.getQtyInvoiced());
            lr.setUnitPrice(l.getUnitPrice());
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
        r.setJournalEntryId(b.getJournalEntryId());
        r.setCreatedAt(b.getCreatedAt());
        return r;
    }

    private VendorPaymentResponse toVendorPaymentListRow(VendorPayment p) {
        VendorPaymentResponse r = new VendorPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(p.getCompanyId());
        r.setVendorBillId(p.getVendorBillId());
        r.setPaymentDate(p.getPaymentDate());
        r.setAmount(p.getAmount());
        r.setCurrencyCode(p.getCurrencyCode());
        r.setExchangeRateToCompany(p.getExchangeRateToCompany());
        r.setState(p.getState());
        r.setJournalEntryId(p.getJournalEntryId());
        r.setReconciliationId(null);
        return r;
    }

    private Map<UUID, BigDecimal> creditedQtyBySourceBillLine(VendorBill source) {
        Map<UUID, BigDecimal> credited = new LinkedHashMap<>();
        for (VendorBill cn : vendorBillRepository.findByReversedBillId(source.getId())) {
            if (cn.getState() == VendorBillState.CANCELLED) {
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
}
