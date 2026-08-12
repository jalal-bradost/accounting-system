package com.jalaldeveloper.accountingsystem.sales.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.SalesOrderInvoiceSyncPort;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.CreditStatusResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PartnerResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockLocation;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateStockPickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockMoveCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.UomApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.StockMoveSalesQueryPort;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.WarehouseRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.FiscalTaxScope;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.FiscalTaxSnapshot;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.PurchaseTaxEngine;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.FiscalTaxResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalInvoicePolicy;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesDomainException;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderDeliveryStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderInvoiceStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderRules;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.Pricelist;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.PricelistItem;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrder;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrderLine;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrderLineTax;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderLineCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderLineResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderSummaryResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.event.SalesOrderConfirmedEvent;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.input.SalesApplicationService;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.messaging.SalesEventPublisher;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.repository.PricelistRepository;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.repository.SalesOrderRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
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
import java.util.UUID;

@Service
@Validated
public class SalesApplicationServiceImpl implements SalesApplicationService, SalesOrderInvoiceSyncPort {

    private final SalesOrderRepository salesOrderRepository;
    private final PricelistRepository pricelistRepository;
    private final PartnerApplicationService partnerApplicationService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository stockLocationRepository;
    private final UomApplicationService uomApplicationService;
    private final StockPickingApplicationService stockPickingApplicationService;
    private final StockMoveSalesQueryPort stockMoveSalesQueryPort;
    private final PurchaseApplicationService purchaseApplicationService;
    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final SalesEventPublisher salesEventPublisher;

