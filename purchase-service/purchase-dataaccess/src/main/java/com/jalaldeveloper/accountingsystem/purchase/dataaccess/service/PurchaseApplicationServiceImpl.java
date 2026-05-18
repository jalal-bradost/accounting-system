package com.jalaldeveloper.accountingsystem.purchase.dataaccess.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
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
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.*;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository.*;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.*;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.FiscalTaxSnapshot;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.PurchaseTaxEngine;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorBillPostedEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorPaymentRegisteredEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.messaging.PurchaseEventPublisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    private final PurPurchaseOrderJpaRepository purchaseOrderRepository;
    private final PurFiscalTaxJpaRepository fiscalTaxRepository;
    private final PurVendorBillJpaRepository vendorBillRepository;
    private final PurVendorPaymentJpaRepository vendorPaymentRepository;
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

    @PersistenceContext
    private EntityManager entityManager;

    public PurchaseApplicationServiceImpl(PurPurchaseOrderJpaRepository purchaseOrderRepository,
                                          PurFiscalTaxJpaRepository fiscalTaxRepository,
                                          PurVendorBillJpaRepository vendorBillRepository,
                                          PurVendorPaymentJpaRepository vendorPaymentRepository,
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
                                          PurchaseEventPublisher purchaseEventPublisher) {
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
        PurPurchaseOrderEntity o = new PurPurchaseOrderEntity();
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
        o.setExchangeRateToCompany(command.getExchangeRateToCompany() != null
                ? command.getExchangeRateToCompany() : BigDecimal.ONE);
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        int seq = 10;
        for (PurchaseOrderLineCommand lc : command.getLines()) {
            PurPurchaseOrderLineEntity line = new PurPurchaseOrderLineEntity();
            line.setId(UUID.randomUUID());
            line.setPurchaseOrder(o);
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
                PurFiscalTaxEntity tax = fiscalTaxRepository.findById(taxId)
                        .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new PurchaseDomainException("Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.PURCHASE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new PurchaseDomainException("Tax scope not valid for purchase: " + taxId);
                }
                PurPurchaseOrderLineTaxEntity lt = new PurPurchaseOrderLineTaxEntity();
                lt.setId(UUID.randomUUID());
                lt.setLine(line);
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
        String qNorm = q != null && !q.isBlank() ? q.trim() : null;
        return purchaseOrderRepository.search(cid, state, vendorPartnerId, qNorm, pageable).map(this::toSummary);
    }

    private PurchaseOrderSummaryResponse toSummary(PurPurchaseOrderEntity o) {
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

    private boolean computeCanCreateVendorBill(PurPurchaseOrderEntity po) {
        if (po.getState() != PurchaseOrderState.CONFIRMED) {
            return false;
        }
        for (PurPurchaseOrderLineEntity pol : po.getLines()) {
            Optional<Product> opt = productRepository.findById(new ProductId(pol.getProductId()));
            if (opt.isEmpty()) {
                continue;
            }
            Product product = opt.get();
            if (product.getProductType() == ProductType.SERVICE) {
                if (pol.getQtyOrdered().subtract(pol.getQtyInvoiced()).signum() > 0) {
                    return true;
                }
            } else if (pol.getQtyReceived().subtract(pol.getQtyInvoiced()).signum() > 0) {
                return true;
            }
        }
        return false;
    }

    private void recalcTotals(PurPurchaseOrderEntity o) {
        BigDecimal untaxed = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (PurPurchaseOrderLineEntity line : o.getLines()) {
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
        PurPurchaseOrderEntity o = loadOrder(id);
        PurchaseOrderRules.ensureCanSend(o.getState());
        o.setState(PurchaseOrderState.SENT);
        o.setSentAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return toResponse(purchaseOrderRepository.save(o));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse confirmPurchaseOrder(UUID id) {
        PurPurchaseOrderEntity o = loadOrder(id);
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

        List<StockMoveCommand> moves = new ArrayList<>();
        for (PurPurchaseOrderLineEntity line : o.getLines()) {
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
            BigDecimal unitCost = oneInStockUom.signum() > 0
                    ? lineNetOne.divide(oneInStockUom, 8, RoundingMode.HALF_UP)
                    : lineNetOne;

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
        }

        o.setState(PurchaseOrderState.CONFIRMED);
        o.setConfirmedAt(Instant.now());
        o.setExchangeRateToCompany(o.getExchangeRateToCompany() != null ? o.getExchangeRateToCompany() : BigDecimal.ONE);
        o.setUpdatedAt(Instant.now());
        return toResponse(purchaseOrderRepository.save(o));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(UUID id) {
        PurPurchaseOrderEntity o = loadOrder(id);
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
        // validatePicking() triggers PurchaseReceiveSyncPort when the picking is linked to a PO.
        return stockPickingApplicationService.validatePicking(pickingId,
                command != null ? command : new ValidatePickingCommand());
    }

    @Override
    @Transactional
    public void syncPurchaseOrderLineQtyReceivedFromStockMoves(UUID purchaseOrderId) {
        entityManager.flush();
        PurPurchaseOrderEntity o = purchaseOrderRepository.findById(purchaseOrderId).orElse(null);
        if (o == null) {
            return;
        }
        Instant now = Instant.now();
        for (PurPurchaseOrderLineEntity line : o.getLines()) {
            BigDecimal sum = stockMovePurchaseQueryPort.sumPickedQuantityForPurchaseOrderLine(line.getId());
            line.setQtyReceived(sum.setScale(4, RoundingMode.HALF_UP));
            line.setUpdatedAt(now);
        }
        boolean allReceived = o.getLines().stream().allMatch(l -> {
            Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
            if (p.isEmpty()) {
                return true;
            }
            if (p.get().getProductType() == ProductType.SERVICE) {
                return true;
            }
            return l.getQtyReceived().compareTo(l.getQtyOrdered()) >= 0;
        });
        if (allReceived) {
            o.setReceivedCompletedAt(Instant.now());
        }
        o.setUpdatedAt(now);
        purchaseOrderRepository.save(o);
    }

    @Override
    @Transactional
    public VendorBillResponse createVendorBillFromPo(CreateVendorBillFromPoCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PurPurchaseOrderEntity po = loadOrder(command.getPurchaseOrderId());
        if (!po.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("Purchase order company mismatch");
        }
        if (po.getState() == PurchaseOrderState.CANCELLED) {
            throw new PurchaseDomainException("Cannot bill a cancelled purchase order");
        }
        if (po.getState() != PurchaseOrderState.CONFIRMED) {
            throw new PurchaseDomainException("Purchase order must be confirmed before billing");
        }
        Instant now = Instant.now();
        PurVendorBillEntity bill = new PurVendorBillEntity();
        bill.setId(UUID.randomUUID());
        bill.setCompanyId(companyId);
        bill.setVendorPartnerId(po.getVendorPartnerId());
        bill.setPurchaseOrderId(po.getId());
        bill.setBillDate(command.getBillDate());
        bill.setDueDate(command.getDueDate());
        bill.setReference(command.getReference() != null ? command.getReference() : "BILL/" + bill.getId().toString().substring(0, 8));
        bill.setCurrencyCode(po.getCurrencyCode());
        bill.setState(VendorBillState.DRAFT);
        bill.setExchangeRateToCompany(po.getExchangeRateToCompany() != null ? po.getExchangeRateToCompany() : BigDecimal.ONE);
        bill.setCreatedAt(now);
        bill.setUpdatedAt(now);

        int seq = 10;
        for (PurPurchaseOrderLineEntity pol : po.getLines()) {
            Product product = productRepository.findById(new ProductId(pol.getProductId()))
                    .orElseThrow(() -> new PurchaseDomainException("Product not found: " + pol.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                PurVendorBillLineEntity vbl = new PurVendorBillLineEntity();
                vbl.setId(UUID.randomUUID());
                vbl.setVendorBill(bill);
                vbl.setSequence(seq);
                vbl.setPurchaseOrderLineId(pol.getId());
                vbl.setProductId(pol.getProductId());
                vbl.setName(pol.getName());
                vbl.setUomId(pol.getUomId());
                BigDecimal qty = pol.getQtyOrdered().subtract(pol.getQtyInvoiced());
                if (qty.signum() > 0) {
                    vbl.setQty(qty);
                    vbl.setUnitPrice(pol.getUnitPrice());
                    vbl.setAccountId(resolveExpenseAccount(product));
                    vbl.setCreatedAt(now);
                    vbl.setUpdatedAt(now);
                    addBillTaxSnapshots(vbl, pol, now);
                    bill.getLines().add(vbl);
                }
                seq += 10;
                continue;
            }
            BigDecimal qty = pol.getQtyReceived().subtract(pol.getQtyInvoiced());
            if (qty.signum() <= 0) {
                continue;
            }
            PurVendorBillLineEntity vbl = new PurVendorBillLineEntity();
            vbl.setId(UUID.randomUUID());
            vbl.setVendorBill(bill);
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
                            + "invoiced); for service lines, bill from ordered quantity.");
        }
        return toBillResponse(vendorBillRepository.save(bill));
    }

    private void addBillTaxSnapshots(PurVendorBillLineEntity vbl, PurPurchaseOrderLineEntity pol, Instant now) {
        List<FiscalTaxSnapshot> snaps = pol.getTaxes().stream()
                .map(lt -> fiscalTaxRepository.findById(lt.getTaxId()).orElseThrow())
                .map(t -> new FiscalTaxSnapshot(t.getId(), t.getAmountType(), t.getAmount(), t.isPriceInclude()))
                .toList();
        PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                vbl.getQty(), vbl.getUnitPrice(), pol.getDiscountPercent(), snaps);
        for (Map.Entry<UUID, BigDecimal> e : split.taxAmountById().entrySet()) {
            PurFiscalTaxEntity t = fiscalTaxRepository.findById(e.getKey()).orElseThrow();
            PurVendorBillLineTaxEntity ts = new PurVendorBillLineTaxEntity();
            ts.setId(UUID.randomUUID());
            ts.setLine(vbl);
            ts.setTaxId(t.getId());
            ts.setTaxName(t.getName());
            ts.setTaxBase(split.net());
            ts.setTaxAmount(e.getValue());
            ts.setAccountId(t.getAccountId());
            vbl.getTaxSnapshots().add(ts);
        }
    }

    private BigDecimal discountForBillLine(PurVendorBillEntity bill, PurVendorBillLineEntity line) {
        if (bill.getPurchaseOrderId() == null || line.getPurchaseOrderLineId() == null) {
            return BigDecimal.ZERO;
        }
        return purchaseOrderRepository.findById(bill.getPurchaseOrderId()).stream()
                .flatMap(po -> po.getLines().stream())
                .filter(l -> l.getId().equals(line.getPurchaseOrderLineId()))
                .map(PurPurchaseOrderLineEntity::getDiscountPercent)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal billTotalDocumentCurrency(PurVendorBillEntity bill) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurVendorBillLineEntity line : bill.getLines()) {
            BigDecimal disc = discountForBillLine(bill, line);
            BigDecimal lineNetDoc = PurchaseOrderRules.lineNet(line.getQty(), line.getUnitPrice(), disc)
                    .setScale(4, RoundingMode.HALF_UP);
            total = total.add(lineNetDoc);
            for (PurVendorBillLineTaxEntity ts : line.getTaxSnapshots()) {
                total = total.add(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }
        return total;
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
    public VendorBillResponse postVendorBill(UUID billId) {
        PurVendorBillEntity bill = vendorBillRepository.findById(billId)
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

        BigDecimal rate = bill.getExchangeRateToCompany() != null && bill.getExchangeRateToCompany().signum() > 0
                ? bill.getExchangeRateToCompany() : BigDecimal.ONE;
        List<JournalItemCommand> items = new ArrayList<>();
        BigDecimal apCreditCompany = BigDecimal.ZERO;

        for (PurVendorBillLineEntity line : bill.getLines()) {
            BigDecimal disc = discountForBillLine(bill, line);
            BigDecimal lineNetDoc = PurchaseOrderRules.lineNet(line.getQty(), line.getUnitPrice(), disc)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal netComp = PurchaseTaxEngine.convertAtRate(lineNetDoc, rate);
            items.add(new JournalItemCommand(line.getAccountId(), line.getName(), netComp, BigDecimal.ZERO,
                    bill.getCurrencyCode(), lineNetDoc, null));
            apCreditCompany = apCreditCompany.add(netComp);
            for (PurVendorBillLineTaxEntity ts : line.getTaxSnapshots()) {
                BigDecimal taxDoc = ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP);
                BigDecimal taxComp = PurchaseTaxEngine.convertAtRate(taxDoc, rate);
                items.add(new JournalItemCommand(ts.getAccountId(), ts.getTaxName(), taxComp, BigDecimal.ZERO,
                        bill.getCurrencyCode(), taxDoc, null));
                apCreditCompany = apCreditCompany.add(taxComp);
            }
        }
        items.add(new JournalItemCommand(payableAccount, "Accounts payable", BigDecimal.ZERO, apCreditCompany,
                bill.getCurrencyCode(), apCreditCompany.negate(), bill.getVendorPartnerId()));

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
        bill.setState(VendorBillState.POSTED);
        bill.setUpdatedAt(Instant.now());
        vendorBillRepository.save(bill);

        if (bill.getPurchaseOrderId() != null) {
            PurPurchaseOrderEntity po = purchaseOrderRepository.findById(bill.getPurchaseOrderId()).orElse(null);
            if (po != null) {
                for (PurVendorBillLineEntity vbl : bill.getLines()) {
                    if (vbl.getPurchaseOrderLineId() != null) {
                        po.getLines().stream()
                                .filter(l -> l.getId().equals(vbl.getPurchaseOrderLineId()))
                                .findFirst()
                                .ifPresent(pol -> {
                                    pol.setQtyInvoiced(pol.getQtyInvoiced().add(vbl.getQty()).setScale(4, RoundingMode.HALF_UP));
                                    pol.setUpdatedAt(Instant.now());
                                });
                    }
                }
                boolean allBilled = po.getLines().stream().allMatch(l ->
                        l.getQtyInvoiced().compareTo(l.getQtyOrdered()) >= 0);
                if (allBilled) {
                    po.setBilledCompletedAt(Instant.now());
                }
                po.setUpdatedAt(Instant.now());
                purchaseOrderRepository.save(po);
            }
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
    public VendorBillResponse getVendorBill(UUID billId) {
        PurVendorBillEntity bill = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found"));
        UUID cid = companyContextProvider.getObject().requireCompany().getId();
        if (!bill.getCompanyId().equals(cid)) {
            throw new PurchaseDomainException("Vendor bill not found");
        }
        bill.getLines().size();
        for (PurVendorBillLineEntity line : bill.getLines()) {
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
    public PartnerStatementSectionResponse payableStatement(UUID companyId,
                                                            UUID partnerId,
                                                            LocalDate from,
                                                            LocalDate to,
                                                            String currencyCode) {
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
        return buildPayableSection(cid, partnerId, from, to, currencyCode);
    }

    private PartnerStatementSectionResponse buildPayableSection(
            UUID cid, UUID partnerId, LocalDate from, LocalDate to, String currencyCode) {
        List<PurVendorBillEntity> bills = vendorBillRepository
                .findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(cid, partnerId);
        for (PurVendorBillEntity b : bills) {
            b.getLines().size();
            for (PurVendorBillLineEntity line : b.getLines()) {
                line.getTaxSnapshots().size();
            }
        }
        List<PurVendorPaymentEntity> payments = vendorPaymentRepository
                .findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(cid, partnerId);

        String currency = currencyCode != null && !currencyCode.isBlank()
                ? currencyCode
                : "USD";

        BigDecimal opening = BigDecimal.ZERO;
        for (PurVendorBillEntity b : bills) {
            if (b.getState() != VendorBillState.POSTED) {
                continue;
            }
            if (b.getBillDate().isBefore(from)) {
                opening = opening.add(billTotalDocumentCurrency(b));
            }
        }
        for (PurVendorPaymentEntity p : payments) {
            if (p.getState() != VendorPaymentState.POSTED) {
                continue;
            }
            if (p.getPaymentDate().isBefore(from)) {
                opening = opening.subtract(p.getAmount());
            }
        }
        opening = opening.setScale(4, RoundingMode.HALF_UP);

        record PayEvt(LocalDate d, Instant created, String idKey, PurVendorBillEntity bill, PurVendorPaymentEntity pay) {}

        List<PayEvt> period = new ArrayList<>();
        for (PurVendorBillEntity b : bills) {
            if (b.getState() != VendorBillState.POSTED) {
                continue;
            }
            if (!b.getBillDate().isBefore(from) && !b.getBillDate().isAfter(to)) {
                period.add(new PayEvt(b.getBillDate(), b.getCreatedAt(), "B:" + b.getId(), b, null));
            }
        }
        for (PurVendorPaymentEntity p : payments) {
            if (p.getState() != VendorPaymentState.POSTED) {
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
                PurVendorBillEntity b = e.bill();
                BigDecimal amt = billTotalDocumentCurrency(b).setScale(4, RoundingMode.HALF_UP);
                row.setLineType("VENDOR_BILL");
                row.setReference(b.getReference() != null && !b.getReference().isBlank()
                        ? b.getReference()
                        : b.getId().toString());
                row.setVendorBillId(b.getId());
                row.setVendorPaymentId(null);
                row.setCustomerInvoiceId(null);
                row.setCustomerPaymentId(null);
                row.setDebit(amt);
                row.setCredit(z);
                running = running.add(amt);
            } else {
                PurVendorPaymentEntity p = e.pay();
                BigDecimal amt = p.getAmount().setScale(4, RoundingMode.HALF_UP);
                row.setLineType("VENDOR_PAYMENT");
                row.setReference(p.getReference() != null && !p.getReference().isBlank()
                        ? p.getReference()
                        : "Payment");
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
        PurVendorBillEntity bill = vendorBillRepository.findById(command.getVendorBillId())
                .orElseThrow(() -> new PurchaseDomainException("Vendor bill not found"));
        if (!bill.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("Bill company mismatch");
        }
        if (bill.getState() != VendorBillState.POSTED || bill.getJournalEntryId() == null) {
            throw new PurchaseDomainException("Bill must be posted before payment");
        }
        PartnerResponse vendor = partnerApplicationService.getPartner(bill.getVendorPartnerId());
        UUID payableAccount = vendor.getPayableAccountId() != null
                ? vendor.getPayableAccountId()
                : accountingReferenceLookupPort.resolveAccountIdByCode(companyId, DEFAULT_AP_ACCOUNT_CODE);

        JournalType paymentJournalType = accountingReferenceLookupPort
                .resolveJournalType(companyId, command.getBankJournalId());
        UUID liquidityAccountId = resolveLiquidityAccountForPaymentJournal(companyId, command.getBankJournalId());
        String liquidityLabel = paymentJournalType == JournalType.CASH ? "Cash payment" : "Bank payment";

        BigDecimal amt = command.getAmount().setScale(4, RoundingMode.HALF_UP);
        List<JournalItemCommand> items = List.of(
                new JournalItemCommand(payableAccount, "Payment " + bill.getReference(), amt, BigDecimal.ZERO,
                        command.getCurrencyCode(), amt, bill.getVendorPartnerId()),
                new JournalItemCommand(liquidityAccountId, liquidityLabel, BigDecimal.ZERO, amt,
                        command.getCurrencyCode(), amt.negate(), null)
        );
        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                command.getBankJournalId(),
                "",
                command.getPaymentDate(),
                command.getCurrencyCode(),
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
        PurVendorPaymentEntity p = new PurVendorPaymentEntity();
        p.setId(UUID.randomUUID());
        p.setCompanyId(companyId);
        p.setVendorPartnerId(bill.getVendorPartnerId());
        p.setVendorBillId(bill.getId());
        p.setPaymentDate(command.getPaymentDate());
        p.setBankJournalId(command.getBankJournalId());
        p.setAmount(amt);
        p.setCurrencyCode(command.getCurrencyCode());
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
        r.setAmount(amt);
        r.setCurrencyCode(command.getCurrencyCode());
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
        PurFiscalTaxEntity t = new PurFiscalTaxEntity();
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
        PurFiscalTaxEntity t = fiscalTaxRepository.findById(taxId)
                .orElseThrow(() -> new PurchaseDomainException("Tax not found: " + taxId));
        return toTaxResponse(t);
    }

    private PurPurchaseOrderEntity loadOrder(UUID id) {
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

    private PurchaseOrderResponse toResponse(PurPurchaseOrderEntity o) {
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
        r.setLines(o.getLines().stream().sorted(Comparator.comparingInt(PurPurchaseOrderLineEntity::getSequence)).map(l -> {
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
            lr.setTaxIds(l.getTaxes().stream().sorted(Comparator.comparingInt(PurPurchaseOrderLineTaxEntity::getSequence))
                    .map(PurPurchaseOrderLineTaxEntity::getTaxId).collect(Collectors.toList()));
            productRepository.findById(new ProductId(l.getProductId()))
                    .ifPresent(p -> lr.setProductType(p.getProductType().name()));
            return lr;
        }).collect(Collectors.toList()));
        return r;
    }

    private VendorBillSummaryResponse toBillSummaryResponse(PurVendorBillEntity b) {
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
        r.setJournalEntryId(b.getJournalEntryId());
        r.setCreatedAt(b.getCreatedAt());
        return r;
    }

    private VendorPaymentResponse toVendorPaymentListRow(PurVendorPaymentEntity p) {
        VendorPaymentResponse r = new VendorPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(p.getCompanyId());
        r.setVendorBillId(p.getVendorBillId());
        r.setPaymentDate(p.getPaymentDate());
        r.setAmount(p.getAmount());
        r.setCurrencyCode(p.getCurrencyCode());
        r.setState(p.getState());
        r.setJournalEntryId(p.getJournalEntryId());
        r.setReconciliationId(null);
        return r;
    }

    private VendorBillResponse toBillResponse(PurVendorBillEntity b) {
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
        r.setJournalEntryId(b.getJournalEntryId());
        r.setLines(b.getLines().stream().sorted(Comparator.comparingInt(PurVendorBillLineEntity::getSequence)).map(l -> {
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

    private FiscalTaxResponse toTaxResponse(PurFiscalTaxEntity t) {
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
}
