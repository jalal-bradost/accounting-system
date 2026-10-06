package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.AllocateCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentAllocationLine;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentAllocationResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.ports.output.CustomerPaymentEventPort;
import com.bradox.erp.accounting.service.domain.customerinvoice.CorrectCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.RefundCustomerCreditCommand;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerInvoiceRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerPaymentAllocationRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerPaymentRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort.ItemSnapshot;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalRepository;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.core.ValueObject.CustomerPaymentAllocationState;
import com.bradox.erp.domain.core.ValueObject.CustomerPaymentKind;
import com.bradox.erp.domain.core.ValueObject.CustomerPaymentState;
import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.bradox.erp.domain.core.entity.CustomerInvoice;
import com.bradox.erp.domain.core.entity.CustomerPayment;
import com.bradox.erp.domain.core.entity.CustomerPaymentAllocation;
import com.bradox.erp.domain.core.entity.Journal;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.MonetaryScale;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Customer receipts/refunds and their allocations to invoices/credit notes. Allocations are the
 * only source of paid, outstanding and unallocated amounts. The payment entry never contains FX:
 * differences between the document rate and the payment rate are booked by a separate entry on
 * the allocation.
 */
@Service
public class CustomerPaymentService {

    static final String DEFAULT_AR_ACCOUNT_CODE = "430003";
    static final String EXCHANGE_GAIN_ACCOUNT_CODE = "430014";
    static final String EXCHANGE_LOSS_ACCOUNT_CODE = "430015";
    static final String EXCHANGE_JOURNAL_CODE = "EXCH";

    private final CustomerInvoiceRepository invoiceRepository;
    private final CustomerPaymentRepository paymentRepository;
    private final CustomerPaymentAllocationRepository allocationRepository;
    private final PartnerApplicationService partnerApplicationService;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final JournalRepository journalRepository;
    private final AccountRepository accountRepository;
    private final JournalItemReconciliationPort journalItemPort;
    private final TradeReconciliationSync tradeReconciliationSync;
    private final PeriodPostingGuard periodPostingGuard;
    private final CurrencyConversionPort currencyConversionPort;
    private final RecordActivityLogger activityLogger;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final ObjectProvider<CustomerPaymentEventPort> paymentEventPortProvider;