    public SalesApplicationServiceImpl(SalesOrderRepository salesOrderRepository,
                                       PricelistRepository pricelistRepository,
                                       PartnerApplicationService partnerApplicationService,
                                       ProductRepository productRepository,
                                       WarehouseRepository warehouseRepository,
                                       StockLocationRepository stockLocationRepository,
                                       UomApplicationService uomApplicationService,
                                       StockPickingApplicationService stockPickingApplicationService,
                                       StockMoveSalesQueryPort stockMoveSalesQueryPort,
                                       PurchaseApplicationService purchaseApplicationService,
                                       @Lazy CustomerInvoiceApplicationService customerInvoiceApplicationService,
                                       ObjectProvider<CompanyContext> companyContextProvider,
                                       SalesEventPublisher salesEventPublisher) {
        this.salesOrderRepository = salesOrderRepository;
        this.pricelistRepository = pricelistRepository;
        this.partnerApplicationService = partnerApplicationService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.uomApplicationService = uomApplicationService;
        this.stockPickingApplicationService = stockPickingApplicationService;
        this.stockMoveSalesQueryPort = stockMoveSalesQueryPort;
        this.purchaseApplicationService = purchaseApplicationService;
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
        this.companyContextProvider = companyContextProvider;
        this.salesEventPublisher = salesEventPublisher;
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) {
            return fromCommand;
        }
        return companyContextProvider.getObject().requireCompany().getId();
    }

    @Override
    @Transactional
    public SalesOrderResponse createSalesOrder(CreateSalesOrderCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PartnerResponse customer = partnerApplicationService.getPartner(command.getCustomerPartnerId());
        if (!customer.isCustomer()) {
            throw new SalesDomainException("Partner is not a customer");
        }
        if (!customer.getCompanyId().equals(companyId)) {
            throw new SalesDomainException("Customer belongs to another company");
        }
        if (command.getPricelistId() != null) {
            Pricelist pl = pricelistRepository.findById(command.getPricelistId())
                    .orElseThrow(() -> new SalesDomainException("Pricelist not found"));
            if (!pl.getCompanyId().equals(companyId)) {
                throw new SalesDomainException("Pricelist company mismatch");
            }
        }
        Instant now = Instant.now();
        LocalDate orderDate = command.getOrderDate() != null ? command.getOrderDate() : LocalDate.now();
        SalesOrder o = new SalesOrder();
        o.setId(UUID.randomUUID());
        o.setCompanyId(companyId);
        o.setCustomerPartnerId(command.getCustomerPartnerId());
        o.setName(command.getName() != null && !command.getName().isBlank()
                ? command.getName()
                : "SO-" + o.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        if (salesOrderRepository.findByCompanyIdAndName(companyId, o.getName()).isPresent()) {
            throw new SalesDomainException("Sales order name already exists: " + o.getName());
        }
        o.setState(SalesOrderState.DRAFT);
        o.setDeliveryStatus(SalesOrderDeliveryStatus.PENDING);
        o.setInvoiceStatus(SalesOrderInvoiceStatus.NOTHING);
        o.setCurrencyCode(command.getCurrencyCode());
        o.setWarehouseId(command.getWarehouseId());
        o.setPricelistId(command.getPricelistId());
        o.setPaymentTermsId(command.getPaymentTermsId() != null ? command.getPaymentTermsId() : customer.getPaymentTermsId());
        o.setOrderDate(orderDate);
        o.setValidityDate(command.getValidityDate());
        o.setIncoterm(command.getIncoterm());
        o.setNotes(command.getNotes());
        o.setExchangeRateToCompany(command.getExchangeRateToCompany() != null
                ? command.getExchangeRateToCompany() : BigDecimal.ONE);
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        int seq = 10;
        for (SalesOrderLineCommand lc : command.getLines()) {
            Product product = productRepository.findById(new ProductId(lc.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + lc.getProductId()));
            if (!product.isSaleOk()) {
                throw new SalesDomainException("Product is not salable: " + lc.getProductId());
            }
            BigDecimal unitPrice = resolveUnitPrice(companyId, o.getPricelistId(), lc.getProductId(),
                    lc.getQtyOrdered(), orderDate, lc.getUnitPrice(), product);
            SalesOrderLine line = new SalesOrderLine();
            line.setId(UUID.randomUUID());
            line.setSequence(seq);
            line.setProductId(lc.getProductId());
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setQtyDelivered(BigDecimal.ZERO);
            line.setQtyInvoiced(BigDecimal.ZERO);
            line.setUnitPrice(unitPrice);
            line.setDiscountPercent(lc.getDiscountPercent() != null ? lc.getDiscountPercent() : BigDecimal.ZERO);
            line.setInvoicePolicy(lc.getInvoicePolicy() != null ? lc.getInvoicePolicy()
                    : (product.getProductType() == ProductType.SERVICE ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED));
            line.setRevenueAccountId(lc.getRevenueAccountId());
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTaxResponse tax = purchaseApplicationService.getFiscalTax(taxId);
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new SalesDomainException("Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.SALE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new SalesDomainException("Tax scope not valid for sale: " + taxId);
                }
                SalesOrderLineTax lt = new SalesOrderLineTax();
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
        refreshOrderStatuses(o);
        return toResponse(salesOrderRepository.save(o));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrderSummaryResponse> searchSalesOrders(UUID companyId,
                                                           SalesOrderState state,
                                                           UUID customerPartnerId,
                                                           String q,
                                                           Pageable pageable) {
        UUID cid = companyIdOrDefault(companyId);
        String qNorm = q != null && !q.isBlank() ? q.trim() : "";
        return salesOrderRepository.search(cid, state, customerPartnerId, qNorm, pageable).map(this::toSummary);
    }

    private SalesOrderSummaryResponse toSummary(SalesOrder o) {
        SalesOrderSummaryResponse r = new SalesOrderSummaryResponse();
        r.setId(o.getId());
        r.setCompanyId(o.getCompanyId());
        r.setCustomerPartnerId(o.getCustomerPartnerId());
        r.setName(o.getName());
        r.setState(o.getState());
        r.setDeliveryStatus(o.getDeliveryStatus());
        r.setInvoiceStatus(o.getInvoiceStatus());
        r.setCurrencyCode(o.getCurrencyCode());
        r.setOrderDate(o.getOrderDate());
        r.setAmountTotal(o.getAmountTotal());
        r.setCreatedAt(o.getCreatedAt());
        return r;
    }

    private boolean computeCanCreateCustomerInvoice(SalesOrder o) {
        if (o.getState() != SalesOrderState.CONFIRMED) {
            return false;
        }
        Map<UUID, BigDecimal> draftAllocated =
                customerInvoiceApplicationService.draftAllocatedQtyBySalesOrderLine(o.getId());
        for (SalesOrderLine sol : o.getLines()) {
            Product product = productRepository.findById(new ProductId(sol.getProductId())).orElse(null);
            if (product == null) {
                continue;
            }
            if (invoiceableQtyForLine(sol, product, draftAllocated).signum() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean computeCanCreateCustomerCreditNote(SalesOrder o) {
        if (o.getState() != SalesOrderState.CONFIRMED) {
            return false;
        }
        Map<UUID, BigDecimal> draftCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(o.getId());
        for (SalesOrderLine sol : o.getLines()) {
            if (creditNoteableQtyForLine(sol, draftCn).signum() > 0) {
                return true;
            }
        }
        return false;
    }

    /** Quantities already reserved on draft customer invoices (not yet posted to qtyInvoiced). */
    private BigDecimal effectiveQtyInvoiced(SalesOrderLine sol, Map<UUID, BigDecimal> draftAllocated) {
        BigDecimal draft = draftAllocated.getOrDefault(sol.getId(), BigDecimal.ZERO);
        return sol.getQtyInvoiced().add(draft).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal invoiceableQtyForLine(SalesOrderLine sol,
                                             Product product,
                                             Map<UUID, BigDecimal> draftAllocated) {
        SalInvoicePolicy pol = sol.getInvoicePolicy() != null ? sol.getInvoicePolicy()
                : (product.getProductType() == ProductType.SERVICE
                ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED);
        BigDecimal targetQty = pol == SalInvoicePolicy.ORDERED
                ? sol.getQtyOrdered() : sol.getQtyDelivered();
        BigDecimal invoiced = effectiveQtyInvoiced(sol, draftAllocated);
        return targetQty.subtract(invoiced).max(BigDecimal.ZERO);
    }

    /** Over-invoiced qty after returns: invoiced − delivered − draft credit notes. */
    private BigDecimal creditNoteableQtyForLine(SalesOrderLine sol, Map<UUID, BigDecimal> draftCreditNotes) {
        BigDecimal draft = draftCreditNotes.getOrDefault(sol.getId(), BigDecimal.ZERO);
        return sol.getQtyInvoiced().subtract(sol.getQtyDelivered()).subtract(draft).max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveUnitPrice(UUID companyId, UUID pricelistId, UUID productId, BigDecimal qty,
                                       LocalDate asOfDate, BigDecimal commandPrice, Product product) {
        if (commandPrice != null) {
            return commandPrice;
        }
        BigDecimal list = product.getListPrice() != null ? product.getListPrice().getAmount() : BigDecimal.ZERO;
        if (pricelistId == null) {
            return list.setScale(4, RoundingMode.HALF_UP);
        }
        Pricelist pl = pricelistRepository.findByIdWithItems(pricelistId).orElse(null);
        if (pl == null || !pl.getCompanyId().equals(companyId) || !pl.isActive()) {
            return list.setScale(4, RoundingMode.HALF_UP);
        }
        List<PricelistItem> candidates = pl.getItems().stream()
                .filter(i -> productId.equals(i.getProductId()))
                .filter(i -> i.getMinQuantity() == null || qty.compareTo(i.getMinQuantity()) >= 0)
                .filter(i -> i.getDateFrom() == null || !asOfDate.isBefore(i.getDateFrom()))
                .filter(i -> i.getDateTo() == null || !asOfDate.isAfter(i.getDateTo()))
                .sorted(Comparator
                        .comparing((PricelistItem i) ->
                                i.getMinQuantity() != null ? i.getMinQuantity() : BigDecimal.ZERO)
                        .reversed()
                        .thenComparingInt(PricelistItem::getSequence))
                .toList();
        if (candidates.isEmpty()) {
            return list.setScale(4, RoundingMode.HALF_UP);
        }
        PricelistItem it = candidates.get(0);
        if (it.getFixedPrice() != null) {
            return it.getFixedPrice().setScale(4, RoundingMode.HALF_UP);
        }
        if (it.getPercentDiscount() != null && it.getPercentDiscount().signum() > 0) {
            BigDecimal factor = BigDecimal.ONE.subtract(it.getPercentDiscount()
                    .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            return list.multiply(factor).setScale(4, RoundingMode.HALF_UP);
        }
        return list.setScale(4, RoundingMode.HALF_UP);
    }

    private void recalcTotals(SalesOrder o) {
        BigDecimal untaxed = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (SalesOrderLine line : o.getLines()) {
            List<FiscalTaxSnapshot> snaps = line.getTaxes().stream()
                    .map(t -> purchaseApplicationService.getFiscalTax(t.getTaxId()))
                    .map(this::toSnapshot)
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

    private FiscalTaxSnapshot toSnapshot(FiscalTaxResponse t) {
        return new FiscalTaxSnapshot(t.getId(), t.getAmountType(), t.getAmount(), t.isPriceInclude());
    }

    private SalesOrder loadOrder(UUID id) {
        return salesOrderRepository.findByIdWithLines(id)
                .orElseThrow(() -> new SalesDomainException("Sales order not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponse getSalesOrder(UUID id) {
        return toResponse(loadOrder(id));
    }

    @Override
    @Transactional
    public SalesOrderResponse sendQuotation(UUID id) {
        SalesOrder o = loadOrder(id);
        SalesOrderRules.ensureCanSendQuotation(o.getState());
        o.setState(SalesOrderState.QUOTATION_SENT);
        o.setQuotationSentAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return toResponse(salesOrderRepository.save(o));
    }

    @Override
    @Transactional
    public SalesOrderResponse confirmSalesOrder(UUID id) {
        SalesOrder o = loadOrder(id);
        if (o.getState() == SalesOrderState.CONFIRMED) {
            return toResponse(o);
        }
        SalesOrderRules.ensureCanConfirm(o.getState());
        recalcTotals(o);
        CreditStatusResponse credit = partnerApplicationService.creditStatus(o.getCustomerPartnerId());
        if (!credit.unlimited() && o.getAmountTotal().compareTo(credit.available()) > 0) {
            throw new SalesDomainException("Credit limit exceeded for this order total");
        }
        UUID warehouseId = o.getWarehouseId();
        if (warehouseId == null) {
            throw new SalesDomainException("warehouseId is required to confirm a sales order");
        }
        Warehouse wh = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new SalesDomainException("Warehouse not found: " + warehouseId));
        if (!wh.getCompanyId().getId().equals(o.getCompanyId())) {
            throw new SalesDomainException("Warehouse company mismatch");
        }
        StockLocation customerLoc = findCustomerVirtual(o.getCompanyId());
        UUID sourceLoc = wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (sourceLoc == null) {
            throw new SalesDomainException("Warehouse stock location could not be resolved");
        }

        List<StockMoveCommand> moves = new ArrayList<>();
        for (SalesOrderLine line : o.getLines()) {
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + line.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                continue;
            }
            BigDecimal remaining = line.getQtyOrdered().subtract(line.getQtyDelivered());
            if (remaining.signum() <= 0) {
                continue;
            }
            UUID stockUom = product.getUomId().getId();
            BigDecimal demandStockUom = uomApplicationService.convert(line.getUomId(), stockUom, remaining);
            BigDecimal oneInStockUom = uomApplicationService.convert(line.getUomId(), stockUom, BigDecimal.ONE);
            BigDecimal lineNetOne = SalesOrderRules.lineNet(BigDecimal.ONE, line.getUnitPrice(), line.getDiscountPercent());
            BigDecimal unitCost = oneInStockUom.signum() > 0
                    ? lineNetOne.divide(oneInStockUom, 8, RoundingMode.HALF_UP)
                    : lineNetOne;

            StockMoveCommand mc = new StockMoveCommand();
            mc.setProductId(line.getProductId());
            mc.setUomId(stockUom);
            mc.setDemandQuantity(demandStockUom);
            mc.setUnitCost(unitCost);
            mc.setSalesOrderLineId(line.getId());
            moves.add(mc);
        }

        if (!moves.isEmpty()) {
            CreateStockPickingCommand cmd = new CreateStockPickingCommand();
            cmd.setCompanyId(o.getCompanyId());
            cmd.setWarehouseId(warehouseId);
            cmd.setPickingType(PickingType.OUTGOING);
            cmd.setSourceLocationId(sourceLoc);
            cmd.setDestinationLocationId(customerLoc.getId().getId());
            cmd.setPartnerId(o.getCustomerPartnerId());
            cmd.setOrigin(o.getName());
            cmd.setReference(o.getName());
            cmd.setSalesOrderId(o.getId());
            cmd.setMoves(moves);
            stockPickingApplicationService.createPicking(cmd);
        }

        o.setState(SalesOrderState.CONFIRMED);
        o.setConfirmedAt(Instant.now());
        o.setExchangeRateToCompany(o.getExchangeRateToCompany() != null ? o.getExchangeRateToCompany() : BigDecimal.ONE);
        o.setUpdatedAt(Instant.now());
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesEventPublisher.publishSalesOrderConfirmed(new SalesOrderConfirmedEvent(
                UUID.randomUUID(),
                Instant.now(),
                saved.getCompanyId(),
                saved.getId()));
        return toResponse(saved);
    }

    private StockLocation findCustomerVirtual(UUID companyId) {
        return stockLocationRepository.findByCompany(new CompanyId(companyId), false).stream()
                .filter(l -> l.getLocationType() == LocationType.CUSTOMER)
                .filter(l -> "VIRT/CUSTOMERS".equalsIgnoreCase(l.getCode()))
                .findFirst()
                .orElseThrow(() -> new SalesDomainException("Virtual customer location VIRT/CUSTOMERS not found"));
    }

    @Override
    @Transactional
    public SalesOrderResponse cancelSalesOrder(UUID id) {
        SalesOrder o = loadOrder(id);
        SalesOrderRules.ensureCanCancel(o.getState());
        if (o.getState() == SalesOrderState.CONFIRMED) {
            if (customerInvoiceApplicationService.hasPostedInvoiceForSalesOrder(o.getId())) {
                throw new SalesDomainException("Cannot cancel: posted customer invoices exist for this order");
            }
            if (stockMoveSalesQueryPort.existsNonTerminalPickingForSalesOrder(o.getId())) {
                throw new SalesDomainException("Cannot cancel: open deliveries exist (finish or cancel pickings first)");
            }
        }
        o.setState(SalesOrderState.CANCELLED);
        o.setCancelledAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return toResponse(salesOrderRepository.save(o));
    }

    @Override
    @Transactional
    public void afterOutgoingPickingValidated(UUID salesOrderId) {
        salesOrderRepository.flush();
        SalesOrder o = salesOrderRepository.findByIdWithLines(salesOrderId).orElse(null);
        if (o == null) {
            return;
        }
        Instant now = Instant.now();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal sum = stockMoveSalesQueryPort.sumPickedQuantityForSalesOrderLine(line.getId());
            line.setQtyDelivered(sum.setScale(4, RoundingMode.HALF_UP));
            line.setUpdatedAt(now);
        }
        boolean allDelivered = o.getLines().stream().allMatch(l -> {
            Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
            if (p.isEmpty()) {
                return true;
            }
            if (p.get().getProductType() == ProductType.SERVICE) {
                return true;
            }
            return l.getQtyDelivered().compareTo(l.getQtyOrdered()) >= 0;
        });
        if (allDelivered) {
            o.setDeliveryCompletedAt(Instant.now());
        }
        refreshOrderStatuses(o);
        o.setUpdatedAt(now);
        salesOrderRepository.save(o);
        tryAutoCreateAndPostCreditNoteFromReturn(o);
    }

    private void tryAutoCreateAndPostCreditNoteFromReturn(SalesOrder o) {
        Map<UUID, BigDecimal> draftCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(o.getId());
        boolean hasCreditable = o.getLines().stream()
                .anyMatch(sol -> creditNoteableQtyForLine(sol, draftCn).signum() > 0);
        if (!hasCreditable) {
            return;
        }
        List<CustomerInvoiceResponse> postedInvoices =
                customerInvoiceApplicationService.listPostedInvoicesForSalesOrder(o.getId());
        if (postedInvoices.size() != 1) {
            return;
        }
        CreateCustomerInvoiceFromSalesOrderCommand cmd = new CreateCustomerInvoiceFromSalesOrderCommand();
        cmd.setCompanyId(o.getCompanyId());
        cmd.setSalesOrderId(o.getId());
        cmd.setSourceInvoiceId(postedInvoices.get(0).getId());
        cmd.setInvoiceDate(LocalDate.now());
        CustomerInvoiceResponse cn = createCustomerCreditNoteFromSalesOrder(cmd);
        customerInvoiceApplicationService.postCustomerInvoice(cn.getId());
    }

    @Override
    @Transactional
    public void applyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId) {
        salesOrderRepository.flush();
        SalesOrder o = salesOrderRepository.findByIdWithLines(salesOrderId).orElse(null);
        if (o == null) {
            return;
        }
        Instant now = Instant.now();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal add = invoicedQtyBySalesLineId.get(line.getId());
            if (add != null && add.signum() != 0) {
                BigDecimal next = line.getQtyInvoiced().add(add).setScale(4, RoundingMode.HALF_UP);
                if (next.signum() < 0) {
                    next = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                }
                line.setQtyInvoiced(next);
                line.setUpdatedAt(now);
            }
        }
        boolean allInvoiced = true;
        for (SalesOrderLine l : o.getLines()) {
            Product p = productRepository.findById(new ProductId(l.getProductId())).orElse(null);
            SalInvoicePolicy pol = l.getInvoicePolicy() != null ? l.getInvoicePolicy()
                    : (p != null && p.getProductType() == ProductType.SERVICE
                    ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED);
            BigDecimal target = pol == SalInvoicePolicy.ORDERED ? l.getQtyOrdered() : l.getQtyDelivered();
            if (l.getQtyInvoiced().compareTo(target) < 0) {
                allInvoiced = false;
                break;
            }
        }
        if (allInvoiced) {
            o.setInvoicingCompletedAt(Instant.now());
        }
        refreshOrderStatuses(o);
        o.setUpdatedAt(now);
        salesOrderRepository.save(o);
    }

    private void refreshOrderStatuses(SalesOrder o) {
        boolean anyStockLine = o.getLines().stream().anyMatch(l -> {
            Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
            return p.map(product -> product.getProductType() != ProductType.SERVICE).orElse(false);
        });
        if (!anyStockLine) {
            o.setDeliveryStatus(SalesOrderDeliveryStatus.NA);
        } else {
            boolean anyDelivered = o.getLines().stream().anyMatch(l -> l.getQtyDelivered().signum() > 0);
            boolean allDelivered = o.getLines().stream().allMatch(l -> {
                Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
                if (p.isEmpty() || p.get().getProductType() == ProductType.SERVICE) {
                    return true;
                }
                return l.getQtyDelivered().compareTo(l.getQtyOrdered()) >= 0;
            });
            if (allDelivered) {
                o.setDeliveryStatus(SalesOrderDeliveryStatus.FULL);
            } else if (anyDelivered) {
                o.setDeliveryStatus(SalesOrderDeliveryStatus.PARTIAL);
            } else {
                o.setDeliveryStatus(SalesOrderDeliveryStatus.PENDING);
            }
        }

        boolean anyBillable = false;
        boolean allInvoiced = true;
        for (SalesOrderLine l : o.getLines()) {
            Product p = productRepository.findById(new ProductId(l.getProductId())).orElse(null);
            SalInvoicePolicy pol = l.getInvoicePolicy() != null ? l.getInvoicePolicy()
                    : (p != null && p.getProductType() == ProductType.SERVICE
                    ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED);
            BigDecimal target = pol == SalInvoicePolicy.ORDERED ? l.getQtyOrdered() : l.getQtyDelivered();
            if (target.signum() > 0 && l.getQtyInvoiced().compareTo(target) < 0) {
                anyBillable = true;
            }
            if (l.getQtyInvoiced().compareTo(target) < 0) {
                allInvoiced = false;
            }
        }
        if (allInvoiced && !o.getLines().isEmpty()) {
            o.setInvoiceStatus(SalesOrderInvoiceStatus.FULL);
        } else if (anyBillable) {
            boolean anyInvoiced = o.getLines().stream().anyMatch(l -> l.getQtyInvoiced().signum() > 0);
            o.setInvoiceStatus(anyInvoiced ? SalesOrderInvoiceStatus.PARTIAL : SalesOrderInvoiceStatus.TO_INVOICE);
        } else {
            o.setInvoiceStatus(SalesOrderInvoiceStatus.NOTHING);
        }
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCustomerInvoiceFromSalesOrder(CreateCustomerInvoiceFromSalesOrderCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        SalesOrder o = loadOrder(command.getSalesOrderId());
        if (!o.getCompanyId().equals(companyId)) {
            throw new SalesDomainException("Sales order company mismatch");
        }
        if (o.getState() == SalesOrderState.CANCELLED) {
            throw new SalesDomainException("Cannot invoice a cancelled sales order");
        }
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException("Sales order must be confirmed before invoicing");
        }
        Map<UUID, BigDecimal> draftAllocated =
                customerInvoiceApplicationService.draftAllocatedQtyBySalesOrderLine(o.getId());
        List<CustomerInvoiceLineCommand> invLines = new ArrayList<>();
        for (SalesOrderLine sol : o.getLines()) {
            Product product = productRepository.findById(new ProductId(sol.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + sol.getProductId()));
            BigDecimal qty = invoiceableQtyForLine(sol, product, draftAllocated);
            if (qty.signum() <= 0) {
                continue;
            }
            CustomerInvoiceLineCommand lc = new CustomerInvoiceLineCommand();
            lc.setName(sol.getName());
            lc.setQty(qty);
            lc.setUnitPrice(sol.getUnitPrice());
            lc.setDiscountPercent(sol.getDiscountPercent());
            lc.setRevenueAccountId(sol.getRevenueAccountId());
            lc.setSalesOrderLineId(sol.getId());
            addInvoiceTaxSnapshots(lc, sol, qty);
            invLines.add(lc);
        }
        if (invLines.isEmpty()) {
            throw new SalesDomainException(
                    "No invoiceable quantity: for storable lines, deliver goods first (qty delivered > qty "
                            + "invoiced); for service lines, invoice from ordered quantity. "
                            + "If a draft invoice already exists, post or remove it first.");
        }
        CreateCustomerInvoiceCommand ic = new CreateCustomerInvoiceCommand();
        ic.setCompanyId(companyId);
        ic.setCustomerPartnerId(o.getCustomerPartnerId());
        ic.setInvoiceDate(command.getInvoiceDate());
        ic.setDueDate(command.getDueDate());
        ic.setCurrencyCode(o.getCurrencyCode());
        ic.setReference(command.getReference() != null ? command.getReference()
                : "INV/SO/" + o.getName());
        ic.setSalesOrderId(o.getId());
        ic.setExchangeRateToCompany(o.getExchangeRateToCompany());
        ic.setLines(invLines);
        return customerInvoiceApplicationService.createCustomerInvoice(ic);
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCustomerCreditNoteFromSalesOrder(CreateCustomerInvoiceFromSalesOrderCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        SalesOrder o = loadOrder(command.getSalesOrderId());
        if (!o.getCompanyId().equals(companyId)) {
            throw new SalesDomainException("Sales order company mismatch");
        }
        if (o.getState() == SalesOrderState.CANCELLED) {
            throw new SalesDomainException("Cannot credit a cancelled sales order");
        }
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException("Sales order must be confirmed before creating a credit note");
        }

        List<CustomerInvoiceResponse> postedInvoices =
                customerInvoiceApplicationService.listPostedInvoicesForSalesOrder(o.getId());
        if (postedInvoices.isEmpty()) {
            throw new SalesDomainException(
                    "No posted invoice on this order; open the customer invoice and create a credit note from there");
        }

        UUID resolvedSourceInvoiceId = command.getSourceInvoiceId();
        if (resolvedSourceInvoiceId == null) {
            if (postedInvoices.size() == 1) {
                resolvedSourceInvoiceId = postedInvoices.get(0).getId();
            } else {
                throw new SalesDomainException(
                        "Multiple posted invoices on this order; open the source invoice and create the credit note there");
            }
        } else {
            UUID sourceInvoiceId = resolvedSourceInvoiceId;
            boolean onOrder = postedInvoices.stream().anyMatch(inv -> inv.getId().equals(sourceInvoiceId));
            if (!onOrder) {
                throw new SalesDomainException("Source invoice does not belong to this sales order");
            }
        }

        CustomerInvoiceResponse sourceInvoice =
                customerInvoiceApplicationService.getCustomerInvoice(resolvedSourceInvoiceId);
        Map<UUID, BigDecimal> draftCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(o.getId());

        CreateCreditNoteFromInvoiceCommand cnCmd = new CreateCreditNoteFromInvoiceCommand();
        cnCmd.setCompanyId(companyId);
        cnCmd.setInvoiceDate(command.getInvoiceDate());
        cnCmd.setDueDate(command.getDueDate());
        cnCmd.setReference(command.getReference() != null ? command.getReference()
                : "CN/SO/" + o.getName());

        List<CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand> cnLines = new ArrayList<>();
        for (SalesOrderLine sol : o.getLines()) {
            BigDecimal qty = creditNoteableQtyForLine(sol, draftCn);
            if (qty.signum() <= 0) {
                continue;
            }
            for (var invLine : sourceInvoice.getLines()) {
                if (sol.getId().equals(invLine.getSalesOrderLineId())) {
                    CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand lc =
                            new CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand();
                    lc.setInvoiceLineId(invLine.getId());
                    lc.setQty(qty);
                    cnLines.add(lc);
                    break;
                }
            }
        }
        if (cnLines.isEmpty()) {
            throw new SalesDomainException(
                    "No credit-note quantity: return delivered goods first so qty invoiced exceeds qty delivered "
                            + "(or post/remove any draft credit note).");
        }
        cnCmd.setLines(cnLines);
        return customerInvoiceApplicationService.createCreditNoteFromInvoice(resolvedSourceInvoiceId, cnCmd);
    }

    private void addInvoiceTaxSnapshots(CustomerInvoiceLineCommand invLine, SalesOrderLine sol, BigDecimal invoiceQty) {
        List<FiscalTaxSnapshot> snaps = sol.getTaxes().stream()
                .map(t -> purchaseApplicationService.getFiscalTax(t.getTaxId()))
                .map(this::toSnapshot)
                .toList();
        PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                invoiceQty, sol.getUnitPrice(), sol.getDiscountPercent(), snaps);
        for (Map.Entry<UUID, BigDecimal> e : split.taxAmountById().entrySet()) {
            FiscalTaxResponse t = purchaseApplicationService.getFiscalTax(e.getKey());
            CustomerInvoiceLineTaxCommand ts = new CustomerInvoiceLineTaxCommand();
            ts.setTaxId(t.getId());
            ts.setTaxName(t.getName());
            ts.setTaxBase(split.net());
            ts.setTaxAmount(e.getValue());
            ts.setAccountId(t.getAccountId());
            invLine.getTaxSnapshots().add(ts);
        }
    }

    private SalesOrderResponse toResponse(SalesOrder o) {
        SalesOrderResponse r = new SalesOrderResponse();
        r.setId(o.getId());
        r.setCompanyId(o.getCompanyId());
        r.setCustomerPartnerId(o.getCustomerPartnerId());
        r.setName(o.getName());
        r.setState(o.getState());
        r.setDeliveryStatus(o.getDeliveryStatus());
        r.setInvoiceStatus(o.getInvoiceStatus());
        r.setOrderDate(o.getOrderDate());
        r.setValidityDate(o.getValidityDate());
        r.setWarehouseId(o.getWarehouseId());
        r.setPricelistId(o.getPricelistId());
        r.setPaymentTermsId(o.getPaymentTermsId());
        r.setCurrencyCode(o.getCurrencyCode());
        r.setExchangeRateToCompany(o.getExchangeRateToCompany());
        r.setIncoterm(o.getIncoterm());
        r.setNotes(o.getNotes());
        r.setAmountUntaxed(o.getAmountUntaxed());
        r.setAmountTax(o.getAmountTax());
        r.setAmountTotal(o.getAmountTotal());
        r.setQuotationSentAt(o.getQuotationSentAt());
        r.setConfirmedAt(o.getConfirmedAt());
        r.setCancelledAt(o.getCancelledAt());
        r.setDeliveryCompletedAt(o.getDeliveryCompletedAt());
        r.setInvoicingCompletedAt(o.getInvoicingCompletedAt());
        r.setDeliveryPickingIds(stockMoveSalesQueryPort.findPickingIdsBySalesOrderId(o.getId()));
        r.setCanCreateCustomerInvoice(computeCanCreateCustomerInvoice(o));
        r.setCanCreateCustomerCreditNote(computeCanCreateCustomerCreditNote(o));
        r.setLines(o.getLines().stream().sorted(Comparator.comparingInt(SalesOrderLine::getSequence)).map(l -> {
            SalesOrderLineResponse lr = new SalesOrderLineResponse();
            lr.setId(l.getId());
            lr.setSequence(l.getSequence());
            lr.setProductId(l.getProductId());
            lr.setName(l.getName());
            lr.setUomId(l.getUomId());
            lr.setQtyOrdered(l.getQtyOrdered());
            lr.setQtyDelivered(l.getQtyDelivered());
            lr.setQtyInvoiced(l.getQtyInvoiced());
            lr.setUnitPrice(l.getUnitPrice());
            lr.setDiscountPercent(l.getDiscountPercent());
            lr.setInvoicePolicy(l.getInvoicePolicy());
            lr.setRevenueAccountId(l.getRevenueAccountId());
            lr.setTaxIds(l.getTaxes().stream().map(SalesOrderLineTax::getTaxId).toList());
            return lr;
        }).toList());
        return r;
    }
}
