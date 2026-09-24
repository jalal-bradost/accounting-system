package com.bradox.erp.sales.service.domain;

import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.SalesCogsClearingPort;
import com.bradox.erp.accounting.service.domain.ports.output.SalesOrderCogsStatePort;
import com.bradox.erp.accounting.service.domain.ports.output.SalesOrderInvoiceSyncPort;
import com.bradox.erp.contacts.service.domain.dto.CreditStatusResponse;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.DiscountMath;
import com.bradox.erp.domain.valueobject.DiscountType;
import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.entity.ProductPackaging;
import com.bradox.erp.inventory.domain.core.entity.StockLocation;
import com.bradox.erp.inventory.domain.core.entity.Warehouse;
import com.bradox.erp.inventory.domain.core.valueobject.LocationType;
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
    private final SalesEventPublisher salesEventPublisher;
    private final SalesOrderQtyWriter salesOrderQtyWriter;
    private final DocumentSequenceService documentSequenceService;
    private final RecordActivityLogger activityLogger;
    private final CompanyDocumentPolicyService companyDocumentPolicyService;
    private final TransactionTemplate afterCommitTx;

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
                                       SalesEventPublisher salesEventPublisher,
                                       SalesOrderQtyWriter salesOrderQtyWriter,
                                       DocumentSequenceService documentSequenceService,
                                       RecordActivityLogger activityLogger,
                                       CompanyDocumentPolicyService companyDocumentPolicyService,
                                       PlatformTransactionManager transactionManager) {
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
        this.salesEventPublisher = salesEventPublisher;
        this.salesOrderQtyWriter = salesOrderQtyWriter;
        this.documentSequenceService = documentSequenceService;
        this.activityLogger = activityLogger;
        this.companyDocumentPolicyService = companyDocumentPolicyService;
        this.afterCommitTx = new TransactionTemplate(transactionManager);
        this.afterCommitTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) {
            return fromCommand;
        }
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
        o.setExchangeRateToCompany(command.getExchangeRateToCompany() != null
                ? command.getExchangeRateToCompany() : BigDecimal.ONE);
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
            applyLineDiscountInput(line, lc);
            line.setInvoicePolicy(lc.getInvoicePolicy() != null ? lc.getInvoicePolicy()
                    : (product.getProductType() == ProductType.SERVICE ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED));
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
        o.setExchangeRateToCompany(command.getExchangeRateToCompany() != null
                ? command.getExchangeRateToCompany() : o.getExchangeRateToCompany());
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
            applyLineDiscountInput(line, lc);
            line.setInvoicePolicy(lc.getInvoicePolicy() != null ? lc.getInvoicePolicy()
                    : (product.getProductType() == ProductType.SERVICE ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED));
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
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        return toResponse(saved);
    }

    private SalesOrderResponse amendConfirmedSalesOrder(SalesOrder o, CreateSalesOrderCommand command) {
        UUID companyId = o.getCompanyId();
        Map<UUID, BigDecimal> qtyOrderedBefore = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            qtyOrderedBefore.put(line.getId(), line.getQtyOrdered() != null ? line.getQtyOrdered() : BigDecimal.ZERO);
        }
        BigDecimal untaxedBefore = o.getAmountUntaxed() != null ? o.getAmountUntaxed() : BigDecimal.ZERO;
        boolean hasPostedDocs = customerInvoiceApplicationService.hasPostedInvoiceForSalesOrder(o.getId());
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
            applyLineDiscountInput(line, lc);
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
        recalcTotals(o);
        refreshOrderStatuses(o);
        SalesOrder saved = salesOrderRepository.save(o);
        salesOrderRepository.flush();
        postSalesAmendmentTracking(saved, qtyOrderedBefore, untaxedBefore);
        return syncDocumentsAfterAmendment(saved);
    }

    private void postSalesAmendmentTracking(
            SalesOrder saved,
            Map<UUID, BigDecimal> qtyOrderedBefore,
            BigDecimal untaxedBefore) {
        List<String> qtyBlocks = new ArrayList<>();
        for (SalesOrderLine line : saved.getLines()) {
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
                block.append("\n  Invoiced Quantity: ")
                        .append(line.getQtyInvoiced().stripTrailingZeros().toPlainString());
            }
            qtyBlocks.add(block.toString());
        }
        if (!qtyBlocks.isEmpty()) {
            activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getId(),
                    "The ordered quantity has been updated.\n" + String.join("\n", qtyBlocks));
        }
        BigDecimal untaxedAfter = saved.getAmountUntaxed() != null ? saved.getAmountUntaxed() : BigDecimal.ZERO;
        if (untaxedBefore.compareTo(untaxedAfter) != 0) {
            activityLogger.logFieldChange(
                    saved.getCompanyId(),
                    RecordActivityLogger.MODEL_SALES_ORDER,
                    saved.getId(),
                    untaxedBefore.stripTrailingZeros().toPlainString(),
                    untaxedAfter.stripTrailingZeros().toPlainString(),
                    "Subtotal");
        }
    }

    /**
     * After a confirmed-order amendment: create draft return/delivery pickings for
     * qty changes. Delivery validation and invoices stay manual actions.
     */
    private SalesOrderResponse syncDocumentsAfterAmendment(SalesOrder saved) {
        if (saved.getWarehouseId() == null) {
            return toResponse(saved);
        }
        salesOrderQtyWriter.updateQtyDeliveredJoiningCurrentTransaction(saved.getId());
        SalesOrder o = loadOrder(saved.getId());

        createReturnPickingForExcess(o);
        o = loadOrder(o.getId());

        createDeliveryPickingForRemaining(o);
        o = loadOrder(o.getId());
        refreshOrderStatuses(o);
        salesOrderRepository.save(o);
        return getSalesOrder(o.getId());
    }

    private void createDeliveryPickingForRemaining(SalesOrder o) {
        List<StockMoveCommand> moves = buildRemainingOutgoingMoves(o);
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

    private void createReturnPickingForExcess(SalesOrder o) {
        List<StockMoveCommand> moves = buildExcessReturnMoves(o);
        if (moves.isEmpty()) {
            return;
        }
        UUID warehouseId = o.getWarehouseId();
        Warehouse wh = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new SalesDomainException("Warehouse not found: " + warehouseId));
        StockLocation customerLoc = findCustomerVirtual(o.getCompanyId());
        UUID destLoc = wh.getStockLocationId() != null ? wh.getStockLocationId().getId() : null;
        if (destLoc == null) {
            throw new SalesDomainException("error.sales.warehouseStockLocationUnresolved", null, "Warehouse stock location could not be resolved");
        }
        CreateStockPickingCommand cmd = new CreateStockPickingCommand();
        cmd.setCompanyId(o.getCompanyId());
        cmd.setWarehouseId(warehouseId);
        cmd.setPickingType(PickingType.INCOMING);
        cmd.setSourceLocationId(customerLoc.getId().getId());
        cmd.setDestinationLocationId(destLoc);
        cmd.setPartnerId(o.getCustomerPartnerId());
        cmd.setOrigin("RETURN OF " + o.getName());
        cmd.setReference(o.getName() != null ? o.getName() + "-RET" : null);
        cmd.setSalesOrderId(o.getId());
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

    private List<StockMoveCommand> buildExcessReturnMoves(SalesOrder o) {
        List<StockMoveCommand> moves = new ArrayList<>();
        for (SalesOrderLine line : o.getLines()) {
            Product product = productRepository.findById(new ProductId(line.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + line.getProductId()));
            if (product.getProductType() == ProductType.SERVICE) {
                continue;
            }
            BigDecimal excess = line.getQtyDelivered().subtract(line.getQtyOrdered());
            if (excess.signum() <= 0) {
                continue;
            }
            moves.add(buildSalesStockMove(line, product, excess));
        }
        return moves;
    }

    private StockMoveCommand buildSalesStockMove(SalesOrderLine line, Product product, BigDecimal qtyInOrderUom) {
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
        applySalesPaymentStatus(r, o);
        return r;
    }

    private record OrderPaymentFields(String paymentStatus, BigDecimal amountPaid, BigDecimal amountDue) {}

    private void applySalesPaymentStatus(SalesOrderSummaryResponse r, SalesOrder o) {
        OrderPaymentFields f = computeSalesPaymentFields(o);
        r.setPaymentStatus(f.paymentStatus());
        r.setAmountPaid(f.amountPaid());
        r.setAmountDue(f.amountDue());
    }

    private OrderPaymentFields computeSalesPaymentFields(SalesOrder o) {
        if (o.getState() == SalesOrderState.CANCELLED) {
            return new OrderPaymentFields("CANCELLED", BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<CustomerInvoiceResponse> invoices = customerInvoiceApplicationService
                .listPostedInvoicesForSalesOrder(o.getId());
        if (invoices.isEmpty()) {
            return new OrderPaymentFields("NEW", BigDecimal.ZERO,
                    o.getAmountTotal() != null ? o.getAmountTotal() : BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal credited = BigDecimal.ZERO;
        LocalDate earliestDue = null;
        List<com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentResponse> allPayments =
                customerInvoiceApplicationService.listCustomerPayments(o.getCompanyId());
        for (CustomerInvoiceResponse inv : invoices) {
            total = total.add(customerInvoiceTotal(inv));
            paid = paid.add(allPayments.stream()
                    .filter(p -> inv.getId().equals(p.getCustomerInvoiceId()))
                    .filter(p -> inv.getCurrencyCode() == null
                            || inv.getCurrencyCode().equalsIgnoreCase(p.getCurrencyCode()))
                    .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            credited = credited.add(sumPostedCreditNotesForInvoice(inv));
            if (inv.getDueDate() != null && (earliestDue == null || inv.getDueDate().isBefore(earliestDue))) {
                earliestDue = inv.getDueDate();
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

    private BigDecimal sumPostedCreditNotesForInvoice(CustomerInvoiceResponse sourceInvoice) {
        if (sourceInvoice == null || sourceInvoice.getId() == null) {
            return BigDecimal.ZERO;
        }
        String currency = sourceInvoice.getCurrencyCode();
        return customerInvoiceApplicationService.listCreditNotesForInvoice(sourceInvoice.getId()).stream()
                .filter(cn -> cn.getState() == CustomerInvoiceState.POSTED)
                .filter(cn -> cn.getMoveType() == null || cn.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE)
                .filter(cn -> currency == null || currency.equalsIgnoreCase(cn.getCurrencyCode()))
                .map(SalesApplicationServiceImpl::customerInvoiceTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal customerInvoiceTotal(CustomerInvoiceResponse inv) {
        BigDecimal total = BigDecimal.ZERO;
        for (var line : inv.getLines()) {
            BigDecimal disc = line.getDiscountPercent() != null ? line.getDiscountPercent() : BigDecimal.ZERO;
            BigDecimal factor = BigDecimal.ONE.subtract(
                    disc.max(BigDecimal.ZERO).min(new BigDecimal("100"))
                            .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            BigDecimal net = line.getQty().multiply(line.getUnitPrice()).multiply(factor)
                    .setScale(4, RoundingMode.HALF_UP);
            total = total.add(net);
            for (var tax : line.getTaxSnapshots()) {
                total = total.add(tax.getTaxAmount() != null
                        ? tax.getTaxAmount().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            }
        }
        return total;
    }

    private boolean computeCanCreateCustomerInvoice(SalesOrder o) {
        if (o.getState() != SalesOrderState.CONFIRMED) {
            return false;
        }
        boolean allowWithoutDelivery = companyDocumentPolicyService.allowInvoiceWithoutDelivery(o.getCompanyId());
        Map<UUID, BigDecimal> draftAllocated =
                customerInvoiceApplicationService.draftAllocatedQtyBySalesOrderLine(o.getId());
        for (SalesOrderLine sol : o.getLines()) {
            Product product = productRepository.findById(new ProductId(sol.getProductId())).orElse(null);
            if (product == null) {
                continue;
            }
            if (invoiceableQtyForLine(sol, product, draftAllocated, allowWithoutDelivery).signum() > 0) {
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
                                             Map<UUID, BigDecimal> draftAllocated,
                                             boolean allowWithoutDelivery) {
        SalInvoicePolicy pol = effectiveInvoicePolicy(sol, product, allowWithoutDelivery);
        BigDecimal targetQty = pol == SalInvoicePolicy.ORDERED
                ? sol.getQtyOrdered() : sol.getQtyDelivered();
        BigDecimal invoiced = effectiveQtyInvoiced(sol, draftAllocated);
        return targetQty.subtract(invoiced).max(BigDecimal.ZERO);
    }

    private static SalInvoicePolicy effectiveInvoicePolicy(SalesOrderLine sol,
                                                           Product product,
                                                           boolean allowWithoutDelivery) {
        if (allowWithoutDelivery) {
            return SalInvoicePolicy.ORDERED;
        }
        if (sol.getInvoicePolicy() != null) {
            return sol.getInvoicePolicy();
        }
        return product != null && product.getProductType() == ProductType.SERVICE
                ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED;
    }

    /** Over-invoiced qty after returns: invoiced − delivered (or ordered for services) − draft credit notes. */
    private BigDecimal creditNoteableQtyForLine(SalesOrderLine sol, Map<UUID, BigDecimal> draftCreditNotes) {
        BigDecimal draft = draftCreditNotes.getOrDefault(sol.getId(), BigDecimal.ZERO);
        Optional<Product> product = productRepository.findById(new ProductId(sol.getProductId()));
        BigDecimal baseline;
        if (product.isPresent() && product.get().getProductType() == ProductType.SERVICE) {
            baseline = sol.getQtyOrdered();
        } else {
            baseline = sol.getQtyDelivered();
        }
        return sol.getQtyInvoiced().subtract(baseline).subtract(draft).max(BigDecimal.ZERO)
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

    /** Order subtotal after line discounts but before the order-level discount. */
    private static BigDecimal orderSubtotalBeforeOrderDiscount(SalesOrder o) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SalesOrderLine line : o.getLines()) {
            subtotal = subtotal.add(SalesOrderRules.lineNet(
                    line.getQtyOrdered(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue()));
        }
        return subtotal;
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
        CreditStatusResponse credit = partnerApplicationService.creditStatus(o.getCustomerPartnerId());
        if (!credit.unlimited() && o.getAmountTotal().compareTo(credit.available()) > 0) {
            throw new SalesDomainException(
                    "error.sales.creditLimitExceeded", null, "Credit limit exceeded for this order total");
        }
        UUID warehouseId = o.getWarehouseId();
        if (warehouseId == null) {
            throw new SalesDomainException("error.sales.warehouseIdRequiredToConfirm", null, "warehouseId is required to confirm a sales order");
        }
        Warehouse wh = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new SalesDomainException("Warehouse not found: " + warehouseId));
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
            if (customerInvoiceApplicationService.hasPostedInvoiceForSalesOrder(o.getId())) {
                throw new SalesDomainException("error.sales.cannotCancelPostedInvoices", null, "Cannot cancel: posted customer invoices exist for this order");
            }
            if (stockMoveSalesQueryPort.existsNonTerminalPickingForSalesOrder(o.getId())) {
                throw new SalesDomainException("error.sales.cannotCancelOpenDeliveries", null, "Cannot cancel: open deliveries exist (finish or cancel pickings first)");
            }
        }
        o.setState(SalesOrderState.CANCELLED);
        o.setCancelledAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        SalesOrder saved = salesOrderRepository.save(o);
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
        SalesOrder o = salesOrderQtyWriter.updateQtyDelivered(salesOrderId);
        if (o != null) {
            UUID id = o.getId();
            reconcileCogsClearing(id);
            boolean toRefund = stockMoveSalesQueryPort.isPickingToRefund(pickingId);
            runAfterCommit(() -> {
                if (!toRefund) {
                    return;
                }
                SalesOrder fresh = salesOrderRepository.findByIdWithLines(id).orElse(null);
                if (fresh != null) {
                    tryAutoCreateDraftCreditNoteFromReturn(fresh);
                }
            });
        }
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
            return new OrderState(o.getCompanyId(), lines);
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

    private void tryAutoCreateDraftCreditNoteFromReturn(SalesOrder o) {
        Map<UUID, BigDecimal> draftCn =
                customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(o.getId());
        final Map<UUID, BigDecimal> initialDraftCn = draftCn;
        boolean hasCreditable = o.getLines().stream()
                .anyMatch(sol -> creditNoteableQtyForLine(sol, initialDraftCn).signum() > 0);
        if (!hasCreditable) {
            return;
        }
        List<CustomerInvoiceResponse> postedInvoices =
                customerInvoiceApplicationService.listPostedInvoicesForSalesOrder(o.getId()).stream()
                        .sorted(Comparator.comparing(CustomerInvoiceResponse::getInvoiceDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(CustomerInvoiceResponse::getId))
                        .toList();
        if (postedInvoices.isEmpty()) {
            return;
        }
        for (CustomerInvoiceResponse source : postedInvoices) {
            final Map<UUID, BigDecimal> loopDraftCn =
                    customerInvoiceApplicationService.draftCreditNoteAllocatedQtyBySalesOrderLine(o.getId());
            final SalesOrder current = loadOrder(o.getId());
            o = current;
            boolean stillCreditable = current.getLines().stream()
                    .anyMatch(sol -> creditNoteableQtyForLine(sol, loopDraftCn).signum() > 0);
            if (!stillCreditable) {
                break;
            }
            CreateCustomerInvoiceFromSalesOrderCommand cmd = new CreateCustomerInvoiceFromSalesOrderCommand();
            cmd.setCompanyId(current.getCompanyId());
            cmd.setSalesOrderId(current.getId());
            cmd.setSourceInvoiceId(source.getId());
            cmd.setInvoiceDate(LocalDate.now());
            try {
                // Leave as DRAFT — do not auto-post (same as purchase return correction).
                createCustomerCreditNoteFromSalesOrder(cmd);
            } catch (RuntimeException ex) {
                // Source invoice may have no remaining credit-noteable lines; try next.
            }
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
        boolean allowWithoutDelivery = companyDocumentPolicyService.allowInvoiceWithoutDelivery(o.getCompanyId());
        BigDecimal orderSubtotal = orderSubtotalBeforeOrderDiscount(o);
        BigDecimal orderDiscountAmount = DiscountMath.discountAmount(
                orderSubtotal, o.getOrderDiscountType(), o.getOrderDiscountValue());
        List<CustomerInvoiceLineCommand> invLines = new ArrayList<>();
        BigDecimal invoicedSubtotal = BigDecimal.ZERO;
        for (SalesOrderLine sol : o.getLines()) {
            Product product = productRepository.findById(new ProductId(sol.getProductId()))
                    .orElseThrow(() -> new SalesDomainException("Product not found: " + sol.getProductId()));
            BigDecimal qty = invoiceableQtyForLine(sol, product, draftAllocated, allowWithoutDelivery);
            if (qty.signum() <= 0) {
                continue;
            }
            // Carry only the sales-order LINE discount. Order-level discount stays on the invoice header.
            BigDecimal invoicedGross = qty.multiply(sol.getUnitPrice());
            BigDecimal orderedGross = sol.getQtyOrdered().multiply(sol.getUnitPrice());
            BigDecimal orderedNet = SalesOrderRules.lineNet(
                    sol.getQtyOrdered(), sol.getUnitPrice(), sol.getDiscountType(), sol.getDiscountValue());
            BigDecimal lineDiscOnOrder = orderedGross.subtract(orderedNet).max(BigDecimal.ZERO);
            BigDecimal lineDiscAmount = orderedGross.signum() > 0
                    ? lineDiscOnOrder.multiply(invoicedGross).divide(orderedGross, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            CustomerInvoiceLineCommand lc = new CustomerInvoiceLineCommand();
            lc.setName(sol.getName());
            lc.setQty(qty);
            lc.setUnitPrice(sol.getUnitPrice());
            lc.setDiscountType(DiscountType.FIXED);
            lc.setDiscountValue(lineDiscAmount);
            lc.setDiscountPercent(DiscountMath.effectivePercent(invoicedGross, DiscountType.FIXED, lineDiscAmount));
            lc.setRevenueAccountId(sol.getRevenueAccountId());
            lc.setSalesOrderLineId(sol.getId());
            addInvoiceTaxSnapshots(lc, sol, qty, DiscountType.FIXED, lineDiscAmount);
            invLines.add(lc);
            invoicedSubtotal = invoicedSubtotal.add(
                    SalesOrderRules.lineNet(qty, sol.getUnitPrice(), DiscountType.FIXED, lineDiscAmount)
                            .setScale(4, RoundingMode.HALF_UP));
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
                throw new SalesDomainException("error.sales.sourceInvoiceOrderMismatch", null, "Source invoice does not belong to this sales order");
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
        // SO names are already "SO/YYYY/NNNNN"; prefix only "CN/" → "CN/SO/YYYY/NNNNN".
        cnCmd.setReference(command.getReference() != null ? command.getReference()
                : (o.getName() != null ? "CN/" + o.getName() : "CN/SO/" + o.getId()));

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
        r.setAmountTotal(o.getAmountTotal());
        r.setOrderDiscountType(o.getOrderDiscountType());
        r.setOrderDiscountValue(o.getOrderDiscountValue());
        r.setOrderDiscountPercent(o.getOrderDiscountPercent());
        OrderPaymentFields payment = computeSalesPaymentFields(o);
        r.setPaymentStatus(payment.paymentStatus());
        r.setAmountPaid(payment.amountPaid());
        r.setAmountDue(payment.amountDue());
        r.setQuotationSentAt(o.getQuotationSentAt());
        r.setConfirmedAt(o.getConfirmedAt());
        r.setCancelledAt(o.getCancelledAt());
        r.setLocked(o.isLocked());
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
