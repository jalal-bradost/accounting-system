package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.CurrencyMath;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.SalesOrderInvoiceSyncPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.event.CustomerInvoicePostedEvent;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.messaging.AccountingEventPublisher;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CustomerInvoiceRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CustomerPaymentRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalRepository;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PartnerResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoice;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoiceLine;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoiceLineTax;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerPayment;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
    private static final String SALE_JOURNAL_CODE = "430003";
    private static final String EXCHANGE_GAIN_ACCOUNT_CODE = "430014";
    private static final String EXCHANGE_LOSS_ACCOUNT_CODE = "430015";

    private final CustomerInvoiceRepository invoiceRepository;
    private final CustomerPaymentRepository paymentRepository;
    private final PartnerApplicationService partnerApplicationService;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final JournalRepository journalRepository;
    private final AccountRepository accountRepository;
    private final ReconciliationApplicationService reconciliationApplicationService;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final ObjectProvider<SalesOrderInvoiceSyncPort> salesOrderInvoiceSyncPortProvider;
    private final AccountingEventPublisher accountingEventPublisher;
    private final CurrencyConversionPort currencyConversionPort;

    public CustomerInvoiceApplicationServiceImpl(CustomerInvoiceRepository invoiceRepository,
                                                 CustomerPaymentRepository paymentRepository,
                                                 PartnerApplicationService partnerApplicationService,
                                                 JournalEntryApplicationService journalEntryApplicationService,
                                                 JournalRepository journalRepository,
                                                 AccountRepository accountRepository,
                                                 ReconciliationApplicationService reconciliationApplicationService,
                                                 ObjectProvider<CompanyContext> companyContextProvider,
                                                 ObjectProvider<SalesOrderInvoiceSyncPort> salesOrderInvoiceSyncPortProvider,
                                                 AccountingEventPublisher accountingEventPublisher,
                                                 CurrencyConversionPort currencyConversionPort) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.partnerApplicationService = partnerApplicationService;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.journalRepository = journalRepository;
        this.accountRepository = accountRepository;
        this.reconciliationApplicationService = reconciliationApplicationService;
        this.companyContextProvider = companyContextProvider;
        this.salesOrderInvoiceSyncPortProvider = salesOrderInvoiceSyncPortProvider;
        this.accountingEventPublisher = accountingEventPublisher;
        this.currencyConversionPort = currencyConversionPort;
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
    public Map<UUID, BigDecimal> draftAllocatedQtyBySalesOrderLine(UUID salesOrderId) {
        return draftAllocatedQtyBySalesOrderLine(salesOrderId, CustomerInvoiceMoveType.INVOICE);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> draftCreditNoteAllocatedQtyBySalesOrderLine(UUID salesOrderId) {
        return draftAllocatedQtyBySalesOrderLine(salesOrderId, CustomerInvoiceMoveType.CREDIT_NOTE);
    }

    private Map<UUID, BigDecimal> draftAllocatedQtyBySalesOrderLine(UUID salesOrderId, CustomerInvoiceMoveType moveType) {
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
                if (line.getSalesOrderLineId() != null) {
                    allocated.merge(line.getSalesOrderLineId(), line.getQty(), BigDecimal::add);
                }
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

    private UUID resolveLiquidityAccountForPaymentJournal(UUID companyId, UUID journalId) {
        Journal j = journalRepository.findById(new JournalId(journalId))
                .orElseThrow(() -> new AccountingDomainException("Payment journal not found"));
        if (!j.getCompanyId().getId().equals(companyId)) {
            throw new AccountingDomainException("Journal company mismatch");
        }
        if (j.getJournalType() != JournalType.CASH && j.getJournalType() != JournalType.BANK) {
            throw new AccountingDomainException("Payment journal must be cash or bank");
        }
        return accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), j.getCode())
                .orElseThrow(() -> new AccountingDomainException(
                        "Liquidity account for journal code " + j.getCode() + " not found"))
                .getId().getId();
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCustomerInvoice(CreateCustomerInvoiceCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PartnerResponse customer = partnerApplicationService.getPartner(command.getCustomerPartnerId());
        if (!customer.isCustomer()) {
            throw new AccountingDomainException("Partner is not a customer");
        }
        if (!customer.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("Customer belongs to another company");
        }
        UUID defaultRevenue = accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), DEFAULT_REVENUE_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException("Default revenue account not found"))
                .getId().getId();

        Instant now = Instant.now();
        CustomerInvoice inv = new CustomerInvoice();
        inv.setId(UUID.randomUUID());
        inv.setCompanyId(companyId);
        inv.setCustomerPartnerId(command.getCustomerPartnerId());
        inv.setInvoiceDate(command.getInvoiceDate());
        inv.setDueDate(command.getDueDate());
        inv.setReference(command.getReference());
        inv.setCurrencyCode(command.getCurrencyCode());
        inv.setSalesOrderId(command.getSalesOrderId());
        inv.setReversedInvoiceId(command.getReversedInvoiceId());
        CustomerInvoiceMoveType moveType = CustomerInvoiceMoveType.INVOICE;
        if (command.getMoveType() != null && !command.getMoveType().isBlank()) {
            try {
                moveType = CustomerInvoiceMoveType.valueOf(command.getMoveType().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new AccountingDomainException("Invalid moveType: " + command.getMoveType());
            }
        }
        inv.setMoveType(moveType);
        inv.setExchangeRateToCompany(resolveExchangeRate(
                companyId, command.getCurrencyCode(), command.getInvoiceDate(), command.getExchangeRateToCompany()));
        inv.setState(CustomerInvoiceState.DRAFT);
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        inv.setRowVersion(0L);

        int seq = 0;
        for (CustomerInvoiceLineCommand lc : command.getLines()) {
            if (lc.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AccountingDomainException("Line unit price must be positive");
            }
            UUID revAcc = lc.getRevenueAccountId() != null ? lc.getRevenueAccountId() : defaultRevenue;
            CustomerInvoiceLine line = new CustomerInvoiceLine();
            line.setId(UUID.randomUUID());
            line.setSequence(++seq);
            line.setName(lc.getName());
            line.setQty(lc.getQty().setScale(4, RoundingMode.HALF_UP));
            line.setUnitPrice(lc.getUnitPrice().setScale(4, RoundingMode.HALF_UP));
            line.setDiscountPercent(lc.getDiscountPercent() != null ? lc.getDiscountPercent() : BigDecimal.ZERO);
            line.setSalesOrderLineId(lc.getSalesOrderLineId());
            line.setRevenueAccountId(revAcc);
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
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
            inv.getLines().add(line);
        }
        return toResponse(invoiceRepository.save(inv));
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCreditNoteFromInvoice(UUID invoiceId, CreateCreditNoteFromInvoiceCommand command) {
        CustomerInvoice source = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        if (!source.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("Invoice company mismatch");
        }
        if (source.getState() != CustomerInvoiceState.POSTED) {
            throw new AccountingDomainException("Only posted invoices can be credited");
        }
        if (source.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE) {
            throw new AccountingDomainException("Cannot create a credit note from another credit note");
        }

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
            lc.setDiscountPercent(srcLine.getDiscountPercent());
            lc.setRevenueAccountId(srcLine.getRevenueAccountId());
            lc.setSalesOrderLineId(srcLine.getSalesOrderLineId());
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
            throw new AccountingDomainException("Credit note has no lines");
        }
        create.setLines(lines);
        return createCustomerInvoice(create);
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse postCustomerInvoice(UUID invoiceId) {
        CustomerInvoice inv = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
        if (inv.getState() != CustomerInvoiceState.DRAFT) {
            throw new AccountingDomainException("Invoice is not draft");
        }

        BigDecimal rate = resolveExchangeRate(
                inv.getCompanyId(), inv.getCurrencyCode(), inv.getInvoiceDate(), inv.getExchangeRateToCompany());
        inv.setExchangeRateToCompany(rate);

        // 100% discount / free invoices net to zero — ledger rejects debit=0 credit=0 lines.
        // Credit notes reverse the invoice pattern: Dr Revenue/Tax, Cr AR.
        boolean creditNote = inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE;
        List<JournalItemCommand> items = new ArrayList<>();
        BigDecimal arTotalComp = BigDecimal.ZERO;
        BigDecimal arDoc = BigDecimal.ZERO;

        for (CustomerInvoiceLine line : inv.getLines()) {
            BigDecimal disc = line.getDiscountPercent() != null ? line.getDiscountPercent() : BigDecimal.ZERO;
            BigDecimal lineNetDoc = lineNet(line.getQty(), line.getUnitPrice(), disc).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineNetComp = CurrencyMath.convertAtRate(lineNetDoc, rate);

            if (lineNetComp.signum() > 0) {
                arTotalComp = arTotalComp.add(lineNetComp);
                arDoc = arDoc.add(lineNetDoc);
                if (creditNote) {
                    items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), lineNetComp, BigDecimal.ZERO,
                            inv.getCurrencyCode(), lineNetDoc, null));
                } else {
                    items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), BigDecimal.ZERO, lineNetComp,
                            inv.getCurrencyCode(), lineNetDoc.negate(), null));
                }
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
        if (arTotalComp.signum() > 0) {
            PartnerResponse customer = partnerApplicationService.getPartner(inv.getCustomerPartnerId());
            UUID receivableAccount = customer.getReceivableAccountId() != null
                    ? customer.getReceivableAccountId()
                    : accountRepository.findByCompanyIdAndCode(new CompanyId(inv.getCompanyId()), DEFAULT_AR_ACCOUNT_CODE)
                    .orElseThrow(() -> new AccountingDomainException("Default AR account not found"))
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
                    .orElseThrow(() -> new AccountingDomainException("Sale journal not found"));
            CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                    inv.getCompanyId(),
                    saleJournal.getId().getId(),
                    "",
                    inv.getInvoiceDate(),
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

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerInvoiceResponse> listCreditNotesForInvoice(UUID invoiceId) {
        invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
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
    public CustomerInvoiceResponse getCustomerInvoice(UUID invoiceId) {
        return invoiceRepository.findByIdWithLines(invoiceId)
                .map(this::toResponse)
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
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
    public List<CustomerPaymentResponse> listCustomerPayments(UUID companyId) {
        UUID cid = companyIdOrDefault(companyId);
        return paymentRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(cid).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerPaymentResponse registerCustomerPayment(RegisterCustomerPaymentCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        CustomerInvoice inv = invoiceRepository.findById(command.getCustomerInvoiceId())
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
        if (!inv.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("Invoice company mismatch");
        }
        if (inv.getState() != CustomerInvoiceState.POSTED || inv.getJournalEntryId() == null) {
            throw new AccountingDomainException("Invoice must be posted before payment");
        }
        if (inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE) {
            throw new AccountingDomainException("Cannot register payment against a credit note");
        }

        BigDecimal docAmt = command.getAmount().setScale(4, RoundingMode.HALF_UP);
        String paymentCurrency = command.getCurrencyCode() != null ? command.getCurrencyCode() : inv.getCurrencyCode();
        ensurePaymentWithinOutstanding(inv, docAmt, paymentCurrency);

        PartnerResponse customer = partnerApplicationService.getPartner(inv.getCustomerPartnerId());
        UUID receivableAccount = customer.getReceivableAccountId() != null
                ? customer.getReceivableAccountId()
                : accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), DEFAULT_AR_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException("Default AR account not found"))
                .getId().getId();

        UUID liquidityAccount = resolveLiquidityAccountForPaymentJournal(companyId, command.getPaymentJournalId());
        Journal paymentJournal = journalRepository.findById(new JournalId(command.getPaymentJournalId()))
                .orElseThrow(() -> new AccountingDomainException("Payment journal not found"));

        BigDecimal invoiceRate = resolveExchangeRate(
                companyId, inv.getCurrencyCode(), inv.getInvoiceDate(), inv.getExchangeRateToCompany());
        BigDecimal paymentRate = resolveExchangeRate(
                companyId, paymentCurrency, command.getPaymentDate(), command.getExchangeRateToCompany());

        BigDecimal arClearComp = CurrencyMath.convertAtRate(docAmt, invoiceRate);
        BigDecimal liquidityComp = CurrencyMath.convertAtRate(docAmt, paymentRate);
        BigDecimal fxDiff = arClearComp.subtract(liquidityComp).setScale(4, RoundingMode.HALF_UP);

        String liqLabel = paymentJournal.getJournalType() == JournalType.CASH ? "Cash receipt" : "Bank receipt";
        List<JournalItemCommand> items = new ArrayList<>();
        items.add(new JournalItemCommand(liquidityAccount, liqLabel, liquidityComp, BigDecimal.ZERO,
                paymentCurrency, docAmt, null));
        items.add(new JournalItemCommand(receivableAccount,
                "Payment " + (inv.getReference() != null ? inv.getReference() : ""),
                BigDecimal.ZERO, arClearComp, paymentCurrency, docAmt.negate(), inv.getCustomerPartnerId()));
        appendCustomerExchangeDifference(items, companyId, fxDiff);
        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                paymentJournal.getId().getId(),
                "",
                command.getPaymentDate(),
                paymentCurrency,
                inv.getCustomerPartnerId(),
                items);
        CreateJournalEntryResponse payEntry = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(payEntry.getJournalEntryId());

        JournalEntryResponse invEntry = journalEntryApplicationService.getJournalEntry(inv.getJournalEntryId());
        UUID invArItem = invEntry.getItems().stream()
                .filter(i -> receivableAccount.equals(i.getAccountId()) && i.getDebit().compareTo(BigDecimal.ZERO) > 0)
                .map(JournalEntryResponse.JournalItemResponse::getId)
                .findFirst()
                .orElseThrow(() -> new AccountingDomainException("Could not find AR line on customer invoice entry"));
        JournalEntryResponse paymentEntry = journalEntryApplicationService.getJournalEntry(payEntry.getJournalEntryId());
        UUID payArItem = paymentEntry.getItems().stream()
                .filter(i -> receivableAccount.equals(i.getAccountId()) && i.getCredit().compareTo(BigDecimal.ZERO) > 0)
                .map(JournalEntryResponse.JournalItemResponse::getId)
                .findFirst()
                .orElseThrow(() -> new AccountingDomainException("Could not find AR line on payment entry"));

        UUID reconciliationId = UUID.randomUUID();
        reconciliationApplicationService.reconcile(
                new ReconciliationApplicationService.ReconcileCommand(
                        List.of(invArItem, payArItem), reconciliationId));

        Instant now = Instant.now();
        CustomerPayment p = new CustomerPayment();
        p.setId(UUID.randomUUID());
        p.setCompanyId(companyId);
        p.setCustomerPartnerId(inv.getCustomerPartnerId());
        p.setCustomerInvoiceId(inv.getId());
        p.setPaymentDate(command.getPaymentDate());
        p.setPaymentJournalId(command.getPaymentJournalId());
        p.setAmount(docAmt);
        p.setCurrencyCode(paymentCurrency);
        p.setExchangeRateToCompany(paymentRate);
        p.setJournalEntryId(payEntry.getJournalEntryId());
        p.setReference(command.getReference());
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        paymentRepository.save(p);

        CustomerPaymentResponse r = new CustomerPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(companyId);
        r.setCustomerInvoiceId(inv.getId());
        r.setPaymentDate(command.getPaymentDate());
        r.setPaymentJournalId(command.getPaymentJournalId());
        r.setAmount(docAmt);
        r.setCurrencyCode(paymentCurrency);
        r.setExchangeRateToCompany(paymentRate);
        r.setJournalEntryId(payEntry.getJournalEntryId());
        r.setReconciliationId(reconciliationId);
        return r;
    }

    private BigDecimal invoiceTotalDocumentCurrency(CustomerInvoice inv) {
        BigDecimal total = BigDecimal.ZERO;
        for (CustomerInvoiceLine line : inv.getLines()) {
            BigDecimal lineNet = customerLineNet(line.getQty(), line.getUnitPrice(), line.getDiscountPercent())
                    .setScale(4, RoundingMode.HALF_UP);
            total = total.add(lineNet);
            for (CustomerInvoiceLineTax ts : line.getTaxSnapshots()) {
                total = total.add(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }
        return total;
    }

    private static BigDecimal customerLineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal disc = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(
                disc.max(BigDecimal.ZERO).min(new BigDecimal("100"))
                        .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        return qty.multiply(unitPrice).multiply(factor);
    }

    private BigDecimal sumPaymentsForInvoice(UUID invoiceId, String invoiceCurrency) {
        return paymentRepository.findByCustomerInvoiceId(invoiceId).stream()
                .filter(p -> invoiceCurrency.equalsIgnoreCase(p.getCurrencyCode()))
                .map(CustomerPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPostedCreditNotesForInvoice(UUID sourceInvoiceId, String invoiceCurrency) {
        return invoiceRepository.findByReversedInvoiceIdWithLines(sourceInvoiceId).stream()
                .filter(cn -> cn.getState() == CustomerInvoiceState.POSTED)
                .filter(cn -> cn.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE)
                .filter(cn -> invoiceCurrency.equalsIgnoreCase(cn.getCurrencyCode()))
                .map(this::invoiceTotalDocumentCurrency)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private void ensurePaymentWithinOutstanding(CustomerInvoice inv, BigDecimal docAmt, String paymentCurrency) {
        CustomerInvoice loaded = invoiceRepository.findByIdWithLines(inv.getId())
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
        String invoiceCurrency = loaded.getCurrencyCode();
        BigDecimal invoiceTotal = invoiceTotalDocumentCurrency(loaded);
        BigDecimal paid = sumPaymentsForInvoice(loaded.getId(), invoiceCurrency);
        BigDecimal credited = sumPostedCreditNotesForInvoice(loaded.getId(), invoiceCurrency);
        BigDecimal outstanding = invoiceTotal.subtract(paid).subtract(credited).setScale(4, RoundingMode.HALF_UP);
        if (outstanding.signum() <= 0) {
            throw new AccountingDomainException("Customer invoice is already fully paid");
        }
        if (!invoiceCurrency.equalsIgnoreCase(paymentCurrency)) {
            return;
        }
        if (docAmt.compareTo(outstanding) > 0) {
            throw new AccountingDomainException(
                    "Payment amount exceeds outstanding balance of " + outstanding.toPlainString()
                            + " " + invoiceCurrency);
        }
    }

    private CustomerPaymentResponse toPaymentResponse(CustomerPayment p) {
        CustomerPaymentResponse r = new CustomerPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(p.getCompanyId());
        r.setCustomerInvoiceId(p.getCustomerInvoiceId());
        r.setPaymentDate(p.getPaymentDate());
        r.setPaymentJournalId(p.getPaymentJournalId());
        r.setAmount(p.getAmount());
        r.setCurrencyCode(p.getCurrencyCode());
        r.setExchangeRateToCompany(p.getExchangeRateToCompany());
        r.setJournalEntryId(p.getJournalEntryId());
        r.setReconciliationId(null);
        return r;
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
            lr.setDiscountPercent(l.getDiscountPercent());
            lr.setRevenueAccountId(l.getRevenueAccountId());
            lr.setSalesOrderLineId(l.getSalesOrderLineId());
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

    private void appendCustomerExchangeDifference(List<JournalItemCommand> items, UUID companyId, BigDecimal fxDiff) {
        if (fxDiff.signum() == 0) {
            return;
        }
        if (fxDiff.signum() > 0) {
            UUID lossAccount = accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), EXCHANGE_LOSS_ACCOUNT_CODE)
                    .orElseThrow(() -> new AccountingDomainException("Exchange loss account not found"))
                    .getId().getId();
            items.add(new JournalItemCommand(lossAccount, "Exchange loss", fxDiff, BigDecimal.ZERO,
                    null, null, null));
        } else {
            UUID gainAccount = accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), EXCHANGE_GAIN_ACCOUNT_CODE)
                    .orElseThrow(() -> new AccountingDomainException("Exchange gain account not found"))
                    .getId().getId();
            items.add(new JournalItemCommand(gainAccount, "Exchange gain", BigDecimal.ZERO, fxDiff.abs(),
                    null, null, null));
        }
    }

    private static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal disc = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(
                disc.max(BigDecimal.ZERO).min(new BigDecimal("100"))
                        .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        return qty.multiply(unitPrice).multiply(factor);
    }
}