    public CustomerPaymentService(CustomerInvoiceRepository invoiceRepository,
                                  CustomerPaymentRepository paymentRepository,
                                  CustomerPaymentAllocationRepository allocationRepository,
                                  PartnerApplicationService partnerApplicationService,
                                  JournalEntryApplicationService journalEntryApplicationService,
                                  JournalRepository journalRepository,
                                  AccountRepository accountRepository,
                                  JournalItemReconciliationPort journalItemPort,
                                  TradeReconciliationSync tradeReconciliationSync,
                                  PeriodPostingGuard periodPostingGuard,
                                  CurrencyConversionPort currencyConversionPort,
                                  RecordActivityLogger activityLogger,
                                  ObjectProvider<CompanyContext> companyContextProvider,
                                  ObjectProvider<CustomerPaymentEventPort> paymentEventPortProvider) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.allocationRepository = allocationRepository;
        this.partnerApplicationService = partnerApplicationService;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.journalRepository = journalRepository;
        this.accountRepository = accountRepository;
        this.journalItemPort = journalItemPort;
        this.tradeReconciliationSync = tradeReconciliationSync;
        this.periodPostingGuard = periodPostingGuard;
        this.currencyConversionPort = currencyConversionPort;
        this.activityLogger = activityLogger;
        this.companyContextProvider = companyContextProvider;
        this.paymentEventPortProvider = paymentEventPortProvider;
    }

    /** Document-currency figures of one invoice or credit note. */
    public record DocumentBalance(BigDecimal total, BigDecimal paid, BigDecimal credited, BigDecimal residual) {}

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<CustomerPaymentResponse> list(UUID companyId) {
        List<CustomerPayment> payments = paymentRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId);
        List<CustomerPaymentAllocation> allocations = allocationRepository.findByCompanyId(companyId);
        return toResponses(payments, allocations);
    }

    @Transactional(readOnly = true)
    public Page<CustomerPaymentResponse> search(UUID companyId, Pageable pageable) {
        Page<CustomerPayment> page = paymentRepository.searchByCompanyId(companyId, pageable);
        List<CustomerPaymentAllocation> allocations = allocationRepository.findByPaymentIdIn(
                page.getContent().stream().map(CustomerPayment::getId).toList());
        List<CustomerPaymentResponse> content = toResponses(page.getContent(), allocations);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CustomerPaymentResponse get(UUID paymentId) {
        return responseFor(loadPayment(paymentId, false));
    }

    private CustomerPaymentResponse responseFor(CustomerPayment p) {
        return toResponses(List.of(p), allocationRepository.findByPaymentIdIn(List.of(p.getId()))).get(0);
    }

    @Transactional(readOnly = true)
    public List<CustomerPaymentResponse> listForPartner(UUID companyId, UUID partnerId) {
        List<CustomerPayment> payments = paymentRepository
                .findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(companyId, partnerId);
        List<CustomerPaymentAllocation> allocations = allocationRepository.findByPaymentIdIn(
                payments.stream().map(CustomerPayment::getId).toList());
        return toResponses(payments, allocations);
    }

    /** Sum of active allocation amounts per document id (missing key = zero). */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> sumActiveAllocationsByInvoiceIds(Collection<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) return Map.of();
        Map<UUID, BigDecimal> sums = new HashMap<>();
        for (CustomerPaymentAllocation a : allocationRepository.findByInvoiceIdIn(invoiceIds)) {
            if (a.isActive()) {
                sums.merge(a.getCustomerInvoiceId(), a.getAmount(), BigDecimal::add);
            }
        }
        sums.replaceAll((id, amt) -> amt.setScale(4, RoundingMode.HALF_UP));
        return sums;
    }

    /** Unallocated amount per posted receipt (refunds are always fully allocated). */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> unallocatedByPaymentId(Collection<CustomerPayment> payments) {
        Map<UUID, BigDecimal> allocated = new HashMap<>();
        for (CustomerPaymentAllocation a : allocationRepository.findByPaymentIdIn(
                payments.stream().map(CustomerPayment::getId).toList())) {
            if (a.isActive()) {
                allocated.merge(a.getPaymentId(), a.getPaymentAmount(), BigDecimal::add);
            }
        }
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (CustomerPayment p : payments) {
            out.put(p.getId(), unallocated(p, allocated.getOrDefault(p.getId(), BigDecimal.ZERO)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public DocumentBalance balanceOf(CustomerInvoice inv) {
        return balancesFor(List.of(inv)).get(inv.getId());
    }

    /** Balances for the given documents (loaded with lines). */
    @Transactional(readOnly = true)
    public Map<UUID, DocumentBalance> balancesFor(Collection<CustomerInvoice> invoices) {
        if (invoices == null || invoices.isEmpty()) return Map.of();
        List<UUID> ids = invoices.stream().map(CustomerInvoice::getId).toList();
        Map<UUID, BigDecimal> paid = sumActiveAllocationsByInvoiceIds(ids);
        Map<UUID, BigDecimal> credited = new HashMap<>();
        Map<UUID, CustomerInvoice> byId = invoices.stream()
                .collect(Collectors.toMap(CustomerInvoice::getId, Function.identity(), (a, b) -> a));
        for (CustomerInvoice cn : invoiceRepository.findByReversedInvoiceIdIn(ids)) {
            CustomerInvoice source = byId.get(cn.getReversedInvoiceId());
            if (source == null || !isPostedCreditNoteFor(cn, source)) continue;
            credited.merge(source.getId(), CustomerInvoiceMath.total(cn), BigDecimal::add);
        }
        Map<UUID, DocumentBalance> out = new LinkedHashMap<>();
        for (CustomerInvoice inv : invoices) {
            BigDecimal total = CustomerInvoiceMath.total(inv);
            BigDecimal p = paid.getOrDefault(inv.getId(), BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            BigDecimal c = isCreditNote(inv) ? BigDecimal.ZERO.setScale(4)
                    : credited.getOrDefault(inv.getId(), BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            out.put(inv.getId(), new DocumentBalance(total, p, c, total.subtract(p).subtract(c).setScale(4, RoundingMode.HALF_UP)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<CustomerPaymentAllocationResponse> allocationResponsesForInvoice(UUID invoiceId) {
        List<CustomerPaymentAllocation> allocations = allocationRepository.findByInvoiceIdIn(List.of(invoiceId));
        Map<UUID, CustomerPayment> payments = paymentRepository.findByIdIn(
                        allocations.stream().map(CustomerPaymentAllocation::getPaymentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CustomerPayment::getId, Function.identity()));
        Map<UUID, CustomerInvoice> invoices = invoiceRepository.findById(invoiceId)
                .map(i -> Map.of(i.getId(), i)).orElse(Map.of());
        return allocations.stream().map(a -> toAllocationResponse(a, payments.get(a.getPaymentId()), invoices)).toList();
    }

    /** True when any active allocation touches one of the documents or payments. */
    @Transactional(readOnly = true)
    public boolean hasActiveAllocations(Collection<UUID> invoiceIds, Collection<UUID> paymentIds) {
        return allocationRepository.findByInvoiceIdIn(invoiceIds).stream().anyMatch(CustomerPaymentAllocation::isActive)
                || allocationRepository.findByPaymentIdIn(paymentIds).stream().anyMatch(CustomerPaymentAllocation::isActive);
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public CustomerPaymentResponse register(RegisterCustomerPaymentCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        List<CustomerPaymentAllocationLine> lines = new ArrayList<>(command.getAllocations());
        BigDecimal amount = scale(command.getAmount());
        boolean shortcut = false;
        if (command.getCustomerInvoiceId() != null) {
            if (!lines.isEmpty()) {
                throw new AccountingDomainException("error.accounting.paymentInvoiceOrAllocations", null,
                        "Provide either customerInvoiceId or allocations, not both");
            }
            lines.add(new CustomerPaymentAllocationLine(command.getCustomerInvoiceId(), amount));
            shortcut = true;
        }

        Map<UUID, CustomerInvoice> docs = new LinkedHashMap<>();
        BigDecimal requested = BigDecimal.ZERO;
        for (CustomerPaymentAllocationLine line : lines) {
            if (docs.containsKey(line.getInvoiceId())) {
                throw new AccountingDomainException("error.accounting.allocationDuplicateDocument", null,
                        "The same document appears twice in the allocations");
            }
            CustomerInvoice inv = invoiceRepository.findById(line.getInvoiceId())
                    .orElseThrow(() -> new AccountingDomainException(
                            "error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
            if (!inv.getCompanyId().equals(companyId)) {
                throw new AccountingDomainException("error.accounting.invoiceCompanyMismatch", null, "Invoice company mismatch");
            }
            if (inv.getState() != CustomerInvoiceState.POSTED || inv.getJournalEntryId() == null) {
                throw new AccountingDomainException("error.accounting.invoiceMustBePostedBeforePayment", null,
                        "Invoice must be posted before payment");
            }
            docs.put(inv.getId(), inv);
        }

        UUID partnerId = command.getCustomerPartnerId();
        for (CustomerInvoice inv : docs.values()) {
            if (partnerId == null) {
                partnerId = inv.getCustomerPartnerId();
            } else if (!partnerId.equals(inv.getCustomerPartnerId())) {
                throw new AccountingDomainException("error.accounting.allocationPartnerMismatch", null,
                        "Payment and document belong to different customers");
            }
        }
        if (partnerId == null) {
            throw new AccountingDomainException("error.accounting.paymentPartnerRequired", null,
                    "Customer is required for a payment without a document");
        }
        PartnerResponse customer = partnerApplicationService.getPartner(partnerId);
        if (!companyId.equals(customer.getCompanyId())) {
            throw new AccountingDomainException("error.accounting.customerCompanyMismatch", null, "Customer belongs to another company");
        }
        if (docs.isEmpty() && !customer.isCustomer()) {
            throw new AccountingDomainException("error.accounting.partnerNotCustomer", null, "Partner is not a customer");
        }

        String currency = command.getCurrencyCode() != null && !command.getCurrencyCode().isBlank()
                ? command.getCurrencyCode().trim().toUpperCase()
                : docs.values().stream().findFirst().map(CustomerInvoice::getCurrencyCode).orElse(null);
        if (currency == null) {
            throw new AccountingDomainException("error.accounting.paymentCurrencyRequired", null, "Payment currency is required");
        }
        boolean anyCreditNote = false;
        boolean anyInvoice = false;
        for (CustomerInvoice inv : docs.values()) {
            if (isCreditNote(inv)) anyCreditNote = true; else anyInvoice = true;
        }
        if (anyCreditNote && anyInvoice) {
            throw new AccountingDomainException("error.accounting.allocationMixedKinds", null,
                    "A payment cannot settle invoices and credit notes at the same time");
        }
        boolean refund = anyCreditNote;

        // Each line becomes (document amount, payment amount). In the same currency they are equal; in
        // another currency the payment amount is converted through the company currency, or given.
        BigDecimal payRate = resolveExchangeRate(companyId, currency,
                command.getPaymentDate().toLocalDate(), command.getExchangeRateToCompany());
        Map<CustomerPaymentAllocationLine, BigDecimal[]> resolved = new LinkedHashMap<>();
        for (CustomerPaymentAllocationLine line : lines) {
            CustomerInvoice inv = docs.get(line.getInvoiceId());
            if (inv.getCurrencyCode().equalsIgnoreCase(currency)) {
                BigDecimal a = scale(line.getAmount());
                resolved.put(line, new BigDecimal[] { a, a });
            } else if (line.getPaymentAmount() != null) {
                resolved.put(line, new BigDecimal[] { scale(line.getAmount()), scale(line.getPaymentAmount()) });
            } else if (shortcut) {
                // "Receive this much (payment currency) toward the invoice": settle what it covers,
                // capped at what is owed; an amount within a cent of the balance settles it fully.
                BigDecimal residual = balanceOf(invoiceRepository.findByIdWithLines(inv.getId()).orElse(inv)).residual();
                BigDecimal docAmount = paymentToDoc(amount, inv, payRate);
                if (docAmount.subtract(residual).abs().compareTo(new BigDecimal("0.01")) <= 0) {
                    resolved.put(line, new BigDecimal[] { residual, amount });
                } else if (docAmount.compareTo(residual) > 0) {
                    resolved.put(line, new BigDecimal[] { residual, docToPayment(residual, inv, payRate) });
                } else {
                    resolved.put(line, new BigDecimal[] { docAmount, amount });
                }
            } else {
                BigDecimal a = scale(line.getAmount());
                resolved.put(line, new BigDecimal[] { a, docToPayment(a, inv, payRate) });
            }
            requested = requested.add(resolved.get(line)[1]);
        }
        if (requested.compareTo(amount) > 0) {
            throw new AccountingDomainException("error.accounting.allocationExceedsPayment", null,
                    "Allocated total exceeds the payment amount");
        }

        Journal paymentJournal = journalRepository.findById(new JournalId(command.getPaymentJournalId()))
                .orElseThrow(() -> new AccountingDomainException("error.accounting.paymentJournalNotFound", null, "Payment journal not found"));
        UUID counterAccount = command.getLiquidityAccountId() != null
                ? command.getLiquidityAccountId()
                : resolveLiquidityAccount(companyId, paymentJournal);
        String liqLabel = paymentJournal.getJournalType() == JournalType.CASH
                ? (refund ? "Cash refund" : "Cash receipt")
                : (refund ? "Bank refund" : "Bank receipt");

        List<UUID> docIdsSorted = docs.keySet().stream().sorted().toList();
        docIdsSorted.forEach(invoiceRepository::lockById);

        CustomerPayment payment = postPaymentEntry(companyId, customer, paymentJournal, counterAccount, liqLabel,
                command.getPaymentDate(), amount, currency, command.getExchangeRateToCompany(),
                command.getReference(), refund, false);

        for (CustomerPaymentAllocationLine line : lines) {
            BigDecimal[] figures = resolved.get(line);
            allocateLocked(payment, docs.get(line.getInvoiceId()), figures[0], figures[1], null);
        }
        syncGraph(docs.keySet(), Set.of(payment.getId()));

        String amountLabel = MonetaryScale.toDisplayString(amount) + " " + currency;
        activityLogger.log(companyId, RecordActivityLogger.MODEL_CUSTOMER_PAYMENT, payment.getId(),
                refund ? "Refund registered" : "Payment registered");
        for (CustomerInvoice inv : docs.values()) {
            if (inv.getSalesOrderId() != null) {
                activityLogger.log(companyId, RecordActivityLogger.MODEL_SALES_ORDER, inv.getSalesOrderId(),
                        (refund ? "Customer refund registered: " : "Customer payment registered: ") + amountLabel);
            }
        }
        return responseFor(paymentRepository.findById(payment.getId()).orElseThrow());
    }

    @Transactional
    public CustomerPaymentResponse allocate(UUID paymentId, AllocateCustomerPaymentCommand command) {
        CustomerPayment payment = loadPayment(paymentId, true);
        if (payment.getPaymentKind() == CustomerPaymentKind.REFUND) {
            throw new AccountingDomainException("error.accounting.refundAllocationFixed", null,
                    "Refunds are allocated when registered and cannot be re-allocated");
        }
        Map<UUID, CustomerInvoice> docs = new LinkedHashMap<>();
        for (CustomerPaymentAllocationLine line : command.getAllocations()) {
            if (docs.containsKey(line.getInvoiceId())) {
                throw new AccountingDomainException("error.accounting.allocationDuplicateDocument", null,
                        "The same document appears twice in the allocations");
            }
            docs.put(line.getInvoiceId(), invoiceRepository.findById(line.getInvoiceId())
                    .orElseThrow(() -> new AccountingDomainException(
                            "error.accounting.customerInvoiceNotFound", null, "Customer invoice not found")));
        }
        docs.keySet().stream().sorted().forEach(invoiceRepository::lockById);
        for (CustomerPaymentAllocationLine line : command.getAllocations()) {
            CustomerPaymentAllocation a = allocateLocked(payment, docs.get(line.getInvoiceId()),
                    scale(line.getAmount()), line.getPaymentAmount(), command.getAllocationDate());
            activityLogger.log(payment.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_PAYMENT, payment.getId(),
                    "Allocated " + MonetaryScale.toDisplayString(a.getAmount()) + " " + payment.getCurrencyCode()
                            + " to " + docLabel(docs.get(line.getInvoiceId())));
        }
        syncGraph(docs.keySet(), Set.of(payment.getId()));
        return responseFor(paymentRepository.findById(payment.getId()).orElseThrow());
    }

    @Transactional
    public CustomerPaymentResponse deallocate(UUID allocationId) {
        CustomerPaymentAllocation a = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new AccountingDomainException("error.accounting.allocationNotFound", null, "Allocation not found"));
        CustomerPayment payment = loadPayment(a.getPaymentId(), true);
        invoiceRepository.lockById(a.getCustomerInvoiceId());
        a = allocationRepository.findById(allocationId).orElseThrow();
        if (!a.isActive()) {
            throw new AccountingDomainException("error.accounting.allocationNotActive", null, "Allocation is already reversed");
        }
        if (payment.getPaymentKind() == CustomerPaymentKind.REFUND) {
            throw new AccountingDomainException("error.accounting.refundAllocationFixed", null,
                    "Refunds are allocated when registered; reverse the refund instead");
        }
        reverseAllocation(a, "Allocation removed");
        CustomerInvoice inv = invoiceRepository.findById(a.getCustomerInvoiceId()).orElse(null);
        activityLogger.log(payment.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_PAYMENT, payment.getId(),
                "Allocation removed: " + MonetaryScale.toDisplayString(a.getAmount()) + " " + payment.getCurrencyCode()
                        + (inv != null ? " from " + docLabel(inv) : ""));
        syncGraph(Set.of(a.getCustomerInvoiceId()), Set.of(payment.getId()));
        return responseFor(paymentRepository.findById(payment.getId()).orElseThrow());
    }

    @Transactional
    public CustomerPaymentResponse reverse(UUID paymentId, String reason) {
        CustomerPayment p = loadPayment(paymentId, true);
        if (p.getState() != CustomerPaymentState.POSTED) {
            throw new AccountingDomainException("error.accounting.onlyPostedPaymentReversible", null,
                    "Only posted payments can be reversed");
        }
        if (p.getJournalEntryId() == null) {
            throw new AccountingDomainException("error.accounting.paymentHasNoEntry", null,
                    "Payment has no journal entry to reverse");
        }
        String why = reason != null && !reason.isBlank() ? reason : "Payment reverse";
        List<CustomerPaymentAllocation> active = allocationRepository.findByPaymentIdIn(List.of(p.getId())).stream()
                .filter(CustomerPaymentAllocation::isActive)
                .toList();
        Set<UUID> invoiceIds = active.stream().map(CustomerPaymentAllocation::getCustomerInvoiceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        invoiceIds.stream().sorted().forEach(invoiceRepository::lockById);
        for (CustomerPaymentAllocation a : active) {
            reverseAllocation(a, why);
        }
        var reverseResp = journalEntryApplicationService.reverseJournalEntry(
                new ReverseJournalEntryCommand(p.getJournalEntryId(), why));
        p.setState(CustomerPaymentState.REVERSED);
        p.setReversalJournalEntryId(reverseResp.getReversalJournalEntryId());
        p.setUpdatedAt(Instant.now());
        paymentRepository.save(p);
        syncGraph(invoiceIds, Set.of(p.getId()));
        CustomerPaymentEventPort events = paymentEventPortProvider.getIfAvailable();
        if (events != null) {
            events.onPaymentReversed(p.getCompanyId(), p.getId());
        }
        activityLogger.log(p.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_PAYMENT, p.getId(),
                "Payment reversed: " + (reason != null ? reason : ""));
        return responseFor(paymentRepository.findById(p.getId()).orElseThrow());
    }

    // ------------------------------------------------------------------ customer credit

    private static boolean isCreditNoteDoc(CustomerInvoice inv) {
        return inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE;
    }

    private List<CustomerInvoice> postedCreditNotesOf(UUID sourceInvoiceId) {
        List<CustomerInvoice> out = new ArrayList<>();
        for (CustomerInvoice cn : invoiceRepository.findByReversedInvoiceIdWithLines(sourceInvoiceId)) {
            if (cn.getState() == CustomerInvoiceState.POSTED && isCreditNoteDoc(cn)) {
                out.add(cn);
            }
        }
        return out;
    }

    /**
     * Money the customer still has with us on a posted invoice: paid beyond what is owed after its
     * credit notes, minus what was already refunded through them. Zero for credit notes.
     */
    @Transactional(readOnly = true)
    public BigDecimal creditAvailableOn(CustomerInvoice invoice) {
        if (isCreditNoteDoc(invoice) || invoice.getState() != CustomerInvoiceState.POSTED) {
            return BigDecimal.ZERO.setScale(4);
        }
        CustomerInvoice source = invoiceRepository.findByIdWithLines(invoice.getId()).orElse(invoice);
        DocumentBalance b = balanceOf(source);
        BigDecimal overpaid = b.paid().add(b.credited()).subtract(b.total());
        if (overpaid.signum() <= 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        List<CustomerInvoice> cns = postedCreditNotesOf(source.getId());
        BigDecimal refunded = BigDecimal.ZERO;
        if (!cns.isEmpty()) {
            refunded = sum(sumActiveAllocationsByInvoiceIds(cns.stream().map(CustomerInvoice::getId).toList())
                    .values().stream().toList(), v -> v);
        }
        return overpaid.subtract(refunded).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /** How much of a credit note can be paid back now: the customer credit on its source, capped by the note. */
    @Transactional(readOnly = true)
    public BigDecimal refundableOf(CustomerInvoice creditNote) {
        if (!isCreditNoteDoc(creditNote) || creditNote.getState() != CustomerInvoiceState.POSTED) {
            return BigDecimal.ZERO.setScale(4);
        }
        BigDecimal own = balanceOf(creditNote).residual().max(BigDecimal.ZERO);
        if (creditNote.getReversedInvoiceId() == null) {
            return own;
        }
        CustomerInvoice source = invoiceRepository.findByIdWithLines(creditNote.getReversedInvoiceId()).orElse(null);
        if (source == null) {
            return own;
        }
        return creditAvailableOn(source).min(own).setScale(4, RoundingMode.HALF_UP);
    }

    /** Pays the customer's credit on {@code invoiceId} back through the invoice's credit notes. */
    @Transactional
    public CustomerPaymentResponse refundCredit(UUID invoiceId, RefundCustomerCreditCommand command) {
        CustomerInvoice source = loadPostedInvoice(invoiceId);
        BigDecimal remaining = creditAvailableOn(source);
        if (remaining.signum() <= 0) {
            throw new AccountingDomainException("error.accounting.noCustomerCredit", null,
                    "The customer has no credit on this invoice to refund");
        }
        List<CustomerPaymentAllocationLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CustomerInvoice cn : postedCreditNotesOf(source.getId())) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = remaining.min(balanceOf(cn).residual().max(BigDecimal.ZERO));
            if (take.signum() > 0) {
                lines.add(new CustomerPaymentAllocationLine(cn.getId(), take));
                total = total.add(take);
                remaining = remaining.subtract(take);
            }
        }
        if (lines.isEmpty()) {
            throw new AccountingDomainException("error.accounting.noCustomerCredit", null,
                    "The customer has no credit on this invoice to refund");
        }
        RegisterCustomerPaymentCommand cmd = new RegisterCustomerPaymentCommand();
        cmd.setCompanyId(source.getCompanyId());
        cmd.setCustomerPartnerId(source.getCustomerPartnerId());
        cmd.setPaymentJournalId(command.getPaymentJournalId());
        cmd.setPaymentDate(command.getPaymentDate() != null ? command.getPaymentDate() : LocalDateTime.now());
        cmd.setAmount(total);
        cmd.setCurrencyCode(source.getCurrencyCode());
        cmd.setReference(command.getReference());
        cmd.setAllocations(lines);
        return register(cmd);
    }

    /**
     * Releases the customer's credit on {@code invoiceId} from the invoice so it becomes an open
     * receipt of the customer, ready to settle their next invoice. Returns the amount released.
     */
    @Transactional
    public BigDecimal keepCredit(UUID invoiceId) {
        CustomerInvoice source = loadPostedInvoice(invoiceId);
        invoiceRepository.lockById(invoiceId);
        BigDecimal amount = creditAvailableOn(source);
        if (amount.signum() <= 0) {
            throw new AccountingDomainException("error.accounting.noCustomerCredit", null,
                    "The customer has no credit on this invoice to keep");
        }
        BigDecimal remaining = amount;
        Set<UUID> paymentIds = new LinkedHashSet<>();
        List<CustomerPaymentAllocation> active = allocationRepository.findByInvoiceIdIn(List.of(invoiceId)).stream()
                .filter(CustomerPaymentAllocation::isActive)
                .sorted(Comparator.comparing(CustomerPaymentAllocation::getCreatedAt).reversed())
                .toList();
        for (CustomerPaymentAllocation a : active) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal cut = a.getAmount().min(remaining);
            CustomerPayment payment = loadPayment(a.getPaymentId(), true);
            BigDecimal keep = a.getAmount().subtract(cut);
            // The kept part keeps its share of the payment amount (they differ across currencies).
            BigDecimal keepPayment = a.getAmount().signum() == 0 ? keep
                    : a.getPaymentAmount().multiply(keep).divide(a.getAmount(), 4, RoundingMode.HALF_UP);
            LocalDate date = a.getAllocationDate();
            reverseAllocation(a, "Credit kept for the customer");
            if (keep.signum() > 0) {
                allocateLocked(payment, source, keep, keepPayment, date);
            }
            paymentIds.add(payment.getId());
            remaining = remaining.subtract(cut);
        }
        syncGraph(Set.of(invoiceId), paymentIds);
        activityLogger.log(source.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, invoiceId,
                "Customer credit kept: " + MonetaryScale.toDisplayString(amount) + " " + source.getCurrencyCode());
        return amount;
    }

    /**
     * Settles a posted invoice with the customer's open receipts (advances, overpayments, credit
     * kept), oldest first. Returns the amount applied.
     */
    @Transactional
    public BigDecimal applyCredit(UUID invoiceId) {
        BigDecimal applied = applyCredit(invoiceId, null);
        if (applied.signum() <= 0) {
            throw new AccountingDomainException("error.accounting.noCustomerCredit", null,
                    "The customer has no open credit to apply");
        }
        return applied;
    }

    /** Like {@link #applyCredit(UUID)} but never applies more than {@code limit} (null = no limit) and may apply nothing. */
    @Transactional
    public BigDecimal applyCredit(UUID invoiceId, BigDecimal limit) {
        CustomerInvoice inv = loadPostedInvoice(invoiceId);
        if (isCreditNoteDoc(inv)) {
            throw new AccountingDomainException("error.accounting.allocationKindMismatch", null,
                    "Receipts can only settle invoices");
        }
        invoiceRepository.lockById(invoiceId);
        BigDecimal residual = balanceOf(inv).residual();
        if (limit != null) {
            residual = residual.min(limit);
        }
        List<CustomerPayment> receipts = paymentRepository
                .findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(
                        inv.getCompanyId(), inv.getCustomerPartnerId()).stream()
                .filter(p -> p.getState() == CustomerPaymentState.POSTED
                        && p.getPaymentKind() != CustomerPaymentKind.REFUND)
                .toList();
        Map<UUID, BigDecimal> open = unallocatedByPaymentId(receipts);
        BigDecimal applied = BigDecimal.ZERO;
        Set<UUID> paymentIds = new LinkedHashSet<>();
        for (CustomerPayment p : receipts) {
            if (residual.signum() <= 0) {
                break;
            }
            BigDecimal openPayment = open.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal take;
            BigDecimal paymentTake;
            if (inv.getCurrencyCode().equalsIgnoreCase(p.getCurrencyCode())) {
                take = openPayment.min(residual);
                paymentTake = take;
            } else {
                // Credit held in another currency: use as much as the invoice still owes.
                BigDecimal coverable = paymentToDoc(openPayment, inv, p.getExchangeRateToCompany());
                if (coverable.compareTo(residual) >= 0) {
                    take = residual;
                    paymentTake = docToPayment(residual, inv, p.getExchangeRateToCompany()).min(openPayment);
                } else {
                    take = coverable;
                    paymentTake = openPayment;
                }
            }
            if (take.signum() <= 0 || paymentTake.signum() <= 0) {
                continue;
            }
            allocateLocked(loadPayment(p.getId(), true), inv, take, paymentTake, null);
            paymentIds.add(p.getId());
            applied = applied.add(take);
            residual = residual.subtract(take);
        }
        if (applied.signum() <= 0) {
            return applied;
        }
        syncGraph(Set.of(invoiceId), paymentIds);
        activityLogger.log(inv.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, invoiceId,
                "Customer credit applied: " + MonetaryScale.toDisplayString(applied) + " " + inv.getCurrencyCode());
        return applied;
    }

    /**
     * Fixes a wrongly recorded payment in one step: the payment is reversed and re-registered with
     * the corrected values, applied to the same invoices (up to what each still owes).
     */
    @Transactional
    public CustomerPaymentResponse correctPayment(UUID paymentId, CorrectCustomerPaymentCommand command) {
        CustomerPayment old = loadPayment(paymentId, true);
        if (old.getState() != CustomerPaymentState.POSTED || old.isOpeningBalance()) {
            throw new AccountingDomainException("error.accounting.onlyPostedPaymentReversible", null,
                    "Only posted payments can be reversed");
        }
        List<CustomerPaymentAllocation> oldAllocations = allocationRepository.findByPaymentIdIn(List.of(old.getId()))
                .stream().filter(CustomerPaymentAllocation::isActive)
                .sorted(Comparator.comparing(CustomerPaymentAllocation::getCreatedAt)).toList();
        UUID liquidity = null;
        for (ItemSnapshot item : journalItemPort.findItemsByEntryIds(List.of(old.getJournalEntryId()))) {
            if (item.accountType() != AccountType.RECEIVABLE) {
                liquidity = item.accountId();
            }
        }
        String why = command.getReason() != null && !command.getReason().isBlank()
                ? command.getReason() : "Payment corrected";
        reverse(old.getId(), why);

        BigDecimal amount = command.getAmount() != null ? scale(command.getAmount()) : old.getAmount();
        RegisterCustomerPaymentCommand cmd = new RegisterCustomerPaymentCommand();
        cmd.setCompanyId(old.getCompanyId());
        cmd.setCustomerPartnerId(old.getCustomerPartnerId());
        cmd.setPaymentJournalId(command.getPaymentJournalId() != null ? command.getPaymentJournalId() : old.getPaymentJournalId());
        cmd.setPaymentDate(command.getPaymentDate() != null ? command.getPaymentDate() : old.getPaymentDate());
        cmd.setAmount(amount);
        cmd.setCurrencyCode(old.getCurrencyCode());
        cmd.setExchangeRateToCompany(old.getExchangeRateToCompany());
        cmd.setReference(command.getReference() != null ? command.getReference() : old.getReference());
        cmd.setLiquidityAccountId(liquidity);
        BigDecimal left = amount;
        List<CustomerPaymentAllocationLine> lines = new ArrayList<>();
        for (CustomerPaymentAllocation a : oldAllocations) {
            if (left.signum() <= 0) {
                break;
            }
            CustomerInvoice doc = invoiceRepository.findByIdWithLines(a.getCustomerInvoiceId()).orElse(null);
            if (doc == null) {
                continue;
            }
            BigDecimal residual = balanceOf(doc).residual();
            if (doc.getCurrencyCode().equalsIgnoreCase(old.getCurrencyCode())) {
                BigDecimal take = left.min(residual);
                if (take.signum() > 0) {
                    lines.add(new CustomerPaymentAllocationLine(doc.getId(), take));
                    left = left.subtract(take);
                }
            } else {
                // Other currency: `left` is in the payment currency, the document balance in its own.
                BigDecimal payCapacity = docToPayment(residual, doc, old.getExchangeRateToCompany());
                BigDecimal payTake = left.min(payCapacity);
                if (payTake.signum() > 0) {
                    BigDecimal docTake = payTake.compareTo(payCapacity) == 0 ? residual
                            : paymentToDoc(payTake, doc, old.getExchangeRateToCompany()).min(residual);
                    CustomerPaymentAllocationLine line = new CustomerPaymentAllocationLine(doc.getId(), docTake);
                    line.setPaymentAmount(payTake);
                    lines.add(line);
                    left = left.subtract(payTake);
                }
            }
        }
        cmd.setAllocations(lines);
        CustomerPaymentResponse created = register(cmd);
        CustomerPaymentEventPort events = paymentEventPortProvider.getIfAvailable();
        if (events != null) {
            events.onPaymentCorrected(old.getCompanyId(), old.getId(), created.getId(), amount);
        }
        activityLogger.log(old.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_PAYMENT, created.getId(),
                "Payment corrected (replaces a reversed payment): " + why);
        return created;
    }

    private CustomerInvoice loadPostedInvoice(UUID invoiceId) {
        CustomerInvoice inv = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException(
                        "error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        if (inv.getState() != CustomerInvoiceState.POSTED || inv.getJournalEntryId() == null) {
            throw new AccountingDomainException("error.accounting.invoiceMustBePostedBeforePayment", null,
                    "Invoice must be posted before payment");
        }
        return inv;
    }

    /**
     * Opening customer credit: an unallocated receipt on the opening journal against Opening
     * Balance Equity (Dr OBE / Cr AR).
     */
    @Transactional
    public CustomerPayment postOpeningPayment(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                              BigDecimal exchangeRate, LocalDate date, String reference,
                                              Journal openingJournal, UUID openingEquityAccountId) {
        PartnerResponse customer = partnerApplicationService.getPartner(partnerId);
        if (!companyId.equals(customer.getCompanyId())) {
            throw new AccountingDomainException("error.accounting.customerCompanyMismatch", null, "Customer belongs to another company");
        }
        return postPaymentEntry(companyId, customer, openingJournal, openingEquityAccountId, "Opening balance",
                date.atStartOfDay(), scale(amount), currency.toUpperCase(), exchangeRate, reference, false, true);
    }

    /** Re-derive reconciliation tags around the given documents (e.g. after a credit note posts). */
    @Transactional
    public void syncForInvoices(Collection<UUID> invoiceIds) {
        syncGraph(invoiceIds, Set.of());
    }

    /** Re-derive every receivable reconciliation tag of the company from its allocations. */
    @Transactional
    public void rebuildCompany(UUID companyId) {
        List<CustomerInvoice> invoices = invoiceRepository.findByCompanyWithLines(companyId);
        List<CustomerPayment> payments = paymentRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId);
        List<CustomerPaymentAllocation> allocations = allocationRepository.findByCompanyId(companyId);
        applyGraph(invoices, payments, allocations);
    }

    // ------------------------------------------------------------------ internals

    private CustomerPayment postPaymentEntry(UUID companyId, PartnerResponse customer, Journal journal,
                                             UUID counterAccount, String counterLabel, LocalDateTime paymentDate,
                                             BigDecimal amount, String currency, BigDecimal explicitRate,
                                             String reference, boolean refund, boolean opening) {
        if (amount.signum() <= 0) {
            throw new AccountingDomainException("error.accounting.paymentAmountPositive", null, "Payment amount must be positive");
        }
        periodPostingGuard.assertDatePostable(companyId, paymentDate.toLocalDate());
        UUID receivable = receivableAccount(companyId, customer);
        BigDecimal rate = resolveExchangeRate(companyId, currency, paymentDate.toLocalDate(), explicitRate);
        BigDecimal comp = CurrencyMath.convertAtRate(amount, rate);
        String arLabel = (refund ? "Refund" : "Payment") + (reference != null && !reference.isBlank() ? " " + reference : "");
        List<JournalItemCommand> items = new ArrayList<>();
        if (refund) {
            items.add(new JournalItemCommand(receivable, arLabel, comp, BigDecimal.ZERO, currency, amount, customer.getId()));
            items.add(new JournalItemCommand(counterAccount, counterLabel, BigDecimal.ZERO, comp, currency, amount.negate(), null));
        } else {
            items.add(new JournalItemCommand(counterAccount, counterLabel, comp, BigDecimal.ZERO, currency, amount, null));
            items.add(new JournalItemCommand(receivable, arLabel, BigDecimal.ZERO, comp, currency, amount.negate(), customer.getId()));
        }
        CreateJournalEntryResponse entry = journalEntryApplicationService.createJournalEntry(new CreateJournalEntryCommand(
                companyId, journal.getId().getId(), "", JournalEntryTiming.ensureTimed(paymentDate), currency,
                customer.getId(), items));
        journalEntryApplicationService.postJournalEntry(entry.getJournalEntryId());
        String sequence = journalEntryApplicationService.getJournalEntry(entry.getJournalEntryId()).getSequenceNumber();

        Instant now = Instant.now();
        CustomerPayment p = new CustomerPayment();
        p.setId(UUID.randomUUID());
        p.setCompanyId(companyId);
        p.setCustomerPartnerId(customer.getId());
        p.setPaymentDate(paymentDate);
        p.setPaymentJournalId(journal.getId().getId());
        p.setAmount(amount);
        p.setCurrencyCode(currency);
        p.setExchangeRateToCompany(rate);
        p.setState(CustomerPaymentState.POSTED);
        p.setPaymentKind(refund ? CustomerPaymentKind.REFUND : CustomerPaymentKind.PAYMENT);
        p.setJournalEntryId(entry.getJournalEntryId());
        p.setReference(reference != null && !reference.isBlank() ? reference : sequence);
        p.setOpeningBalance(opening);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return paymentRepository.save(p);
    }

    /** Caller must hold the payment lock and the document lock. */
    private CustomerPaymentAllocation allocateLocked(CustomerPayment payment, CustomerInvoice docRef,
                                                     BigDecimal amount, LocalDate requestedDate) {
        return allocateLocked(payment, docRef, amount, null, requestedDate);
    }

    /**
     * {@code amount} is in the document's currency. When the payment is in another currency,
     * {@code explicitPaymentAmount} is the part of the payment used (otherwise it is derived from the
     * two exchange rates). Same-currency allocations use the same figure for both.
     */
    private CustomerPaymentAllocation allocateLocked(CustomerPayment payment, CustomerInvoice docRef,
                                                     BigDecimal amount, BigDecimal explicitPaymentAmount,
                                                     LocalDate requestedDate) {
        CustomerInvoice inv = invoiceRepository.findByIdWithLines(docRef.getId())
                .orElseThrow(() -> new AccountingDomainException(
                        "error.accounting.customerInvoiceNotFound", null, "Customer invoice not found"));
        boolean refund = payment.getPaymentKind() == CustomerPaymentKind.REFUND;
        if (payment.getState() != CustomerPaymentState.POSTED) {
            throw new AccountingDomainException("error.accounting.allocationPaymentNotPosted", null,
                    "Only posted payments can be allocated");
        }
        if (!inv.getCompanyId().equals(payment.getCompanyId())) {
            throw new AccountingDomainException("error.accounting.invoiceCompanyMismatch", null, "Invoice company mismatch");
        }
        if (inv.getState() != CustomerInvoiceState.POSTED || inv.getJournalEntryId() == null) {
            throw new AccountingDomainException("error.accounting.invoiceMustBePostedBeforePayment", null,
                    "Invoice must be posted before payment");
        }
        if (!inv.getCustomerPartnerId().equals(payment.getCustomerPartnerId())) {
            throw new AccountingDomainException("error.accounting.allocationPartnerMismatch", null,
                    "Payment and document belong to different customers");
        }
        boolean sameCurrency = inv.getCurrencyCode().equalsIgnoreCase(payment.getCurrencyCode());
        if (refund != isCreditNote(inv)) {
            throw new AccountingDomainException("error.accounting.allocationKindMismatch", null,
                    refund ? "Refunds can only settle credit notes" : "Receipts can only settle invoices");
        }
        if (amount.signum() <= 0) {
            throw new AccountingDomainException("error.accounting.allocationAmountPositive", null,
                    "Allocation amount must be positive");
        }

        List<CustomerPaymentAllocation> paymentAllocs = allocationRepository.findByPaymentIdIn(List.of(payment.getId()))
                .stream().filter(CustomerPaymentAllocation::isActive).toList();
        BigDecimal paymentAmount = sameCurrency ? amount
                : explicitPaymentAmount != null ? scale(explicitPaymentAmount)
                : docToPayment(amount, inv, payment.getExchangeRateToCompany());
        if (paymentAmount.signum() <= 0) {
            throw new AccountingDomainException("error.accounting.allocationAmountPositive", null,
                    "Allocation amount must be positive");
        }
        BigDecimal unallocated = unallocated(payment, sum(paymentAllocs, CustomerPaymentAllocation::getPaymentAmount));
        if (paymentAmount.compareTo(unallocated) > 0) {
            throw new AccountingDomainException("error.accounting.allocationExceedsUnallocated",
                    new Object[] { MonetaryScale.toDisplayString(unallocated), payment.getCurrencyCode() },
                    "Allocation exceeds the payment's unallocated amount of "
                            + MonetaryScale.toDisplayString(unallocated) + " " + payment.getCurrencyCode());
        }
        if (refund) {
            // A credit note first cancels what the customer still owes; only money the customer has
            // actually paid on the source invoice can go back to them.
            BigDecimal refundable = refundableOf(inv);
            if (amount.compareTo(refundable) > 0) {
                throw new AccountingDomainException("error.accounting.refundExceedsCustomerCredit",
                        new Object[] { MonetaryScale.toDisplayString(refundable), inv.getCurrencyCode() },
                        "Refund exceeds the customer credit of " + MonetaryScale.toDisplayString(refundable)
                                + " " + inv.getCurrencyCode() + ": the customer has not paid that much on the invoice");
            }
        }
        DocumentBalance balance = balanceOf(inv);
        if (balance.residual().signum() <= 0) {
            if (refund) {
                throw new AccountingDomainException("error.accounting.creditNoteFullyRefunded", null,
                        "Credit note is already fully refunded");
            }
            throw new AccountingDomainException("error.accounting.invoiceFullyPaid", null, "Customer invoice is already fully paid");
        }
        if (amount.compareTo(balance.residual()) > 0) {
            throw new AccountingDomainException("error.accounting.allocationExceedsOutstanding",
                    new Object[] { MonetaryScale.toDisplayString(balance.residual()), inv.getCurrencyCode() },
                    "Payment amount exceeds outstanding balance of "
                            + MonetaryScale.toDisplayString(balance.residual()) + " " + inv.getCurrencyCode());
        }
        LocalDate paymentDay = payment.getPaymentDate().toLocalDate();
        LocalDate latest = paymentDay.isAfter(inv.getInvoiceDate()) ? paymentDay : inv.getInvoiceDate();
        LocalDate allocationDate = requestedDate != null ? requestedDate : latest;
        if (allocationDate.isBefore(latest)) {
            throw new AccountingDomainException("error.accounting.allocationDateTooEarly", null,
                    "Allocation date cannot be before the payment or document date");
        }

        TradeLine docLine = tradeLine(inv.getJournalEntryId(), inv.getCustomerPartnerId());
        BigDecimal docSide;
        if (amount.compareTo(balance.residual()) == 0) {
            BigDecimal creditedComp = BigDecimal.ZERO;
            if (!refund) {
                for (CustomerInvoice cn : invoiceRepository.findByReversedInvoiceIdIn(List.of(inv.getId()))) {
                    if (isPostedCreditNoteFor(cn, inv) && cn.getJournalEntryId() != null) {
                        creditedComp = creditedComp.add(tradeLine(cn.getJournalEntryId(), cn.getCustomerPartnerId()).absAmount());
                    }
                }
            }
            BigDecimal priorComp = sum(allocationRepository.findByInvoiceIdIn(List.of(inv.getId())).stream()
                    .filter(CustomerPaymentAllocation::isActive).toList(), CustomerPaymentAllocation::getAmountCompany);
            docSide = docLine.absAmount().subtract(creditedComp).subtract(priorComp);
        } else {
            docSide = CurrencyMath.convertAtRate(amount, inv.getExchangeRateToCompany());
        }
        BigDecimal paySide;
        if (paymentAmount.compareTo(unallocated) == 0) {
            paySide = tradeLine(payment.getJournalEntryId(), payment.getCustomerPartnerId()).absAmount()
                    .subtract(sum(paymentAllocs, CustomerPaymentAllocation::getPaymentAmountCompany));
        } else {
            paySide = CurrencyMath.convertAtRate(paymentAmount, payment.getExchangeRateToCompany());
        }
        // Receivable left open by this pair: document side (debit for invoices) + payment side.
        BigDecimal residualComp = (refund ? docSide.negate().add(paySide) : docSide.subtract(paySide))
                .setScale(4, RoundingMode.HALF_UP);

        UUID fxEntryId = null;
        if (residualComp.signum() != 0) {
            fxEntryId = postExchangeDifference(payment, inv, docLine.accountId(), residualComp, allocationDate);
        }
        Instant now = Instant.now();
        CustomerPaymentAllocation a = new CustomerPaymentAllocation();
        a.setId(UUID.randomUUID());
        a.setCompanyId(payment.getCompanyId());
        a.setPaymentId(payment.getId());
        a.setCustomerInvoiceId(inv.getId());
        a.setAmount(amount);
        a.setPaymentAmount(paymentAmount);
        a.setAmountCompany(docSide.setScale(4, RoundingMode.HALF_UP));
        a.setPaymentAmountCompany(paySide.setScale(4, RoundingMode.HALF_UP));
        a.setAllocationDate(allocationDate);
        a.setFxJournalEntryId(fxEntryId);
        a.setState(CustomerPaymentAllocationState.ACTIVE);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        CustomerPaymentAllocation saved = allocationRepository.save(a);
        activityLogger.log(inv.getCompanyId(), RecordActivityLogger.MODEL_CUSTOMER_INVOICE, inv.getId(),
                (refund ? "Refund registered: " : "Payment registered: ")
                        + MonetaryScale.toDisplayString(amount) + " " + payment.getCurrencyCode());
        return saved;
    }

    /** Positive residual = receivable still debit → credit AR against exchange loss. */
    private UUID postExchangeDifference(CustomerPayment payment, CustomerInvoice inv, UUID arAccount,
                                        BigDecimal residualComp, LocalDate date) {
        UUID companyId = payment.getCompanyId();
        periodPostingGuard.assertDatePostable(companyId, date);
        BigDecimal abs = residualComp.abs();
        String label = "Exchange difference " + (payment.getReference() != null ? payment.getReference() : "")
                + " / " + docLabel(inv);
        List<JournalItemCommand> items = new ArrayList<>();
        if (residualComp.signum() > 0) {
            items.add(new JournalItemCommand(accountByCode(companyId, EXCHANGE_LOSS_ACCOUNT_CODE,
                    "error.accounting.exchangeLossAccountNotFound"), "Exchange loss", abs, BigDecimal.ZERO, null, null, null));
            items.add(new JournalItemCommand(arAccount, label, BigDecimal.ZERO, abs,
                    inv.getCurrencyCode(), BigDecimal.ZERO, inv.getCustomerPartnerId()));
        } else {
            items.add(new JournalItemCommand(arAccount, label, abs, BigDecimal.ZERO,
                    inv.getCurrencyCode(), BigDecimal.ZERO, inv.getCustomerPartnerId()));
            items.add(new JournalItemCommand(accountByCode(companyId, EXCHANGE_GAIN_ACCOUNT_CODE,
                    "error.accounting.exchangeGainAccountNotFound"), "Exchange gain", BigDecimal.ZERO, abs, null, null, null));
        }
        Journal journal = exchangeJournal(companyId);
        CreateJournalEntryResponse entry = journalEntryApplicationService.createJournalEntry(new CreateJournalEntryCommand(
                companyId, journal.getId().getId(), "", JournalEntryTiming.ofBusinessDate(date),
                currencyConversionPort.baseCurrencyCode(companyId), inv.getCustomerPartnerId(), items));
        journalEntryApplicationService.postJournalEntry(entry.getJournalEntryId());
        return entry.getJournalEntryId();
    }

    private void reverseAllocation(CustomerPaymentAllocation a, String reason) {
        if (a.getFxJournalEntryId() != null) {
            var resp = journalEntryApplicationService.reverseJournalEntry(
                    new ReverseJournalEntryCommand(a.getFxJournalEntryId(), reason));
            a.setFxReversalJournalEntryId(resp.getReversalJournalEntryId());
        }
        a.setState(CustomerPaymentAllocationState.REVERSED);
        a.setUpdatedAt(Instant.now());
        allocationRepository.save(a);
    }

    private void syncGraph(Collection<UUID> invoiceSeeds, Collection<UUID> paymentSeeds) {
        Map<UUID, CustomerInvoice> invoices = new LinkedHashMap<>();
        Map<UUID, CustomerPayment> payments = new LinkedHashMap<>();
        Map<UUID, CustomerPaymentAllocation> allocations = new LinkedHashMap<>();
        Set<UUID> pendingInvoices = new LinkedHashSet<>(invoiceSeeds);
        Set<UUID> pendingPayments = new LinkedHashSet<>(paymentSeeds);
        while (!pendingInvoices.isEmpty() || !pendingPayments.isEmpty()) {
            Set<UUID> invBatch = new LinkedHashSet<>(pendingInvoices);
            invBatch.removeAll(invoices.keySet());
            pendingInvoices.clear();
            Set<UUID> payBatch = new LinkedHashSet<>(pendingPayments);
            payBatch.removeAll(payments.keySet());
            pendingPayments.clear();
            if (!invBatch.isEmpty()) {
                for (CustomerInvoice inv : invoiceRepository.findByIdIn(invBatch)) {
                    invoices.put(inv.getId(), inv);
                    if (inv.getReversedInvoiceId() != null && !invoices.containsKey(inv.getReversedInvoiceId())) {
                        pendingInvoices.add(inv.getReversedInvoiceId());
                    }
                }
                for (CustomerInvoice cn : invoiceRepository.findByReversedInvoiceIdIn(invBatch)) {
                    if (!invoices.containsKey(cn.getId())) pendingInvoices.add(cn.getId());
                }
                for (CustomerPaymentAllocation a : allocationRepository.findByInvoiceIdIn(invBatch)) {
                    allocations.put(a.getId(), a);
                    if (a.isActive() && !payments.containsKey(a.getPaymentId())) pendingPayments.add(a.getPaymentId());
                }
            }
            if (!payBatch.isEmpty()) {
                for (CustomerPayment p : paymentRepository.findByIdIn(payBatch)) {
                    payments.put(p.getId(), p);
                }
                for (CustomerPaymentAllocation a : allocationRepository.findByPaymentIdIn(payBatch)) {
                    allocations.put(a.getId(), a);
                    if (a.isActive() && !invoices.containsKey(a.getCustomerInvoiceId())) {
                        pendingInvoices.add(a.getCustomerInvoiceId());
                    }
                }
            }
        }
        applyGraph(invoices.values(), payments.values(), allocations.values());
    }

    private void applyGraph(Collection<CustomerInvoice> invoices, Collection<CustomerPayment> payments,
                            Collection<CustomerPaymentAllocation> allocations) {
        Map<UUID, CustomerInvoice> invoiceById = invoices.stream()
                .collect(Collectors.toMap(CustomerInvoice::getId, Function.identity(), (a, b) -> a));
        Map<UUID, CustomerPayment> paymentById = payments.stream()
                .collect(Collectors.toMap(CustomerPayment::getId, Function.identity(), (a, b) -> a));
        List<UUID> cancelledEntries = invoices.stream()
                .filter(i -> i.getState() == CustomerInvoiceState.CANCELLED && i.getJournalEntryId() != null)
                .map(CustomerInvoice::getJournalEntryId)
                .toList();
        Map<UUID, UUID> reversals = journalItemPort.findPostedReversalEntryIds(cancelledEntries);

        Map<UUID, TradeReconciliationGraph> graphs = new LinkedHashMap<>();
        Function<UUID, TradeReconciliationGraph> graphFor = partner -> graphs.computeIfAbsent(partner, k -> new TradeReconciliationGraph());
        for (CustomerInvoice inv : invoices) {
            TradeReconciliationGraph g = graphFor.apply(inv.getCustomerPartnerId());
            if (inv.getState() == CustomerInvoiceState.POSTED) {
                g.addNode(inv.getId(), listOfNonNull(inv.getJournalEntryId()));
                CustomerInvoice source = inv.getReversedInvoiceId() != null ? invoiceById.get(inv.getReversedInvoiceId()) : null;
                if (source != null && isPostedCreditNoteFor(inv, source)) {
                    g.addEdge(inv.getId(), source.getId());
                }
            } else if (inv.getState() == CustomerInvoiceState.CANCELLED && inv.getJournalEntryId() != null) {
                g.addStandalone(listOfNonNull(inv.getJournalEntryId(), reversals.get(inv.getJournalEntryId())));
            }
        }
        for (CustomerPayment p : payments) {
            graphFor.apply(p.getCustomerPartnerId())
                    .addNode(p.getId(), listOfNonNull(p.getJournalEntryId(), p.getReversalJournalEntryId()));
        }
        for (CustomerPaymentAllocation a : allocations) {
            CustomerPayment p = paymentById.get(a.getPaymentId());
            if (p == null) continue;
            TradeReconciliationGraph g = graphFor.apply(p.getCustomerPartnerId());
            if (a.isActive()) {
                g.addNode(a.getId(), listOfNonNull(a.getFxJournalEntryId()));
                g.addEdge(a.getId(), p.getId());
                if (invoiceById.containsKey(a.getCustomerInvoiceId())) {
                    g.addEdge(a.getId(), a.getCustomerInvoiceId());
                }
            } else if (a.getFxJournalEntryId() != null) {
                g.addStandalone(listOfNonNull(a.getFxJournalEntryId(), a.getFxReversalJournalEntryId()));
            }
        }
        graphs.forEach((partner, g) -> tradeReconciliationSync.apply(partner, AccountType.RECEIVABLE, g.components()));
    }

    private List<CustomerPaymentResponse> toResponses(List<CustomerPayment> payments,
                                                      Collection<CustomerPaymentAllocation> allocations) {
        Map<UUID, List<CustomerPaymentAllocation>> byPayment = new HashMap<>();
        for (CustomerPaymentAllocation a : allocations) {
            byPayment.computeIfAbsent(a.getPaymentId(), k -> new ArrayList<>()).add(a);
        }
        Set<UUID> invoiceIds = allocations.stream().map(CustomerPaymentAllocation::getCustomerInvoiceId).collect(Collectors.toSet());
        Map<UUID, CustomerInvoice> invoices = invoiceRepository.findByIdIn(invoiceIds).stream()
                .collect(Collectors.toMap(CustomerInvoice::getId, Function.identity()));
        List<CustomerPaymentResponse> out = new ArrayList<>();
        for (CustomerPayment p : payments) {
            List<CustomerPaymentAllocation> list = byPayment.getOrDefault(p.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(CustomerPaymentAllocation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            BigDecimal allocated = sum(list.stream().filter(CustomerPaymentAllocation::isActive).toList(),
                    CustomerPaymentAllocation::getPaymentAmount);
            CustomerPaymentResponse r = new CustomerPaymentResponse();
            r.setId(p.getId());
            r.setCompanyId(p.getCompanyId());
            r.setCustomerPartnerId(p.getCustomerPartnerId());
            r.setPaymentDate(p.getPaymentDate());
            r.setPaymentJournalId(p.getPaymentJournalId());
            r.setAmount(p.getAmount());
            r.setCurrencyCode(p.getCurrencyCode());
            r.setExchangeRateToCompany(p.getExchangeRateToCompany());
            r.setJournalEntryId(p.getJournalEntryId());
            r.setReversalJournalEntryId(p.getReversalJournalEntryId());
            r.setReference(p.getReference());
            r.setState(p.getState() != null ? p.getState().name() : CustomerPaymentState.POSTED.name());
            r.setPaymentKind(p.getPaymentKind() != null ? p.getPaymentKind().name() : CustomerPaymentKind.PAYMENT.name());
            r.setOpeningBalance(p.isOpeningBalance());
            r.setAllocatedAmount(allocated);
            r.setUnallocatedAmount(unallocated(p, allocated));
            r.setAllocations(list.stream().map(a -> toAllocationResponse(a, p, invoices)).toList());
            out.add(r);
        }
        return out;
    }

    private CustomerPaymentAllocationResponse toAllocationResponse(CustomerPaymentAllocation a, CustomerPayment p,
                                                                   Map<UUID, CustomerInvoice> invoices) {
        CustomerPaymentAllocationResponse r = new CustomerPaymentAllocationResponse();
        r.setId(a.getId());
        r.setPaymentId(a.getPaymentId());
        if (p != null) {
            r.setPaymentReference(p.getReference());
            r.setPaymentDate(p.getPaymentDate());
            r.setCurrencyCode(p.getCurrencyCode());
        }
        r.setCustomerInvoiceId(a.getCustomerInvoiceId());
        CustomerInvoice inv = invoices.get(a.getCustomerInvoiceId());
        if (inv != null) {
            r.setInvoiceReference(inv.getReference());
            r.setInvoiceMoveType((inv.getMoveType() != null ? inv.getMoveType() : CustomerInvoiceMoveType.INVOICE).name());
            r.setInvoiceOpeningBalance(inv.isOpeningBalance());
            // `amount` is in the document's currency (the payment's own part is paymentAmount).
            r.setCurrencyCode(inv.getCurrencyCode());
        }
        r.setAmount(a.getAmount());
        r.setPaymentAmount(a.getPaymentAmount());
        r.setAmountCompany(a.getAmountCompany());
        r.setPaymentAmountCompany(a.getPaymentAmountCompany());
        r.setAllocationDate(a.getAllocationDate());
        r.setFxJournalEntryId(a.getFxJournalEntryId());
        r.setFxReversalJournalEntryId(a.getFxReversalJournalEntryId());
        r.setState(a.getState().name());
        return r;
    }

    private record TradeLine(UUID accountId, BigDecimal absAmount) {}

    /** The receivable line(s) of one entry for the partner; all trade lines share one account. */
    private TradeLine tradeLine(UUID journalEntryId, UUID partnerId) {
        List<ItemSnapshot> items = journalItemPort.findItemsByEntryIds(List.of(journalEntryId)).stream()
                .filter(i -> i.accountType() == AccountType.RECEIVABLE && Objects.equals(partnerId, i.partnerId()))
                .toList();
        if (items.isEmpty()) {
            throw new AccountingDomainException("error.accounting.arLineNotFoundOnEntry", null,
                    "Could not find the receivable line on the journal entry");
        }
        BigDecimal net = items.stream().map(ItemSnapshot::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TradeLine(items.get(0).accountId(), net.abs().setScale(4, RoundingMode.HALF_UP));
    }

    private CustomerPayment loadPayment(UUID paymentId, boolean forUpdate) {
        CustomerPayment p = (forUpdate ? paymentRepository.findByIdForUpdate(paymentId) : paymentRepository.findById(paymentId))
                .orElseThrow(() -> new AccountingDomainException("error.accounting.customerPaymentNotFound", null,
                        "Customer payment not found"));
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        UUID companyId = ctx != null ? ctx.currentCompany().map(CompanyId::getId).orElse(null) : null;
        if (companyId != null && !p.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("error.accounting.customerPaymentNotFound", null, "Customer payment not found");
        }
        return p;
    }

    private static BigDecimal unallocated(CustomerPayment p, BigDecimal allocated) {
        if (p.getState() != CustomerPaymentState.POSTED) {
            return BigDecimal.ZERO.setScale(4);
        }
        return p.getAmount().subtract(allocated).setScale(4, RoundingMode.HALF_UP);
    }

    /** Document-currency amount expressed in the payment currency, through the company currency. */
    private static BigDecimal docToPayment(BigDecimal docAmount, CustomerInvoice inv, BigDecimal paymentRate) {
        BigDecimal docRate = inv.getExchangeRateToCompany() != null && inv.getExchangeRateToCompany().signum() > 0
                ? inv.getExchangeRateToCompany() : BigDecimal.ONE;
        BigDecimal payRate = paymentRate != null && paymentRate.signum() > 0 ? paymentRate : BigDecimal.ONE;
        return docAmount.multiply(docRate).divide(payRate, 4, RoundingMode.HALF_UP);
    }

    /** Payment-currency amount expressed in the document currency, through the company currency. */
    private static BigDecimal paymentToDoc(BigDecimal paymentAmount, CustomerInvoice inv, BigDecimal paymentRate) {
        BigDecimal docRate = inv.getExchangeRateToCompany() != null && inv.getExchangeRateToCompany().signum() > 0
                ? inv.getExchangeRateToCompany() : BigDecimal.ONE;
        BigDecimal payRate = paymentRate != null && paymentRate.signum() > 0 ? paymentRate : BigDecimal.ONE;
        return paymentAmount.multiply(payRate).divide(docRate, 4, RoundingMode.HALF_UP);
    }

    private static boolean isCreditNote(CustomerInvoice inv) {
        return inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE;
    }

    private static boolean isPostedCreditNoteFor(CustomerInvoice cn, CustomerInvoice source) {
        return cn.getState() == CustomerInvoiceState.POSTED
                && cn.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE
                && source.getId().equals(cn.getReversedInvoiceId())
                && source.getCurrencyCode().equalsIgnoreCase(cn.getCurrencyCode());
    }

    private static String docLabel(CustomerInvoice inv) {
        return inv.getReference() != null ? inv.getReference() : inv.getId().toString();
    }

    private static <T> BigDecimal sum(Collection<T> items, Function<T, BigDecimal> f) {
        return items.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private static List<UUID> listOfNonNull(UUID... ids) {
        List<UUID> out = new ArrayList<>();
        for (UUID id : ids) if (id != null) out.add(id);
        return out;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(4, RoundingMode.HALF_UP);
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) {
            return fromCommand;
        }
        return companyContextProvider.getObject().requireCompany().getId();
    }

    private UUID receivableAccount(UUID companyId, PartnerResponse customer) {
        if (customer.getReceivableAccountId() != null) {
            return customer.getReceivableAccountId();
        }
        return accountByCode(companyId, DEFAULT_AR_ACCOUNT_CODE, "error.accounting.defaultArAccountNotFound");
    }

    private UUID accountByCode(UUID companyId, String code, String errorKey) {
        return accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), code)
                .orElseThrow(() -> new AccountingDomainException(errorKey, null, "Account " + code + " not found"))
                .getId().getId();
    }

    private UUID resolveLiquidityAccount(UUID companyId, Journal j) {
        if (!j.getCompanyId().getId().equals(companyId)) {
            throw new AccountingDomainException("error.accounting.journalCompanyMismatch", null, "Journal company mismatch");
        }
        if (j.getJournalType() != JournalType.CASH && j.getJournalType() != JournalType.BANK) {
            throw new AccountingDomainException("error.accounting.paymentJournalCashOrBank", null, "Payment journal must be cash or bank");
        }
        return accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), j.getCode())
                .orElseThrow(() -> new AccountingDomainException(
                        "error.accounting.liquidityAccountNotFoundForJournal",
                        new Object[] { j.getCode() },
                        "Liquidity account for journal code " + j.getCode() + " not found"))
                .getId().getId();
    }

    /** Dedicated exchange-difference journal (seeded by the company bootstrap, created on first use otherwise). */
    Journal exchangeJournal(UUID companyId) {
        return journalRepository.findByCompanyIdAndCode(new CompanyId(companyId), EXCHANGE_JOURNAL_CODE)
                .orElseGet(() -> journalRepository.save(Journal.builder()
                        .id(new JournalId(UUID.randomUUID()))
                        .companyId(new CompanyId(companyId))
                        .code(EXCHANGE_JOURNAL_CODE)
                        .name("Exchange Difference")
                        .journalType(JournalType.MISC)
                        .build()));
    }

    private BigDecimal resolveExchangeRate(UUID companyId, String currencyCode, LocalDate asOf, BigDecimal explicit) {
        if (explicit != null && explicit.signum() > 0) {
            String base = currencyConversionPort.baseCurrencyCode(companyId);
            if (explicit.compareTo(BigDecimal.ONE) != 0 || currencyCode.equalsIgnoreCase(base)) {
                return explicit.setScale(12, RoundingMode.HALF_UP);
            }
        }
        return currencyConversionPort.exchangeRateToCompany(companyId, currencyCode, asOf);
    }

    @Transactional(readOnly = true)
    public List<CustomerPaymentAllocation> allocationsForInvoices(Collection<UUID> invoiceIds) {
        return allocationRepository.findByInvoiceIdIn(invoiceIds);
    }
}
