package com.bradox.erp.sales.service.domain;

import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxCommand;
import com.bradox.erp.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.SalesCogsClearingPort;
import com.bradox.erp.accounting.service.domain.ports.output.SalesOrderCogsStatePort;
import com.bradox.erp.accounting.service.domain.ports.output.SalesOrderInvoiceSyncPort;
import com.bradox.erp.contacts.service.domain.dto.CreditStatusResponse;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.bradox.erp.domain.settlement.OrderSettlement;
import com.bradox.erp.domain.settlement.OrderSettlementCalculator;
import com.bradox.erp.domain.settlement.SettlementDoc;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.DiscountMath;
import com.bradox.erp.domain.valueobject.DiscountType;
import com.bradox.erp.domain.valueobject.MonetaryScale;
import com.bradox.erp.inventory.domain.core.entity.Product;
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
import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import com.bradox.erp.inventory.service.domain.ports.input.StockHoldApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.StockMoveSalesQueryPort;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductPackagingRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.WarehouseRepository;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.platform.document.DocumentSequenceService;
import com.bradox.erp.platform.settings.CompanyDocumentPolicyService;
import com.bradox.erp.platform.web.CompanyContext;
import com.bradox.erp.purchase.domain.core.FiscalTaxScope;
import com.bradox.erp.purchase.service.domain.FiscalTaxSnapshot;
import com.bradox.erp.purchase.service.domain.PurchaseTaxEngine;
import com.bradox.erp.purchase.service.domain.dto.FiscalTaxResponse;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.bradox.erp.sales.domain.core.SalInvoicePolicy;
import com.bradox.erp.sales.domain.core.SalesDomainException;
import com.bradox.erp.sales.domain.core.SalesOrderDeliveryStatus;
import com.bradox.erp.sales.domain.core.SalesOrderInvoiceStatus;
import com.bradox.erp.sales.domain.core.SalesOrderRules;
import com.bradox.erp.sales.domain.core.SalesOrderState;
import com.bradox.erp.sales.domain.core.entity.Pricelist;
import com.bradox.erp.sales.domain.core.entity.PricelistItem;
import com.bradox.erp.sales.domain.core.entity.SalesOrder;
import com.bradox.erp.sales.domain.core.entity.SalesOrderLine;
import com.bradox.erp.sales.domain.core.entity.SalesOrderLineTax;
import com.bradox.erp.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.bradox.erp.sales.service.domain.dto.CreateSalesOrderCommand;
import com.bradox.erp.sales.service.domain.dto.CreateSalesReturnCommand;
import com.bradox.erp.sales.service.domain.dto.ReturnGoodsCommand;
import com.bradox.erp.sales.service.domain.dto.SalesCorrectionCommand;
import com.bradox.erp.sales.service.domain.dto.SalesCorrectionResult;
import com.bradox.erp.sales.service.domain.dto.SalesOrderLineCommand;
import com.bradox.erp.sales.service.domain.dto.SalesOrderLineResponse;
import com.bradox.erp.sales.service.domain.dto.SalesOrderResponse;
import com.bradox.erp.sales.service.domain.dto.SalesOrderSummaryResponse;
import com.bradox.erp.sales.service.domain.event.SalesOrderConfirmedEvent;
import com.bradox.erp.sales.service.domain.ports.input.SalesApplicationService;
import com.bradox.erp.sales.service.domain.ports.output.messaging.SalesEventPublisher;
import com.bradox.erp.sales.service.domain.ports.output.repository.PricelistRepository;
import com.bradox.erp.sales.service.domain.ports.output.repository.SalesOrderRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Validated
public class SalesApplicationServiceImpl implements SalesApplicationService, SalesOrderInvoiceSyncPort, SalesOrderCogsStatePort {

    private final SalesOrderRepository salesOrderRepository;
    private final PricelistRepository pricelistRepository;
    private final PartnerApplicationService partnerApplicationService;
    private final ProductRepository productRepository;
    private final ProductPackagingRepository productPackagingRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository stockLocationRepository;
    private final UomApplicationService uomApplicationService;
    private final StockPickingApplicationService stockPickingApplicationService;
    private final StockMoveSalesQueryPort stockMoveSalesQueryPort;
    private final PurchaseApplicationService purchaseApplicationService;
    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final ObjectProvider<SalesCogsClearingPort> salesCogsClearingPortProvider;
    private final ObjectProvider<StockHoldApplicationService> stockHoldProvider;
    private final SalesEventPublisher salesEventPublisher;
    private final SalesOrderQtyWriter salesOrderQtyWriter;
    private final DocumentSequenceService documentSequenceService;
    private final RecordActivityLogger activityLogger;
    private final CompanyDocumentPolicyService companyDocumentPolicyService;
    private final CurrencyConversionPort currencyConversionPort;
    private final AccountingReferenceLookupPort accountingReferenceLookupPort;

