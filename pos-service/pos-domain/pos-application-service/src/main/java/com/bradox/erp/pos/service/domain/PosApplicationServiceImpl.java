package com.bradox.erp.pos.service.domain;

import com.bradox.erp.accounting.service.domain.JournalEntryTiming;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.CompanyCurrencyApplicationService;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.CurrencyRow;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.ProductPackaging;
import com.bradox.erp.inventory.domain.core.valueobject.ProductType;
import com.bradox.erp.inventory.service.domain.dto.ProductCategoryResponse;
import com.bradox.erp.inventory.service.domain.dto.ProductPackagingResponse;
import com.bradox.erp.inventory.service.domain.dto.ProductResponse;
import com.bradox.erp.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.erp.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.erp.inventory.service.domain.ports.input.ProductApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.ProductPackagingApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.StockValuationApplicationService;
import com.bradox.erp.pos.domain.core.entity.PosConfig;
import com.bradox.erp.pos.domain.core.entity.PosOrder;
import com.bradox.erp.pos.domain.core.entity.PosOrderLine;
import com.bradox.erp.pos.domain.core.entity.PosPayment;
import com.bradox.erp.pos.domain.core.entity.PosReceipt;
import com.bradox.erp.pos.domain.core.entity.PosSession;
import com.bradox.erp.pos.service.domain.ports.output.repository.PosConfigRepository;
import com.bradox.erp.pos.service.domain.ports.output.repository.PosOrderRepository;
import com.bradox.erp.pos.service.domain.ports.output.repository.PosReceiptRepository;
import com.bradox.erp.pos.service.domain.ports.output.repository.PosSessionRepository;
import com.bradox.erp.pos.domain.core.PosDomainException;
import com.bradox.erp.pos.domain.core.PosOrderState;
import com.bradox.erp.pos.domain.core.PosPaymentMethod;
import com.bradox.erp.pos.domain.core.PosRules;
import com.bradox.erp.pos.domain.core.PosSessionState;
import com.bradox.erp.pos.service.domain.dto.CheckoutPosOrderCommand;
import com.bradox.erp.pos.service.domain.dto.ClosePosSessionCommand;
import com.bradox.erp.pos.service.domain.dto.CreatePosOrderCommand;
import com.bradox.erp.pos.service.domain.dto.OpenPosSessionCommand;
import com.bradox.erp.pos.service.domain.dto.PosCatalogItemResponse;
import com.bradox.erp.pos.service.domain.dto.PosConfigCardResponse;
import com.bradox.erp.pos.service.domain.dto.PosConfigCommand;
import com.bradox.erp.pos.service.domain.dto.PosConfigResponse;
import com.bradox.erp.pos.service.domain.dto.PosOrderLineCommand;
import com.bradox.erp.pos.service.domain.dto.PosOrderLineResponse;
import com.bradox.erp.pos.service.domain.dto.PosOrderResponse;
import com.bradox.erp.pos.service.domain.dto.PosPaymentResponse;
import com.bradox.erp.pos.service.domain.dto.PosReceiptResponse;
import com.bradox.erp.pos.service.domain.dto.PosSessionResponse;
import com.bradox.erp.pos.service.domain.dto.RegisterPosPaymentCommand;
import com.bradox.erp.pos.service.domain.dto.UpdatePosOrderLineCommand;
import com.bradox.erp.pos.service.domain.ports.input.PosApplicationService;
import com.bradox.erp.purchase.service.domain.FiscalTaxSnapshot;
import com.bradox.erp.purchase.service.domain.PurchaseTaxEngine;
import com.bradox.erp.purchase.service.domain.dto.FiscalTaxResponse;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.bradox.erp.sales.domain.core.SalInvoicePolicy;
import com.bradox.erp.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.bradox.erp.sales.service.domain.dto.CreateSalesOrderCommand;
import com.bradox.erp.sales.service.domain.dto.SalesOrderLineCommand;
import com.bradox.erp.sales.service.domain.dto.SalesOrderResponse;
import com.bradox.erp.sales.service.domain.ports.input.SalesApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PosApplicationServiceImpl implements PosApplicationService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PosConfigRepository configRepository;
    private final PosSessionRepository sessionRepository;
    private final PosOrderRepository orderRepository;
    private final PosReceiptRepository receiptRepository;
    private final ProductApplicationService productApplicationService;
    private final ProductPackagingApplicationService productPackagingApplicationService;
    private final SalesApplicationService salesApplicationService;
    private final StockPickingApplicationService stockPickingApplicationService;
    private final StockValuationApplicationService stockValuationApplicationService;
    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;
    private final PurchaseApplicationService purchaseApplicationService;
    private final CompanyCurrencyApplicationService companyCurrencyApplicationService;

    public PosApplicationServiceImpl(PosConfigRepository configRepository,
                                     PosSessionRepository sessionRepository,
                                     PosOrderRepository orderRepository,
                                     PosReceiptRepository receiptRepository,
                                     ProductApplicationService productApplicationService,
                                     ProductPackagingApplicationService productPackagingApplicationService,
                                     SalesApplicationService salesApplicationService,
                                     StockPickingApplicationService stockPickingApplicationService,
                                     StockValuationApplicationService stockValuationApplicationService,
                                     CustomerInvoiceApplicationService customerInvoiceApplicationService,
                                     PurchaseApplicationService purchaseApplicationService,
                                     CompanyCurrencyApplicationService companyCurrencyApplicationService) {
        this.configRepository = configRepository;
        this.sessionRepository = sessionRepository;
        this.orderRepository = orderRepository;
        this.receiptRepository = receiptRepository;
        this.productApplicationService = productApplicationService;
        this.productPackagingApplicationService = productPackagingApplicationService;
        this.salesApplicationService = salesApplicationService;
        this.stockPickingApplicationService = stockPickingApplicationService;
        this.stockValuationApplicationService = stockValuationApplicationService;
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
        this.purchaseApplicationService = purchaseApplicationService;
        this.companyCurrencyApplicationService = companyCurrencyApplicationService;
    }

    @Override
    @Transactional
    public PosConfigResponse createConfig(PosConfigCommand command) {
        configRepository.findByCompanyIdAndName(command.getCompanyId(), command.getName())
                .ifPresent(existing -> {
                    throw new PosDomainException("error.pos.configAlreadyExists", new Object[] { existing.getName() }, "POS config already exists: " + existing.getName());
                });
        Instant now = Instant.now();
        PosConfig entity = new PosConfig();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(command.getCompanyId());
        entity.setName(command.getName());
        entity.setWarehouseId(command.getWarehouseId());
        entity.setDefaultCustomerPartnerId(command.getDefaultCustomerPartnerId());
        entity.setCashJournalId(command.getCashJournalId());
        entity.setBankJournalId(command.getBankJournalId());
        entity.setPricelistId(command.getPricelistId());
        entity.setCurrencyCode(resolveCompanyCurrencyCode(command.getCompanyId(), command.getCurrencyCode()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toConfigResponse(configRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosConfigResponse> listConfigs(CompanyId companyId) {
        return configRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId.getId())
                .stream()
                .map(this::toConfigResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosConfigCardResponse> listConfigCards(CompanyId companyId) {
        return configRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId.getId()).stream()
                .map(this::toConfigCardResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PosSessionResponse getSession(UUID sessionId) {
        return toSessionResponse(loadSession(sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public PosSessionResponse getOpenSessionForConfig(CompanyId companyId, UUID configId) {
        PosConfig config = loadConfig(configId);
        ensureCompany(config.getCompanyId(), companyId.getId());
        return sessionRepository.findFirstByConfigIdAndStateOrderByOpenedAtDesc(configId, PosSessionState.OPEN)
                .map(this::toSessionResponse)
                .orElseThrow(() -> new PosDomainException("No open session for this POS config"));
    }

    @Override
    @Transactional
    public PosSessionResponse openSession(OpenPosSessionCommand command) {
        PosConfig config = loadConfig(command.getConfigId());
        ensureCompany(config.getCompanyId(), command.getCompanyId());
        sessionRepository.findFirstByConfigIdAndStateOrderByOpenedAtDesc(config.getId(), PosSessionState.OPEN)
                .ifPresent(open -> {
                    throw new PosDomainException("error.pos.configHasOpenSession", null, "POS config already has an open session");
                });
        PosSession session = new PosSession();
        session.setId(UUID.randomUUID());
        session.setCompanyId(config.getCompanyId());
        session.setConfigId(config.getId());
        session.setState(PosSessionState.OPEN);
        session.setWarehouseId(config.getWarehouseId());
        session.setDefaultCustomerPartnerId(config.getDefaultCustomerPartnerId());
        session.setCashJournalId(config.getCashJournalId());
        session.setBankJournalId(config.getBankJournalId());
        session.setPricelistId(config.getPricelistId());
        String currency = resolveCompanyCurrencyCode(config.getCompanyId(), config.getCurrencyCode());
        session.setCurrencyCode(currency);
        if (currency != null && !currency.equalsIgnoreCase(config.getCurrencyCode())) {
            config.setCurrencyCode(currency);
            config.setUpdatedAt(Instant.now());
            configRepository.save(config);
        }
        session.setOpeningCash(defaultZero(command.getOpeningCash()));
        session.setOpenedAt(Instant.now());
        return toSessionResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional
    public PosSessionResponse closeSession(UUID sessionId, ClosePosSessionCommand command) {
        PosSession session = loadSession(sessionId);
        PosRules.ensureSessionOpen(session.getState());
        session.setState(PosSessionState.CLOSED);
        session.setClosingCash(command.getClosingCash());
        session.setClosedAt(Instant.now());
        return toSessionResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PosCatalogItemResponse> searchCatalog(CompanyId companyId, UUID sessionId, String query, UUID categoryId,
                                                      Pageable pageable) {
        PosSession session = loadSession(sessionId);
        ensureCompany(session.getCompanyId(), companyId.getId());
        PosRules.ensureSessionOpen(session.getState());
        Map<UUID, String> categoryNames = categoryNameMap(companyId);
        Page<ProductResponse> products = productApplicationService.searchSaleableProducts(companyId, query, pageable);
        List<PosCatalogItemResponse> saleable = products.stream()
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .map(p -> toCatalogItemResponse(p, categoryNames, companyId, session.getWarehouseId()))
                .toList();
        return new PageImpl<>(saleable, pageable, products.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PosCatalogItemResponse findCatalogByBarcode(CompanyId companyId, UUID sessionId, String barcode) {
        PosSession session = loadSession(sessionId);
        ensureCompany(session.getCompanyId(), companyId.getId());
        PosRules.ensureSessionOpen(session.getState());
        if (barcode == null || barcode.isBlank()) {
            throw new PosDomainException(
                    "error.pos.productNotFoundByBarcode",
                    new Object[]{""},
                    "No saleable product found for barcode");
        }
        String trimmed = barcode.trim();
        Map<UUID, String> categoryNames = categoryNameMap(companyId);
        var packagingOpt = productPackagingApplicationService.findActiveByBarcode(companyId, trimmed);
        if (packagingOpt.isPresent()) {
            ProductPackaging packaging = packagingOpt.get();
            UUID catalogProductId = packaging.getPackagedProductId() != null
                    ? packaging.getPackagedProductId().getId()
                    : packaging.getProductId().getId();
            ProductResponse product = productApplicationService.getProduct(catalogProductId);
            if (!product.isSaleOk()) {
                throw new PosDomainException(
                        "error.pos.productNotFoundByBarcode",
                        new Object[]{trimmed},
                        "No saleable product found for barcode: " + trimmed);
            }
            return toCatalogItemResponse(product, categoryNames, companyId, session.getWarehouseId());
        }
        ProductResponse product = productApplicationService.findSaleableByBarcodeOrSku(companyId, trimmed)
                .orElseThrow(() -> new PosDomainException(
                        "error.pos.productNotFoundByBarcode",
                        new Object[]{trimmed},
                        "No saleable product found for barcode: " + trimmed));
        PosCatalogItemResponse item = toCatalogItemResponse(product, categoryNames, companyId, session.getWarehouseId());
        item.setQtyPerPackage(BigDecimal.ONE);
        return item;
    }

    @Override
    @Transactional
    public PosOrderResponse createOrder(CreatePosOrderCommand command) {
        PosSession session = loadSession(command.getSessionId());
        ensureCompany(session.getCompanyId(), command.getCompanyId());
        PosRules.ensureSessionOpen(session.getState());
        Instant now = Instant.now();
        PosOrder order = new PosOrder();
        order.setId(UUID.randomUUID());
        order.setCompanyId(session.getCompanyId());
        order.setSessionId(session.getId());
        order.setCustomerPartnerId(command.getCustomerPartnerId() != null
                ? command.getCustomerPartnerId()
                : session.getDefaultCustomerPartnerId());
        order.setName(nextOrderName(order.getCompanyId()));
        order.setState(PosOrderState.DRAFT);
        String currency = resolveCompanyCurrencyCode(session.getCompanyId(), session.getCurrencyCode());
        order.setCurrencyCode(currency);
        if (currency != null && !currency.equalsIgnoreCase(session.getCurrencyCode())) {
            session.setCurrencyCode(currency);
            sessionRepository.save(session);
        }
        order.setNote(command.getNote());
        order.setAmountUntaxed(BigDecimal.ZERO);
        order.setAmountTax(BigDecimal.ZERO);
        order.setAmountTotal(BigDecimal.ZERO);
        order.setAmountPaid(BigDecimal.ZERO);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        int sequence = 10;
        for (PosOrderLineCommand lineCommand : command.getLines()) {
            order.getLines().add(toLineEntity(order, lineCommand, sequence));
            sequence += 10;
        }
        recalc(order);
        return toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public PosOrderResponse addOrderLine(UUID orderId, PosOrderLineCommand command) {
        PosOrder order = loadOrder(orderId);
        PosRules.ensureOrderDraft(order.getState());
        int nextSequence = order.getLines().stream().map(PosOrderLine::getSequence).max(Integer::compareTo).orElse(0) + 10;
        order.getLines().add(toLineEntity(order, command, nextSequence));
        order.setUpdatedAt(Instant.now());
        recalc(order);
        return toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public PosOrderResponse updateOrderLine(UUID orderId, UUID lineId, UpdatePosOrderLineCommand command) {
        PosOrder order = loadOrder(orderId);
        PosRules.ensureOrderDraft(order.getState());
        PosOrderLine line = order.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new PosDomainException("POS order line not found: " + lineId));
        if (command.getQuantity() != null) {
            line.setQuantity(command.getQuantity());
        }
        if (command.getUnitPrice() != null) {
            line.setUnitPrice(command.getUnitPrice());
        }
        if (command.getDiscountPercent() != null) {
            line.setDiscountPercent(command.getDiscountPercent());
        }
        if (command.getTaxIds() != null) {
            line.setTaxIds(command.getTaxIds());
        }
        recalcLine(line);
        recalc(order);
        order.setUpdatedAt(Instant.now());
        return toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public PosOrderResponse registerPayment(UUID orderId, RegisterPosPaymentCommand command) {
        PosOrder order = loadOrder(orderId);
        PosRules.ensureOrderDraft(order.getState());
        PosSession session = loadSession(order.getSessionId());
        UUID journalId = resolveJournalId(session, command);
        PosPayment payment = new PosPayment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(order);
        payment.setMethod(command.getMethod());
        payment.setJournalId(journalId);
        payment.setAmount(command.getAmount());
        payment.setReference(command.getReference());
        payment.setPaidAt(Instant.now());
        order.getPayments().add(payment);
        recalc(order);
        if (order.getAmountPaid().compareTo(order.getAmountTotal()) >= 0) {
            order.setState(PosOrderState.PAID);
        }
        order.setUpdatedAt(Instant.now());
        return toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public PosOrderResponse finalizeOrder(UUID orderId) {
        return finalizeOrder(orderId, LocalDate.now());
    }

    private PosOrderResponse finalizeOrder(UUID orderId, LocalDate businessDate) {
        PosOrder order = loadOrder(orderId);
        PosSession session = loadSession(order.getSessionId());
        PosRules.ensureSessionOpen(session.getState());
        PosRules.ensureCanFinalize(order.getState(), order.getAmountTotal(), order.getAmountPaid());

        SalesOrderResponse salesOrder = createAndDeliverSalesOrder(order, session, businessDate);
        CustomerInvoiceResponse invoice = createAndPostInvoice(order, salesOrder, businessDate);
        registerAccountingPayments(order, invoice, businessDate);

        order.setSalesOrderId(salesOrder.getId());
        order.setCustomerInvoiceId(invoice.getId());
        order.setState(PosOrderState.FINALIZED);
        order.setFinalizedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        PosReceipt receipt = createReceipt(order);
        order.setReceiptId(receipt.getId());
        return toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public PosOrderResponse checkout(CheckoutPosOrderCommand command) {
        CreatePosOrderCommand orderCommand = new CreatePosOrderCommand();
        orderCommand.setCompanyId(command.getCompanyId());
        orderCommand.setSessionId(command.getSessionId());
        orderCommand.setCustomerPartnerId(command.getCustomerPartnerId());
        orderCommand.setNote(command.getNote());
        orderCommand.setLines(command.getLines());

        PosOrderResponse created = createOrder(orderCommand);
        PosOrderResponse paid = created;
        for (RegisterPosPaymentCommand payment : command.getPayments()) {
            paid = registerPayment(created.getId(), payment);
        }
        LocalDate businessDate = command.getBusinessDate() != null ? command.getBusinessDate() : LocalDate.now();
        return finalizeOrder(paid.getId(), businessDate);
    }

    @Override
    @Transactional(readOnly = true)
    public PosOrderResponse getOrder(UUID orderId) {
        return toOrderResponse(loadOrder(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosOrderResponse> listSessionOrders(UUID sessionId) {
        loadSession(sessionId);
        return orderRepository.findBySessionIdAndStateOrderByCreatedAtDesc(sessionId, PosOrderState.FINALIZED).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PosReceiptResponse getReceipt(UUID receiptId) {
        return toReceiptResponse(receiptRepository.findById(receiptId)
                .orElseThrow(() -> new PosDomainException("POS receipt not found: " + receiptId)));
    }

    private SalesOrderResponse createAndDeliverSalesOrder(PosOrder order, PosSession session, LocalDate businessDate) {
        CreateSalesOrderCommand command = new CreateSalesOrderCommand();
        command.setCompanyId(order.getCompanyId());
        command.setCustomerPartnerId(order.getCustomerPartnerId());
        command.setCurrencyCode(order.getCurrencyCode());
        command.setWarehouseId(session.getWarehouseId());
        command.setPricelistId(session.getPricelistId());
        command.setOrderDate(businessDate);
        command.setNotes("POS " + order.getName());
        List<SalesOrderLineCommand> salesLines = new ArrayList<>();
        for (PosOrderLine line : order.getLines()) {
            SalesOrderLineCommand salesLine = new SalesOrderLineCommand();
            salesLine.setProductId(line.getProductId());
            salesLine.setName(line.getName());
            salesLine.setUomId(line.getUomId());
            salesLine.setPackagingId(line.getPackagingId());
            salesLine.setQtyOrdered(line.getQuantity());
            salesLine.setUnitPrice(line.getUnitPrice());
            salesLine.setDiscountPercent(line.getDiscountPercent());
            salesLine.setTaxIds(line.getTaxIds());
            salesLine.setRevenueAccountId(line.getRevenueAccountId());
            salesLine.setInvoicePolicy(SalInvoicePolicy.ORDERED);
            salesLines.add(salesLine);
        }
        command.setLines(salesLines);
        SalesOrderResponse created = salesApplicationService.createSalesOrder(command);
        SalesOrderResponse confirmed = salesApplicationService.confirmSalesOrder(created.getId());
        List<UUID> deliveryIds = confirmed.getDeliveryPickingIds() != null
                ? confirmed.getDeliveryPickingIds()
                : List.of();
        for (UUID pickingId : deliveryIds) {
            salesApplicationService.validateDeliveryPicking(pickingId, new ValidatePickingCommand());
        }
        return salesApplicationService.getSalesOrder(created.getId());
    }

    private CustomerInvoiceResponse createAndPostInvoice(PosOrder order, SalesOrderResponse salesOrder, LocalDate businessDate) {
        // Prefer an invoice already posted for this sales order (if any).
        List<CustomerInvoiceResponse> posted =
                customerInvoiceApplicationService.listPostedInvoicesForSalesOrder(salesOrder.getId());
        if (!posted.isEmpty()) {
            return posted.get(0);
        }
        CreateCustomerInvoiceFromSalesOrderCommand command = new CreateCustomerInvoiceFromSalesOrderCommand();
        command.setCompanyId(order.getCompanyId());
        command.setSalesOrderId(salesOrder.getId());
        command.setInvoiceDate(businessDate);
        command.setDueDate(businessDate);
        command.setReference(order.getName());
        CustomerInvoiceResponse draftInvoice = salesApplicationService.createCustomerInvoiceFromSalesOrder(command);
        return customerInvoiceApplicationService.postCustomerInvoice(draftInvoice.getId());
    }

    private void registerAccountingPayments(PosOrder order, CustomerInvoiceResponse invoice, LocalDate businessDate) {
        BigDecimal remaining = order.getAmountTotal();
        for (PosPayment payment : order.getPayments().stream().sorted(Comparator.comparing(PosPayment::getPaidAt)).toList()) {
            if (payment.getMethod() == PosPaymentMethod.CUSTOMER_ACCOUNT) {
                continue;
            }
            if (remaining.signum() <= 0) {
                return;
            }
            BigDecimal amount = payment.getAmount().min(remaining);
            RegisterCustomerPaymentCommand command = new RegisterCustomerPaymentCommand();
            command.setCompanyId(order.getCompanyId());
            command.setCustomerInvoiceId(invoice.getId());
            command.setPaymentJournalId(payment.getJournalId());
            command.setPaymentDate(JournalEntryTiming.ofBusinessDate(businessDate));
            command.setAmount(amount);
            command.setCurrencyCode(order.getCurrencyCode());
            command.setReference(order.getName() + " " + payment.getMethod());
            customerInvoiceApplicationService.registerCustomerPayment(command);
            remaining = remaining.subtract(amount);
        }
    }

    private PosReceipt createReceipt(PosOrder order) {
        PosReceipt receipt = new PosReceipt();
        receipt.setId(UUID.randomUUID());
        receipt.setCompanyId(order.getCompanyId());
        receipt.setOrderId(order.getId());
        String receiptNumber = nextReceiptNumber(order.getCompanyId());
        receipt.setReceiptNumber(receiptNumber);
        receipt.setPayloadJson(receiptJson(order, receiptNumber));
        receipt.setCreatedAt(Instant.now());
        return receiptRepository.save(receipt);
    }

    private PosOrderLine toLineEntity(PosOrder order, PosOrderLineCommand command, int sequence) {
        ProductResponse product = productApplicationService.getProduct(command.getProductId());
        if (!product.isSaleOk()) {
            throw new PosDomainException("error.pos.productNotSaleable", new Object[] { product.getName() }, "Product is not saleable: " + product.getName());
        }
        PosOrderLine line = new PosOrderLine();
        line.setId(UUID.randomUUID());
        line.setOrder(order);
        line.setSequence(sequence);
        ProductPackagingResponse packaging = applyPackagingSnapshot(line, command.getPackagingId(), product);
        if (packaging != null && packaging.getPackagedProductId() != null && !packaging.isBase()) {
            product = productApplicationService.getProduct(packaging.getPackagedProductId());
        }
        line.setProductId(product.getId());
        line.setName(command.getName() != null && !command.getName().isBlank() ? command.getName() : product.getName());
        line.setUomId(command.getUomId() != null ? command.getUomId() : product.getUomId());
        BigDecimal defaultPrice = packaging != null && packaging.getListPrice() != null
                ? packaging.getListPrice()
                : defaultZero(product.getListPrice());
        line.setQuantity(command.getQuantity());
        line.setUnitPrice(command.getUnitPrice() != null ? command.getUnitPrice() : defaultPrice);
        line.setDiscountPercent(defaultZero(command.getDiscountPercent()));
        line.setTaxIds(command.getTaxIds());
        line.setRevenueAccountId(command.getRevenueAccountId());
        recalcLine(line);
        return line;
    }

    private ProductPackagingResponse applyPackagingSnapshot(PosOrderLine line, UUID packagingId, ProductResponse product) {
        if (packagingId == null) {
            line.setPackagingId(null);
            line.setPackagingName(null);
            line.setQtyPerPackage(null);
            return null;
        }
        ProductPackagingResponse packaging = productPackagingApplicationService.get(packagingId);
        boolean belongs = product.getId().equals(packaging.getProductId())
                || product.getId().equals(packaging.getPackagedProductId());
        if (!belongs) {
            throw new PosDomainException(
                    "error.inventory.packagingProductMismatch",
                    null,
                    "Packaging does not belong to this product");
        }
        if (!packaging.isActive()) {
            throw new PosDomainException(
                    "error.inventory.packagingInactive",
                    new Object[]{packaging.getName()},
                    "Packaging is inactive: " + packaging.getName());
        }
        if (!packaging.isBase() && packaging.getPackagedProductId() != null) {
            line.setPackagingId(packaging.getId());
            line.setPackagingName(packaging.getName());
            line.setQtyPerPackage(null);
            return packaging;
        }
        line.setPackagingId(packaging.getId());
        line.setPackagingName(packaging.getName());
        line.setQtyPerPackage(packaging.getQty());
        return packaging;
    }

    private void recalc(PosOrder order) {
        BigDecimal untaxed = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (PosOrderLine line : order.getLines()) {
            untaxed = untaxed.add(line.getSubtotal());
            tax = tax.add(line.getTaxAmount());
        }
        BigDecimal paid = order.getPayments().stream()
                .map(PosPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmountUntaxed(scale(untaxed));
        order.setAmountTax(scale(tax));
        order.setAmountTotal(scale(untaxed.add(tax)));
        order.setAmountPaid(scale(paid));
    }

    private void recalcLine(PosOrderLine line) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(defaultZero(line.getDiscountPercent())
                .divide(HUNDRED, 8, RoundingMode.HALF_UP));
        BigDecimal subtotal = line.getQuantity().multiply(line.getUnitPrice()).multiply(discountMultiplier);
        line.setSubtotal(scale(subtotal));
        BigDecimal tax = BigDecimal.ZERO;
        if (line.getTaxIds() != null && !line.getTaxIds().isEmpty() && line.getOrder() != null) {
            List<FiscalTaxSnapshot> snaps = fiscalSnapshots(line.getOrder().getCompanyId(), line.getTaxIds());
            if (!snaps.isEmpty()) {
                tax = PurchaseTaxEngine.computeLineTaxes(
                        line.getQuantity(), line.getUnitPrice(), line.getDiscountPercent(), snaps).taxTotal();
            }
        }
        line.setTaxAmount(scale(tax));
        line.setTotal(line.getSubtotal().add(line.getTaxAmount()));
    }

    private List<FiscalTaxSnapshot> fiscalSnapshots(UUID companyId, List<UUID> taxIds) {
        Map<UUID, FiscalTaxResponse> byId = purchaseApplicationService.listFiscalTaxes(companyId).stream()
                .collect(Collectors.toMap(FiscalTaxResponse::getId, t -> t, (a, b) -> a));
        List<FiscalTaxSnapshot> snaps = new ArrayList<>();
        for (UUID taxId : taxIds) {
            FiscalTaxResponse t = byId.get(taxId);
            if (t != null) {
                snaps.add(new FiscalTaxSnapshot(t.getId(), t.getAmountType(), t.getAmount(), t.isPriceInclude()));
            }
        }
        return snaps;
    }

    private UUID resolveJournalId(PosSession session, RegisterPosPaymentCommand command) {
        if (command.getJournalId() != null) {
            return command.getJournalId();
        }
        if (command.getMethod() == PosPaymentMethod.CUSTOMER_ACCOUNT) {
            return session.getBankJournalId() != null ? session.getBankJournalId() : session.getCashJournalId();
        }
        if (command.getMethod() == PosPaymentMethod.CASH) {
            return session.getCashJournalId();
        }
        if (session.getBankJournalId() != null) {
            return session.getBankJournalId();
        }
        throw new PosDomainException("error.pos.bankJournalRequiredForPayment", null, "A bank/card journal is required for this POS payment");
    }

    private PosConfig loadConfig(UUID id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new PosDomainException("POS config not found: " + id));
    }

    private PosSession loadSession(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new PosDomainException("POS session not found: " + id));
    }

    private PosOrder loadOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new PosDomainException("POS order not found: " + id));
    }

    private void ensureCompany(UUID actual, UUID expected) {
        if (!actual.equals(expected)) {
            throw new PosDomainException("error.pos.companyMismatch", null, "POS company mismatch");
        }
    }

    /**
     * Prefer the company's accounting base currency for POS sales/display.
     * Falls back to {@code preferred} when no base currency is configured.
     */
    private String resolveCompanyCurrencyCode(UUID companyId, String preferred) {
        return companyCurrencyApplicationService.baseCurrency(companyId)
                .map(CurrencyRow::code)
                .map(code -> code == null ? null : code.trim().toUpperCase())
                .filter(code -> !code.isEmpty())
                .orElseGet(() -> preferred == null ? null : preferred.trim().toUpperCase());
    }

    private String nextOrderName(UUID companyId) {
        return "POS/" + String.format("%06d", orderRepository.countByCompanyId(companyId) + 1);
    }

    private String nextReceiptNumber(UUID companyId) {
        return "RCP/" + String.format("%06d", receiptRepository.countByCompanyId(companyId) + 1);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal scale(BigDecimal value) {
        return defaultZero(value).setScale(4, RoundingMode.HALF_UP);
    }

    private PosConfigResponse toConfigResponse(PosConfig entity) {
        PosConfigResponse response = new PosConfigResponse();
        response.setId(entity.getId());
        response.setCompanyId(entity.getCompanyId());
        response.setName(entity.getName());
        response.setWarehouseId(entity.getWarehouseId());
        response.setDefaultCustomerPartnerId(entity.getDefaultCustomerPartnerId());
        response.setCashJournalId(entity.getCashJournalId());
        response.setBankJournalId(entity.getBankJournalId());
        response.setPricelistId(entity.getPricelistId());
        response.setCurrencyCode(resolveCompanyCurrencyCode(entity.getCompanyId(), entity.getCurrencyCode()));
        response.setActive(entity.isActive());
        return response;
    }

    private PosSessionResponse toSessionResponse(PosSession entity) {
        PosSessionResponse response = new PosSessionResponse();
        response.setId(entity.getId());
        response.setCompanyId(entity.getCompanyId());
        response.setConfigId(entity.getConfigId());
        response.setState(entity.getState());
        response.setWarehouseId(entity.getWarehouseId());
        response.setCashJournalId(entity.getCashJournalId());
        response.setBankJournalId(entity.getBankJournalId());
        response.setPricelistId(entity.getPricelistId());
        response.setCurrencyCode(resolveCompanyCurrencyCode(entity.getCompanyId(), entity.getCurrencyCode()));
        response.setOpeningCash(entity.getOpeningCash());
        response.setClosingCash(entity.getClosingCash());
        response.setExpectedCash(entity.getOpeningCash().add(expectedCashSales(entity.getId())));
        response.setOpenedAt(entity.getOpenedAt());
        response.setClosedAt(entity.getClosedAt());
        return response;
    }

    private BigDecimal expectedCashSales(UUID sessionId) {
        return orderRepository.sumCashPaymentsBySessionId(sessionId, PosOrderState.FINALIZED);
    }

    private Map<UUID, String> categoryNameMap(CompanyId companyId) {
        Map<UUID, String> map = new HashMap<>();
        for (ProductCategoryResponse cat : productApplicationService.listCategories(companyId, false)) {
            map.put(cat.getId(), cat.getName());
        }
        return map;
    }

    private PosConfigCardResponse toConfigCardResponse(PosConfig config) {
        PosConfigCardResponse card = new PosConfigCardResponse();
        card.setId(config.getId());
        card.setName(config.getName());
        card.setCurrencyCode(resolveCompanyCurrencyCode(config.getCompanyId(), config.getCurrencyCode()));
        card.setActive(config.isActive());
        sessionRepository.findFirstByConfigIdAndStateOrderByOpenedAtDesc(config.getId(), PosSessionState.OPEN)
                .ifPresent(session -> {
                    card.setOpenSessionId(session.getId());
                    card.setOpenSessionState(session.getState().name());
                    card.setSessionOpenedAt(session.getOpenedAt());
                    card.setOpeningCash(session.getOpeningCash());
                    card.setSessionSalesTotal(orderRepository.sumAmountTotalBySessionIdAndState(
                            session.getId(), PosOrderState.FINALIZED));
                    card.setSessionOrderCount(orderRepository.countBySessionIdAndState(
                            session.getId(), PosOrderState.FINALIZED));
                });
        sessionRepository.findFirstByConfigIdAndStateOrderByClosedAtDesc(config.getId(), PosSessionState.CLOSED)
                .ifPresent(closed -> {
                    card.setLastClosingCash(closed.getClosingCash());
                    card.setLastClosedAt(closed.getClosedAt());
                });
        return card;
    }

    private PosCatalogItemResponse toCatalogItemResponse(ProductResponse product,
                                                         Map<UUID, String> categoryNames,
                                                         CompanyId companyId,
                                                         UUID warehouseId) {
        PosCatalogItemResponse response = new PosCatalogItemResponse();
        response.setProductId(product.getId());
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setBarcode(product.getBarcode());
        response.setUomId(product.getUomId());
        response.setListPrice(product.getListPrice());
        response.setSaleOk(product.isSaleOk());
        response.setCategoryId(product.getCategoryId());
        if (product.getCategoryId() != null) {
            response.setCategoryName(categoryNames.get(product.getCategoryId()));
        }
        response.setImageUrl(product.getImageUrl());
        if (product.getProductType() != null) {
            response.setProductType(product.getProductType().name());
        }
        if (product.getProductType() == ProductType.STOCKABLE) {
            response.setQtyOnHand(stockValuationApplicationService.totalOnHandForWarehouse(
                    companyId, product.getId(), warehouseId));
        }
        return response;
    }

    private PosOrderResponse toOrderResponse(PosOrder entity) {
        PosOrderResponse response = new PosOrderResponse();
        response.setId(entity.getId());
        response.setCompanyId(entity.getCompanyId());
        response.setSessionId(entity.getSessionId());
        response.setCustomerPartnerId(entity.getCustomerPartnerId());
        response.setName(entity.getName());
        response.setState(entity.getState());
        response.setCurrencyCode(entity.getCurrencyCode());
        response.setAmountUntaxed(entity.getAmountUntaxed());
        response.setAmountTax(entity.getAmountTax());
        response.setAmountTotal(entity.getAmountTotal());
        response.setAmountPaid(entity.getAmountPaid());
        response.setAmountDue(entity.getAmountTotal().subtract(entity.getAmountPaid()).max(BigDecimal.ZERO));
        response.setNote(entity.getNote());
        response.setSalesOrderId(entity.getSalesOrderId());
        response.setCustomerInvoiceId(entity.getCustomerInvoiceId());
        response.setReceiptId(entity.getReceiptId());
        response.setFinalizedAt(entity.getFinalizedAt());
        response.setLines(entity.getLines().stream().map(this::toLineResponse).toList());
        response.setPayments(entity.getPayments().stream().map(this::toPaymentResponse).toList());
        return response;
    }

    private PosOrderLineResponse toLineResponse(PosOrderLine entity) {
        PosOrderLineResponse response = new PosOrderLineResponse();
        response.setId(entity.getId());
        response.setProductId(entity.getProductId());
        response.setName(entity.getName());
        response.setUomId(entity.getUomId());
        response.setPackagingId(entity.getPackagingId());
        response.setPackagingName(entity.getPackagingName());
        response.setQtyPerPackage(entity.getQtyPerPackage());
        response.setQuantity(entity.getQuantity());
        response.setUnitPrice(entity.getUnitPrice());
        response.setDiscountPercent(entity.getDiscountPercent());
        response.setSubtotal(entity.getSubtotal());
        response.setTaxAmount(entity.getTaxAmount());
        response.setTotal(entity.getTotal());
        response.setTaxIds(entity.getTaxIds());
        return response;
    }

    private PosPaymentResponse toPaymentResponse(PosPayment entity) {
        PosPaymentResponse response = new PosPaymentResponse();
        response.setId(entity.getId());
        response.setMethod(entity.getMethod());
        response.setJournalId(entity.getJournalId());
        response.setAmount(entity.getAmount());
        response.setReference(entity.getReference());
        response.setPaidAt(entity.getPaidAt());
        return response;
    }

    private PosReceiptResponse toReceiptResponse(PosReceipt entity) {
        PosReceiptResponse response = new PosReceiptResponse();
        response.setId(entity.getId());
        response.setCompanyId(entity.getCompanyId());
        response.setOrderId(entity.getOrderId());
        response.setReceiptNumber(entity.getReceiptNumber());
        response.setPayloadJson(entity.getPayloadJson());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private String receiptJson(PosOrder order, String receiptNumber) {
        PosSession session = null;
        try {
            session = loadSession(order.getSessionId());
        } catch (RuntimeException ignored) {
            // Session may be missing for legacy rows; payload still includes order fields.
        }

        StringBuilder linesJson = new StringBuilder("[");
        boolean firstLine = true;
        for (PosOrderLine line : order.getLines().stream()
                .sorted(Comparator.comparing(PosOrderLine::getSequence))
                .toList()) {
            if (!firstLine) {
                linesJson.append(',');
            }
            firstLine = false;
            String sku = "";
            String barcode = "";
            try {
                ProductResponse product = productApplicationService.getProduct(line.getProductId());
                sku = product.getSku() != null ? product.getSku() : "";
                barcode = product.getBarcode() != null ? product.getBarcode() : "";
            } catch (RuntimeException ignored) {
                // Keep line even if product lookup fails.
            }
            linesJson.append('{')
                    .append("\"productId\":\"").append(line.getProductId()).append("\",")
                    .append("\"name\":\"").append(escape(line.getName())).append("\",")
                    .append("\"sku\":\"").append(escape(sku)).append("\",")
                    .append("\"barcode\":\"").append(escape(barcode)).append("\",")
                    .append("\"quantity\":").append(line.getQuantity()).append(',')
                    .append("\"unitPrice\":").append(line.getUnitPrice()).append(',')
                    .append("\"discountPercent\":").append(defaultZero(line.getDiscountPercent())).append(',')
                    .append("\"subtotal\":").append(line.getSubtotal()).append(',')
                    .append("\"taxAmount\":").append(line.getTaxAmount()).append(',')
                    .append("\"total\":").append(line.getTotal())
                    .append('}');
        }
        linesJson.append(']');

        StringBuilder paymentsJson = new StringBuilder("[");
        boolean firstPayment = true;
        BigDecimal paid = BigDecimal.ZERO;
        for (PosPayment payment : order.getPayments()) {
            if (!firstPayment) {
                paymentsJson.append(',');
            }
            firstPayment = false;
            paid = paid.add(defaultZero(payment.getAmount()));
            paymentsJson.append('{')
                    .append("\"method\":\"").append(escape(payment.getMethod() != null ? payment.getMethod().name() : "")).append("\",")
                    .append("\"amount\":").append(payment.getAmount()).append(',')
                    .append("\"reference\":\"").append(escape(payment.getReference())).append("\",")
                    .append("\"paidAt\":\"").append(payment.getPaidAt() != null ? payment.getPaidAt() : "").append("\"")
                    .append('}');
        }
        paymentsJson.append(']');

        BigDecimal change = paid.subtract(defaultZero(order.getAmountTotal())).max(BigDecimal.ZERO);

        return new StringBuilder(512)
                .append('{')
                .append("\"companyId\":\"").append(order.getCompanyId()).append("\",")
                .append("\"orderId\":\"").append(order.getId()).append("\",")
                .append("\"orderName\":\"").append(escape(order.getName())).append("\",")
                .append("\"name\":\"").append(escape(order.getName())).append("\",")
                .append("\"receiptNumber\":\"").append(escape(receiptNumber)).append("\",")
                .append("\"datetime\":\"").append(order.getFinalizedAt() != null ? order.getFinalizedAt() : Instant.now()).append("\",")
                .append("\"sessionId\":\"").append(order.getSessionId() != null ? order.getSessionId() : "").append("\",")
                .append("\"configId\":\"").append(session != null && session.getConfigId() != null ? session.getConfigId() : "").append("\",")
                .append("\"customerPartnerId\":\"").append(order.getCustomerPartnerId() != null ? order.getCustomerPartnerId() : "").append("\",")
                .append("\"currencyCode\":\"").append(escape(order.getCurrencyCode())).append("\",")
                .append("\"note\":\"").append(escape(order.getNote())).append("\",")
                .append("\"amountUntaxed\":").append(defaultZero(order.getAmountUntaxed())).append(',')
                .append("\"amountTax\":").append(defaultZero(order.getAmountTax())).append(',')
                .append("\"amountTotal\":").append(defaultZero(order.getAmountTotal())).append(',')
                .append("\"amountPaid\":").append(defaultZero(order.getAmountPaid())).append(',')
                .append("\"change\":").append(change).append(',')
                .append("\"salesOrderId\":\"").append(order.getSalesOrderId() != null ? order.getSalesOrderId() : "").append("\",")
                .append("\"customerInvoiceId\":\"").append(order.getCustomerInvoiceId() != null ? order.getCustomerInvoiceId() : "").append("\",")
                .append("\"lines\":").append(linesJson).append(',')
                .append("\"payments\":").append(paymentsJson)
                .append('}')
                .toString();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
