package com.jalaldeveloper.accountingsystem.pos.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockPickingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValidatePickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockValuationApplicationService;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosConfig;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosOrder;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosOrderLine;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosPayment;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosReceipt;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosSession;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository.PosConfigRepository;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository.PosOrderRepository;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository.PosReceiptRepository;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository.PosSessionRepository;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosDomainException;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosOrderState;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosPaymentMethod;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosRules;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosSessionState;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.CheckoutPosOrderCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.ClosePosSessionCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.CreatePosOrderCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.OpenPosSessionCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosCatalogItemResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosConfigCardResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosConfigCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosConfigResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosOrderLineCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosOrderLineResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosOrderResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosPaymentResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosReceiptResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosSessionResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.RegisterPosPaymentCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.UpdatePosOrderLineCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.input.PosApplicationService;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalInvoicePolicy;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderLineCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.input.SalesApplicationService;
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

@Service
public class PosApplicationServiceImpl implements PosApplicationService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PosConfigRepository configRepository;
    private final PosSessionRepository sessionRepository;
    private final PosOrderRepository orderRepository;
    private final PosReceiptRepository receiptRepository;
    private final ProductApplicationService productApplicationService;
    private final SalesApplicationService salesApplicationService;
    private final StockPickingApplicationService stockPickingApplicationService;
    private final StockValuationApplicationService stockValuationApplicationService;
    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;

    public PosApplicationServiceImpl(PosConfigRepository configRepository,
                                     PosSessionRepository sessionRepository,
                                     PosOrderRepository orderRepository,
                                     PosReceiptRepository receiptRepository,
                                     ProductApplicationService productApplicationService,
                                     SalesApplicationService salesApplicationService,
                                     StockPickingApplicationService stockPickingApplicationService,
                                     StockValuationApplicationService stockValuationApplicationService,
                                     CustomerInvoiceApplicationService customerInvoiceApplicationService) {
        this.configRepository = configRepository;
        this.sessionRepository = sessionRepository;
        this.orderRepository = orderRepository;
        this.receiptRepository = receiptRepository;
        this.productApplicationService = productApplicationService;
        this.salesApplicationService = salesApplicationService;
        this.stockPickingApplicationService = stockPickingApplicationService;
        this.stockValuationApplicationService = stockValuationApplicationService;
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
    }

    @Override
    @Transactional
    public PosConfigResponse createConfig(PosConfigCommand command) {
        configRepository.findByCompanyIdAndName(command.getCompanyId(), command.getName())
                .ifPresent(existing -> {
                    throw new PosDomainException("POS config already exists: " + existing.getName());
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
        entity.setCurrencyCode(command.getCurrencyCode());
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
                    throw new PosDomainException("POS config already has an open session");
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
        session.setCurrencyCode(config.getCurrencyCode());
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
        Page<ProductResponse> products = productApplicationService.searchProducts(companyId, query, false, pageable);
        List<PosCatalogItemResponse> saleable = products.stream()
                .filter(ProductResponse::isSaleOk)
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .map(p -> toCatalogItemResponse(p, categoryNames, companyId, session.getWarehouseId()))
                .toList();
        return new PageImpl<>(saleable, pageable, saleable.size());
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
        order.setCurrencyCode(session.getCurrencyCode());
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
        PosOrder order = loadOrder(orderId);
        PosSession session = loadSession(order.getSessionId());
        PosRules.ensureSessionOpen(session.getState());
        PosRules.ensureCanFinalize(order.getState(), order.getAmountTotal(), order.getAmountPaid());

        SalesOrderResponse salesOrder = createAndDeliverSalesOrder(order, session);
        CustomerInvoiceResponse invoice = createAndPostInvoice(order, salesOrder);
        registerAccountingPayments(order, invoice);

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
        return finalizeOrder(paid.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PosOrderResponse getOrder(UUID orderId) {
        return toOrderResponse(loadOrder(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public PosReceiptResponse getReceipt(UUID receiptId) {
        return toReceiptResponse(receiptRepository.findById(receiptId)
                .orElseThrow(() -> new PosDomainException("POS receipt not found: " + receiptId)));
    }

    private SalesOrderResponse createAndDeliverSalesOrder(PosOrder order, PosSession session) {
        CreateSalesOrderCommand command = new CreateSalesOrderCommand();
        command.setCompanyId(order.getCompanyId());
        command.setCustomerPartnerId(order.getCustomerPartnerId());
        command.setCurrencyCode(order.getCurrencyCode());
        command.setWarehouseId(session.getWarehouseId());
        command.setPricelistId(session.getPricelistId());
        command.setOrderDate(LocalDate.now());
        command.setNotes("POS " + order.getName());
        List<SalesOrderLineCommand> salesLines = new ArrayList<>();
        for (PosOrderLine line : order.getLines()) {
            SalesOrderLineCommand salesLine = new SalesOrderLineCommand();
            salesLine.setProductId(line.getProductId());
            salesLine.setName(line.getName());
            salesLine.setUomId(line.getUomId());
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
        for (UUID pickingId : confirmed.getDeliveryPickingIds()) {
            stockPickingApplicationService.confirmPicking(pickingId);
            stockPickingApplicationService.assignPicking(pickingId);
            ValidatePickingCommand validate = new ValidatePickingCommand();
            validate.setCreateBackorder(false);
            stockPickingApplicationService.validatePicking(pickingId, validate);
        }
        return salesApplicationService.getSalesOrder(created.getId());
    }

    private CustomerInvoiceResponse createAndPostInvoice(PosOrder order, SalesOrderResponse salesOrder) {
        CreateCustomerInvoiceFromSalesOrderCommand command = new CreateCustomerInvoiceFromSalesOrderCommand();
        command.setCompanyId(order.getCompanyId());
        command.setSalesOrderId(salesOrder.getId());
        command.setInvoiceDate(LocalDate.now());
        command.setDueDate(LocalDate.now());
        command.setReference(order.getName());
        CustomerInvoiceResponse draftInvoice = salesApplicationService.createCustomerInvoiceFromSalesOrder(command);
        return customerInvoiceApplicationService.postCustomerInvoice(draftInvoice.getId());
    }

    private void registerAccountingPayments(PosOrder order, CustomerInvoiceResponse invoice) {
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
            command.setPaymentDate(LocalDate.now());
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
        receipt.setReceiptNumber(nextReceiptNumber(order.getCompanyId()));
        receipt.setPayloadJson(receiptJson(order));
        receipt.setCreatedAt(Instant.now());
        return receiptRepository.save(receipt);
    }

    private PosOrderLine toLineEntity(PosOrder order, PosOrderLineCommand command, int sequence) {
        ProductResponse product = productApplicationService.getProduct(command.getProductId());
        if (!product.isSaleOk()) {
            throw new PosDomainException("Product is not saleable: " + product.getName());
        }
        PosOrderLine line = new PosOrderLine();
        line.setId(UUID.randomUUID());
        line.setOrder(order);
        line.setSequence(sequence);
        line.setProductId(product.getId());
        line.setName(command.getName() != null && !command.getName().isBlank() ? command.getName() : product.getName());
        line.setUomId(command.getUomId() != null ? command.getUomId() : product.getUomId());
        line.setQuantity(command.getQuantity());
        line.setUnitPrice(command.getUnitPrice() != null ? command.getUnitPrice() : defaultZero(product.getListPrice()));
        line.setDiscountPercent(defaultZero(command.getDiscountPercent()));
        line.setTaxIds(command.getTaxIds());
        line.setRevenueAccountId(command.getRevenueAccountId());
        recalcLine(line);
        return line;
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
        line.setTaxAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        line.setTotal(line.getSubtotal().add(line.getTaxAmount()));
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
        throw new PosDomainException("A bank/card journal is required for this POS payment");
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
            throw new PosDomainException("POS company mismatch");
        }
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
        response.setCurrencyCode(entity.getCurrencyCode());
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
        response.setCurrencyCode(entity.getCurrencyCode());
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
        card.setCurrencyCode(config.getCurrencyCode());
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

    private String receiptJson(PosOrder order) {
        return """
                {"orderId":"%s","name":"%s","currencyCode":"%s","amountTotal":%s,"amountPaid":%s,"salesOrderId":"%s","customerInvoiceId":"%s"}
                """.formatted(order.getId(), escape(order.getName()), order.getCurrencyCode(), order.getAmountTotal(), order.getAmountPaid(),
                order.getSalesOrderId(), order.getCustomerInvoiceId()).trim();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