    public SalesApplicationServiceImpl(SalesOrderRepository salesOrderRepository,
                                       PricelistRepository pricelistRepository,
                                       PartnerApplicationService partnerApplicationService,
                                       ProductRepository productRepository,
                                       ProductPackagingRepository productPackagingRepository,
                                       WarehouseRepository warehouseRepository,
                                       StockLocationRepository stockLocationRepository,
                                       UomApplicationService uomApplicationService,
                                       StockPickingApplicationService stockPickingApplicationService,
                                       StockMoveSalesQueryPort stockMoveSalesQueryPort,
                                       PurchaseApplicationService purchaseApplicationService,
                                       @Lazy CustomerInvoiceApplicationService customerInvoiceApplicationService,
                                       ObjectProvider<CompanyContext> companyContextProvider,
                                       ObjectProvider<SalesCogsClearingPort> salesCogsClearingPortProvider,
                                       ObjectProvider<StockHoldApplicationService> stockHoldProvider,
                                       SalesEventPublisher salesEventPublisher,
                                       SalesOrderQtyWriter salesOrderQtyWriter,
                                       DocumentSequenceService documentSequenceService,
                                       RecordActivityLogger activityLogger,
                                       CompanyDocumentPolicyService companyDocumentPolicyService,
                                       AccountingReferenceLookupPort accountingReferenceLookupPort,
                                       CurrencyConversionPort currencyConversionPort) {
        this.salesOrderRepository = salesOrderRepository;
        this.pricelistRepository = pricelistRepository;
        this.partnerApplicationService = partnerApplicationService;
        this.productRepository = productRepository;
        this.productPackagingRepository = productPackagingRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.uomApplicationService = uomApplicationService;
        this.stockPickingApplicationService = stockPickingApplicationService;
        this.stockMoveSalesQueryPort = stockMoveSalesQueryPort;
        this.purchaseApplicationService = purchaseApplicationService;
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
        this.companyContextProvider = companyContextProvider;
        this.salesCogsClearingPortProvider = salesCogsClearingPortProvider;
        this.stockHoldProvider = stockHoldProvider;
        this.salesEventPublisher = salesEventPublisher;
        this.salesOrderQtyWriter = salesOrderQtyWriter;
        this.documentSequenceService = documentSequenceService;
        this.activityLogger = activityLogger;
        this.companyDocumentPolicyService = companyDocumentPolicyService;
        this.currencyConversionPort = currencyConversionPort;
        this.accountingReferenceLookupPort = accountingReferenceLookupPort;
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
            throw new SalesDomainException(
                    "error.sales.partnerNotCustomer", null, "Partner is not a customer");
        }
        if (!customer.getCompanyId().equals(companyId)) {
            throw new SalesDomainException("error.sales.customerCompanyMismatch", null, "Customer belongs to another company");
        }
        if (command.getPricelistId() != null) {
            Pricelist pl = pricelistRepository.findById(command.getPricelistId())
                    .orElseThrow(() -> new SalesDomainException("Pricelist not found"));
            if (!pl.getCompanyId().equals(companyId)) {
                throw new SalesDomainException("error.sales.pricelistCompanyMismatch", null, "Pricelist company mismatch");
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
                : documentSequenceService.next(companyId, "SO"));
        if (salesOrderRepository.findByCompanyIdAndName(companyId, o.getName()).isPresent()) {
            throw new SalesDomainException("error.sales.orderNameExists", new Object[] { o.getName() }, "Sales order name already exists: " + o.getName());
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
        applyOrderDiscountInput(o, command);
        o.setExchangeRateToCompany(resolveExchangeRate(
                o.getCompanyId(), o.getCurrencyCode(), o.getOrderDate(), command.getExchangeRateToCompany()));
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        int seq = 10;
        for (SalesOrderLineCommand lc : command.getLines()) {
            Product product = productRepository.findById(new ProductId(lc.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + lc.getProductId()));
            if (!product.isSaleOk()) {
                throw new SalesDomainException("error.sales.productNotSalable", new Object[] { lc.getProductId() }, "Product is not salable: " + lc.getProductId());
            }
            BigDecimal unitPrice = resolveUnitPrice(companyId, o.getPricelistId(), lc.getProductId(),
                    lc.getQtyOrdered(), orderDate, lc.getUnitPrice(), product);
            SalesOrderLine line = new SalesOrderLine();
            line.setId(UUID.randomUUID());
            line.setSequence(seq);
            line.setProductId(lc.getProductId());
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            applyPackagingSnapshot(line, lc.getPackagingId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setQtyDelivered(BigDecimal.ZERO);
            line.setQtyInvoiced(BigDecimal.ZERO);
            line.setQtyCogsCleared(BigDecimal.ZERO);
            line.setUnitPrice(unitPrice);
            line.setGift(Boolean.TRUE.equals(lc.getIsGift()));
            applyLineDiscountInput(line, lc);
            if (line.isGift()) {
                line.setDiscountType(DiscountType.PERCENT);
                line.setDiscountValue(BigDecimal.ZERO);
                line.setDiscountPercent(BigDecimal.ZERO);
            }
            line.setInvoicePolicy(lc.getInvoicePolicy() != null ? lc.getInvoicePolicy()
                    : (product.getProductType() == ProductType.SERVICE ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED));
            requireServiceForTimesheetPolicy(line, product);
            line.setRevenueAccountId(lc.getRevenueAccountId());
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTaxResponse tax = purchaseApplicationService.getFiscalTax(taxId);
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new SalesDomainException("error.sales.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.SALE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new SalesDomainException("error.sales.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for sale: " + taxId);
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
        requireWithinCreditLimitUnlessPaid(o);
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Sales Order created");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalesOrderResponse updateSalesOrder(UUID id, CreateSalesOrderCommand command) {
        SalesOrder o = loadOrder(id);
        if (command.getRowVersion() != null && command.getRowVersion() != o.getRowVersion()) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Sales order " + id + " was changed by someone else (version " + o.getRowVersion()
                            + ", client had " + command.getRowVersion() + ")");
        }
        SalesOrderRules.ensureCanUpdate(o.getState());
        if (o.isLocked()) {
            throw new SalesDomainException(
                    "error.sales.orderLocked", null, "Sales order is locked; unlock before amending");
        }
        if (o.getState() == SalesOrderState.CONFIRMED) {
            return amendConfirmedSalesOrder(o, command);
        }
        return replaceDraftOrQuotationSalesOrder(o, command);
    }

    private SalesOrderResponse replaceDraftOrQuotationSalesOrder(SalesOrder o, CreateSalesOrderCommand command) {
        UUID companyId = o.getCompanyId();
        PartnerResponse customer = partnerApplicationService.getPartner(command.getCustomerPartnerId());
        if (!customer.isCustomer()) {
            throw new SalesDomainException(
                    "error.sales.partnerNotCustomer", null, "Partner is not a customer");
        }
        if (!customer.getCompanyId().equals(companyId)) {
            throw new SalesDomainException("error.sales.customerCompanyMismatch", null, "Customer belongs to another company");
        }
        if (command.getPricelistId() != null) {
            Pricelist pl = pricelistRepository.findById(command.getPricelistId())
                    .orElseThrow(() -> new SalesDomainException("Pricelist not found"));
            if (!pl.getCompanyId().equals(companyId)) {
                throw new SalesDomainException("error.sales.pricelistCompanyMismatch", null, "Pricelist company mismatch");
            }
        }
        Instant now = Instant.now();
        LocalDate orderDate = command.getOrderDate() != null ? command.getOrderDate() : o.getOrderDate();
        o.setCustomerPartnerId(command.getCustomerPartnerId());
        o.setCurrencyCode(command.getCurrencyCode());
        o.setWarehouseId(command.getWarehouseId());
        o.setPricelistId(command.getPricelistId());
        o.setPaymentTermsId(command.getPaymentTermsId() != null ? command.getPaymentTermsId() : customer.getPaymentTermsId());
        o.setOrderDate(orderDate);
        o.setValidityDate(command.getValidityDate());
        o.setIncoterm(command.getIncoterm());
        o.setNotes(command.getNotes());
        applyOrderDiscountInput(o, command);
        // An explicit rate wins; otherwise the rate follows the order's currency and date, so
        // changing a draft from IQD to USD does not keep the old rate.
        o.setExchangeRateToCompany(resolveExchangeRate(
                o.getCompanyId(), o.getCurrencyCode(), o.getOrderDate(), command.getExchangeRateToCompany()));
        o.setUpdatedAt(now);
        o.getLines().clear();

        int seq = 10;
        for (SalesOrderLineCommand lc : command.getLines()) {
            Product product = productRepository.findById(new ProductId(lc.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + lc.getProductId()));
            if (!product.isSaleOk()) {
                throw new SalesDomainException("error.sales.productNotSalable", new Object[] { lc.getProductId() }, "Product is not salable: " + lc.getProductId());
            }
            BigDecimal unitPrice = resolveUnitPrice(companyId, o.getPricelistId(), lc.getProductId(),
                    lc.getQtyOrdered(), orderDate, lc.getUnitPrice(), product);
            SalesOrderLine line = new SalesOrderLine();
            line.setId(UUID.randomUUID());
            line.setSequence(seq);
            line.setProductId(lc.getProductId());
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            applyPackagingSnapshot(line, lc.getPackagingId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setQtyDelivered(BigDecimal.ZERO);
            line.setQtyInvoiced(BigDecimal.ZERO);
            line.setQtyCogsCleared(BigDecimal.ZERO);
            line.setUnitPrice(unitPrice);
            line.setGift(Boolean.TRUE.equals(lc.getIsGift()));
            applyLineDiscountInput(line, lc);
            if (line.isGift()) {
                line.setDiscountType(DiscountType.PERCENT);
                line.setDiscountValue(BigDecimal.ZERO);
                line.setDiscountPercent(BigDecimal.ZERO);
            }
            line.setInvoicePolicy(lc.getInvoicePolicy() != null ? lc.getInvoicePolicy()
                    : (product.getProductType() == ProductType.SERVICE ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED));
            requireServiceForTimesheetPolicy(line, product);
            line.setRevenueAccountId(lc.getRevenueAccountId());
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTaxResponse tax = purchaseApplicationService.getFiscalTax(taxId);
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new SalesDomainException("error.sales.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.SALE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new SalesDomainException("error.sales.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for sale: " + taxId);
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
        requireWithinCreditLimitUnlessPaid(o);
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        // Re-read so the response carries the version after this save (the client sends it back).
        return toResponse(loadOrder(saved.getId()));
    }

    private SalesOrderResponse amendConfirmedSalesOrder(SalesOrder o, CreateSalesOrderCommand command) {
        UUID companyId = o.getCompanyId();
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        Map<UUID, Boolean> giftBefore = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), line.getQtyOrdered() != null ? line.getQtyOrdered() : BigDecimal.ZERO);
            giftBefore.put(line.getId(), line.isGift());
        }
        BigDecimal untaxedBefore = o.getAmountUntaxed() != null ? o.getAmountUntaxed() : BigDecimal.ZERO;
        Map<UUID, String> lineTermsBefore = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            lineTermsBefore.put(line.getId(), lineTermsSignature(line));
        }
        CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(o.getId());
        boolean anyDelivered = o.getLines().stream().anyMatch(l -> nz(l.getQtyDelivered()).signum() > 0);
        boolean anyInvoiced = o.getLines().stream().anyMatch(l -> fulfilledInvoiced(postedNets, l).signum() > 0);
        boolean hasPostedDocs = anyDelivered
                || customerInvoiceApplicationService.hasPostedInvoiceForSalesOrder(o.getId());
        if (anyInvoiced && orderDiscountChanged(o, command)) {
            throw new SalesDomainException(
                    "error.sales.orderDiscountLockedAfterInvoicing", null,
                    "The order discount cannot change after invoicing; credit the invoice first");
        }
        if (hasPostedDocs) {
            if (command.getCustomerPartnerId() != null
                    && !command.getCustomerPartnerId().equals(o.getCustomerPartnerId())) {
                throw new SalesDomainException(
                        "error.sales.cannotChangeCustomerAfterInvoicing", null,
                        "Cannot change customer after posted invoices exist");
            }
            if (command.getCurrencyCode() != null && !command.getCurrencyCode().equalsIgnoreCase(o.getCurrencyCode())) {
                throw new SalesDomainException(
                        "error.sales.cannotChangeCurrencyAfterInvoicing", null,
                        "Cannot change currency after posted invoices exist");
            }
            if (command.getWarehouseId() != null && o.getWarehouseId() != null
                    && !command.getWarehouseId().equals(o.getWarehouseId())) {
                throw new SalesDomainException(
                        "error.sales.cannotChangeWarehouseAfterInvoicing", null,
                        "Cannot change warehouse after posted invoices exist");
            }
        } else {
            PartnerResponse customer = partnerApplicationService.getPartner(command.getCustomerPartnerId());
            if (!customer.isCustomer()) {
                throw new SalesDomainException(
                        "error.sales.partnerNotCustomer", null, "Partner is not a customer");
            }
            if (!customer.getCompanyId().equals(companyId)) {
                throw new SalesDomainException("error.sales.customerCompanyMismatch", null, "Customer belongs to another company");
            }
            o.setCustomerPartnerId(command.getCustomerPartnerId());
            o.setCurrencyCode(command.getCurrencyCode());
            o.setWarehouseId(command.getWarehouseId());
        }

        Instant now = Instant.now();
        LocalDate orderDate = command.getOrderDate() != null ? command.getOrderDate() : o.getOrderDate();
        if (command.getPricelistId() != null) {
            Pricelist pl = pricelistRepository.findById(command.getPricelistId())
                    .orElseThrow(() -> new SalesDomainException("Pricelist not found"));
            if (!pl.getCompanyId().equals(companyId)) {
                throw new SalesDomainException("error.sales.pricelistCompanyMismatch", null, "Pricelist company mismatch");
            }
            o.setPricelistId(command.getPricelistId());
        }
        o.setPaymentTermsId(command.getPaymentTermsId() != null ? command.getPaymentTermsId() : o.getPaymentTermsId());
        o.setOrderDate(orderDate);
        o.setValidityDate(command.getValidityDate());
        o.setIncoterm(command.getIncoterm());
        o.setNotes(command.getNotes());
        applyOrderDiscountInput(o, command);
        if (command.getExchangeRateToCompany() != null) {
            o.setExchangeRateToCompany(command.getExchangeRateToCompany());
        }
        o.setUpdatedAt(now);

        Map<UUID, SalesOrderLine> existingById = o.getLines().stream()
                .collect(java.util.stream.Collectors.toMap(SalesOrderLine::getId, l -> l, (a, b) -> a, java.util.LinkedHashMap::new));
        java.util.Set<UUID> keptIds = new java.util.LinkedHashSet<>();
        List<SalesOrderLine> nextLines = new ArrayList<>();
        int seq = 10;
        for (SalesOrderLineCommand lc : command.getLines()) {
            Product product = productRepository.findById(new ProductId(lc.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + lc.getProductId()));
            if (!product.isSaleOk()) {
                throw new SalesDomainException("error.sales.productNotSalable", new Object[] { lc.getProductId() }, "Product is not salable: " + lc.getProductId());
            }
            BigDecimal unitPrice = resolveUnitPrice(companyId, o.getPricelistId(), lc.getProductId(),
                    lc.getQtyOrdered(), orderDate, lc.getUnitPrice(), product);
            SalesOrderLine line;
            if (lc.getId() != null && existingById.containsKey(lc.getId())) {
                line = existingById.get(lc.getId());
                if (!line.getProductId().equals(lc.getProductId())) {
                    throw new SalesDomainException(
                            "error.sales.cannotChangeProductOnConfirmedLine", null,
                            "Cannot change product on a confirmed order line");
                }
                BigDecimal invoicedQty = fulfilledInvoiced(postedNets, line);
                assertLineAmendmentAllowed(line, lc, invoicedQty);
                if (invoicedQty.signum() > 0) {
                    // Invoiced lines keep their price; a pricelist re-resolve must not move it.
                    unitPrice = line.getUnitPrice();
                }
                keptIds.add(line.getId());
            } else {
                line = new SalesOrderLine();
                line.setId(UUID.randomUUID());
                line.setQtyDelivered(BigDecimal.ZERO);
                line.setQtyInvoiced(BigDecimal.ZERO);
                line.setQtyCogsCleared(BigDecimal.ZERO);
                line.setCreatedAt(now);
                line.setProductId(lc.getProductId());
                line.setInvoicePolicy(lc.getInvoicePolicy() != null ? lc.getInvoicePolicy()
                        : (product.getProductType() == ProductType.SERVICE ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED));
            }
            line.setSequence(seq);
            line.setName(lc.getName());
            line.setUomId(lc.getUomId());
            applyPackagingSnapshot(line, lc.getPackagingId());
            line.setQtyOrdered(lc.getQtyOrdered());
            line.setUnitPrice(unitPrice);
            line.setGift(Boolean.TRUE.equals(lc.getIsGift()));
            applyLineDiscountInput(line, lc);
            if (line.isGift()) {
                line.setDiscountType(DiscountType.PERCENT);
                line.setDiscountValue(BigDecimal.ZERO);
                line.setDiscountPercent(BigDecimal.ZERO);
            }
            if (lc.getInvoicePolicy() != null) {
                line.setInvoicePolicy(lc.getInvoicePolicy());
            }
            line.setRevenueAccountId(lc.getRevenueAccountId());
            line.setUpdatedAt(now);
            line.getTaxes().clear();
            int tseq = 10;
            for (UUID taxId : lc.getTaxIds()) {
                FiscalTaxResponse tax = purchaseApplicationService.getFiscalTax(taxId);
                if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                    throw new SalesDomainException("error.sales.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
                }
                if (tax.getScope() != FiscalTaxScope.SALE && tax.getScope() != FiscalTaxScope.BOTH) {
                    throw new SalesDomainException("error.sales.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for sale: " + taxId);
                }
                SalesOrderLineTax lt = new SalesOrderLineTax();
                lt.setId(UUID.randomUUID());
                lt.setTaxId(taxId);
                lt.setSequence(tseq);
                line.getTaxes().add(lt);
                tseq += 10;
            }
            nextLines.add(line);
            seq += 10;
        }
        for (SalesOrderLine existing : new ArrayList<>(o.getLines())) {
            if (keptIds.contains(existing.getId())) {
                continue;
            }
            if (existing.getQtyDelivered().signum() > 0 || existing.getQtyInvoiced().signum() > 0) {
                throw new SalesDomainException(
                        "error.sales.cannotRemoveDeliveredOrInvoicedLine", null,
                        "Cannot remove a line that has been delivered or invoiced");
            }
            o.getLines().remove(existing);
        }
        for (SalesOrderLine line : nextLines) {
            if (!o.getLines().contains(line)) {
                o.getLines().add(line);
            }
        }
        o.getLines().sort(Comparator.comparingInt(SalesOrderLine::getSequence));
        Map<UUID, String> lineTermsAfter = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            lineTermsAfter.put(line.getId(), lineTermsSignature(line));
        }
        if (!lineTermsAfter.equals(lineTermsBefore) && hasDraftInvoiceDocuments(o.getId())) {
            throw new SalesDomainException(
                    "error.sales.draftInvoiceBlocksAmendment", null,
                    "A draft invoice or credit note exists for this order; post or delete it before changing lines");
        }
        recalcTotals(o);
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        postSalesAmendmentTracking(saved, qtyOrderedBefore, giftBefore, untaxedBefore);
        return syncDocumentsAfterAmendment(saved);
    }

    private void postSalesAmendmentTracking(
            SalesOrder saved,
            Map<UUID, BigDecimal> qtyOrderedBefore,
            Map<UUID, Boolean> giftBefore,
            BigDecimal untaxedBefore) {
        List<String> qtyBlocks = new ArrayList<>();
        List<String> giftBlocks = new ArrayList<>();
        for (SalesOrderLine line : saved.getLines()) {
            String label = line.getName() != null && !line.getName().isBlank()
                    ? line.getName()
                    : line.getProductId().toString().substring(0, 8);
            Optional<Product> product = productRepository.findById(new ProductId(line.getProductId()));
            if (product.isPresent()) {
                label = "[" + product.get().getSku() + "] " + product.get().getName();
            }
            BigDecimal oldQty = qtyOrderedBefore.get(line.getId());
            if (oldQty != null) {
                BigDecimal newQty = line.getQtyOrdered() != null ? line.getQtyOrdered() : BigDecimal.ZERO;
                if (oldQty.compareTo(newQty) != 0) {
                    StringBuilder block = new StringBuilder("• ").append(label).append(":\n  Ordered Quantity: ")
                            .append(MonetaryScale.toDisplayString(oldQty))
                            .append(" → ")
                            .append(MonetaryScale.toDisplayString(newQty));
                    if (line.getQtyInvoiced() != null && line.getQtyInvoiced().signum() > 0) {
                        block.append("\n  Invoiced Quantity: ")
                                .append(MonetaryScale.toDisplayString(line.getQtyInvoiced()));
                    }
                    qtyBlocks.add(block.toString());
                }
            }
            Boolean wasGift = giftBefore.get(line.getId());
            if (wasGift != null && wasGift != line.isGift()) {
                giftBlocks.add("• " + label + ":\n  "
                        + (wasGift ? "Gift → Sale" : "Sale → Gift"));
            }
        }
        if (!qtyBlocks.isEmpty()) {
            activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                    "The ordered quantity has been updated.\n" + String.join("\n", qtyBlocks));
        }
        if (!giftBlocks.isEmpty()) {
            activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                    "The gift flag has been updated.\n" + String.join("\n", giftBlocks));
        }
        BigDecimal untaxedAfter = saved.getAmountUntaxed() != null ? saved.getAmountUntaxed() : BigDecimal.ZERO;
        if (untaxedBefore.compareTo(untaxedAfter) != 0) {
            activityLogger.logFieldChange(
                    saved.getCompanyId(),
                    RecordActivityLogger.MODEL_SALES_ORDER,
                    saved.getId(),
                    MonetaryScale.toDisplayString(untaxedBefore),
                    MonetaryScale.toDisplayString(untaxedAfter),
                    "Subtotal");
        }
    }

    /**
     * After a confirmed-order amendment: make the open deliveries match what is still to deliver
     * (ordered − delivered) per line. Quantities below delivered are refused before this point, so
     * no return is ever created here. Delivery validation and invoices stay manual actions.
     */
    private SalesOrderResponse syncDocumentsAfterAmendment(SalesOrder saved) {
        if (saved.getWarehouseId() == null) {
            return toResponse(saved);
        }
        salesOrderQtyWriter.updateQtyDeliveredJoiningCurrentTransaction(saved.getId());
        SalesOrder o = loadOrder(saved.getId());

        syncOpenDeliveries(o);
        o = loadOrder(o.getId());
        refreshOrderStatuses(o);
        salesOrderRepository.save(o);
        return getSalesOrder(o.getId());
    }

    /**
     * Open (not done, not cancelled) outgoing pickings must carry exactly the remaining demand per
     * line. When they already do, nothing changes; otherwise they are cancelled (releasing their
     * reservations) and replaced by one delivery for the remaining quantities, so an amendment can
     * never leave two deliveries for the same goods.
     */
    private void syncOpenDeliveries(SalesOrder o) {
        syncOpenDeliveries(o, false);
    }

    /**
     * @param trimOnly only remove open demand beyond what is left to deliver (after a delivery was
     *                 validated some other way); never add demand that is not already open.
     */
    private void syncOpenDeliveries(SalesOrder o, boolean trimOnly) {
        Map<UUID, BigDecimal> wanted = new LinkedHashMap<>();
        for (StockMoveCommand mc : buildRemainingOutgoingMoves(o)) {
            wanted.merge(mc.getSalesOrderLineId(), mc.getDemandQuantity(), BigDecimal::add);
        }
        List<UUID> openPickings = new ArrayList<>();
        Map<UUID, BigDecimal> open = new LinkedHashMap<>();
        for (UUID pickingId : stockMoveSalesQueryPort.findPickingIdsBySalesOrderId(o.getId())) {
            StockPickingResponse p = stockPickingApplicationService.getPicking(pickingId);
            if (p.getState() == PickingState.DONE || p.getState() == PickingState.CANCELLED) {
                continue;
            }
            openPickings.add(pickingId);
            for (StockPickingResponse.MoveResponse m : p.getMoves()) {
                if (m.getSalesOrderLineId() == null || m.getState() == MoveState.DONE
                        || m.getState() == MoveState.CANCELLED) {
                    continue;
                }
                open.merge(m.getSalesOrderLineId(), nz(m.getDemandQuantity()), BigDecimal::add);
            }
        }
        if (trimOnly) {
            boolean excess = false;
            for (Map.Entry<UUID, BigDecimal> e : open.entrySet()) {
                if (e.getValue().compareTo(nz(wanted.get(e.getKey()))) > 0) {
                    excess = true;
                }
            }
            if (!excess) {
                return;
            }
            // Keep each line's open demand, capped at what is still to deliver.
            Map<UUID, BigDecimal> capped = new LinkedHashMap<>();
            for (Map.Entry<UUID, BigDecimal> e : open.entrySet()) {
                capped.put(e.getKey(), e.getValue().min(nz(wanted.get(e.getKey()))));
            }
            wanted = capped;
        } else if (sameQuantities(wanted, open)) {
            return;
        }
        for (UUID pickingId : openPickings) {
            stockPickingApplicationService.cancelPicking(pickingId);
        }
        createDeliveryPicking(o, wanted);
    }

    /** Cancels every delivery and return of the order that is not done or already cancelled. */
    private void cancelOpenPickings(UUID salesOrderId) {
        List<UUID> pickingIds = new ArrayList<>(stockMoveSalesQueryPort.findPickingIdsBySalesOrderId(salesOrderId));
        pickingIds.addAll(stockMoveSalesQueryPort.findReturnPickingIdsBySalesOrderId(salesOrderId));
        for (UUID pickingId : pickingIds) {
            PickingState state = stockPickingApplicationService.getPicking(pickingId).getState();
            if (state != PickingState.DONE && state != PickingState.CANCELLED) {
                stockPickingApplicationService.cancelPicking(pickingId);
            }
        }
    }

    @Override
    @Transactional
    public SalesOrderResponse returnGoods(UUID id, ReturnGoodsCommand command) {
        SalesOrder o = loadOrder(id);
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException(
                    "error.sales.returnRequiresConfirmed", null,
                    "Sales order must be confirmed to create a return");
        }
        if (o.isLocked()) {
            throw new SalesDomainException(
                    "error.sales.orderLocked", null, "Sales order is locked; unlock before amending");
        }
        Map<UUID, SalesOrderLine> byId = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            byId.put(line.getId(), line);
        }
        // Requested quantities, checked against what is delivered (R7) and converted to stock units.
        Map<UUID, BigDecimal> wantedStockQty = new LinkedHashMap<>();
        Map<UUID, Product> productByLine = new LinkedHashMap<>();
        for (ReturnGoodsCommand.Line rl : command.getLines()) {
            BigDecimal qty = nz(rl.getQty());
            if (qty.signum() <= 0) {
                continue;
            }
            SalesOrderLine sol = byId.get(rl.getSalesOrderLineId());
            if (sol == null) {
                throw new SalesDomainException("error.sales.documentLineNotOnOrder", null,
                        "The document has a line that no longer exists on the sales order");
            }
            Product product = productRepository.findById(new ProductId(sol.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + sol.getProductId()));
            if (product.getProductType() == ProductType.SERVICE || qty.compareTo(nz(sol.getQtyDelivered())) > 0) {
                throw new SalesDomainException("error.sales.returnExceedsDelivered",
                        new Object[] { sol.getName() },
                        "Cannot return more than was delivered for '" + sol.getName() + "'");
            }
            wantedStockQty.merge(sol.getId(), toStockUomQty(sol, product, qty), BigDecimal::add);
            productByLine.put(sol.getId(), product);
        }
        if (wantedStockQty.isEmpty()) {
            throw new SalesDomainException("error.sales.nothingToReturn", null, "No delivered stock available to return");
        }

        // Validated deliveries, newest first, and what earlier returns already took back from each.
        List<StockPickingResponse> deliveries = new ArrayList<>();
        for (UUID pid : stockMoveSalesQueryPort.findPickingIdsBySalesOrderId(o.getId())) {
            StockPickingResponse p = stockPickingApplicationService.getPicking(pid);
            if (p.getState() == PickingState.DONE) {
                deliveries.add(p);
            }
        }
        deliveries.sort(Comparator.comparing(StockPickingResponse::getValidatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        Map<String, BigDecimal> alreadyReturned = new LinkedHashMap<>();
        for (UUID rid : stockMoveSalesQueryPort.findReturnPickingIdsBySalesOrderId(o.getId())) {
            StockPickingResponse r = stockPickingApplicationService.getPicking(rid);
            if (r.getState() == PickingState.CANCELLED || r.getBackorderOf() == null) {
                continue;
            }
            for (StockPickingResponse.MoveResponse m : r.getMoves()) {
                if (m.getSalesOrderLineId() == null || m.getState() == MoveState.CANCELLED) {
                    continue;
                }
                BigDecimal q = m.getState() == MoveState.DONE ? nz(m.getPickedQuantity()) : nz(m.getDemandQuantity());
                alreadyReturned.merge(r.getBackorderOf() + "|" + m.getSalesOrderLineId(), q, BigDecimal::add);
            }
        }

        // Spread each line's quantity over the deliveries it came from.
        Map<UUID, Map<UUID, BigDecimal>> planByDelivery = new LinkedHashMap<>();
        for (Map.Entry<UUID, BigDecimal> want : wantedStockQty.entrySet()) {
            BigDecimal rem = want.getValue();
            for (StockPickingResponse d : deliveries) {
                String key = d.getId() + "|" + want.getKey();
                BigDecimal returnedHere = nz(alreadyReturned.get(key));
                for (StockPickingResponse.MoveResponse m : d.getMoves()) {
                    if (rem.signum() <= 0) {
                        break;
                    }
                    if (!want.getKey().equals(m.getSalesOrderLineId()) || m.getState() != MoveState.DONE) {
                        continue;
                    }
                    BigDecimal picked = nz(m.getPickedQuantity());
                    BigDecimal usedByEarlier = returnedHere.min(picked);
                    returnedHere = returnedHere.subtract(usedByEarlier);
                    BigDecimal take = rem.min(picked.subtract(usedByEarlier));
                    if (take.signum() <= 0) {
                        continue;
                    }
                    planByDelivery.computeIfAbsent(d.getId(), k -> new LinkedHashMap<>()).merge(m.getId(), take, BigDecimal::add);
                    rem = rem.subtract(take);
                }
                if (rem.signum() <= 0) {
                    break;
                }
            }
            if (rem.signum() > 0) {
                SalesOrderLine sol = byId.get(want.getKey());
                throw new SalesDomainException("error.sales.returnExceedsDelivered",
                        new Object[] { sol.getName() },
                        "Cannot return more than was delivered for '" + sol.getName() + "'");
            }
        }

        for (Map.Entry<UUID, Map<UUID, BigDecimal>> plan : planByDelivery.entrySet()) {
            // A move missing from the quantity map is returned in full, so every other move of the
            // delivery gets an explicit zero: only the requested lines come back.
            Map<UUID, BigDecimal> quantities = new LinkedHashMap<>();
            for (StockPickingResponse d : deliveries) {
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
        if (command.isDamaged()) {
            scrapReturnedGoods(o, wantedStockQty, productByLine);
        }
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, o.getId(),
                (command.isRefund() ? "Goods returned and refunded" : "Goods returned for replacement")
                        + (command.isDamaged() ? " (damaged, scrapped)" : "")
                        + (command.getReason() != null && !command.getReason().isBlank()
                                ? ": " + command.getReason().trim() : ""));
        return getSalesOrder(id);
    }

    /** Damaged returns leave sellable stock right away: stock to inventory loss (Dr COGS, Cr Inventory). */
    private void scrapReturnedGoods(SalesOrder o, Map<UUID, BigDecimal> stockQtyByLine, Map<UUID, Product> productByLine) {
        Warehouse wh = warehouseRepository.findById(new WarehouseId(o.getWarehouseId()))
                .orElseThrow(() -> new SalesDomainException("Warehouse not found: " + o.getWarehouseId()));
        if (wh.getStockLocationId() == null) {
            throw new SalesDomainException("error.sales.warehouseStockLocationUnresolved", null,
                    "Warehouse stock location could not be resolved");
        }
        StockLocation loss = stockLocationRepository.findByCompany(new CompanyId(o.getCompanyId()), false).stream()
                .filter(l -> l.getLocationType() == LocationType.INVENTORY_LOSS)
                .findFirst()
                .orElseThrow(() -> new SalesDomainException("error.sales.scrapLocationMissing", null,
                        "No inventory loss location is configured for scrapping"));
        List<StockMoveCommand> moves = new ArrayList<>();
        for (Map.Entry<UUID, BigDecimal> e : stockQtyByLine.entrySet()) {
            Product product = productByLine.get(e.getKey());
            StockMoveCommand mc = new StockMoveCommand();
            mc.setProductId(product.getId().getId());
            mc.setUomId(product.getUomId().getId());
            mc.setDemandQuantity(e.getValue());
            moves.add(mc);
        }
        CreateStockPickingCommand cmd = new CreateStockPickingCommand();
        cmd.setCompanyId(o.getCompanyId());
        cmd.setWarehouseId(o.getWarehouseId());
        cmd.setPickingType(PickingType.OUTGOING);
        cmd.setSourceLocationId(wh.getStockLocationId().getId());
        cmd.setDestinationLocationId(loss.getId().getId());
        cmd.setOrigin("Scrap of returned goods " + o.getName());
        cmd.setReference(o.getName() != null ? o.getName() + "-SCRAP" : null);
        cmd.setMoves(moves);
        StockPickingResponse scrap = stockPickingApplicationService.createPicking(cmd);
        stockPickingApplicationService.validatePicking(scrap.getId(), null);
    }

    // ---------------------------------------------------------------- guided corrections

    /** Corrections need a confirmed, unlocked order with no draft invoice or credit note in the way. */
    private void assertCorrectable(SalesOrder o) {
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException("error.sales.correctionRequiresConfirmed", null,
                    "Only confirmed sales orders can be corrected");
        }
        if (o.isLocked()) {
            throw new SalesDomainException(
                    "error.sales.orderLocked", null, "Sales order is locked; unlock before amending");
        }
        if (hasDraftInvoiceDocuments(o.getId())) {
            throw new SalesDomainException(
                    "error.sales.draftInvoiceBlocksAmendment", null,
                    "A draft invoice or credit note exists for this order; post or delete it before changing lines");
        }
    }

    private java.util.Set<UUID> postedDocumentIds(UUID salesOrderId) {
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        for (CustomerInvoiceResponse d : customerInvoiceApplicationService.listPostedDocumentsForSalesOrder(salesOrderId)) {
            ids.add(d.getId());
        }
        return ids;
    }

    /** The order afterwards plus every invoice and credit note posted since {@code before}. */
    private SalesCorrectionResult correctionResult(UUID salesOrderId, java.util.Set<UUID> before) {
        SalesCorrectionResult result = new SalesCorrectionResult();
        result.setOrder(getSalesOrder(salesOrderId));
        for (CustomerInvoiceResponse d : customerInvoiceApplicationService.listPostedDocumentsForSalesOrder(salesOrderId)) {
            if (!before.contains(d.getId())) {
                result.getDocuments().add(d);
            }
        }
        return result;
    }

    private void reinvoice(SalesOrder o, Map<UUID, BigDecimal> lineQuantities) {
        Map<UUID, BigDecimal> positive = new LinkedHashMap<>();
        lineQuantities.forEach((k, v) -> {
            if (nz(v).signum() > 0) {
                positive.put(k, v);
            }
        });
        if (positive.isEmpty()) {
            return;
        }
        CreateCustomerInvoiceFromSalesOrderCommand cmd = new CreateCustomerInvoiceFromSalesOrderCommand();
        cmd.setCompanyId(o.getCompanyId());
        cmd.setSalesOrderId(o.getId());
        cmd.setInvoiceDate(LocalDate.now());
        cmd.setLineQuantities(positive);
        CustomerInvoiceResponse inv = createCustomerInvoiceFromSalesOrder(cmd);
        if (inv.getState() != com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState.POSTED) {
            customerInvoiceApplicationService.postCustomerInvoice(inv.getId());
        }
    }

    private List<SalesOrderLineTax> buildLineTaxes(UUID companyId, List<UUID> taxIds) {
        List<SalesOrderLineTax> taxes = new ArrayList<>();
        int seq = 10;
        for (UUID taxId : taxIds) {
            FiscalTaxResponse tax = purchaseApplicationService.getFiscalTax(taxId);
            if (!tax.getCompanyId().equals(companyId) || !tax.isActive()) {
                throw new SalesDomainException("error.sales.invalidTax", new Object[] { taxId }, "Invalid tax: " + taxId);
            }
            if (tax.getScope() != FiscalTaxScope.SALE && tax.getScope() != FiscalTaxScope.BOTH) {
                throw new SalesDomainException("error.sales.invalidTaxScope", new Object[] { taxId }, "Tax scope not valid for sale: " + taxId);
            }
            SalesOrderLineTax lt = new SalesOrderLineTax();
            lt.setId(UUID.randomUUID());
            lt.setTaxId(taxId);
            lt.setSequence(seq);
            taxes.add(lt);
            seq += 10;
        }
        return taxes;
    }

    /**
     * Rate to the company currency for an order. An explicit rate other than 1 wins; for a
     * foreign currency a missing rate or 1 is replaced by the rate in force on the order date.
     */
    private BigDecimal resolveExchangeRate(UUID companyId, String currencyCode, LocalDate asOf, BigDecimal explicit) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return explicit != null && explicit.signum() > 0 ? explicit : BigDecimal.ONE;
        }
        if (explicit != null && explicit.signum() > 0) {
            String base = currencyConversionPort.baseCurrencyCode(companyId);
            if (explicit.compareTo(BigDecimal.ONE) != 0 || currencyCode.equalsIgnoreCase(base)) {
                return explicit.setScale(12, java.math.RoundingMode.HALF_UP);
            }
        }
        return currencyConversionPort.exchangeRateToCompany(companyId, currencyCode, asOf);
    }

    private void saveCorrectedOrder(SalesOrder o, Map<UUID, BigDecimal> qtyOrderedBefore,
                                    Map<UUID, Boolean> giftBefore, BigDecimal untaxedBefore, String message,
                                    String reason) {
        o.setUpdatedAt(Instant.now());
        recalcTotals(o);
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        postSalesAmendmentTracking(saved, qtyOrderedBefore, giftBefore, untaxedBefore);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                message + (reason != null && !reason.isBlank() ? ": " + reason.trim() : ""));
    }

    @Override
    @Transactional
    public SalesCorrectionResult changeTerms(UUID id, SalesCorrectionCommand command) {
        SalesOrder o = loadOrder(id);
        assertCorrectable(o);
        java.util.Set<UUID> before = postedDocumentIds(id);
        CustomerInvoiceApplicationService.PostedSoLineQtyNets nets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(id);
        Map<UUID, SalesOrderLine> byId = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            byId.put(line.getId(), line);
        }
        boolean orderDiscountChanged = false;
        if (command.getOrderDiscountType() != null || command.getOrderDiscountValue() != null) {
            DiscountType t = command.getOrderDiscountType() != null ? command.getOrderDiscountType() : o.getOrderDiscountType();
            orderDiscountChanged = t != o.getOrderDiscountType()
                    || (command.getOrderDiscountValue() != null && !sameAmount(command.getOrderDiscountValue(), o.getOrderDiscountValue()));
        }
        // Which invoiced quantities must be credited at the old terms and re-invoiced at the new ones.
        Map<UUID, BigDecimal> affected = new LinkedHashMap<>();
        boolean anyChange = orderDiscountChanged;
        for (SalesCorrectionCommand.Line cl : command.getLines()) {
            SalesOrderLine sol = byId.get(cl.getSalesOrderLineId());
            if (sol == null) {
                throw new SalesDomainException("error.sales.documentLineNotOnOrder", null,
                        "The document has a line that no longer exists on the sales order");
            }
            if (!lineTermsChange(sol, cl)) {
                continue;
            }
            anyChange = true;
            if (sol.isGift() && nets.giftNetFor(sol.getId()).signum() > 0) {
                throw new SalesDomainException("error.sales.invoicedLineTermsLocked", null,
                        "Price, discount and taxes are locked once a line is invoiced; credit the invoice first");
            }
            BigDecimal charged = nets.chargeNetFor(sol.getId());
            if (charged.signum() > 0) {
                affected.put(sol.getId(), charged);
            }
        }
        if (!anyChange) {
            throw new SalesDomainException("error.sales.correctionNothingToChange", null, "Nothing to change");
        }
        if (orderDiscountChanged) {
            for (SalesOrderLine sol : o.getLines()) {
                BigDecimal charged = nets.chargeNetFor(sol.getId());
                if (!sol.isGift() && charged.signum() > 0) {
                    affected.put(sol.getId(), charged);
                }
            }
        }
        creditQuantities(o, affected, false);

        o = loadOrder(id);
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        Map<UUID, Boolean> giftBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        Map<UUID, SalesCorrectionCommand.Line> changes = new LinkedHashMap<>();
        for (SalesCorrectionCommand.Line cl : command.getLines()) {
            changes.put(cl.getSalesOrderLineId(), cl);
        }
        for (SalesOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            giftBefore.put(line.getId(), line.isGift());
            SalesCorrectionCommand.Line cl = changes.get(line.getId());
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
                line.getTaxes().addAll(buildLineTaxes(o.getCompanyId(), cl.getTaxIds()));
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
        saveCorrectedOrder(o, qtyOrderedBefore, giftBefore, untaxedBefore,
                "Prices, discounts or taxes corrected after invoicing", command.getReason());
        reinvoice(loadOrder(id), affected);
        rematchCustomerCredit(id, before);
        reconcileCogsClearing(id);
        return correctionResult(id, before);
    }

    /**
     * Money the customer already paid on the credited invoices moves to the invoices that replaced
     * them: the credit is released from the old invoices and applied to the new ones, never more
     * than was released.
     */
    private void rematchCustomerCredit(UUID salesOrderId, java.util.Set<UUID> before) {
        BigDecimal released = BigDecimal.ZERO;
        List<CustomerInvoiceResponse> invoices = customerInvoiceApplicationService.listPostedInvoicesForSalesOrder(salesOrderId);
        for (CustomerInvoiceResponse inv : invoices) {
            if (before.contains(inv.getId()) && nz(inv.getAmountOverpaid()).signum() > 0) {
                released = released.add(customerInvoiceApplicationService.keepCustomerCredit(inv.getId()));
            }
        }
        for (CustomerInvoiceResponse inv : invoices) {
            if (released.signum() <= 0) {
                break;
            }
            if (!before.contains(inv.getId())) {
                released = released.subtract(customerInvoiceApplicationService.applyCustomerCreditUpTo(inv.getId(), released));
            }
        }
    }

    private static boolean lineTermsChange(SalesOrderLine sol, SalesCorrectionCommand.Line cl) {
        if (cl.getUnitPrice() != null && !sameAmount(cl.getUnitPrice(), sol.getUnitPrice())) {
            return true;
        }
        if (cl.getDiscountType() != null && cl.getDiscountType() != sol.getDiscountType()) {
            return true;
        }
        if (cl.getDiscountValue() != null && !sameAmount(cl.getDiscountValue(), sol.getDiscountValue())) {
            return true;
        }
        if (cl.getTaxIds() != null) {
            java.util.Set<UUID> current = new java.util.HashSet<>();
            for (SalesOrderLineTax t : sol.getTaxes()) {
                current.add(t.getTaxId());
            }
            return !current.equals(new java.util.HashSet<>(cl.getTaxIds()));
        }
        return false;
    }

    @Override
    @Transactional
    public SalesCorrectionResult reduceQuantities(UUID id, SalesCorrectionCommand command) {
        SalesOrder o = loadOrder(id);
        assertCorrectable(o);
        java.util.Set<UUID> before = postedDocumentIds(id);
        CustomerInvoiceApplicationService.PostedSoLineQtyNets nets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(id);
        Map<UUID, SalesOrderLine> byId = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            byId.put(line.getId(), line);
        }
        Map<UUID, BigDecimal> target = new LinkedHashMap<>();
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        for (SalesCorrectionCommand.Line cl : command.getLines()) {
            SalesOrderLine sol = byId.get(cl.getSalesOrderLineId());
            if (sol == null || cl.getQty() == null) {
                throw new SalesDomainException("error.sales.documentLineNotOnOrder", null,
                        "The document has a line that no longer exists on the sales order");
            }
            BigDecimal t = cl.getQty();
            if (t.signum() < 0 || t.compareTo(nz(sol.getQtyOrdered())) > 0) {
                throw new SalesDomainException("error.sales.reduceOnlyLowers", null,
                        "Reduce quantities can only lower a line; increase it by editing the order");
            }
            if (t.compareTo(nz(sol.getQtyDelivered())) < 0) {
                throw new SalesDomainException(
                        "error.sales.qtyBelowDelivered",
                        new Object[] { MonetaryScale.toDisplayString(nz(sol.getQtyDelivered())) },
                        "Quantity cannot be below the delivered quantity; return the goods first");
            }
            target.put(sol.getId(), t);
            BigDecimal excess = fulfilledInvoiced(nets, sol).subtract(t);
            if (excess.signum() > 0) {
                toCredit.put(sol.getId(), excess);
            }
        }
        creditQuantities(o, toCredit, true);

        o = loadOrder(id);
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        Map<UUID, Boolean> giftBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        for (SalesOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            giftBefore.put(line.getId(), line.isGift());
            BigDecimal t = target.get(line.getId());
            if (t != null) {
                line.setQtyOrdered(t);
                line.setUpdatedAt(Instant.now());
            }
        }
        saveCorrectedOrder(o, qtyOrderedBefore, giftBefore, untaxedBefore,
                "Quantities reduced after invoicing", command.getReason());
        if (o.getWarehouseId() != null) {
            syncOpenDeliveries(loadOrder(id));
        }
        reconcileCogsClearing(id);
        return correctionResult(id, before);
    }

    @Override
    @Transactional
    public SalesCorrectionResult cancelWithDocuments(UUID id, SalesCorrectionCommand command) {
        SalesOrder o = loadOrder(id);
        if (o.getState() != SalesOrderState.CONFIRMED) {
            SalesOrderResponse cancelled = cancelSalesOrder(id);
            SalesCorrectionResult r = new SalesCorrectionResult();
            r.setOrder(cancelled);
            return r;
        }
        assertCorrectable(o);
        java.util.Set<UUID> before = postedDocumentIds(id);
        String reason = command != null ? command.getReason() : null;
        // 1. Everything delivered comes back, refunded (credits the delivered and invoiced part).
        ReturnGoodsCommand ret = new ReturnGoodsCommand();
        for (SalesOrderLine line : o.getLines()) {
            if (nz(line.getQtyDelivered()).signum() > 0) {
                ReturnGoodsCommand.Line rl = new ReturnGoodsCommand.Line();
                rl.setSalesOrderLineId(line.getId());
                rl.setQty(line.getQtyDelivered());
                ret.getLines().add(rl);
            }
        }
        if (!ret.getLines().isEmpty()) {
            ret.setRefund(true);
            ret.setReason(reason);
            returnGoods(id, ret);
        }
        // 2. Whatever is still invoiced (invoiced before delivery) is credited.
        o = loadOrder(id);
        CustomerInvoiceApplicationService.PostedSoLineQtyNets nets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(id);
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal invoiced = nets.chargeNetFor(line.getId()).add(nets.giftNetFor(line.getId()));
            if (invoiced.signum() > 0) {
                toCredit.put(line.getId(), invoiced);
            }
        }
        creditQuantities(o, toCredit, true);
        // 3. Nothing delivered or invoiced is left: cancel (open deliveries and drafts go with it).
        cancelSalesOrder(id);
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, id,
                "Cancelled with its documents" + (reason != null && !reason.isBlank() ? ": " + reason.trim() : ""));
        return correctionResult(id, before);
    }

    @Override
    @Transactional
    public SalesCorrectionResult reassignCustomer(UUID id, SalesCorrectionCommand command) {
        SalesOrder o = loadOrder(id);
        assertCorrectable(o);
        if (command.getCustomerPartnerId() == null || command.getCustomerPartnerId().equals(o.getCustomerPartnerId())) {
            throw new SalesDomainException("error.sales.correctionNothingToChange", null, "Nothing to change");
        }
        if (o.getLines().stream().anyMatch(l -> nz(l.getQtyDelivered()).signum() > 0)) {
            throw new SalesDomainException("error.sales.reassignAfterDelivery", null,
                    "Goods were already delivered to this customer; return them first");
        }
        PartnerResponse customer = partnerApplicationService.getPartner(command.getCustomerPartnerId());
        if (!customer.isCustomer()) {
            throw new SalesDomainException("error.sales.partnerNotCustomer", null, "Partner is not a customer");
        }
        if (!customer.getCompanyId().equals(o.getCompanyId())) {
            throw new SalesDomainException("error.sales.customerCompanyMismatch", null, "Customer belongs to another company");
        }
        java.util.Set<UUID> before = postedDocumentIds(id);
        CustomerInvoiceApplicationService.PostedSoLineQtyNets nets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(id);
        Map<UUID, BigDecimal> invoiced = new LinkedHashMap<>();
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal fulfilled = fulfilledInvoiced(nets, line);
            if (fulfilled.signum() > 0) {
                invoiced.put(line.getId(), fulfilled);
                toCredit.put(line.getId(), nets.chargeNetFor(line.getId()).add(nets.giftNetFor(line.getId())));
            }
        }
        creditQuantities(o, toCredit, true);

        o = loadOrder(id);
        UUID previous = o.getCustomerPartnerId();
        o.setCustomerPartnerId(customer.getId());
        if (customer.getPaymentTermsId() != null) {
            o.setPaymentTermsId(customer.getPaymentTermsId());
        }
        o.setUpdatedAt(Instant.now());
        salesOrderRepository.save(o);
        salesOrderRepository.flush();
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, id,
                "Customer reassigned (" + previous + " → " + customer.getDisplayName() + ")"
                        + (command.getReason() != null && !command.getReason().isBlank() ? ": " + command.getReason().trim() : ""));
        if (o.getWarehouseId() != null) {
            // Open deliveries carry the partner: replace them so they go to the new customer.
            cancelOpenPickings(id);
            createDeliveryPickingForRemaining(loadOrder(id));
        }
        reinvoice(loadOrder(id), invoiced);
        reconcileCogsClearing(id);
        return correctionResult(id, before);
    }

    @Override
    @Transactional
    public SalesOrderResponse closeRemainingQuantities(UUID id, String reason) {
        SalesOrder o = loadOrder(id);
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException("error.sales.closeRemainingRequiresConfirmed", null,
                    "Only confirmed orders can be closed short");
        }
        if (o.isLocked()) {
            throw new SalesDomainException(
                    "error.sales.orderLocked", null, "Sales order is locked; unlock before amending");
        }
        if (hasDraftInvoiceDocuments(o.getId())) {
            throw new SalesDomainException(
                    "error.sales.draftInvoiceBlocksAmendment", null,
                    "A draft invoice or credit note exists for this order; post or delete it before changing lines");
        }
        CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(o.getId());
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        Map<UUID, Boolean> giftBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        Instant now = Instant.now();
        for (SalesOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            giftBefore.put(line.getId(), line.isGift());
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + line.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                continue;
            }
            BigDecimal delivered = nz(line.getQtyDelivered());
            if (fulfilledInvoiced(postedNets, line).compareTo(delivered) > 0) {
                throw new SalesDomainException("error.sales.closeRemainingInvoicedUndelivered", null,
                        "Some quantity is invoiced but not delivered; create a credit note for it first");
            }
            if (nz(line.getQtyOrdered()).compareTo(delivered) != 0) {
                line.setQtyOrdered(delivered);
                line.setUpdatedAt(now);
            }
        }
        cancelOpenPickings(o.getId());
        o.setUpdatedAt(now);
        recalcTotals(o);
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        postSalesAmendmentTracking(saved, qtyOrderedBefore, giftBefore, untaxedBefore);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Remaining quantities closed" + (reason != null && !reason.isBlank() ? ": " + reason.trim() : ""));
        return getSalesOrder(saved.getId());
    }

    private static boolean sameQuantities(Map<UUID, BigDecimal> a, Map<UUID, BigDecimal> b) {
        java.util.Set<UUID> keys = new java.util.HashSet<>(a.keySet());
        keys.addAll(b.keySet());
        for (UUID k : keys) {
            if (nz(a.get(k)).compareTo(nz(b.get(k))) != 0) {
                return false;
            }
        }
        return true;
    }

    private void createDeliveryPickingForRemaining(SalesOrder o) {
        createDeliveryPicking(o, null);
    }

    /** One delivery for the remaining quantities, or for {@code stockQtyByLine} (stock units) when given. */
    private void createDeliveryPicking(SalesOrder o, Map<UUID, BigDecimal> stockQtyByLine) {
        List<StockMoveCommand> moves = new ArrayList<>();
        for (StockMoveCommand mc : buildRemainingOutgoingMoves(o)) {
            BigDecimal qty = stockQtyByLine == null ? mc.getDemandQuantity()
                    : nz(stockQtyByLine.get(mc.getSalesOrderLineId())).min(mc.getDemandQuantity());
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
                .orElseThrow(() -> new SalesDomainException("Warehouse not found: " + warehouseId));
        StockLocation customerLoc = findCustomerVirtual(o.getCompanyId());
        UUID sourceLoc = wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (sourceLoc == null) {
            throw new SalesDomainException("error.sales.warehouseStockLocationUnresolved", null, "Warehouse stock location could not be resolved");
        }
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
        if (o.getOrderDate() != null) {
            cmd.setScheduledAt(o.getOrderDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        }
        cmd.setMoves(moves);
        stockPickingApplicationService.createPicking(cmd);
    }

    private List<StockMoveCommand> buildRemainingOutgoingMoves(SalesOrder o) {
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
            moves.add(buildSalesStockMove(line, product, remaining));
        }
        return moves;
    }

    /** Order-line quantity (order unit or packaging) expressed in the product's stock unit. */
    private BigDecimal toStockUomQty(SalesOrderLine line, Product product, BigDecimal qtyInOrderUom) {
        if (line.getQtyPerPackage() != null && line.getQtyPerPackage().signum() > 0) {
            return ProductPackaging.toBaseQty(qtyInOrderUom, line.getQtyPerPackage());
        }
        return uomApplicationService.convert(line.getUomId(), product.getUomId().getId(), qtyInOrderUom);
    }

    private StockMoveCommand buildSalesStockMove(SalesOrderLine line, Product product, BigDecimal qtyInOrderUom) {
        UUID stockUom = product.getUomId().getId();
        BigDecimal demandStockUom = toStockUomQty(line, product, qtyInOrderUom);
        // Inventory valuation must use product cost — never the sales unit price.
        BigDecimal unitCost = product.getStandardCost() != null
                ? product.getStandardCost().getAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        StockMoveCommand mc = new StockMoveCommand();
        mc.setProductId(line.getProductId());
        mc.setUomId(stockUom);
        mc.setDemandQuantity(demandStockUom);
        if (unitCost.signum() > 0) {
            mc.setUnitCost(unitCost);
        }
        mc.setSalesOrderLineId(line.getId());
        return mc;
    }

    /**
     * After confirm: draft delivery pickings already exist. Delivery validation and
     * customer invoice creation are separate user actions.
     */
    private SalesOrderResponse finalizeAfterConfirm(SalesOrder saved) {
        return getSalesOrder(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrderSummaryResponse> searchSalesOrders(UUID companyId,
                                                           SalesOrderState state,
                                                           UUID customerPartnerId,
                                                           String q,
                                                           Pageable pageable) {
        return searchSalesOrders(companyId, state, customerPartnerId, q, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrderSummaryResponse> searchSalesOrders(UUID companyId,
                                                           SalesOrderState state,
                                                           UUID customerPartnerId,
                                                           String q,
                                                           LocalDate orderDateFrom,
                                                           LocalDate orderDateTo,
                                                           Pageable pageable) {
        UUID cid = companyIdOrDefault(companyId);
        String qNorm = q != null && !q.isBlank() ? q.trim() : "";
        var page = salesOrderRepository.search(cid, state, customerPartnerId, qNorm,
                orderDateFrom, orderDateTo, pageable);
        List<UUID> orderIds = page.getContent().stream().map(SalesOrder::getId).toList();
        Map<UUID, List<CustomerInvoiceResponse>> docsByOrder =
                customerInvoiceApplicationService.listPostedDocumentsForSalesOrders(orderIds);
        List<UUID> allInvoiceIds = docsByOrder.values().stream()
                .flatMap(List::stream)
                .map(CustomerInvoiceResponse::getId)
                .toList();
        Map<UUID, java.math.BigDecimal> settledByInvoice =
                customerInvoiceApplicationService.sumPostedPaymentsByInvoiceIds(allInvoiceIds);
        return page.map(o -> toSummary(o, docsByOrder.getOrDefault(o.getId(), List.of()), settledByInvoice));
    }

    private SalesOrderSummaryResponse toSummary(SalesOrder o) {
        return toSummary(o, null, null);
    }

    private SalesOrderSummaryResponse toSummary(
            SalesOrder o,
            List<CustomerInvoiceResponse> postedDocs,
            Map<UUID, java.math.BigDecimal> settledByInvoice) {
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
        r.setCreatedAt(o.getCreatedAt());
        r.setConfirmedAt(o.getConfirmedAt());
        if (postedDocs != null && settledByInvoice != null) {
            applySalesPaymentStatus(r, o, postedDocs, settledByInvoice);
        } else {
            applySalesPaymentStatus(r, o);
        }
        return r;
    }

    /**
     * List statuses: New / Unpaid / Partial Paid / Paid / To refund / Cancelled (+ amounts).
     * Total is net billed once posted invoices/credit notes exist; otherwise the order total.
     * Paid is net of refunds; amountDue (balance) may be negative when a refund is owed.
     */
    private record OrderPaymentFields(
            String paymentStatus, BigDecimal amountTotal, BigDecimal amountPaid, BigDecimal amountDue) {}

    private void applySalesPaymentStatus(SalesOrderSummaryResponse r, SalesOrder o) {
        List<CustomerInvoiceResponse> posted =
                customerInvoiceApplicationService.listPostedDocumentsForSalesOrder(o.getId());
        List<UUID> ids = posted.stream().map(CustomerInvoiceResponse::getId).toList();
        Map<UUID, BigDecimal> settledByInvoice =
                customerInvoiceApplicationService.sumPostedPaymentsByInvoiceIds(ids);
        applySalesPaymentStatus(r, o, posted, settledByInvoice);
    }

    private void applySalesPaymentStatus(
            SalesOrderSummaryResponse r,
            SalesOrder o,
            List<CustomerInvoiceResponse> posted,
            Map<UUID, BigDecimal> settledByInvoice) {
        OrderPaymentFields f = computeSalesPaymentFields(o, posted, settledByInvoice);
        r.setPaymentStatus(f.paymentStatus());
        r.setAmountTotal(f.amountTotal());
        r.setAmountPaid(f.amountPaid());
        r.setAmountDue(f.amountDue());
    }

    private OrderPaymentFields computeSalesPaymentFields(SalesOrder o) {
        List<CustomerInvoiceResponse> posted =
                customerInvoiceApplicationService.listPostedDocumentsForSalesOrder(o.getId());
        List<UUID> ids = posted.stream().map(CustomerInvoiceResponse::getId).toList();
        Map<UUID, BigDecimal> settledByInvoice =
                customerInvoiceApplicationService.sumPostedPaymentsByInvoiceIds(ids);
        return computeSalesPaymentFields(o, posted, settledByInvoice);
    }

    private OrderPaymentFields computeSalesPaymentFields(
            SalesOrder o,
            List<CustomerInvoiceResponse> posted,
            Map<UUID, BigDecimal> settledByInvoice) {
        boolean cancelled = o.getState() == SalesOrderState.CANCELLED;
        List<SettlementDoc> docs = new ArrayList<>();
        if (!cancelled) {
            for (CustomerInvoiceResponse inv : posted) {
                BigDecimal total = customerInvoiceTotal(inv);
                BigDecimal settled = settledByInvoice.getOrDefault(inv.getId(), BigDecimal.ZERO);
                boolean creditNote = inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE;
                SettlementDoc.Kind kind = creditNote
                        ? SettlementDoc.Kind.CREDIT_NOTE
                        : SettlementDoc.Kind.INVOICE;
                docs.add(new SettlementDoc(kind, total, settled, inv.getDueDate()));
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

    private static BigDecimal customerInvoiceTotal(CustomerInvoiceResponse inv) {
        BigDecimal total = BigDecimal.ZERO;
        for (var line : inv.getLines()) {
            if (line.isGift()) {
                continue;
            }
            BigDecimal discountValue = line.getDiscountValue() != null
                    ? line.getDiscountValue()
                    : (line.getDiscountPercent() != null ? line.getDiscountPercent() : BigDecimal.ZERO);
            BigDecimal net = DiscountMath.lineNet(
                    line.getQty(),
                    line.getUnitPrice(),
                    line.getDiscountType() != null ? line.getDiscountType() : DiscountType.PERCENT,
                    discountValue);
            total = total.add(net);
            for (var tax : line.getTaxSnapshots()) {
                total = total.add(tax.getTaxAmount() != null
                        ? tax.getTaxAmount().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            }
        }
        BigDecimal orderDisc = inv.getOrderDiscountAmount() != null
                ? inv.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        return total.subtract(orderDisc).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean computeCanCreateCustomerInvoice(SalesOrder o) {
        if (o.getState() != SalesOrderState.CONFIRMED) {
            return false;
        }
        boolean allowWithoutDelivery = companyDocumentPolicyService.allowInvoiceWithoutDelivery(o.getCompanyId());
        Map<UUID, BigDecimal> draftAllocated =
                customerInvoiceApplicationService.draftAllocatedQtyBySalesOrderLine(o.getId());
        Map<UUID, BigDecimal> draftChargeAllocated =
                customerInvoiceApplicationService.draftAllocatedChargeQtyBySalesOrderLine(o.getId());
        CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(o.getId());
        for (SalesOrderLine sol : o.getLines()) {
            Product product = productRepository.findById(new ProductId(sol.getProductId())).orElse(null);
            if (product == null) {
                continue;
            }
            if (invoiceableQtyForLine(sol, product, draftAllocated, draftChargeAllocated, postedNets, allowWithoutDelivery)
                    .signum() > 0) {
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
        Map<UUID, BigDecimal> draftChargeCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedChargeQtyBySalesOrderLine(o.getId());
        CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(o.getId());
        for (SalesOrderLine sol : o.getLines()) {
            if (creditNoteableQtyForLine(sol, draftCn, draftChargeCn, postedNets).signum() > 0) {
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
                                             Map<UUID, BigDecimal> draftAllocated,
                                             Map<UUID, BigDecimal> draftChargeAllocated,
                                             CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets,
                                             boolean allowWithoutDelivery) {
        SalInvoicePolicy pol = effectiveInvoicePolicy(sol, product, allowWithoutDelivery);
        BigDecimal targetQty = pol == SalInvoicePolicy.ORDERED
                ? sol.getQtyOrdered() : sol.getQtyDelivered();
        if (!sol.isGift()) {
            // Gift invoices do not satisfy commercial billing — residual uses charge net only.
            BigDecimal charged = postedNets.chargeNetFor(sol.getId());
            BigDecimal draftCharge = draftChargeAllocated.getOrDefault(sol.getId(), BigDecimal.ZERO);
            return targetQty.subtract(charged).subtract(draftCharge).max(BigDecimal.ZERO)
                    .setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal invoiced = effectiveQtyInvoiced(sol, draftAllocated);
        return targetQty.subtract(invoiced).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /** TSH-06: only service products can be billed from timesheets. */
    private static void requireServiceForTimesheetPolicy(SalesOrderLine line, Product product) {
        if (line.getInvoicePolicy() == SalInvoicePolicy.TIMESHEET && product.getProductType() != ProductType.SERVICE) {
            throw new SalesDomainException("error.sales.timesheetPolicyServiceOnly", null,
                    "Only service products can be invoiced from timesheets");
        }
    }

    private static SalInvoicePolicy effectiveInvoicePolicy(SalesOrderLine sol,
                                                           Product product,
                                                           boolean allowWithoutDelivery) {
        if (sol.getInvoicePolicy() == SalInvoicePolicy.TIMESHEET) {
            return SalInvoicePolicy.TIMESHEET;
        }
        if (allowWithoutDelivery) {
            return SalInvoicePolicy.ORDERED;
        }
        if (sol.getInvoicePolicy() != null) {
            return sol.getInvoicePolicy();
        }
        return product != null && product.getProductType() == ProductType.SERVICE
                ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED;
    }

    /**
     * Credit-note residual:
     * <ul>
     *   <li>Return-based: fulfillment invoiced ({@code max(giftNet, chargeNet)}) − delivered/ordered − draft CN</li>
     *   <li>Commercial gift: when the SO line is gift, also {@code chargeNet} (sale→gift without return)</li>
     * </ul>
     * Using {@code max(gift, charge)} avoids treating gift→sale reclass (both nets present for the same units)
     * as over-invoiced for returns.
     */
    private BigDecimal creditNoteableQtyForLine(SalesOrderLine sol,
                                                Map<UUID, BigDecimal> draftCreditNotes,
                                                Map<UUID, BigDecimal> draftChargeCreditNotes,
                                                CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets) {
        BigDecimal draft = draftCreditNotes.getOrDefault(sol.getId(), BigDecimal.ZERO);
        BigDecimal giftNet = postedNets.giftNetFor(sol.getId());
        BigDecimal chargeNet = postedNets.chargeNetFor(sol.getId());
        BigDecimal fulfillmentInvoiced = giftNet.max(chargeNet);
        Optional<Product> product = productRepository.findById(new ProductId(sol.getProductId()));
        BigDecimal baseline;
        if (sol.getInvoicePolicy() == SalInvoicePolicy.TIMESHEET) {
            baseline = sol.getQtyDelivered();
        } else if (product.isPresent() && product.get().getProductType() == ProductType.SERVICE) {
            baseline = sol.getQtyOrdered();
        } else {
            baseline = sol.getQtyDelivered();
        }
        BigDecimal returnResidual = fulfillmentInvoiced.subtract(baseline).subtract(draft).max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
        if (!sol.isGift()) {
            return returnResidual;
        }
        BigDecimal draftChargeCn = draftChargeCreditNotes.getOrDefault(sol.getId(), BigDecimal.ZERO);
        BigDecimal commercialGift = chargeNet.subtract(draftChargeCn).max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
        return returnResidual.max(commercialGift);
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

    /** Normalizes the order-level discount input, accepting the legacy percent-only payload. */
    private static void applyOrderDiscountInput(SalesOrder o, CreateSalesOrderCommand command) {
        if (command.getOrderDiscountType() != null) {
            o.setOrderDiscountType(command.getOrderDiscountType());
        }
        if (command.getOrderDiscountValue() != null) {
            o.setOrderDiscountValue(command.getOrderDiscountValue());
        } else if (command.getOrderDiscountPercent() != null) {
            if (command.getOrderDiscountType() == null) {
                o.setOrderDiscountType(DiscountType.PERCENT);
            }
            o.setOrderDiscountValue(command.getOrderDiscountPercent());
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) == 0;
    }

    /** Commercial terms of a line (qty, price, discount, gift, taxes) used to detect edits. */
    private static String lineTermsSignature(SalesOrderLine line) {
        List<String> taxIds = new ArrayList<>();
        for (SalesOrderLineTax t : line.getTaxes()) {
            taxIds.add(String.valueOf(t.getTaxId()));
        }
        java.util.Collections.sort(taxIds);
        return nz(line.getQtyOrdered()).stripTrailingZeros().toPlainString()
                + "|" + nz(line.getUnitPrice()).stripTrailingZeros().toPlainString()
                + "|" + line.getDiscountType()
                + "|" + nz(line.getDiscountValue()).stripTrailingZeros().toPlainString()
                + "|" + line.isGift()
                + "|" + String.join(",", taxIds);
    }

    private static boolean orderDiscountChanged(SalesOrder o, CreateSalesOrderCommand command) {
        SalesOrder probe = new SalesOrder();
        probe.setOrderDiscountType(o.getOrderDiscountType());
        probe.setOrderDiscountValue(o.getOrderDiscountValue());
        applyOrderDiscountInput(probe, command);
        return probe.getOrderDiscountType() != o.getOrderDiscountType()
                || !sameAmount(probe.getOrderDiscountValue(), o.getOrderDiscountValue());
    }

    /**
     * Rules for editing a line of a confirmed order: quantity never below what was delivered or
     * invoiced, and price, discount and taxes are fixed once any quantity is invoiced.
     */
    private static void assertLineAmendmentAllowed(SalesOrderLine line, SalesOrderLineCommand lc, BigDecimal invoiced) {
        BigDecimal newQty = nz(lc.getQtyOrdered());
        BigDecimal delivered = nz(line.getQtyDelivered());
        if (newQty.compareTo(delivered) < 0) {
            throw new SalesDomainException(
                    "error.sales.qtyBelowDelivered",
                    new Object[] { MonetaryScale.toDisplayString(delivered) },
                    "Quantity cannot be below the delivered quantity (" + delivered.toPlainString()
                            + "); return the goods first");
        }
        if (newQty.compareTo(invoiced) < 0) {
            throw new SalesDomainException(
                    "error.sales.qtyBelowInvoiced",
                    new Object[] { MonetaryScale.toDisplayString(invoiced) },
                    "Quantity cannot be below the invoiced quantity (" + invoiced.toPlainString()
                            + "); create a credit note first");
        }
        if (invoiced.signum() <= 0) {
            return;
        }
        boolean priceChanged = lc.getUnitPrice() != null && !sameAmount(lc.getUnitPrice(), line.getUnitPrice());
        SalesOrderLine probe = new SalesOrderLine();
        probe.setDiscountType(line.getDiscountType());
        probe.setDiscountValue(line.getDiscountValue());
        applyLineDiscountInput(probe, lc);
        boolean giftToggle = Boolean.TRUE.equals(lc.getIsGift()) != line.isGift();
        boolean discountChanged = !giftToggle && !line.isGift()
                && (probe.getDiscountType() != line.getDiscountType()
                        || !sameAmount(probe.getDiscountValue(), line.getDiscountValue()));
        java.util.Set<UUID> taxesBefore = new java.util.HashSet<>();
        for (SalesOrderLineTax t : line.getTaxes()) {
            taxesBefore.add(t.getTaxId());
        }
        boolean taxesChanged = !taxesBefore.equals(new java.util.HashSet<>(lc.getTaxIds()));
        if (priceChanged || discountChanged || taxesChanged) {
            throw new SalesDomainException(
                    "error.sales.invoicedLineTermsLocked", null,
                    "Price, discount and taxes are locked once a line is invoiced; credit the invoice first");
        }
    }

    /**
     * Invoiced quantity that counts against the order line: gift and charge invoicing are tracked
     * separately (a line can be invoiced as a gift, then re-invoiced as a sale), so the line is
     * invoiced up to the larger of the two nets, not their sum.
     */
    private static BigDecimal fulfilledInvoiced(CustomerInvoiceApplicationService.PostedSoLineQtyNets nets,
                                                SalesOrderLine line) {
        return nets.chargeNetFor(line.getId()).max(nets.giftNetFor(line.getId())).max(BigDecimal.ZERO);
    }

    private boolean hasDraftInvoiceDocuments(UUID salesOrderId) {
        return !customerInvoiceApplicationService.draftAllocatedQtyBySalesOrderLine(salesOrderId).isEmpty()
                || !customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(salesOrderId).isEmpty();
    }

    private static void applyLineDiscountInput(SalesOrderLine line, SalesOrderLineCommand lc) {
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

    /** Order subtotal after line discounts but before the order-level discount. Gifts are free. */
    private static BigDecimal orderSubtotalBeforeOrderDiscount(SalesOrder o) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SalesOrderLine line : o.getLines()) {
            if (line.isGift()) {
                continue;
            }
            subtotal = subtotal.add(SalesOrderRules.lineNet(
                    line.getQtyOrdered(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue()));
        }
        return subtotal;
    }

    /**
     * Blocks unpaid orders that would push the customer past their credit limit.
     */
    private void requireWithinCreditLimitUnlessPaid(SalesOrder o) {
        if (o.getAmountTotal() == null || o.getAmountTotal().signum() <= 0) {
            return;
        }
        CreditStatusResponse credit = partnerApplicationService.creditStatus(o.getCustomerPartnerId());
        if (!credit.unlimited() && o.getAmountTotal().compareTo(credit.available()) > 0) {
            throw new SalesDomainException(
                    "error.sales.creditLimitExceeded", null, "Credit limit exceeded for this order total");
        }
    }

    private void recalcTotals(SalesOrder o) {
        BigDecimal untaxed = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (SalesOrderLine line : o.getLines()) {
            line.setDiscountPercent(DiscountMath.effectivePercent(
                    line.getQtyOrdered().multiply(line.getUnitPrice()),
                    line.getDiscountType(),
                    line.getDiscountValue()));
            if (line.isGift()) {
                continue;
            }
            List<FiscalTaxSnapshot> snaps = line.getTaxes().stream()
                    .map(t -> purchaseApplicationService.getFiscalTax(t.getTaxId()))
                    .map(this::toSnapshot)
                    .toList();
            PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                    line.getQtyOrdered(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue(), snaps);
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
        SalesOrder saved = salesOrderRepository.save(o);
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Quotation", "Quotation Sent", "Status");
        return toResponse(saved);
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
        requireWithinCreditLimitUnlessPaid(o);
        UUID warehouseId = o.getWarehouseId();
        if (warehouseId == null) {
            List<Warehouse> companyWarehouses = warehouseRepository.findByCompany(
                    new CompanyId(o.getCompanyId()), false);
            Warehouse main = companyWarehouses.stream()
                    .filter(w -> "WH".equalsIgnoreCase(w.getCode()))
                    .findFirst()
                    .orElse(companyWarehouses.stream().findFirst().orElse(null));
            if (main == null) {
                throw new SalesDomainException("error.sales.warehouseIdRequiredToConfirm", null, "warehouseId is required to confirm a sales order");
            }
            warehouseId = main.getId().getId();
            o.setWarehouseId(warehouseId);
        }
        final UUID resolvedWarehouseId = warehouseId;
        Warehouse wh = warehouseRepository.findById(new WarehouseId(resolvedWarehouseId))
                .orElseThrow(() -> new SalesDomainException("Warehouse not found: " + resolvedWarehouseId));
        if (!wh.getCompanyId().getId().equals(o.getCompanyId())) {
            throw new SalesDomainException("error.sales.warehouseCompanyMismatch", null, "Warehouse company mismatch");
        }
        StockLocation customerLoc = findCustomerVirtual(o.getCompanyId());
        UUID sourceLoc = wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (sourceLoc == null) {
            throw new SalesDomainException("error.sales.warehouseStockLocationUnresolved", null, "Warehouse stock location could not be resolved");
        }

        for (SalesOrderLine line : o.getLines()) {
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + line.getProductId()));
            if (!product.isSaleOk()) {
                throw new SalesDomainException(
                        "error.sales.productNotSalable",
                        new Object[] { line.getProductId() },
                        "Product is not salable: " + line.getProductId());
            }
        }

        List<StockMoveCommand> moves = buildRemainingOutgoingMoves(o);

        if (!moves.isEmpty()) {
            CreateStockPickingCommand cmd = new CreateStockPickingCommand();
            cmd.setCompanyId(o.getCompanyId());
            cmd.setWarehouseId(resolvedWarehouseId);
            cmd.setPickingType(PickingType.OUTGOING);
            cmd.setSourceLocationId(sourceLoc);
            cmd.setDestinationLocationId(customerLoc.getId().getId());
            cmd.setPartnerId(o.getCustomerPartnerId());
            cmd.setOrigin(o.getName());
            cmd.setReference(o.getName());
            cmd.setSalesOrderId(o.getId());
            if (o.getOrderDate() != null) {
                cmd.setScheduledAt(o.getOrderDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            }
            cmd.setMoves(moves);
            stockPickingApplicationService.createPicking(cmd);
        }

        o.setState(SalesOrderState.CONFIRMED);
        o.setConfirmedAt(Instant.now());
        o.setExchangeRateToCompany(resolveExchangeRate(
                o.getCompanyId(), o.getCurrencyCode(), o.getOrderDate(), o.getExchangeRateToCompany()));
        o.setUpdatedAt(Instant.now());
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesEventPublisher.publishSalesOrderConfirmed(new SalesOrderConfirmedEvent(
                UUID.randomUUID(),
                Instant.now(),
                saved.getCompanyId(),
                saved.getId()));
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Quotation", "Sales Order", "Status");
        return finalizeAfterConfirm(saved);
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
        if (o.isLocked()) {
            throw new SalesDomainException(
                    "error.sales.orderLocked", null, "Sales order is locked; unlock before cancelling");
        }
        if (o.getState() == SalesOrderState.CONFIRMED) {
            // Net quantities: fully returned goods and fully credited invoices no longer block.
            if (o.getLines().stream().anyMatch(l -> nz(l.getQtyDelivered()).signum() > 0)) {
                throw new SalesDomainException("error.sales.cannotCancelDelivered", null,
                        "Cannot cancel: goods have been delivered; return them first");
            }
            if (o.getLines().stream().anyMatch(l -> nz(l.getQtyInvoiced()).signum() > 0)) {
                throw new SalesDomainException("error.sales.cannotCancelPostedInvoices", null, "Cannot cancel: posted customer invoices exist for this order");
            }
            customerInvoiceApplicationService.deleteDraftDocumentsForSalesOrder(o.getId());
            cancelOpenPickings(o.getId());
        }
        o.setState(SalesOrderState.CANCELLED);
        o.setCancelledAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        SalesOrder saved = salesOrderRepository.save(o);
        StockHoldApplicationService holds = stockHoldProvider.getIfAvailable();
        if (holds != null) {
            holds.releaseOwner(new CompanyId(saved.getCompanyId()),
                    StockHoldOwnerType.SALES_ORDER, saved.getId());
        }
        activityLogger.logFieldChange(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Sales Order", "Cancelled", "Status");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalesOrderResponse lockSalesOrder(UUID id) {
        SalesOrder o = loadOrder(id);
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException(
                    "error.sales.lockRequiresConfirmed", null, "Only confirmed sales orders can be locked");
        }
        if (o.isLocked()) {
            return toResponse(o);
        }
        o.setLocked(true);
        o.setUpdatedAt(Instant.now());
        SalesOrder saved = salesOrderRepository.save(o);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Sales order locked");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalesOrderResponse unlockSalesOrder(UUID id) {
        SalesOrder o = loadOrder(id);
        if (!o.isLocked()) {
            return toResponse(o);
        }
        o.setLocked(false);
        o.setUpdatedAt(Instant.now());
        SalesOrder saved = salesOrderRepository.save(o);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Sales order unlocked");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StockPickingResponse validateDeliveryPicking(UUID pickingId, ValidatePickingCommand command) {
        return stockPickingApplicationService.validatePicking(pickingId,
                command != null ? command : new ValidatePickingCommand());
    }

    @Override
    public void syncSalesOrderLineQtyDeliveredFromStockMoves(UUID salesOrderId) {
        syncSalesOrderLineQtyDeliveredFromStockMoves(salesOrderId, null);
    }

    @Override
    public void syncSalesOrderLineQtyDeliveredFromStockMoves(UUID salesOrderId, UUID pickingId) {
        Map<UUID, BigDecimal> deliveredBefore = new LinkedHashMap<>();
        salesOrderRepository.findByIdWithLines(salesOrderId).ifPresent(before -> before.getLines()
                .forEach(l -> deliveredBefore.put(l.getId(), nz(l.getQtyDelivered()))));
        SalesOrder o = salesOrderQtyWriter.updateQtyDelivered(salesOrderId);
        if (o == null) {
            return;
        }
        if (pickingId != null) {
            // Runs inside the picking validation transaction: refusing here rolls the whole
            // validation back, so goods can never be delivered beyond the ordered quantity
            // (e.g. a return of a return validated twice).
            for (SalesOrderLine line : o.getLines()) {
                if (nz(line.getQtyDelivered()).compareTo(nz(line.getQtyOrdered())) > 0) {
                    throw new SalesDomainException("error.sales.deliveredExceedsOrdered",
                            new Object[] { line.getName() },
                            "Cannot deliver more than ordered for '" + line.getName() + "'");
                }
            }
        }
        reconcileCogsClearing(o.getId());
        if (pickingId == null) {
            return;
        }
        StockPickingResponse picking = stockPickingApplicationService.getPicking(pickingId);
        if (picking.getPickingType() != PickingType.INCOMING) {
            syncOpenDeliveries(loadOrder(o.getId()), true);
            return;
        }
        Map<UUID, BigDecimal> returned = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal delta = nz(deliveredBefore.get(line.getId())).subtract(nz(line.getQtyDelivered()));
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
            applyExchangeReturn(o.getId());
        }
    }

    /**
     * Goods came back and the customer is refunded: they are no longer ordered, and whatever of
     * them was invoiced is credited and posted now, in the same transaction as the return.
     */
    private void applyRefundReturn(UUID salesOrderId, Map<UUID, BigDecimal> returned) {
        SalesOrder o = loadOrder(salesOrderId);
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        Map<UUID, Boolean> giftBefore = new LinkedHashMap<>();
        BigDecimal untaxedBefore = nz(o.getAmountUntaxed());
        Instant now = Instant.now();
        for (SalesOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), nz(line.getQtyOrdered()));
            giftBefore.put(line.getId(), line.isGift());
            BigDecimal back = returned.get(line.getId());
            if (back == null) {
                continue;
            }
            BigDecimal next = nz(line.getQtyOrdered()).subtract(back).max(nz(line.getQtyDelivered()));
            line.setQtyOrdered(next);
            line.setUpdatedAt(now);
        }
        o.setUpdatedAt(now);
        recalcTotals(o);
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        postSalesAmendmentTracking(saved, qtyOrderedBefore, giftBefore, untaxedBefore);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                "Returned goods refunded: ordered quantity reduced");
        creditReturnedGoods(loadOrder(salesOrderId));
        reconcileCogsClearing(salesOrderId);
    }

    /** Goods came back to be replaced: the order is unchanged and a delivery is created for them. */
    private void applyExchangeReturn(UUID salesOrderId) {
        SalesOrder o = loadOrder(salesOrderId);
        syncOpenDeliveries(o);
        o = loadOrder(salesOrderId);
        refreshOrderStatuses(o);
        salesOrderRepository.save(o);
        activityLogger.log(o.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, o.getId(),
                "Returned goods to be replaced: re-delivery created");
    }

    /**
     * Credits, and posts, every invoiced quantity that is no longer delivered. The credit is spread
     * over the order's posted invoices newest first, each line at its original invoice price,
     * discount and tax, never beyond what an invoice line still has uncredited.
     */
    private void creditReturnedGoods(SalesOrder o) {
        Map<UUID, BigDecimal> draftCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(o.getId());
        Map<UUID, BigDecimal> draftChargeCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedChargeQtyBySalesOrderLine(o.getId());
        CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(o.getId());
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        for (SalesOrderLine sol : o.getLines()) {
            BigDecimal qty = creditNoteableQtyForLine(sol, draftCn, draftChargeCn, postedNets);
            if (qty.signum() > 0) {
                toCredit.put(sol.getId(), qty);
            }
        }
        creditQuantities(o, toCredit, true);
    }

    /**
     * Creates and posts credit notes for the given quantity per order line, spread over the order's
     * posted invoices newest first, at each invoice line's original price, discount and tax and never
     * beyond what an invoice line still has uncredited. Charge lines are credited before gift lines;
     * with {@code includeGift} false only charge lines are credited. Returns the posted credit notes.
     */
    private List<CustomerInvoiceResponse> creditQuantities(SalesOrder o, Map<UUID, BigDecimal> quantities,
                                                           boolean includeGift) {
        List<CustomerInvoiceResponse> posted = new ArrayList<>();
        Map<UUID, BigDecimal> toCredit = new LinkedHashMap<>();
        quantities.forEach((k, v) -> {
            if (nz(v).signum() > 0) {
                toCredit.put(k, v);
            }
        });
        if (toCredit.isEmpty()) {
            return posted;
        }
        List<CustomerInvoiceResponse> invoices =
                customerInvoiceApplicationService.listPostedInvoicesForSalesOrder(o.getId()).stream()
                        .sorted(Comparator.comparing(CustomerInvoiceResponse::getInvoiceDate,
                                        Comparator.nullsFirst(Comparator.naturalOrder()))
                                .thenComparing(CustomerInvoiceResponse::getId).reversed())
                        .toList();
        for (CustomerInvoiceResponse inv : invoices) {
            Map<UUID, BigDecimal> remaining =
                    customerInvoiceApplicationService.remainingCreditableQtyByInvoiceLine(inv.getId());
            List<CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand> lines = new ArrayList<>();
            for (Map.Entry<UUID, BigDecimal> e : toCredit.entrySet()) {
                for (boolean gift : includeGift ? new boolean[] { false, true } : new boolean[] { false }) {
                    for (var invLine : inv.getLines()) {
                        if (e.getValue().signum() <= 0) {
                            break;
                        }
                        if (!e.getKey().equals(invLine.getSalesOrderLineId()) || invLine.isGift() != gift) {
                            continue;
                        }
                        BigDecimal take = e.getValue().min(nz(remaining.get(invLine.getId())));
                        if (take.signum() <= 0) {
                            continue;
                        }
                        CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand lc =
                                new CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand();
                        lc.setInvoiceLineId(invLine.getId());
                        lc.setQty(take);
                        lines.add(lc);
                        remaining.put(invLine.getId(), nz(remaining.get(invLine.getId())).subtract(take));
                        e.setValue(e.getValue().subtract(take));
                    }
                }
            }
            if (lines.isEmpty()) {
                continue;
            }
            CreateCreditNoteFromInvoiceCommand cmd = new CreateCreditNoteFromInvoiceCommand();
            cmd.setCompanyId(o.getCompanyId());
            cmd.setInvoiceDate(LocalDate.now());
            cmd.setDueDate(LocalDate.now());
            cmd.setReference(o.getName() != null ? "CN/" + o.getName() : null);
            cmd.setLines(lines);
            CustomerInvoiceResponse cn = customerInvoiceApplicationService.createCreditNoteFromInvoice(inv.getId(), cmd);
            posted.add(customerInvoiceApplicationService.postCustomerInvoice(cn.getId()));
        }
        if (toCredit.values().stream().anyMatch(q -> q.signum() > 0)) {
            throw new SalesDomainException("error.sales.cannotCreditReturn", null,
                    "The returned goods could not be fully credited against this order's invoices");
        }
        return posted;
    }

    @Override
    public void refreshSalesOrderQtyDeliveredInCurrentTransaction(UUID salesOrderId) {
        salesOrderQtyWriter.updateQtyDeliveredJoiningCurrentTransaction(salesOrderId);
        reconcileCogsClearing(salesOrderId);
    }

    @Override
    public void afterOutgoingPickingValidated(UUID salesOrderId, UUID pickingId) {
        syncSalesOrderLineQtyDeliveredFromStockMoves(salesOrderId, pickingId);
    }

    @Override
    public void applyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId) {
        salesOrderQtyWriter.applyPostedInvoiceQuantities(salesOrderId, invoicedQtyBySalesLineId);
        reconcileCogsClearing(salesOrderId);
    }

    @Override
    public void assertDocumentFitsOrder(UUID salesOrderId, boolean creditNote,
                                        List<SalesOrderInvoiceSyncPort.DocumentLine> lines) {
        SalesOrder o = loadOrder(salesOrderId);
        Map<UUID, SalesOrderLine> byId = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            byId.put(line.getId(), line);
        }
        Map<UUID, BigDecimal> chargeQty = new LinkedHashMap<>();
        Map<UUID, BigDecimal> giftQty = new LinkedHashMap<>();
        for (SalesOrderInvoiceSyncPort.DocumentLine dl : lines) {
            if (dl.salesOrderLineId() == null) {
                continue;
            }
            SalesOrderLine sol = byId.get(dl.salesOrderLineId());
            if (sol == null) {
                throw new SalesDomainException("error.sales.documentLineNotOnOrder", null,
                        "The document has a line that no longer exists on the sales order");
            }
            (dl.gift() ? giftQty : chargeQty).merge(sol.getId(), nz(dl.qty()), BigDecimal::add);
            if (!creditNote && !dl.gift() && !sol.isGift() && !sameAmount(dl.unitPrice(), sol.getUnitPrice())) {
                throw new SalesDomainException("error.sales.invoicePriceDiffersFromOrder",
                        new Object[] { sol.getName() },
                        "The invoice price for '" + sol.getName() + "' differs from the sales order");
            }
        }
        CustomerInvoiceApplicationService.PostedSoLineQtyNets nets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(salesOrderId);
        assertQtyFits(byId, chargeQty, nets.chargeNet(), creditNote);
        assertQtyFits(byId, giftQty, nets.giftNet(), creditNote);
    }

    /** Gift and charge quantities are checked separately, each against its own posted net. */
    private static void assertQtyFits(Map<UUID, SalesOrderLine> byId, Map<UUID, BigDecimal> docQty,
                                      Map<UUID, BigDecimal> postedNet, boolean creditNote) {
        for (Map.Entry<UUID, BigDecimal> e : docQty.entrySet()) {
            SalesOrderLine sol = byId.get(e.getKey());
            BigDecimal posted = nz(postedNet.get(e.getKey())).max(BigDecimal.ZERO);
            if (creditNote) {
                if (e.getValue().compareTo(posted) > 0) {
                    throw new SalesDomainException("error.sales.creditExceedsInvoiced",
                            new Object[] { sol.getName() },
                            "Cannot credit more than was invoiced for '" + sol.getName() + "'");
                }
            } else if (posted.add(e.getValue()).compareTo(nz(sol.getQtyOrdered())) > 0) {
                throw new SalesDomainException("error.sales.invoiceExceedsOrdered",
                        new Object[] { sol.getName() },
                        "Cannot invoice more than ordered for '" + sol.getName() + "'");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderState> findOrder(UUID salesOrderId) {
        if (salesOrderId == null) {
            return Optional.empty();
        }
        return salesOrderRepository.findByIdWithLines(salesOrderId).map(o -> {
            List<LineState> lines = new ArrayList<>();
            for (SalesOrderLine l : o.getLines()) {
                lines.add(new LineState(
                        l.getId(),
                        l.getProductId(),
                        l.getName(),
                        l.getQtyDelivered() != null ? l.getQtyDelivered() : BigDecimal.ZERO,
                        l.getQtyInvoiced() != null ? l.getQtyInvoiced() : BigDecimal.ZERO,
                        l.getQtyCogsCleared() != null ? l.getQtyCogsCleared() : BigDecimal.ZERO));
            }
            return new OrderState(o.getCompanyId(), o.getOrderDate(), lines);
        });
    }

    @Override
    @Transactional
    public void updateQtyCogsCleared(UUID salesOrderId, Map<UUID, BigDecimal> qtyCogsClearedByLineId) {
        if (salesOrderId == null || qtyCogsClearedByLineId == null || qtyCogsClearedByLineId.isEmpty()) {
            return;
        }
        SalesOrder o = salesOrderRepository.findByIdForUpdate(salesOrderId).orElse(null);
        if (o == null) {
            return;
        }
        Instant now = Instant.now();
        boolean changed = false;
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal next = qtyCogsClearedByLineId.get(line.getId());
            if (next == null) {
                continue;
            }
            BigDecimal scaled = next.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            BigDecimal current = line.getQtyCogsCleared() != null ? line.getQtyCogsCleared() : BigDecimal.ZERO;
            if (current.compareTo(scaled) != 0) {
                line.setQtyCogsCleared(scaled);
                line.setUpdatedAt(now);
                changed = true;
            }
        }
        if (changed) {
            o.setUpdatedAt(now);
            salesOrderRepository.save(o);
        }
    }

    private void reconcileCogsClearing(UUID salesOrderId) {
        SalesCogsClearingPort port = salesCogsClearingPortProvider.getIfAvailable();
        if (port != null && salesOrderId != null) {
            port.reconcileForSalesOrder(salesOrderId);
        }
    }

    private boolean computeCanCreateReturn(SalesOrder o) {
        if (o.getState() != SalesOrderState.CONFIRMED) {
            return false;
        }
        return o.getLines().stream().anyMatch(sol -> {
            Optional<Product> opt = productRepository.findById(new ProductId(sol.getProductId()));
            if (opt.isEmpty() || opt.get().getProductType() == ProductType.SERVICE) {
                return false;
            }
            return sol.getQtyDelivered().signum() > 0;
        }) && stockMoveSalesQueryPort.findReturnableDeliveryPickingId(o.getId()).isPresent();
    }

    @Override
    @Transactional
    public StockPickingResponse createReturnFromSalesOrder(UUID salesOrderId) {
        return createReturnFromSalesOrder(salesOrderId, new CreateSalesReturnCommand());
    }

    @Override
    @Transactional
    public StockPickingResponse createReturnFromSalesOrder(UUID salesOrderId, CreateSalesReturnCommand command) {
        SalesOrder o = loadOrder(salesOrderId);
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException(
                    "error.sales.returnRequiresConfirmed", null,
                    "Sales order must be confirmed to create a return");
        }
        if (!computeCanCreateReturn(o)) {
            throw new SalesDomainException(
                    "error.sales.nothingToReturn", null,
                    "No delivered stock available to return");
        }
        UUID deliveryId = stockMoveSalesQueryPort.findReturnableDeliveryPickingId(o.getId())
                .orElseThrow(() -> new SalesDomainException(
                        "error.sales.nothingToReturn", null,
                        "No delivered stock available to return"));
        CreateSalesReturnCommand cmd = command != null ? command : new CreateSalesReturnCommand();
        ReturnPickingCommand returnCmd = new ReturnPickingCommand();
        returnCmd.setMoveQuantities(cmd.getMoveQuantities());
        returnCmd.setToRefund(cmd.isToRefund());
        return stockPickingApplicationService.returnPicking(deliveryId, returnCmd);
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
        boolean allowWithoutDelivery = companyDocumentPolicyService.allowInvoiceWithoutDelivery(o.getCompanyId());
        for (SalesOrderLine l : o.getLines()) {
            Product p = productRepository.findById(new ProductId(l.getProductId())).orElse(null);
            SalInvoicePolicy pol = effectiveInvoicePolicy(l, p, allowWithoutDelivery);
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
            throw new SalesDomainException("error.sales.orderCompanyMismatch", null, "Sales order company mismatch");
        }
        if (o.getState() == SalesOrderState.CANCELLED) {
            throw new SalesDomainException("error.sales.cannotInvoiceCancelledOrder", null, "Cannot invoice a cancelled sales order");
        }
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException("error.sales.orderMustBeConfirmedBeforeInvoicing", null, "Sales order must be confirmed before invoicing");
        }
        Map<UUID, BigDecimal> draftAllocated =
                customerInvoiceApplicationService.draftAllocatedQtyBySalesOrderLine(o.getId());
        Map<UUID, BigDecimal> draftChargeAllocated =
                customerInvoiceApplicationService.draftAllocatedChargeQtyBySalesOrderLine(o.getId());
        CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(o.getId());
        boolean allowWithoutDelivery = companyDocumentPolicyService.allowInvoiceWithoutDelivery(o.getCompanyId());
        BigDecimal orderSubtotal = orderSubtotalBeforeOrderDiscount(o);
        BigDecimal orderDiscountAmount = DiscountMath.discountAmount(
                orderSubtotal, o.getOrderDiscountType(), o.getOrderDiscountValue());
        List<CustomerInvoiceLineCommand> invLines = new ArrayList<>();
        BigDecimal invoicedSubtotal = BigDecimal.ZERO;
        for (SalesOrderLine sol : o.getLines()) {
            Product product = productRepository.findById(new ProductId(sol.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + sol.getProductId()));
            BigDecimal qty = invoiceableQtyForLine(
                    sol, product, draftAllocated, draftChargeAllocated, postedNets, allowWithoutDelivery);
            if (command.getLineQuantities() != null) {
                BigDecimal wanted = nz(command.getLineQuantities().get(sol.getId()));
                if (wanted.signum() <= 0) {
                    continue;
                }
                if (wanted.compareTo(qty) > 0) {
                    throw new SalesDomainException("error.sales.invoiceExceedsOrdered",
                            new Object[] { sol.getName() },
                            "Cannot invoice more than ordered for '" + sol.getName() + "'");
                }
                qty = wanted;
            }
            if (qty.signum() <= 0) {
                continue;
            }
            // Carry only the sales-order LINE discount. Order-level discount stays on the invoice header.
            // Gift lines keep unit price for Gift Expense posting but never affect AR / order discount.
            BigDecimal unitPrice = sol.getUnitPrice();
            if (sol.isGift() && (unitPrice == null || unitPrice.signum() <= 0)) {
                unitPrice = product.getListPrice() != null ? product.getListPrice().getAmount() : BigDecimal.ZERO;
            }
            if (unitPrice == null || unitPrice.signum() <= 0) {
                continue;
            }
            BigDecimal invoicedGross = qty.multiply(unitPrice);
            BigDecimal orderedGross = sol.getQtyOrdered().multiply(unitPrice);
            BigDecimal lineDiscAmount = BigDecimal.ZERO;
            if (!sol.isGift()) {
                BigDecimal orderedNet = SalesOrderRules.lineNet(
                        sol.getQtyOrdered(), unitPrice, sol.getDiscountType(), sol.getDiscountValue());
                BigDecimal lineDiscOnOrder = orderedGross.subtract(orderedNet).max(BigDecimal.ZERO);
                lineDiscAmount = orderedGross.signum() > 0
                        ? lineDiscOnOrder.multiply(invoicedGross).divide(orderedGross, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            }
            CustomerInvoiceLineCommand lc = new CustomerInvoiceLineCommand();
            lc.setName(sol.getName());
            lc.setQty(qty);
            lc.setUnitPrice(unitPrice);
            lc.setDiscountType(DiscountType.FIXED);
            lc.setDiscountValue(lineDiscAmount);
            lc.setDiscountPercent(DiscountMath.effectivePercent(invoicedGross, DiscountType.FIXED, lineDiscAmount));
            lc.setRevenueAccountId(sol.getRevenueAccountId());
            lc.setSalesOrderLineId(sol.getId());
            lc.setIsGift(sol.isGift());
            if (!sol.isGift()) {
                addInvoiceTaxSnapshots(lc, sol, qty, DiscountType.FIXED, lineDiscAmount);
                invoicedSubtotal = invoicedSubtotal.add(
                        SalesOrderRules.lineNet(qty, unitPrice, DiscountType.FIXED, lineDiscAmount)
                                .setScale(4, RoundingMode.HALF_UP));
            }
            invLines.add(lc);
        }
        if (invLines.isEmpty()) {
            throw new SalesDomainException(
                    "No invoiceable quantity: for storable lines, deliver goods first (qty delivered > qty "
                            + "invoiced); for service lines, invoice from ordered quantity. "
                            + "If a draft invoice already exists, post or remove it first.");
        }
        BigDecimal allocatedOrderDisc = BigDecimal.ZERO;
        if (orderSubtotal.signum() > 0 && orderDiscountAmount.signum() > 0 && invoicedSubtotal.signum() > 0) {
            allocatedOrderDisc = orderDiscountAmount.multiply(invoicedSubtotal)
                    .divide(orderSubtotal, 4, RoundingMode.HALF_UP)
                    .min(invoicedSubtotal);
        }
        // Scale tax snapshots so document tax matches the SO factor after order discount.
        if (invoicedSubtotal.signum() > 0 && allocatedOrderDisc.signum() > 0) {
            BigDecimal factor = invoicedSubtotal.subtract(allocatedOrderDisc)
                    .max(BigDecimal.ZERO)
                    .divide(invoicedSubtotal, 12, RoundingMode.HALF_UP);
            for (CustomerInvoiceLineCommand lc : invLines) {
                for (CustomerInvoiceLineTaxCommand ts : lc.getTaxSnapshots()) {
                    ts.setTaxAmount(ts.getTaxAmount().multiply(factor).setScale(4, RoundingMode.HALF_UP));
                    ts.setTaxBase(ts.getTaxBase().multiply(factor).setScale(4, RoundingMode.HALF_UP));
                }
            }
        }
        CreateCustomerInvoiceCommand ic = new CreateCustomerInvoiceCommand();
        ic.setCompanyId(companyId);
        ic.setCustomerPartnerId(o.getCustomerPartnerId());
        ic.setInvoiceDate(command.getInvoiceDate());
        ic.setDueDate(command.getDueDate());
        ic.setCurrencyCode(o.getCurrencyCode());
        ic.setReference(command.getReference() != null && !command.getReference().isBlank()
                ? command.getReference()
                : documentSequenceService.next(companyId, "INV"));
        ic.setSalesOrderId(o.getId());
        ic.setExchangeRateToCompany(o.getExchangeRateToCompany());
        ic.setOrderDiscountAmount(allocatedOrderDisc);
        ic.setLines(invLines);
        return customerInvoiceApplicationService.createCustomerInvoice(ic);
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCustomerCreditNoteFromSalesOrder(CreateCustomerInvoiceFromSalesOrderCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        SalesOrder o = loadOrder(command.getSalesOrderId());
        if (!o.getCompanyId().equals(companyId)) {
            throw new SalesDomainException("error.sales.orderCompanyMismatch", null, "Sales order company mismatch");
        }
        if (o.getState() == SalesOrderState.CANCELLED) {
            throw new SalesDomainException("error.sales.cannotCreditCancelledOrder", null, "Cannot credit a cancelled sales order");
        }
        if (o.getState() != SalesOrderState.CONFIRMED) {
            throw new SalesDomainException("error.sales.orderMustBeConfirmedBeforeCreditNote", null, "Sales order must be confirmed before creating a credit note");
        }

        List<CustomerInvoiceResponse> postedInvoices =
                customerInvoiceApplicationService.listPostedInvoicesForSalesOrder(o.getId());
        if (postedInvoices.isEmpty()) {
            throw new SalesDomainException(
                    "No posted invoice on this order; open the customer invoice and create a credit note from there");
        }

        UUID resolvedSourceInvoiceId = command.getSourceInvoiceId();
        Map<UUID, BigDecimal> draftCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(o.getId());
        Map<UUID, BigDecimal> draftChargeCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedChargeQtyBySalesOrderLine(o.getId());
        CustomerInvoiceApplicationService.PostedSoLineQtyNets postedNets =
                customerInvoiceApplicationService.postedQtyNetsBySalesOrderLine(o.getId());
        if (resolvedSourceInvoiceId == null) {
            if (postedInvoices.size() == 1) {
                resolvedSourceInvoiceId = postedInvoices.get(0).getId();
            } else {
                List<UUID> candidates = new ArrayList<>();
                for (CustomerInvoiceResponse inv : postedInvoices) {
                    boolean useful = false;
                    for (SalesOrderLine sol : o.getLines()) {
                        if (creditNoteableQtyForLine(sol, draftCn, draftChargeCn, postedNets).signum() <= 0) {
                            continue;
                        }
                        for (var invLine : inv.getLines()) {
                            if (sol.getId().equals(invLine.getSalesOrderLineId())) {
                                useful = true;
                                break;
                            }
                        }
                        if (useful) {
                            break;
                        }
                    }
                    if (useful) {
                        candidates.add(inv.getId());
                    }
                }
                if (candidates.size() == 1) {
                    resolvedSourceInvoiceId = candidates.get(0);
                } else {
                    throw new SalesDomainException(
                            "Multiple posted invoices on this order; open the source invoice and create the credit note there");
                }
            }
        } else {
            UUID sourceInvoiceId = resolvedSourceInvoiceId;
            boolean onOrder = postedInvoices.stream().anyMatch(inv -> inv.getId().equals(sourceInvoiceId));
            if (!onOrder) {
                throw new SalesDomainException("error.sales.sourceInvoiceOrderMismatch", null, "Source invoice does not belong to this sales order");
            }
        }

        CustomerInvoiceResponse sourceInvoice =
                customerInvoiceApplicationService.getCustomerInvoice(resolvedSourceInvoiceId);

        CreateCreditNoteFromInvoiceCommand cnCmd = new CreateCreditNoteFromInvoiceCommand();
        cnCmd.setCompanyId(companyId);
        cnCmd.setInvoiceDate(command.getInvoiceDate());
        cnCmd.setDueDate(command.getDueDate());
        // SO names are already "SO/YYYY/NNNNN"; prefix only "CN/" → "CN/SO/YYYY/NNNNN".
        cnCmd.setReference(command.getReference() != null ? command.getReference()
                : (o.getName() != null ? "CN/" + o.getName() : "CN/SO/" + o.getId()));

        List<CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand> cnLines = new ArrayList<>();
        for (SalesOrderLine sol : o.getLines()) {
            BigDecimal qty = creditNoteableQtyForLine(sol, draftCn, draftChargeCn, postedNets);
            if (qty.signum() <= 0) {
                continue;
            }
            // Prefer charge (non-gift) source lines so sale→gift reverses AR; then gift lines for returns.
            BigDecimal remaining = appendCreditNoteLineQtys(sourceInvoice, sol.getId(), qty, cnLines, false);
            if (remaining.signum() > 0) {
                appendCreditNoteLineQtys(sourceInvoice, sol.getId(), remaining, cnLines, true);
            }
        }
        if (cnLines.isEmpty()) {
            throw new SalesDomainException(
                    "No credit-note quantity: return delivered goods first so qty invoiced exceeds qty delivered, "
                            + "or mark charged lines as gift to credit commercially "
                            + "(or post/remove any draft credit note).");
        }
        cnCmd.setLines(cnLines);
        return customerInvoiceApplicationService.createCreditNoteFromInvoice(resolvedSourceInvoiceId, cnCmd);
    }

    /**
     * Allocates {@code qty} onto source invoice lines for {@code salesOrderLineId} filtered by gift flag.
     * @return leftover qty not allocated
     */
    private static BigDecimal appendCreditNoteLineQtys(
            CustomerInvoiceResponse sourceInvoice,
            UUID salesOrderLineId,
            BigDecimal qty,
            List<CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand> cnLines,
            boolean gift) {
        BigDecimal remaining = qty;
        for (var invLine : sourceInvoice.getLines()) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (!salesOrderLineId.equals(invLine.getSalesOrderLineId()) || invLine.isGift() != gift) {
                continue;
            }
            BigDecimal take = remaining.min(invLine.getQty() != null ? invLine.getQty() : BigDecimal.ZERO);
            if (take.signum() <= 0) {
                continue;
            }
            CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand lc =
                    new CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand();
            lc.setInvoiceLineId(invLine.getId());
            lc.setQty(take);
            cnLines.add(lc);
            remaining = remaining.subtract(take);
        }
        return remaining.max(BigDecimal.ZERO);
    }

    private void addInvoiceTaxSnapshots(
            CustomerInvoiceLineCommand invLine,
            SalesOrderLine sol,
            BigDecimal invoiceQty,
            DiscountType discountType,
            BigDecimal discountValue) {
        List<FiscalTaxSnapshot> snaps = sol.getTaxes().stream()
                .map(t -> purchaseApplicationService.getFiscalTax(t.getTaxId()))
                .map(this::toSnapshot)
                .toList();
        PurchaseTaxEngine.TaxSplit split = PurchaseTaxEngine.computeLineTaxes(
                invoiceQty, sol.getUnitPrice(), discountType, discountValue, snaps);
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
        r.setOrderDiscountType(o.getOrderDiscountType());
        r.setOrderDiscountValue(o.getOrderDiscountValue());
        r.setOrderDiscountPercent(o.getOrderDiscountPercent());
        OrderPaymentFields payment = computeSalesPaymentFields(o);
        r.setAmountTotal(payment.amountTotal());
        r.setPaymentStatus(payment.paymentStatus());
        r.setAmountPaid(payment.amountPaid());
        r.setAmountDue(payment.amountDue());
        r.setQuotationSentAt(o.getQuotationSentAt());
        r.setConfirmedAt(o.getConfirmedAt());
        r.setCancelledAt(o.getCancelledAt());
        r.setLocked(o.isLocked());
        r.setRowVersion(o.getRowVersion());
        r.setDeliveryCompletedAt(o.getDeliveryCompletedAt());
        r.setInvoicingCompletedAt(o.getInvoicingCompletedAt());
        r.setDeliveryPickingIds(stockMoveSalesQueryPort.findPickingIdsBySalesOrderId(o.getId()));
        r.setReturnPickingIds(stockMoveSalesQueryPort.findReturnPickingIdsBySalesOrderId(o.getId()));
        r.setCanCreateCustomerInvoice(computeCanCreateCustomerInvoice(o));
        r.setCanCreateCustomerCreditNote(computeCanCreateCustomerCreditNote(o));
        r.setCanCreateReturn(computeCanCreateReturn(o));
        r.setLines(o.getLines().stream().sorted(Comparator.comparingInt(SalesOrderLine::getSequence)).map(l -> {
            SalesOrderLineResponse lr = new SalesOrderLineResponse();
            lr.setId(l.getId());
            lr.setSequence(l.getSequence());
            lr.setProductId(l.getProductId());
            lr.setName(l.getName());
            lr.setUomId(l.getUomId());
            lr.setPackagingId(l.getPackagingId());
            lr.setPackagingName(l.getPackagingName());
            lr.setQtyPerPackage(l.getQtyPerPackage());
            lr.setQtyOrdered(l.getQtyOrdered());
            lr.setQtyDelivered(l.getQtyDelivered());
            lr.setQtyInvoiced(l.getQtyInvoiced());
            lr.setUnitPrice(l.getUnitPrice());
            lr.setGift(l.isGift());
            lr.setDiscountType(l.getDiscountType());
            lr.setDiscountValue(l.getDiscountValue());
            lr.setDiscountPercent(l.getDiscountPercent());
            lr.setInvoicePolicy(l.getInvoicePolicy());
            lr.setRevenueAccountId(l.getRevenueAccountId());
            lr.setTaxIds(l.getTaxes().stream().map(SalesOrderLineTax::getTaxId).toList());
            return lr;
        }).toList());
        return r;
    }

    private void applyPackagingSnapshot(SalesOrderLine line, UUID packagingId) {
        if (packagingId == null) {
            line.setPackagingId(null);
            line.setPackagingName(null);
            line.setQtyPerPackage(null);
            return;
        }
        ProductPackaging packaging = productPackagingRepository.findById(new ProductPackagingId(packagingId))
                .orElseThrow(() -> new SalesDomainException(
                        "error.inventory.packagingNotFound",
                        new Object[]{packagingId},
                        "Packaging not found: " + packagingId));
        if (!packaging.isActive()) {
            throw new SalesDomainException(
                    "error.inventory.packagingInactive",
                    new Object[]{packaging.getName()},
                    "Packaging is inactive: " + packaging.getName());
        }
        boolean belongsToLine = packaging.getProductId().getId().equals(line.getProductId())
                || (packaging.getPackagedProductId() != null
                && packaging.getPackagedProductId().getId().equals(line.getProductId()));
        if (!belongsToLine) {
            throw new SalesDomainException(
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
