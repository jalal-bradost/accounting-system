package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.domain.valueobject.DiscountMath;
import com.bradox.erp.domain.valueobject.DiscountType;
import com.bradox.erp.domain.valueobject.MonetaryScale;
import com.bradox.erp.accounting.service.domain.CurrencyMath;
import com.bradox.erp.accounting.service.domain.JournalEntryTiming;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.AllocateCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CorrectCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.RefundCustomerCreditCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.SalesOrderInvoiceSyncPort;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.event.CustomerInvoicePostedEvent;
import com.bradox.erp.accounting.service.domain.ports.output.messaging.AccountingEventPublisher;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerInvoiceRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalRepository;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.core.entity.CustomerInvoice;
import com.bradox.erp.domain.core.entity.CustomerInvoiceLine;
import com.bradox.erp.domain.core.entity.CustomerInvoiceLineTax;
import com.bradox.erp.domain.core.entity.Journal;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.platform.document.DocumentSequenceService;
import com.bradox.erp.platform.web.CompanyContext;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class CustomerInvoiceApplicationServiceImpl implements CustomerInvoiceApplicationService {

    private static final String DEFAULT_AR_ACCOUNT_CODE = "430003";
    private static final String DEFAULT_REVENUE_ACCOUNT_CODE = "430005";
    private static final String SALES_DISCOUNT_ACCOUNT_CODE = "430006";
    private static final String GIFT_EXPENSE_ACCOUNT_CODE = "430008";
    private static final String SALE_JOURNAL_CODE = "430003";

    private final CustomerInvoiceRepository invoiceRepository;
    private final CustomerPaymentService customerPaymentService;
    private final PartnerApplicationService partnerApplicationService;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final JournalRepository journalRepository;
    private final AccountRepository accountRepository;
    private final PeriodPostingGuard periodPostingGuard;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final ObjectProvider<SalesOrderInvoiceSyncPort> salesOrderInvoiceSyncPortProvider;
    private final AccountingEventPublisher accountingEventPublisher;
    private final CurrencyConversionPort currencyConversionPort;
    private final DocumentSequenceService documentSequenceService;
    private final RecordActivityLogger activityLogger;

    public CustomerInvoiceApplicationServiceImpl(CustomerInvoiceRepository invoiceRepository,
                                                 CustomerPaymentService customerPaymentService,
                                                 PartnerApplicationService partnerApplicationService,
                                                 JournalEntryApplicationService journalEntryApplicationService,
                                                 JournalRepository journalRepository,
                                                 AccountRepository accountRepository,
                                                 PeriodPostingGuard periodPostingGuard,
                                                 ObjectProvider<CompanyContext> companyContextProvider,
                                                 ObjectProvider<SalesOrderInvoiceSyncPort> salesOrderInvoiceSyncPortProvider,
                                                 AccountingEventPublisher accountingEventPublisher,
                                                 CurrencyConversionPort currencyConversionPort,
                                                 DocumentSequenceService documentSequenceService,
                                                 RecordActivityLogger activityLogger) {
        this.invoiceRepository = invoiceRepository;
        this.customerPaymentService = customerPaymentService;
        this.partnerApplicationService = partnerApplicationService;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.journalRepository = journalRepository;
        this.accountRepository = accountRepository;
        this.periodPostingGuard = periodPostingGuard;
        this.companyContextProvider = companyContextProvider;
        this.salesOrderInvoiceSyncPortProvider = salesOrderInvoiceSyncPortProvider;
        this.accountingEventPublisher = accountingEventPublisher;
        this.currencyConversionPort = currencyConversionPort;
        this.documentSequenceService = documentSequenceService;
        this.activityLogger = activityLogger;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPostedInvoiceForSalesOrder(UUID salesOrderId) {
        if (salesOrderId == null) {
            return false;
        }
        return invoiceRepository.existsBySalesOrderIdAndState(salesOrderId, CustomerInvoiceState.POSTED);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> remainingCreditableQtyByInvoiceLine(UUID invoiceId) {
        CustomerInvoice source = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        Map<UUID, BigDecimal> credited = creditedQtyBySourceInvoiceLine(source);
        Map<UUID, BigDecimal> remaining = new LinkedHashMap<>();
        for (CustomerInvoiceLine line : source.getLines()) {
            BigDecimal left = line.getQty().subtract(credited.getOrDefault(line.getId(), BigDecimal.ZERO));
            remaining.put(line.getId(), left.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP));
        }
        return remaining;
    }

    @Override
    @Transactional
    public void deleteDraftCustomerInvoice(UUID invoiceId) {
        CustomerInvoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        if (inv.getState() != CustomerInvoiceState.DRAFT || inv.isOpeningBalance()) {
            throw new AccountingDomainException("error.accounting.onlyDraftInvoiceCanBeDeleted", null,
                    "Only draft invoices and credit notes can be deleted");
        }
        invoiceRepository.deleteById(invoiceId);
        activityLogger.log(inv.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, invoiceId,
                "Draft deleted: " + (inv.getReference() != null ? inv.getReference() : invoiceId));
    }

    @Override
    @Transactional
    public int deleteDraftDocumentsForSalesOrder(UUID salesOrderId) {
        if (salesOrderId == null) {
            return 0;
        }
        int deleted = 0;
        for (CustomerInvoice inv : invoiceRepository.findBySalesOrderIdWithLines(salesOrderId)) {
            if (inv.getState() == CustomerInvoiceState.DRAFT && !inv.isOpeningBalance()) {
                deleteDraftCustomerInvoice(inv.getId());
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> draftAllocatedQtyBySalesOrderLine(UUID salesOrderId) {
        return draftAllocatedQtyBySalesOrderLine(salesOrderId, CustomerInvoiceMoveType.INVOICE, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> draftAllocatedChargeQtyBySalesOrderLine(UUID salesOrderId) {
        return draftAllocatedQtyBySalesOrderLine(salesOrderId, CustomerInvoiceMoveType.INVOICE, Boolean.FALSE);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> draftCreditNoteAllocatedQtyBySalesOrderLine(UUID salesOrderId) {
        return draftAllocatedQtyBySalesOrderLine(salesOrderId, CustomerInvoiceMoveType.CREDIT_NOTE, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> draftCreditNoteAllocatedChargeQtyBySalesOrderLine(UUID salesOrderId) {
        return draftAllocatedQtyBySalesOrderLine(salesOrderId, CustomerInvoiceMoveType.CREDIT_NOTE, Boolean.FALSE);
    }

    @Override
    @Transactional(readOnly = true)
    public PostedSoLineQtyNets postedQtyNetsBySalesOrderLine(UUID salesOrderId) {
        if (salesOrderId == null) {
            return new PostedSoLineQtyNets(Map.of(), Map.of());
        }
        Map<UUID, BigDecimal> charge = new LinkedHashMap<>();
        Map<UUID, BigDecimal> gift = new LinkedHashMap<>();
        for (CustomerInvoice inv : invoiceRepository.findBySalesOrderIdWithLines(salesOrderId)) {
            if (inv.getState() != CustomerInvoiceState.POSTED) {
                continue;
            }
            CustomerInvoiceMoveType type = inv.getMoveType() != null ? inv.getMoveType() : CustomerInvoiceMoveType.INVOICE;
            boolean credit = type == CustomerInvoiceMoveType.CREDIT_NOTE;
            for (CustomerInvoiceLine line : inv.getLines()) {
                if (line.getSalesOrderLineId() == null || line.getQty() == null) {
                    continue;
                }
                BigDecimal signed = credit ? line.getQty().negate() : line.getQty();
                if (line.isGift()) {
                    gift.merge(line.getSalesOrderLineId(), signed, BigDecimal::add);
                } else {
                    charge.merge(line.getSalesOrderLineId(), signed, BigDecimal::add);
                }
            }
        }
        charge.replaceAll((id, qty) -> qty.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP));
        gift.replaceAll((id, qty) -> qty.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP));
        return new PostedSoLineQtyNets(charge, gift);
    }

    /**
     * @param giftFilter null = all lines; true = gift only; false = charge (non-gift) only
     */
    private Map<UUID, BigDecimal> draftAllocatedQtyBySalesOrderLine(
            UUID salesOrderId, CustomerInvoiceMoveType moveType, Boolean giftFilter) {
        if (salesOrderId == null) {
            return Map.of();
        }
        Map<UUID, BigDecimal> allocated = new LinkedHashMap<>();
        for (CustomerInvoice inv : invoiceRepository.findBySalesOrderIdWithLines(salesOrderId)) {
            if (inv.getState() != CustomerInvoiceState.DRAFT) {
                continue;
            }
            CustomerInvoiceMoveType type = inv.getMoveType() != null ? inv.getMoveType() : CustomerInvoiceMoveType.INVOICE;
            if (type != moveType) {
                continue;
            }
            for (CustomerInvoiceLine line : inv.getLines()) {
                if (line.getSalesOrderLineId() == null) {
                    continue;
                }
                if (giftFilter != null && line.isGift() != giftFilter) {
                    continue;
                }
                allocated.merge(line.getSalesOrderLineId(), line.getQty(), BigDecimal::add);
            }
        }
        return allocated;
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) {
            return fromCommand;
        }
        return companyContextProvider.getObject().requireCompany().getId();
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCustomerInvoice(CreateCustomerInvoiceCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PartnerResponse customer = partnerApplicationService.getPartner(command.getCustomerPartnerId());
        if (!customer.isCustomer()) {
            throw new AccountingDomainException("error.accounting.partnerNotCustomer", null, "Partner is not a customer");
        }
        if (!customer.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("error.accounting.customerCompanyMismatch", null, "Customer belongs to another company");
        }
        periodPostingGuard.assertDatePostable(companyId, command.getInvoiceDate());
        UUID defaultRevenue = accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), DEFAULT_REVENUE_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.defaultRevenueAccountNotFound", null, "Default revenue account not found"))
                .getId().getId();

        Instant now = Instant.now();
        CustomerInvoice inv = new CustomerInvoice();
        inv.setId(UUID.randomUUID());
        inv.setCompanyId(companyId);
        inv.setCustomerPartnerId(command.getCustomerPartnerId());
        inv.setInvoiceDate(command.getInvoiceDate());
        inv.setDueDate(command.getDueDate());
        String invoiceReference = command.getReference();
        if (invoiceReference == null || invoiceReference.isBlank()) {
            invoiceReference = documentSequenceService.next(companyId, "INV");
        }
        inv.setReference(invoiceReference);
        inv.setCurrencyCode(command.getCurrencyCode());
        inv.setSalesOrderId(command.getSalesOrderId());
        inv.setReversedInvoiceId(command.getReversedInvoiceId());
        CustomerInvoiceMoveType moveType = CustomerInvoiceMoveType.INVOICE;
        if (command.getMoveType() != null && !command.getMoveType().isBlank()) {
            try {
                moveType = CustomerInvoiceMoveType.valueOf(command.getMoveType().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new AccountingDomainException("error.accounting.invalidMoveType", new Object[] { command.getMoveType() }, "Invalid moveType: " + command.getMoveType());
            }
        }
        inv.setMoveType(moveType);
        inv.setExchangeRateToCompany(resolveExchangeRate(
                companyId, command.getCurrencyCode(), command.getInvoiceDate(), command.getExchangeRateToCompany()));
        inv.setOrderDiscountAmount(command.getOrderDiscountAmount() != null
                ? command.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO);
        inv.setState(CustomerInvoiceState.DRAFT);
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        inv.setRowVersion(0L);

        int seq = 0;
        for (CustomerInvoiceLineCommand lc : command.getLines()) {
            boolean gift = Boolean.TRUE.equals(lc.getIsGift());
            if (lc.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AccountingDomainException("error.accounting.lineUnitPricePositive", null, "Line unit price must be positive");
            }
            UUID revAcc = lc.getRevenueAccountId() != null ? lc.getRevenueAccountId() : defaultRevenue;
            CustomerInvoiceLine line = new CustomerInvoiceLine();
            line.setId(UUID.randomUUID());
            line.setSequence(++seq);
            line.setName(lc.getName());
            line.setQty(lc.getQty().setScale(4, RoundingMode.HALF_UP));
            line.setUnitPrice(lc.getUnitPrice().setScale(4, RoundingMode.HALF_UP));
            line.setDiscountType(gift ? DiscountType.PERCENT : lc.getDiscountType());
            line.setDiscountValue(gift ? BigDecimal.ZERO
                    : (lc.getDiscountValue() != null ? lc.getDiscountValue() : lc.getDiscountPercent()));
            line.setDiscountPercent(DiscountMath.effectivePercent(
                    lc.getQty().multiply(lc.getUnitPrice()), line.getDiscountType(), line.getDiscountValue()));
            line.setSalesOrderLineId(lc.getSalesOrderLineId());
            line.setRevenueAccountId(revAcc);
            line.setGift(gift);
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            if (!gift) {
                for (CustomerInvoiceLineTaxCommand ts : lc.getTaxSnapshots()) {
                    CustomerInvoiceLineTax te = new CustomerInvoiceLineTax();
                    te.setId(UUID.randomUUID());
                    te.setTaxId(ts.getTaxId());
                    te.setTaxName(ts.getTaxName());
                    te.setTaxBase(ts.getTaxBase().setScale(4, RoundingMode.HALF_UP));
                    te.setTaxAmount(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
                    te.setAccountId(ts.getAccountId());
                    line.getTaxSnapshots().add(te);
                }
            }
            inv.getLines().add(line);
        }
        CustomerInvoice saved = invoiceRepository.save(inv);
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, saved.getId(),
                "Customer Invoice created");
        if (saved.getSalesOrderId() != null) {
            activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getSalesOrderId(),
                    "Customer invoice created: " + (saved.getReference() != null ? saved.getReference() : saved.getId()));
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCreditNoteFromInvoice(UUID invoiceId, CreateCreditNoteFromInvoiceCommand command) {
        CustomerInvoice source = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        if (!source.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("error.accounting.invoiceCompanyMismatch", null, "Invoice company mismatch");
        }
        if (source.getState() != CustomerInvoiceState.POSTED) {
            throw new AccountingDomainException("error.accounting.onlyPostedInvoiceCanBeCredited", null, "Only posted invoices can be credited");
        }
        if (source.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE) {
            throw new AccountingDomainException("error.accounting.cannotCreditNoteFromCreditNote", null, "Cannot create a credit note from another credit note");
        }

        periodPostingGuard.assertDatePostable(companyId, command.getInvoiceDate());

        Map<UUID, BigDecimal> qtyByLineId = new LinkedHashMap<>();
        if (command.getLines() == null || command.getLines().isEmpty()) {
            for (CustomerInvoiceLine line : source.getLines()) {
                qtyByLineId.put(line.getId(), line.getQty());
            }
        } else {
            for (CreateCreditNoteFromInvoiceCommand.CreditNoteLineQtyCommand lc : command.getLines()) {
                qtyByLineId.merge(lc.getInvoiceLineId(), lc.getQty(), BigDecimal::add);
            }
        }

        Map<UUID, BigDecimal> alreadyCredited = creditedQtyBySourceInvoiceLine(source);
        for (CustomerInvoiceLine srcLine : source.getLines()) {
            BigDecimal qty = qtyByLineId.get(srcLine.getId());
            if (qty == null || qty.signum() <= 0) {
                continue;
            }
            BigDecimal prior = alreadyCredited.getOrDefault(srcLine.getId(), BigDecimal.ZERO);
            BigDecimal remaining = srcLine.getQty().subtract(prior).setScale(4, RoundingMode.HALF_UP);
            if (qty.compareTo(remaining) > 0) {
                throw new AccountingDomainException(
                        "Credit qty " + qty + " exceeds remaining creditable qty " + remaining
                                + " on invoice line " + srcLine.getName()
                                + " (already credited " + prior + " of " + srcLine.getQty() + ")");
            }
        }

        CreateCustomerInvoiceCommand create = new CreateCustomerInvoiceCommand();
        create.setCompanyId(companyId);
        create.setCustomerPartnerId(source.getCustomerPartnerId());
        create.setInvoiceDate(command.getInvoiceDate());
        create.setDueDate(command.getDueDate());
        create.setCurrencyCode(source.getCurrencyCode());
        create.setReference(command.getReference() != null ? command.getReference()
                : "CN/" + (source.getReference() != null ? source.getReference() : source.getId()));
        create.setSalesOrderId(source.getSalesOrderId());
        create.setExchangeRateToCompany(source.getExchangeRateToCompany());
        create.setMoveType(CustomerInvoiceMoveType.CREDIT_NOTE.name());
        create.setReversedInvoiceId(source.getId());

        List<CustomerInvoiceLineCommand> lines = new ArrayList<>();
        for (CustomerInvoiceLine srcLine : source.getLines()) {
            BigDecimal qty = qtyByLineId.get(srcLine.getId());
            if (qty == null || qty.signum() <= 0) {
                continue;
            }
            BigDecimal ratio = qty.divide(srcLine.getQty(), 8, RoundingMode.HALF_UP);
            CustomerInvoiceLineCommand lc = new CustomerInvoiceLineCommand();
            lc.setName(srcLine.getName());
            lc.setQty(qty.setScale(4, RoundingMode.HALF_UP));
            lc.setUnitPrice(srcLine.getUnitPrice());
            lc.setDiscountType(srcLine.getDiscountType());
            lc.setDiscountValue(srcLine.getDiscountType() == DiscountType.FIXED
                    ? srcLine.getDiscountValue().multiply(ratio).setScale(4, RoundingMode.HALF_UP)
                    : srcLine.getDiscountValue());
            lc.setRevenueAccountId(srcLine.getRevenueAccountId());
            lc.setSalesOrderLineId(srcLine.getSalesOrderLineId());
            lc.setIsGift(srcLine.isGift());
            for (CustomerInvoiceLineTax tax : srcLine.getTaxSnapshots()) {
                CustomerInvoiceLineTaxCommand ts = new CustomerInvoiceLineTaxCommand();
                ts.setTaxId(tax.getTaxId());
                ts.setTaxName(tax.getTaxName());
                ts.setTaxBase(tax.getTaxBase().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                ts.setTaxAmount(tax.getTaxAmount().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                ts.setAccountId(tax.getAccountId());
                lc.getTaxSnapshots().add(ts);
            }
            lines.add(lc);
        }
        if (lines.isEmpty()) {
            throw new AccountingDomainException("error.accounting.creditNoteNoLines", null, "Credit note has no lines");
        }
        BigDecimal sourceSubtotal = BigDecimal.ZERO;
        for (CustomerInvoiceLine srcLine : source.getLines()) {
            if (!srcLine.isGift()) {
                sourceSubtotal = sourceSubtotal.add(CustomerInvoiceMath.lineNet(srcLine));
            }
        }
        BigDecimal cnSubtotal = BigDecimal.ZERO;
        for (CustomerInvoiceLineCommand lc : lines) {
            if (Boolean.TRUE.equals(lc.getIsGift())) {
                continue;
            }
            cnSubtotal = cnSubtotal.add(DiscountMath.lineNet(
                    lc.getQty(), lc.getUnitPrice(), lc.getDiscountType(),
                    lc.getDiscountValue() != null ? lc.getDiscountValue() : lc.getDiscountPercent()));
        }
        BigDecimal sourceOrderDisc = source.getOrderDiscountAmount() != null
                ? source.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (sourceSubtotal.signum() > 0 && sourceOrderDisc.signum() > 0 && cnSubtotal.signum() > 0) {
            create.setOrderDiscountAmount(sourceOrderDisc.multiply(cnSubtotal)
                    .divide(sourceSubtotal, 4, RoundingMode.HALF_UP)
                    .min(cnSubtotal));
        }
        create.setLines(lines);
        return createCustomerInvoice(create);
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse postCustomerInvoice(UUID invoiceId) {
        CustomerInvoice inv = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        if (inv.getState() != CustomerInvoiceState.DRAFT) {
            throw new AccountingDomainException("error.accounting.invoiceNotDraft", null, "Invoice is not draft");
        }
        if (inv.getSalesOrderId() != null && !inv.isOpeningBalance()) {
            SalesOrderInvoiceSyncPort salesCheck = salesOrderInvoiceSyncPortProvider.getIfAvailable();
            if (salesCheck != null) {
                List<SalesOrderInvoiceSyncPort.DocumentLine> docLines = new ArrayList<>();
                for (CustomerInvoiceLine line : inv.getLines()) {
                    docLines.add(new SalesOrderInvoiceSyncPort.DocumentLine(
                            line.getSalesOrderLineId(), line.getQty(), line.getUnitPrice(), line.isGift()));
                }
                salesCheck.assertDocumentFitsOrder(inv.getSalesOrderId(),
                        inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE, docLines);
            }
        }

        BigDecimal rate = resolveExchangeRate(
                inv.getCompanyId(), inv.getCurrencyCode(), inv.getInvoiceDate(), inv.getExchangeRateToCompany());
        inv.setExchangeRateToCompany(rate);

        // Gross revenue / gift expense with discounts on the Sales Discount account so discounts
        // remain visible. Gift lines hit Gift Expense and never Accounts Receivable.
        boolean creditNote = inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE;
        List<JournalItemCommand> items = new ArrayList<>();
        BigDecimal arTotalComp = BigDecimal.ZERO;
        BigDecimal arDoc = BigDecimal.ZERO;

        UUID salesDiscountAccount = accountRepository
                .findByCompanyIdAndCode(new CompanyId(inv.getCompanyId()), SALES_DISCOUNT_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "error.accounting.salesDiscountAccountNotFound", null, "Sales discount account not found"))
                .getId().getId();
        UUID giftExpenseAccount = accountRepository
                .findByCompanyIdAndCode(new CompanyId(inv.getCompanyId()), GIFT_EXPENSE_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "error.accounting.giftExpenseAccountNotFound", null, "Gift expense account not found"))
                .getId().getId();

        for (CustomerInvoiceLine line : inv.getLines()) {
            BigDecimal grossDoc = CustomerInvoiceMath.lineGross(line).setScale(4, RoundingMode.HALF_UP);
            BigDecimal grossComp = CurrencyMath.convertAtRate(grossDoc, rate);
            BigDecimal discDoc = CustomerInvoiceMath.lineDiscount(line).setScale(4, RoundingMode.HALF_UP);
            BigDecimal discComp = CurrencyMath.convertAtRate(discDoc, rate);
            BigDecimal lineNetDoc = CustomerInvoiceMath.lineNet(line).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineNetComp = CurrencyMath.convertAtRate(lineNetDoc, rate);

            if (line.isGift()) {
                if (grossComp.signum() > 0) {
                    if (creditNote) {
                        items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), grossComp, BigDecimal.ZERO,
                                inv.getCurrencyCode(), grossDoc, null));
                        items.add(new JournalItemCommand(giftExpenseAccount, "Gift: " + line.getName(), BigDecimal.ZERO, grossComp,
                                inv.getCurrencyCode(), grossDoc.negate(), null));
                    } else {
                        items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), BigDecimal.ZERO, grossComp,
                                inv.getCurrencyCode(), grossDoc.negate(), null));
                        items.add(new JournalItemCommand(giftExpenseAccount, "Gift: " + line.getName(), grossComp, BigDecimal.ZERO,
                                inv.getCurrencyCode(), grossDoc, null));
                    }
                }
                continue;
            }

            if (grossComp.signum() > 0) {
                if (creditNote) {
                    items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), grossComp, BigDecimal.ZERO,
                            inv.getCurrencyCode(), grossDoc, null));
                } else {
                    items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), BigDecimal.ZERO, grossComp,
                            inv.getCurrencyCode(), grossDoc.negate(), null));
                }
            }
            if (discComp.signum() > 0) {
                if (creditNote) {
                    items.add(new JournalItemCommand(salesDiscountAccount, "Discount: " + line.getName(), BigDecimal.ZERO, discComp,
                            inv.getCurrencyCode(), discDoc.negate(), null));
                } else {
                    items.add(new JournalItemCommand(salesDiscountAccount, "Discount: " + line.getName(), discComp, BigDecimal.ZERO,
                            inv.getCurrencyCode(), discDoc, null));
                }
            }
            if (lineNetComp.signum() > 0) {
                arTotalComp = arTotalComp.add(lineNetComp);
                arDoc = arDoc.add(lineNetDoc);
            }
            for (CustomerInvoiceLineTax ts : line.getTaxSnapshots()) {
                BigDecimal taxDoc = ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP);
                BigDecimal taxComp = CurrencyMath.convertAtRate(taxDoc, rate);
                if (taxComp.signum() <= 0) {
                    continue;
                }
                arTotalComp = arTotalComp.add(taxComp);
                arDoc = arDoc.add(taxDoc);
                if (creditNote) {
                    items.add(new JournalItemCommand(ts.getAccountId(), ts.getTaxName(), taxComp, BigDecimal.ZERO,
                            inv.getCurrencyCode(), taxDoc, null));
                } else {
                    items.add(new JournalItemCommand(ts.getAccountId(), ts.getTaxName(), BigDecimal.ZERO, taxComp,
                            inv.getCurrencyCode(), taxDoc.negate(), null));
                }
            }
        }
        BigDecimal orderDiscDoc = inv.getOrderDiscountAmount() != null
                ? inv.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (orderDiscDoc.signum() > 0) {
            BigDecimal untaxedDoc = BigDecimal.ZERO;
            for (CustomerInvoiceLine line : inv.getLines()) {
                if (!line.isGift()) {
                    untaxedDoc = untaxedDoc.add(CustomerInvoiceMath.lineNet(line));
                }
            }
            BigDecimal discDoc = orderDiscDoc.min(untaxedDoc);
            BigDecimal discComp = CurrencyMath.convertAtRate(discDoc, rate);
            if (discComp.signum() > 0) {
                if (creditNote) {
                    items.add(new JournalItemCommand(salesDiscountAccount, "Order discount", BigDecimal.ZERO, discComp,
                            inv.getCurrencyCode(), discDoc.negate(), null));
                } else {
                    items.add(new JournalItemCommand(salesDiscountAccount, "Order discount", discComp, BigDecimal.ZERO,
                            inv.getCurrencyCode(), discDoc, null));
                }
                arTotalComp = arTotalComp.subtract(discComp).max(BigDecimal.ZERO);
                arDoc = arDoc.subtract(discDoc).max(BigDecimal.ZERO);
            }
        }
        if (arTotalComp.signum() > 0) {
            PartnerResponse customer = partnerApplicationService.getPartner(inv.getCustomerPartnerId());
            UUID receivableAccount = customer.getReceivableAccountId() != null
                    ? customer.getReceivableAccountId()
                    : accountRepository.findByCompanyIdAndCode(new CompanyId(inv.getCompanyId()), DEFAULT_AR_ACCOUNT_CODE)
                    .orElseThrow(() -> new AccountingDomainException("error.accounting.defaultArAccountNotFound", null, "Default AR account not found"))
                    .getId().getId();
            if (creditNote) {
                items.add(0, new JournalItemCommand(receivableAccount, "Accounts receivable", BigDecimal.ZERO, arTotalComp,
                        inv.getCurrencyCode(), arDoc.negate(), inv.getCustomerPartnerId()));
            } else {
                items.add(0, new JournalItemCommand(receivableAccount, "Accounts receivable", arTotalComp, BigDecimal.ZERO,
                        inv.getCurrencyCode(), arDoc, inv.getCustomerPartnerId()));
            }
        }

        if (!items.isEmpty()) {
            Journal saleJournal = journalRepository.findByCompanyIdAndCode(new CompanyId(inv.getCompanyId()), SALE_JOURNAL_CODE)
                    .orElseThrow(() -> new AccountingDomainException("error.accounting.saleJournalNotFound", null, "Sale journal not found"));
            CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                    inv.getCompanyId(),
                    saleJournal.getId().getId(),
                    "",
                    JournalEntryTiming.ofBusinessDate(inv.getInvoiceDate()),
                    inv.getCurrencyCode(),
                    inv.getCustomerPartnerId(),
                    items);
            CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(jcmd);
            journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());
            inv.setJournalEntryId(created.getJournalEntryId());
        } else {
            inv.setJournalEntryId(null);
        }
        inv.setState(CustomerInvoiceState.POSTED);
        inv.setUpdatedAt(Instant.now());
        CustomerInvoice saved = invoiceRepository.save(inv);

        if (saved.getSalesOrderId() != null) {
            SalesOrderInvoiceSyncPort salesSync = salesOrderInvoiceSyncPortProvider.getIfAvailable();
            if (salesSync != null) {
                Map<UUID, BigDecimal> qtyByLine = new LinkedHashMap<>();
                BigDecimal sign = creditNote ? BigDecimal.ONE.negate() : BigDecimal.ONE;
                for (CustomerInvoiceLine line : saved.getLines()) {
                    if (line.getSalesOrderLineId() != null) {
                        qtyByLine.merge(line.getSalesOrderLineId(), line.getQty().multiply(sign), BigDecimal::add);
                    }
                }
                if (!qtyByLine.isEmpty()) {
                    salesSync.applyPostedInvoiceQuantities(saved.getSalesOrderId(), qtyByLine);
                }
            }
        }
        accountingEventPublisher.publishCustomerInvoicePosted(new CustomerInvoicePostedEvent(
                UUID.randomUUID(),
                Instant.now(),
                saved.getCompanyId(),
                saved.getId(),
                saved.getCustomerPartnerId()));

        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, saved.getId(),
                "Draft → Posted (Status)");
        if (saved.getSalesOrderId() != null) {
            activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_SALES_ORDER, saved.getSalesOrderId(),
                    "Customer invoice posted: " + (saved.getReference() != null ? saved.getReference() : saved.getId()));
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createOpeningCustomerInvoice(UUID companyId, UUID partnerId, BigDecimal amount,
                                                                String currency, LocalDate date, LocalDate dueDate,
                                                                String reference, UUID openingJournalId,
                                                                UUID openingEquityAccountId) {
        if (amount == null || amount.signum() <= 0) {
            throw new AccountingDomainException("error.accounting.openingAmountPositive", null,
                    "Opening invoice amount must be positive");
        }
        PartnerResponse customer = partnerApplicationService.getPartner(partnerId);
        if (!customer.isCustomer()) {
            throw new AccountingDomainException("error.accounting.partnerNotCustomer", null, "Partner is not a customer");
        }
        if (!companyId.equals(customer.getCompanyId())) {
            throw new AccountingDomainException("error.accounting.customerCompanyMismatch", null, "Customer belongs to another company");
        }
        periodPostingGuard.assertDatePostable(companyId, date);
        String currencyCode = currency != null ? currency.trim().toUpperCase() : null;
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new AccountingDomainException("error.accounting.paymentCurrencyRequired", null, "Currency is required");
        }
        BigDecimal scaled = amount.setScale(4, RoundingMode.HALF_UP);
        BigDecimal rate = resolveExchangeRate(companyId, currencyCode, date, null);
        BigDecimal companyAmount = CurrencyMath.convertAtRate(scaled, rate);

        Instant now = Instant.now();
        CustomerInvoice inv = new CustomerInvoice();
        inv.setId(UUID.randomUUID());
        inv.setCompanyId(companyId);
        inv.setCustomerPartnerId(partnerId);
        inv.setInvoiceDate(date);
        inv.setDueDate(dueDate != null ? dueDate : date);
        inv.setReference(reference != null && !reference.isBlank()
                ? reference.trim()
                : documentSequenceService.next(companyId, "OB-INV"));
        inv.setCurrencyCode(currencyCode);
        inv.setMoveType(CustomerInvoiceMoveType.INVOICE);
        inv.setExchangeRateToCompany(rate);
        inv.setOrderDiscountAmount(BigDecimal.ZERO);
        inv.setOpeningBalance(true);
        inv.setState(CustomerInvoiceState.DRAFT);
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        inv.setRowVersion(0L);

        CustomerInvoiceLine line = new CustomerInvoiceLine();
        line.setId(UUID.randomUUID());
        line.setSequence(1);
        line.setName("Opening balance");
        line.setQty(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        line.setUnitPrice(scaled);
        line.setDiscountType(DiscountType.PERCENT);
        line.setDiscountValue(BigDecimal.ZERO);
        line.setDiscountPercent(BigDecimal.ZERO);
        line.setRevenueAccountId(openingEquityAccountId);
        line.setGift(false);
        line.setCreatedAt(now);
        line.setUpdatedAt(now);
        inv.getLines().add(line);

        UUID receivableAccount = customer.getReceivableAccountId() != null
                ? customer.getReceivableAccountId()
                : accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), DEFAULT_AR_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "error.accounting.defaultArAccountNotFound", null, "Default AR account not found"))
                .getId().getId();

        List<JournalItemCommand> items = List.of(
                new JournalItemCommand(receivableAccount, "Accounts receivable", companyAmount, BigDecimal.ZERO,
                        currencyCode, scaled, partnerId),
                new JournalItemCommand(openingEquityAccountId, "Opening balance", BigDecimal.ZERO, companyAmount,
                        currencyCode, scaled.negate(), null));
        CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(
                new CreateJournalEntryCommand(companyId, openingJournalId, "",
                        JournalEntryTiming.ofBusinessDate(date), currencyCode, partnerId, items));
        journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());

        inv.setJournalEntryId(created.getJournalEntryId());
        inv.setState(CustomerInvoiceState.POSTED);
        inv.setUpdatedAt(Instant.now());
        CustomerInvoice saved = invoiceRepository.save(inv);
        customerPaymentService.syncForInvoices(List.of(saved.getId()));
        activityLogger.log(saved.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, saved.getId(),
                "Opening balance invoice posted");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOpeningCustomerInvoice(UUID invoiceId) {
        CustomerInvoice inv = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException(
                        "error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        if (!inv.isOpeningBalance()) {
            throw new AccountingDomainException("error.accounting.notOpeningInvoice", null,
                    "Invoice is not an opening balance document");
        }
        if (inv.getState() == CustomerInvoiceState.CANCELLED) {
            return;
        }
        if (inv.getState() != CustomerInvoiceState.POSTED) {
            throw new AccountingDomainException("error.accounting.openingInvoiceNotPosted", null,
                    "Only posted opening invoices can be cancelled");
        }
        if (inv.getJournalEntryId() != null) {
            journalEntryApplicationService.reverseJournalEntry(
                    new ReverseJournalEntryCommand(inv.getJournalEntryId(), "Opening balances replaced"));
        }
        inv.setState(CustomerInvoiceState.CANCELLED);
        inv.setUpdatedAt(Instant.now());
        invoiceRepository.save(inv);
        customerPaymentService.syncForInvoices(List.of(inv.getId()));
        activityLogger.log(inv.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, inv.getId(),
                "Opening balance invoice cancelled");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerInvoiceResponse> listCreditNotesForInvoice(UUID invoiceId) {
        invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        return invoiceRepository.findByReversedInvoiceIdWithLines(invoiceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerInvoiceResponse> listPostedInvoicesForSalesOrder(UUID salesOrderId) {
        if (salesOrderId == null) {
            return List.of();
        }
        return invoiceRepository.findBySalesOrderIdWithLines(salesOrderId).stream()
                .filter(inv -> inv.getState() == CustomerInvoiceState.POSTED)
                .filter(inv -> inv.getMoveType() == null || inv.getMoveType() == CustomerInvoiceMoveType.INVOICE)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerInvoiceResponse> listPostedDocumentsForSalesOrder(UUID salesOrderId) {
        if (salesOrderId == null) {
            return List.of();
        }
        return invoiceRepository.findBySalesOrderIdWithLines(salesOrderId).stream()
                .filter(inv -> inv.getState() == CustomerInvoiceState.POSTED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<CustomerInvoiceResponse>> listPostedDocumentsForSalesOrders(java.util.Collection<UUID> salesOrderIds) {
        if (salesOrderIds == null || salesOrderIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<CustomerInvoiceResponse>> out = new LinkedHashMap<>();
        for (CustomerInvoice inv : invoiceRepository.findBySalesOrderIdInWithLines(salesOrderIds)) {
            if (inv.getState() != CustomerInvoiceState.POSTED || inv.getSalesOrderId() == null) {
                continue;
            }
            out.computeIfAbsent(inv.getSalesOrderId(), k -> new ArrayList<>()).add(toResponse(inv));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> sumPostedPaymentsByInvoiceIds(java.util.Collection<UUID> invoiceIds) {
        return customerPaymentService.sumActiveAllocationsByInvoiceIds(invoiceIds);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerInvoiceResponse getCustomerInvoice(UUID invoiceId) {
        return invoiceRepository.findByIdWithLines(invoiceId)
                .map(this::toResponse)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerInvoiceResponse> listCustomerInvoices(UUID companyId) {
        UUID cid = companyIdOrDefault(companyId);
        return invoiceRepository.findByCompanyWithLines(cid).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerInvoiceResponse> searchCustomerInvoices(UUID companyId, String state, UUID salesOrderId, String q, Pageable pageable) {
        UUID cid = companyIdOrDefault(companyId);
        CustomerInvoiceState stateFilter = null;
        if (state != null && !state.isBlank()) {
            stateFilter = CustomerInvoiceState.valueOf(state.trim().toUpperCase(java.util.Locale.ROOT));
        }
        return invoiceRepository.search(cid, stateFilter, salesOrderId, q, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerPaymentResponse> searchCustomerPayments(UUID companyId, Pageable pageable) {
        return customerPaymentService.search(companyIdOrDefault(companyId), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerPaymentResponse> listCustomerPayments(UUID companyId) {
        return customerPaymentService.list(companyIdOrDefault(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPaymentResponse getCustomerPayment(UUID paymentId) {
        return customerPaymentService.get(paymentId);
    }

    @Override
    @Transactional
    public CustomerPaymentResponse registerCustomerPayment(RegisterCustomerPaymentCommand command) {
        return customerPaymentService.register(command);
    }

    @Override
    @Transactional
    public CustomerPaymentResponse allocateCustomerPayment(UUID paymentId, AllocateCustomerPaymentCommand command) {
        return customerPaymentService.allocate(paymentId, command);
    }

    @Override
    @Transactional
    public CustomerPaymentResponse deallocateCustomerPayment(UUID allocationId) {
        return customerPaymentService.deallocate(allocationId);
    }

    @Override
    @Transactional
    public CustomerPaymentResponse reverseCustomerPayment(UUID paymentId, String reason) {
        return customerPaymentService.reverse(paymentId, reason);
    }

    @Override
    @Transactional
    public CustomerPaymentResponse correctCustomerPayment(UUID paymentId, CorrectCustomerPaymentCommand command) {
        return customerPaymentService.correctPayment(paymentId, command);
    }

    @Override
    @Transactional
    public CustomerPaymentResponse refundCustomerCredit(UUID invoiceId, RefundCustomerCreditCommand command) {
        return customerPaymentService.refundCredit(invoiceId, command);
    }

    @Override
    @Transactional
    public BigDecimal keepCustomerCredit(UUID invoiceId) {
        return customerPaymentService.keepCredit(invoiceId);
    }

    @Override
    @Transactional
    public BigDecimal applyCustomerCredit(UUID invoiceId) {
        return customerPaymentService.applyCredit(invoiceId);
    }

    @Override
    @Transactional
    public BigDecimal applyCustomerCreditUpTo(UUID invoiceId, BigDecimal limit) {
        return customerPaymentService.applyCredit(invoiceId, limit);
    }

    private CustomerInvoiceResponse toResponse(CustomerInvoice inv) {
        CustomerInvoiceResponse r = new CustomerInvoiceResponse();
        r.setId(inv.getId());
        r.setCompanyId(inv.getCompanyId());
        r.setCustomerPartnerId(inv.getCustomerPartnerId());
        r.setInvoiceDate(inv.getInvoiceDate());
        r.setDueDate(inv.getDueDate());
        r.setReference(inv.getReference());
        r.setCurrencyCode(inv.getCurrencyCode());
        r.setState(inv.getState());
        r.setMoveType(inv.getMoveType() != null ? inv.getMoveType() : CustomerInvoiceMoveType.INVOICE);
        r.setReversedInvoiceId(inv.getReversedInvoiceId());
        r.setJournalEntryId(inv.getJournalEntryId());
        List<CustomerInvoiceLineResponse> lines = new ArrayList<>();
        for (CustomerInvoiceLine l : inv.getLines()) {
            CustomerInvoiceLineResponse lr = new CustomerInvoiceLineResponse();
            lr.setId(l.getId());
            lr.setSequence(l.getSequence());
            lr.setName(l.getName());
            lr.setQty(l.getQty());
            lr.setUnitPrice(l.getUnitPrice());
            lr.setDiscountType(l.getDiscountType());
            lr.setDiscountValue(l.getDiscountValue());
            lr.setDiscountPercent(l.getDiscountPercent());
            lr.setRevenueAccountId(l.getRevenueAccountId());
            lr.setSalesOrderLineId(l.getSalesOrderLineId());
            lr.setGift(l.isGift());
            for (CustomerInvoiceLineTax t : l.getTaxSnapshots()) {
                CustomerInvoiceLineTaxResponse tr = new CustomerInvoiceLineTaxResponse();
                tr.setTaxId(t.getTaxId());
                tr.setTaxName(t.getTaxName());
                tr.setTaxBase(t.getTaxBase());
                tr.setTaxAmount(t.getTaxAmount());
                tr.setAccountId(t.getAccountId());
                lr.getTaxSnapshots().add(tr);
            }
            lines.add(lr);
        }
        r.setLines(lines);
        r.setSalesOrderId(inv.getSalesOrderId());
        r.setExchangeRateToCompany(inv.getExchangeRateToCompany());
        r.setOrderDiscountAmount(inv.getOrderDiscountAmount());
        r.setOpeningBalance(inv.isOpeningBalance());
        if (inv.getState() == CustomerInvoiceState.POSTED) {
            BigDecimal total = CustomerInvoiceMath.total(inv);
            BigDecimal paid = customerPaymentService.sumActiveAllocationsByInvoiceIds(List.of(inv.getId()))
                    .getOrDefault(inv.getId(), BigDecimal.ZERO);
            BigDecimal credited = BigDecimal.ZERO;
            if (inv.getMoveType() != CustomerInvoiceMoveType.CREDIT_NOTE) {
                for (CustomerInvoice cn : invoiceRepository.findByReversedInvoiceIdWithLines(inv.getId())) {
                    if (cn.getState() == CustomerInvoiceState.POSTED) {
                        credited = credited.add(CustomerInvoiceMath.total(cn));
                    }
                }
            }
            r.setAmountTotal(total);
            r.setAmountPaid(paid);
            r.setAmountCredited(credited);
            r.setAmountResidual(total.subtract(paid).subtract(credited).max(BigDecimal.ZERO));
            if (inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE) {
                r.setAmountOverpaid(BigDecimal.ZERO);
                r.setAmountRefundable(customerPaymentService.refundableOf(inv));
            } else {
                r.setAmountOverpaid(customerPaymentService.creditAvailableOn(inv));
                r.setAmountRefundable(BigDecimal.ZERO);
            }
            r.setPaymentAllocations(customerPaymentService.allocationResponsesForInvoice(inv.getId()));
        } else {
            r.setAmountTotal(CustomerInvoiceMath.total(inv));
            r.setAmountPaid(BigDecimal.ZERO);
            r.setAmountCredited(BigDecimal.ZERO);
            r.setAmountResidual(BigDecimal.ZERO);
            r.setAmountOverpaid(BigDecimal.ZERO);
            r.setAmountRefundable(BigDecimal.ZERO);
        }
        return r;
    }

    private Map<UUID, BigDecimal> creditedQtyBySourceInvoiceLine(CustomerInvoice source) {
        Map<UUID, BigDecimal> credited = new LinkedHashMap<>();
        for (CustomerInvoice cn : invoiceRepository.findByReversedInvoiceIdWithLines(source.getId())) {
            for (CustomerInvoiceLine cnLine : cn.getLines()) {
                UUID sourceLineId = matchSourceInvoiceLineId(source, cnLine);
                if (sourceLineId != null) {
                    credited.merge(sourceLineId, cnLine.getQty(), BigDecimal::add);
                }
            }
        }
        return credited;
    }

    private UUID matchSourceInvoiceLineId(CustomerInvoice source, CustomerInvoiceLine cnLine) {
        for (CustomerInvoiceLine src : source.getLines()) {
            if (java.util.Objects.equals(src.getSalesOrderLineId(), cnLine.getSalesOrderLineId())
                    && java.util.Objects.equals(src.getName(), cnLine.getName())
                    && src.getUnitPrice().compareTo(cnLine.getUnitPrice()) == 0) {
                return src.getId();
            }
        }
        return null;
    }

    private BigDecimal resolveExchangeRate(
            UUID companyId, String currencyCode, java.time.LocalDate asOf, BigDecimal explicit) {
        if (explicit != null && explicit.signum() > 0) {
            String base = currencyConversionPort.baseCurrencyCode(companyId);
            if (explicit.compareTo(BigDecimal.ONE) != 0 || currencyCode.equalsIgnoreCase(base)) {
                return explicit.setScale(12, RoundingMode.HALF_UP);
            }
        }
        return currencyConversionPort.exchangeRateToCompany(companyId, currencyCode, asOf);
    }
}
