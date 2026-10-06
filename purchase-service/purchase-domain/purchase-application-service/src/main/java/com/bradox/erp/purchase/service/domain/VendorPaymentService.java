package com.bradox.erp.purchase.service.domain;

import com.bradox.erp.accounting.service.domain.CurrencyMath;
import com.bradox.erp.accounting.service.domain.JournalEntryTiming;
import com.bradox.erp.accounting.service.domain.PeriodPostingGuard;
import com.bradox.erp.accounting.service.domain.TradeReconciliationGraph;
import com.bradox.erp.accounting.service.domain.TradeReconciliationSync;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort.ItemSnapshot;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalRepository;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.purchase.service.domain.dto.CorrectVendorPaymentCommand;
import com.bradox.erp.purchase.service.domain.dto.RefundVendorCreditCommand;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.bradox.erp.domain.core.entity.Journal;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.MonetaryScale;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.platform.web.CompanyContext;
import com.bradox.erp.purchase.domain.core.PurchaseDomainException;
import com.bradox.erp.purchase.domain.core.VendorBillMoveType;
import com.bradox.erp.purchase.domain.core.VendorBillState;
import com.bradox.erp.purchase.domain.core.VendorPaymentAllocationState;
import com.bradox.erp.purchase.domain.core.VendorPaymentKind;
import com.bradox.erp.purchase.domain.core.VendorPaymentState;
import com.bradox.erp.purchase.domain.core.entity.VendorBill;
import com.bradox.erp.purchase.domain.core.entity.VendorPayment;
import com.bradox.erp.purchase.domain.core.entity.VendorPaymentAllocation;
import com.bradox.erp.purchase.service.domain.dto.AllocateVendorPaymentCommand;
import com.bradox.erp.purchase.service.domain.dto.RegisterVendorPaymentCommand;
import com.bradox.erp.purchase.service.domain.dto.VendorPaymentAllocationLine;
import com.bradox.erp.purchase.service.domain.dto.VendorPaymentAllocationResponse;
import com.bradox.erp.purchase.service.domain.dto.VendorPaymentResponse;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorBillRepository;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorPaymentAllocationRepository;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorPaymentRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
 * Vendor payouts/refunds and their allocations to bills/credit notes. Allocations are the only
 * source of paid, outstanding and unallocated amounts. The payment entry never contains FX:
 * differences between the document rate and the payment rate are booked by a separate entry on
 * the allocation.
 */
@Service
public class VendorPaymentService {

    static final String DEFAULT_AP_ACCOUNT_CODE = "430004";
    static final String EXCHANGE_GAIN_ACCOUNT_CODE = "430014";
    static final String EXCHANGE_LOSS_ACCOUNT_CODE = "430015";
    static final String EXCHANGE_JOURNAL_CODE = "EXCH";

    private final VendorBillRepository billRepository;
    private final VendorPaymentRepository paymentRepository;
    private final VendorPaymentAllocationRepository allocationRepository;
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

    public VendorPaymentService(VendorBillRepository billRepository,
                                VendorPaymentRepository paymentRepository,
                                VendorPaymentAllocationRepository allocationRepository,
                                PartnerApplicationService partnerApplicationService,
                                JournalEntryApplicationService journalEntryApplicationService,
                                JournalRepository journalRepository,
                                AccountRepository accountRepository,
                                JournalItemReconciliationPort journalItemPort,
                                TradeReconciliationSync tradeReconciliationSync,
                                PeriodPostingGuard periodPostingGuard,
                                CurrencyConversionPort currencyConversionPort,
                                RecordActivityLogger activityLogger,
                                ObjectProvider<CompanyContext> companyContextProvider) {
        this.billRepository = billRepository;
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
    }

    /** Document-currency figures of one bill or credit note. */
    public record DocumentBalance(BigDecimal total, BigDecimal paid, BigDecimal credited, BigDecimal residual) {}

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<VendorPaymentResponse> list(UUID companyId) {
        List<VendorPayment> payments = paymentRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId);
        List<VendorPaymentAllocation> allocations = allocationRepository.findByCompanyId(companyId);
        return toResponses(payments, allocations);
    }

    @Transactional(readOnly = true)
    public Page<VendorPaymentResponse> search(UUID companyId, Pageable pageable) {
        Page<VendorPayment> page = paymentRepository.searchByCompanyId(companyId, pageable);
        List<VendorPaymentAllocation> allocations = allocationRepository.findByPaymentIdIn(
                page.getContent().stream().map(VendorPayment::getId).toList());
        List<VendorPaymentResponse> content = toResponses(page.getContent(), allocations);
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }


    @Transactional(readOnly = true)
    public VendorPaymentResponse get(UUID paymentId) {
        return responseFor(loadPayment(paymentId, false));
    }

    private VendorPaymentResponse responseFor(VendorPayment p) {
        return toResponses(List.of(p), allocationRepository.findByPaymentIdIn(List.of(p.getId()))).get(0);
    }

    @Transactional(readOnly = true)
    public List<VendorPaymentResponse> listForPartner(UUID companyId, UUID partnerId) {
        List<VendorPayment> payments = paymentRepository
                .findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(companyId, partnerId);
        List<VendorPaymentAllocation> allocations = allocationRepository.findByPaymentIdIn(
                payments.stream().map(VendorPayment::getId).toList());
        return toResponses(payments, allocations);
    }

    /** Sum of active allocation amounts per document id (missing key = zero). */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> sumActiveAllocationsByBillIds(Collection<UUID> billIds) {
        if (billIds == null || billIds.isEmpty()) return Map.of();
        Map<UUID, BigDecimal> sums = new HashMap<>();
        for (VendorPaymentAllocation a : allocationRepository.findByBillIdIn(billIds)) {
            if (a.isActive()) {
                sums.merge(a.getVendorBillId(), a.getAmount(), BigDecimal::add);
            }
        }
        sums.replaceAll((id, amt) -> amt.setScale(4, RoundingMode.HALF_UP));
        return sums;
    }

    /** Unallocated amount per posted payout (refunds are always fully allocated). */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> unallocatedByPaymentId(Collection<VendorPayment> payments) {
        Map<UUID, BigDecimal> allocated = new HashMap<>();
        for (VendorPaymentAllocation a : allocationRepository.findByPaymentIdIn(
                payments.stream().map(VendorPayment::getId).toList())) {
            if (a.isActive()) {
                allocated.merge(a.getPaymentId(), a.getPaymentAmount(), BigDecimal::add);
            }
        }
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (VendorPayment p : payments) {
            out.put(p.getId(), unallocated(p, allocated.getOrDefault(p.getId(), BigDecimal.ZERO)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public DocumentBalance balanceOf(VendorBill bill) {
        return balancesFor(List.of(bill)).get(bill.getId());
    }

    /** Balances for the given documents (loaded with lines). */
    @Transactional(readOnly = true)
    public Map<UUID, DocumentBalance> balancesFor(Collection<VendorBill> bills) {
        if (bills == null || bills.isEmpty()) return Map.of();
        List<UUID> ids = bills.stream().map(VendorBill::getId).toList();
        Map<UUID, BigDecimal> paid = sumActiveAllocationsByBillIds(ids);
        Map<UUID, BigDecimal> credited = new HashMap<>();
        Map<UUID, VendorBill> byId = bills.stream()
                .collect(Collectors.toMap(VendorBill::getId, Function.identity(), (a, b) -> a));
        for (VendorBill cn : billRepository.findByReversedBillIdIn(ids)) {
            VendorBill source = byId.get(cn.getReversedBillId());
            if (source == null || !isPostedCreditNoteFor(cn, source)) continue;
            ensureLinesLoaded(cn);
            credited.merge(source.getId(), VendorBillMath.total(cn), BigDecimal::add);
        }
        Map<UUID, DocumentBalance> out = new LinkedHashMap<>();
        for (VendorBill bill : bills) {
            ensureLinesLoaded(bill);
            BigDecimal total = VendorBillMath.total(bill);
            BigDecimal p = paid.getOrDefault(bill.getId(), BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            BigDecimal c = isCreditNote(bill) ? BigDecimal.ZERO.setScale(4)
                    : credited.getOrDefault(bill.getId(), BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            out.put(bill.getId(), new DocumentBalance(total, p, c, total.subtract(p).subtract(c).setScale(4, RoundingMode.HALF_UP)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<VendorPaymentAllocationResponse> allocationResponsesForBill(UUID billId) {
        List<VendorPaymentAllocation> allocations = allocationRepository.findByBillIdIn(List.of(billId));
        Map<UUID, VendorPayment> payments = paymentRepository.findByIdIn(
                        allocations.stream().map(VendorPaymentAllocation::getPaymentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(VendorPayment::getId, Function.identity()));
        Map<UUID, VendorBill> bills = billRepository.findById(billId)
                .map(b -> Map.of(b.getId(), b)).orElse(Map.of());
        return allocations.stream().map(a -> toAllocationResponse(a, payments.get(a.getPaymentId()), bills)).toList();
    }

    /** True when any active allocation touches one of the documents or payments. */
    @Transactional(readOnly = true)
    public boolean hasActiveAllocations(Collection<UUID> billIds, Collection<UUID> paymentIds) {
        return allocationRepository.findByBillIdIn(billIds).stream().anyMatch(VendorPaymentAllocation::isActive)
                || allocationRepository.findByPaymentIdIn(paymentIds).stream().anyMatch(VendorPaymentAllocation::isActive);
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public VendorPaymentResponse register(RegisterVendorPaymentCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        List<VendorPaymentAllocationLine> lines = new ArrayList<>(command.getAllocations());
        BigDecimal amount = scale(command.getAmount());
        boolean shortcut = false;
        if (command.getVendorBillId() != null) {
            if (!lines.isEmpty()) {
                throw new PurchaseDomainException("error.purchase.paymentBillOrAllocations", null,
                        "Provide either vendorBillId or allocations, not both");
            }
            lines.add(new VendorPaymentAllocationLine(command.getVendorBillId(), amount));
            shortcut = true;
        }

        Map<UUID, VendorBill> docs = new LinkedHashMap<>();
        BigDecimal requested = BigDecimal.ZERO;
        for (VendorPaymentAllocationLine line : lines) {
            if (docs.containsKey(line.getBillId())) {
                throw new PurchaseDomainException("error.purchase.allocationDuplicateDocument", null,
                        "The same document appears twice in the allocations");
            }
            VendorBill bill = billRepository.findById(line.getBillId())
                    .orElseThrow(() -> new PurchaseDomainException(
                            "error.purchase.vendorBillNotFound", null, "Vendor bill not found"));
            if (!bill.getCompanyId().equals(companyId)) {
                throw new PurchaseDomainException("error.purchase.billCompanyMismatch", null, "Bill company mismatch");
            }
            if (bill.getState() != VendorBillState.POSTED || bill.getJournalEntryId() == null) {
                throw new PurchaseDomainException("error.purchase.billMustBePostedBeforePayment", null,
                        "Bill must be posted before payment");
            }
            docs.put(bill.getId(), bill);
        }

        UUID partnerId = command.getVendorPartnerId();
        for (VendorBill bill : docs.values()) {
            if (partnerId == null) {
                partnerId = bill.getVendorPartnerId();
            } else if (!partnerId.equals(bill.getVendorPartnerId())) {
                throw new PurchaseDomainException("error.purchase.allocationPartnerMismatch", null,
                        "Payment and document belong to different vendors");
            }
        }
        if (partnerId == null) {
            throw new PurchaseDomainException("error.purchase.paymentPartnerRequired", null,
                    "Vendor is required for a payment without a document");
        }
        PartnerResponse vendor = partnerApplicationService.getPartner(partnerId);
        if (!companyId.equals(vendor.getCompanyId())) {
            throw new PurchaseDomainException("error.purchase.partnerCompanyMismatch", null, "Vendor belongs to another company");
        }
        if (docs.isEmpty() && !vendor.isVendor()) {
            throw new PurchaseDomainException("error.purchase.partnerNotVendor", null, "Partner is not a vendor");
        }

        String currency = command.getCurrencyCode() != null && !command.getCurrencyCode().isBlank()
                ? command.getCurrencyCode().trim().toUpperCase()
                : docs.values().stream().findFirst().map(VendorBill::getCurrencyCode).orElse(null);
        if (currency == null) {
            throw new PurchaseDomainException("error.purchase.paymentCurrencyRequired", null, "Payment currency is required");
        }
        boolean anyCreditNote = false;
        boolean anyBill = false;
        for (VendorBill bill : docs.values()) {
            if (isCreditNote(bill)) anyCreditNote = true; else anyBill = true;
        }
        if (anyCreditNote && anyBill) {
            throw new PurchaseDomainException("error.purchase.allocationMixedKinds", null,
                    "A payment cannot settle bills and credit notes at the same time");
        }
        boolean refund = anyCreditNote;

        // Each line becomes (document amount, payment amount). In the same currency they are equal; in
        // another currency the payment amount is converted through the company currency, or given.
        BigDecimal payRate = resolveExchangeRate(companyId, currency,
                command.getPaymentDate().toLocalDate(), command.getExchangeRateToCompany());
        Map<VendorPaymentAllocationLine, BigDecimal[]> resolved = new LinkedHashMap<>();
        for (VendorPaymentAllocationLine line : lines) {
            VendorBill bill = docs.get(line.getBillId());
            if (bill.getCurrencyCode().equalsIgnoreCase(currency)) {
                BigDecimal a = scale(line.getAmount());
                resolved.put(line, new BigDecimal[] { a, a });
            } else if (line.getPaymentAmount() != null) {
                resolved.put(line, new BigDecimal[] { scale(line.getAmount()), scale(line.getPaymentAmount()) });
            } else if (shortcut) {
                // "Pay this much (payment currency) toward the bill": settle what it covers, capped at
                // what the bill still owes; an amount within a cent of the balance settles it fully.
                BigDecimal residual = balanceOf(bill).residual();
                BigDecimal docAmount = paymentToDoc(amount, bill, payRate);
                if (docAmount.subtract(residual).abs().compareTo(new BigDecimal("0.01")) <= 0) {
                    resolved.put(line, new BigDecimal[] { residual, amount });
                } else if (docAmount.compareTo(residual) > 0) {
                    resolved.put(line, new BigDecimal[] { residual, docToPayment(residual, bill, payRate) });
                } else {
                    resolved.put(line, new BigDecimal[] { docAmount, amount });
                }
            } else {
                BigDecimal a = scale(line.getAmount());
                resolved.put(line, new BigDecimal[] { a, docToPayment(a, bill, payRate) });
            }
            requested = requested.add(resolved.get(line)[1]);
        }
        if (requested.compareTo(amount) > 0) {
            throw new PurchaseDomainException("error.purchase.allocationExceedsPayment", null,
                    "Allocated total exceeds the payment amount");
        }

        Journal paymentJournal = journalRepository.findById(new JournalId(command.getBankJournalId()))
                .orElseThrow(() -> new PurchaseDomainException("error.purchase.paymentJournalNotFound", null, "Payment journal not found"));
        UUID counterAccount = command.getLiquidityAccountId() != null
                ? command.getLiquidityAccountId()
                : resolveLiquidityAccount(companyId, paymentJournal);
        String liqLabel = paymentJournal.getJournalType() == JournalType.CASH
                ? (refund ? "Cash refund" : "Cash payment")
                : (refund ? "Bank refund" : "Bank payment");

        List<UUID> docIdsSorted = docs.keySet().stream().sorted().toList();
        docIdsSorted.forEach(billRepository::lockById);

        VendorPayment payment = postPaymentEntry(companyId, vendor, paymentJournal, counterAccount, liqLabel,
                command.getPaymentDate(), amount, currency, command.getExchangeRateToCompany(),
                command.getReference(), refund, false);

        for (VendorPaymentAllocationLine line : lines) {
            BigDecimal[] figures = resolved.get(line);
            allocateLocked(payment, docs.get(line.getBillId()), figures[0], figures[1], null);
        }
        syncGraph(docs.keySet(), Set.of(payment.getId()));

        String amountLabel = MonetaryScale.toDisplayString(amount) + " " + currency;
        activityLogger.log(companyId, RecordActivityLogger.MODEL_VENDOR_PAYMENT, payment.getId(),
                refund ? "Refund registered" : "Payment registered");
        for (VendorBill bill : docs.values()) {
            if (bill.getPurchaseOrderId() != null) {
                activityLogger.log(companyId, RecordActivityLogger.MODEL_PURCHASE_ORDER, bill.getPurchaseOrderId(),
                        (refund ? "Vendor refund registered: " : "Vendor payment registered: ") + amountLabel);
            }
        }
        return responseFor(paymentRepository.findById(payment.getId()).orElseThrow());
    }

    @Transactional
    public VendorPaymentResponse allocate(UUID paymentId, AllocateVendorPaymentCommand command) {
        VendorPayment payment = loadPayment(paymentId, true);
        if (payment.getPaymentKind() == VendorPaymentKind.REFUND) {
            throw new PurchaseDomainException("error.purchase.refundAllocationFixed", null,
                    "Refunds are allocated when registered and cannot be re-allocated");
        }
        Map<UUID, VendorBill> docs = new LinkedHashMap<>();
        for (VendorPaymentAllocationLine line : command.getAllocations()) {
            if (docs.containsKey(line.getBillId())) {
                throw new PurchaseDomainException("error.purchase.allocationDuplicateDocument", null,
                        "The same document appears twice in the allocations");
            }
            docs.put(line.getBillId(), billRepository.findById(line.getBillId())
                    .orElseThrow(() -> new PurchaseDomainException(
                            "error.purchase.vendorBillNotFound", null, "Vendor bill not found")));
        }
        docs.keySet().stream().sorted().forEach(billRepository::lockById);
        for (VendorPaymentAllocationLine line : command.getAllocations()) {
            VendorPaymentAllocation a = allocateLocked(payment, docs.get(line.getBillId()),
                    scale(line.getAmount()), line.getPaymentAmount(), command.getAllocationDate());
            activityLogger.log(payment.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_PAYMENT, payment.getId(),
                    "Allocated " + MonetaryScale.toDisplayString(a.getAmount()) + " " + payment.getCurrencyCode()
                            + " to " + docLabel(docs.get(line.getBillId())));
        }
        syncGraph(docs.keySet(), Set.of(payment.getId()));
        return responseFor(paymentRepository.findById(payment.getId()).orElseThrow());
    }

    @Transactional
    public VendorPaymentResponse deallocate(UUID allocationId) {
        VendorPaymentAllocation a = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new PurchaseDomainException("error.purchase.allocationNotFound", null, "Allocation not found"));
        VendorPayment payment = loadPayment(a.getPaymentId(), true);
        billRepository.lockById(a.getVendorBillId());
        a = allocationRepository.findById(allocationId).orElseThrow();
        if (!a.isActive()) {
            throw new PurchaseDomainException("error.purchase.allocationNotActive", null, "Allocation is already reversed");
        }
        if (payment.getPaymentKind() == VendorPaymentKind.REFUND) {
            throw new PurchaseDomainException("error.purchase.refundAllocationFixed", null,
                    "Refunds are allocated when registered; reverse the refund instead");
        }
        reverseAllocation(a, "Allocation removed");
        VendorBill bill = billRepository.findById(a.getVendorBillId()).orElse(null);
        activityLogger.log(payment.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_PAYMENT, payment.getId(),
                "Allocation removed: " + MonetaryScale.toDisplayString(a.getAmount()) + " " + payment.getCurrencyCode()
                        + (bill != null ? " from " + docLabel(bill) : ""));
        syncGraph(Set.of(a.getVendorBillId()), Set.of(payment.getId()));
        return responseFor(paymentRepository.findById(payment.getId()).orElseThrow());
    }

    @Transactional
    public VendorPaymentResponse reverse(UUID paymentId, String reason) {
        VendorPayment p = loadPayment(paymentId, true);
        if (p.getState() != VendorPaymentState.POSTED) {
            throw new PurchaseDomainException("error.purchase.onlyPostedPaymentReversible", null,
                    "Only posted payments can be reversed");
        }
        if (p.getJournalEntryId() == null) {
            throw new PurchaseDomainException("error.purchase.paymentHasNoEntry", null,
                    "Payment has no journal entry to reverse");
        }
        String why = reason != null && !reason.isBlank() ? reason : "Payment reverse";
        List<VendorPaymentAllocation> active = allocationRepository.findByPaymentIdIn(List.of(p.getId())).stream()
                .filter(VendorPaymentAllocation::isActive)
                .toList();
        Set<UUID> billIds = active.stream().map(VendorPaymentAllocation::getVendorBillId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        billIds.stream().sorted().forEach(billRepository::lockById);
        for (VendorPaymentAllocation a : active) {
            reverseAllocation(a, why);
        }
        var reverseResp = journalEntryApplicationService.reverseJournalEntry(
                new ReverseJournalEntryCommand(p.getJournalEntryId(), why));
        p.setState(VendorPaymentState.REVERSED);
        p.setReversalJournalEntryId(reverseResp.getReversalJournalEntryId());
        p.setUpdatedAt(Instant.now());
        paymentRepository.save(p);
        syncGraph(billIds, Set.of(p.getId()));
        activityLogger.log(p.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_PAYMENT, p.getId(),
                "Payment reversed: " + (reason != null ? reason : ""));
        return responseFor(paymentRepository.findById(p.getId()).orElseThrow());
    }

    // ------------------------------------------------------------------ vendor credit

    private List<VendorBill> postedCreditNotesOf(UUID sourceBillId) {
        List<VendorBill> out = new ArrayList<>();
        for (VendorBill cn : billRepository.findByReversedBillIdIn(List.of(sourceBillId))) {
            if (cn.getState() == VendorBillState.POSTED && isCreditNote(cn)) {
                ensureLinesLoaded(cn);
                out.add(cn);
            }
        }
        return out;
    }

    private VendorBill loadPostedBill(UUID billId) {
        VendorBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new PurchaseDomainException(
                        "error.purchase.vendorBillNotFound", null, "Vendor bill not found"));
        if (bill.getState() != VendorBillState.POSTED || bill.getJournalEntryId() == null) {
            throw new PurchaseDomainException("error.purchase.billMustBePostedBeforePayment", null,
                    "Bill must be posted before payment");
        }
        ensureLinesLoaded(bill);
        return bill;
    }

    /**
     * Money the vendor still holds for us on a posted bill: paid beyond what is owed after its credit
     * notes, minus what was already refunded through them. Zero for credit notes.
     */
    @Transactional(readOnly = true)
    public BigDecimal creditAvailableOn(VendorBill bill) {
        if (isCreditNote(bill) || bill.getState() != VendorBillState.POSTED) {
            return BigDecimal.ZERO.setScale(4);
        }
        VendorBill source = billRepository.findById(bill.getId()).orElse(bill);
        DocumentBalance b = balanceOf(source);
        BigDecimal overpaid = b.paid().add(b.credited()).subtract(b.total());
        if (overpaid.signum() <= 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        List<VendorBill> cns = postedCreditNotesOf(source.getId());
        BigDecimal refunded = BigDecimal.ZERO;
        if (!cns.isEmpty()) {
            refunded = sum(sumActiveAllocationsByBillIds(cns.stream().map(VendorBill::getId).toList())
                    .values().stream().toList(), v -> v);
        }
        return overpaid.subtract(refunded).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /** How much of a credit note the vendor can pay back now: our credit on its source, capped by the note. */
    @Transactional(readOnly = true)
    public BigDecimal refundableOf(VendorBill creditNote) {
        if (!isCreditNote(creditNote) || creditNote.getState() != VendorBillState.POSTED) {
            return BigDecimal.ZERO.setScale(4);
        }
        BigDecimal own = balanceOf(creditNote).residual().max(BigDecimal.ZERO);
        if (creditNote.getReversedBillId() == null) {
            return own;
        }
        VendorBill source = billRepository.findById(creditNote.getReversedBillId()).orElse(null);
        if (source == null) {
            return own;
        }
        return creditAvailableOn(source).min(own).setScale(4, RoundingMode.HALF_UP);
    }

    /** Collects our credit on {@code billId} back from the vendor through the bill's credit notes. */
    @Transactional
    public VendorPaymentResponse refundCredit(UUID billId, RefundVendorCreditCommand command) {
        VendorBill source = loadPostedBill(billId);
        BigDecimal remaining = creditAvailableOn(source);
        if (remaining.signum() <= 0) {
            throw new PurchaseDomainException("error.purchase.noVendorCredit", null,
                    "There is no vendor credit on this bill to refund");
        }
        List<VendorPaymentAllocationLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (VendorBill cn : postedCreditNotesOf(source.getId())) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = remaining.min(balanceOf(cn).residual().max(BigDecimal.ZERO));
            if (take.signum() > 0) {
                lines.add(new VendorPaymentAllocationLine(cn.getId(), take));
                total = total.add(take);
                remaining = remaining.subtract(take);
            }
        }
        if (lines.isEmpty()) {
            throw new PurchaseDomainException("error.purchase.noVendorCredit", null,
                    "There is no vendor credit on this bill to refund");
        }
        RegisterVendorPaymentCommand cmd = new RegisterVendorPaymentCommand();
        cmd.setCompanyId(source.getCompanyId());
        cmd.setVendorPartnerId(source.getVendorPartnerId());
        cmd.setBankJournalId(command.getBankJournalId());
        cmd.setPaymentDate(command.getPaymentDate() != null ? command.getPaymentDate() : LocalDateTime.now());
        cmd.setAmount(total);
        cmd.setCurrencyCode(source.getCurrencyCode());
        cmd.setReference(command.getReference());
        cmd.setAllocations(lines);
        return register(cmd);
    }

    /**
     * Releases our credit on {@code billId} from the bill so it becomes an open payment to the
     * vendor, ready to settle their next bill. Returns the amount released.
     */
    @Transactional
    public BigDecimal keepCredit(UUID billId) {
        VendorBill source = loadPostedBill(billId);
        billRepository.lockById(billId);
        BigDecimal amount = creditAvailableOn(source);
        if (amount.signum() <= 0) {
            throw new PurchaseDomainException("error.purchase.noVendorCredit", null,
                    "There is no vendor credit on this bill to keep");
        }
        BigDecimal remaining = amount;
        Set<UUID> paymentIds = new LinkedHashSet<>();
        List<VendorPaymentAllocation> active = allocationRepository.findByBillIdIn(List.of(billId)).stream()
                .filter(VendorPaymentAllocation::isActive)
                .sorted(java.util.Comparator.comparing(VendorPaymentAllocation::getCreatedAt).reversed())
                .toList();
        for (VendorPaymentAllocation a : active) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal cut = a.getAmount().min(remaining);
            VendorPayment payment = loadPayment(a.getPaymentId(), true);
            BigDecimal keep = a.getAmount().subtract(cut);
            // The kept part keeps its share of the payment amount (they differ across currencies).
            BigDecimal keepPayment = a.getAmount().signum() == 0 ? keep
                    : a.getPaymentAmount().multiply(keep).divide(a.getAmount(), 4, RoundingMode.HALF_UP);
            LocalDate date = a.getAllocationDate();
            reverseAllocation(a, "Credit kept with the vendor");
            if (keep.signum() > 0) {
                allocateLocked(payment, source, keep, keepPayment, date);
            }
            paymentIds.add(payment.getId());
            remaining = remaining.subtract(cut);
        }
        syncGraph(Set.of(billId), paymentIds);
        activityLogger.log(source.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, billId,
                "Vendor credit kept: " + MonetaryScale.toDisplayString(amount) + " " + source.getCurrencyCode());
        return amount;
    }

    /**
     * Settles a posted bill with the vendor's open payments (advances, overpayments, credit kept),
     * oldest first. Returns the amount applied.
     */
    @Transactional
    public BigDecimal applyCredit(UUID billId) {
        BigDecimal applied = applyCredit(billId, null);
        if (applied.signum() <= 0) {
            throw new PurchaseDomainException("error.purchase.noVendorCredit", null,
                    "There is no open vendor credit to apply");
        }
        return applied;
    }

    /** Like {@link #applyCredit(UUID)} but never applies more than {@code limit} (null = no limit) and may apply nothing. */
    @Transactional
    public BigDecimal applyCredit(UUID billId, BigDecimal limit) {
        VendorBill bill = loadPostedBill(billId);
        if (isCreditNote(bill)) {
            throw new PurchaseDomainException("error.purchase.allocationKindMismatch", null,
                    "Payouts can only settle bills");
        }
        billRepository.lockById(billId);
        BigDecimal residual = balanceOf(bill).residual();
        if (limit != null) {
            residual = residual.min(limit);
        }
        List<VendorPayment> payouts = paymentRepository
                .findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(
                        bill.getCompanyId(), bill.getVendorPartnerId()).stream()
                .filter(p -> p.getState() == VendorPaymentState.POSTED
                        && p.getPaymentKind() != VendorPaymentKind.REFUND)
                .toList();
        Map<UUID, BigDecimal> open = unallocatedByPaymentId(payouts);
        BigDecimal applied = BigDecimal.ZERO;
        Set<UUID> paymentIds = new LinkedHashSet<>();
        for (VendorPayment p : payouts) {
            if (residual.signum() <= 0) {
                break;
            }
            BigDecimal openPayment = open.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal take;
            BigDecimal paymentTake;
            if (bill.getCurrencyCode().equalsIgnoreCase(p.getCurrencyCode())) {
                take = openPayment.min(residual);
                paymentTake = take;
            } else {
                // Credit held in another currency: use as much as the bill still owes.
                BigDecimal coverable = paymentToDoc(openPayment, bill, p.getExchangeRateToCompany());
                if (coverable.compareTo(residual) >= 0) {
                    take = residual;
                    paymentTake = docToPayment(residual, bill, p.getExchangeRateToCompany()).min(openPayment);
                } else {
                    take = coverable;
                    paymentTake = openPayment;
                }
            }
            if (take.signum() <= 0 || paymentTake.signum() <= 0) {
                continue;
            }
            allocateLocked(loadPayment(p.getId(), true), bill, take, paymentTake, null);
            paymentIds.add(p.getId());
            applied = applied.add(take);
            residual = residual.subtract(take);
        }
        if (applied.signum() <= 0) {
            return applied;
        }
        syncGraph(Set.of(billId), paymentIds);
        activityLogger.log(bill.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, billId,
                "Vendor credit applied: " + MonetaryScale.toDisplayString(applied) + " " + bill.getCurrencyCode());
        return applied;
    }

    /**
     * Fixes a wrongly recorded payment in one step: the payment is reversed and re-registered with
     * the corrected values, applied to the same bills (up to what each still owes).
     */
    @Transactional
    public VendorPaymentResponse correctPayment(UUID paymentId, CorrectVendorPaymentCommand command) {
        VendorPayment old = loadPayment(paymentId, true);
        if (old.getState() != VendorPaymentState.POSTED || old.isOpeningBalance()) {
            throw new PurchaseDomainException("error.purchase.onlyPostedPaymentReversible", null,
                    "Only posted payments can be reversed");
        }
        List<VendorPaymentAllocation> oldAllocations = allocationRepository.findByPaymentIdIn(List.of(old.getId()))
                .stream().filter(VendorPaymentAllocation::isActive)
                .sorted(java.util.Comparator.comparing(VendorPaymentAllocation::getCreatedAt)).toList();
        UUID liquidity = null;
        for (ItemSnapshot item : journalItemPort.findItemsByEntryIds(List.of(old.getJournalEntryId()))) {
            if (item.accountType() != AccountType.PAYABLE) {
                liquidity = item.accountId();
            }
        }
        String why = command.getReason() != null && !command.getReason().isBlank()
                ? command.getReason() : "Payment corrected";
        reverse(old.getId(), why);

        BigDecimal amount = command.getAmount() != null ? scale(command.getAmount()) : old.getAmount();
        RegisterVendorPaymentCommand cmd = new RegisterVendorPaymentCommand();
        cmd.setCompanyId(old.getCompanyId());
        cmd.setVendorPartnerId(old.getVendorPartnerId());
        cmd.setBankJournalId(command.getBankJournalId() != null ? command.getBankJournalId() : old.getBankJournalId());
        cmd.setPaymentDate(command.getPaymentDate() != null ? command.getPaymentDate() : old.getPaymentDate());
        cmd.setAmount(amount);
        cmd.setCurrencyCode(old.getCurrencyCode());
        cmd.setExchangeRateToCompany(old.getExchangeRateToCompany());
        cmd.setReference(command.getReference() != null ? command.getReference() : old.getReference());
        cmd.setLiquidityAccountId(liquidity);
        BigDecimal left = amount;
        List<VendorPaymentAllocationLine> lines = new ArrayList<>();
        for (VendorPaymentAllocation a : oldAllocations) {
            if (left.signum() <= 0) {
                break;
            }
            VendorBill doc = billRepository.findById(a.getVendorBillId()).orElse(null);
            if (doc == null) {
                continue;
            }
            ensureLinesLoaded(doc);
            BigDecimal residual = balanceOf(doc).residual();
            if (doc.getCurrencyCode().equalsIgnoreCase(old.getCurrencyCode())) {
                BigDecimal take = left.min(residual);
                if (take.signum() > 0) {
                    lines.add(new VendorPaymentAllocationLine(doc.getId(), take));
                    left = left.subtract(take);
                }
            } else {
                // Other currency: `left` is in the payment currency, the document balance in its own.
                BigDecimal payCapacity = docToPayment(residual, doc, old.getExchangeRateToCompany());
                BigDecimal payTake = left.min(payCapacity);
                if (payTake.signum() > 0) {
                    BigDecimal docTake = payTake.compareTo(payCapacity) == 0 ? residual
                            : paymentToDoc(payTake, doc, old.getExchangeRateToCompany()).min(residual);
                    VendorPaymentAllocationLine line = new VendorPaymentAllocationLine(doc.getId(), docTake);
                    line.setPaymentAmount(payTake);
                    lines.add(line);
                    left = left.subtract(payTake);
                }
            }
        }
        cmd.setAllocations(lines);
        VendorPaymentResponse created = register(cmd);
        activityLogger.log(old.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_PAYMENT, created.getId(),
                "Payment corrected (replaces a reversed payment): " + why);
        return created;
    }

    /**
     * Opening vendor debit: an unallocated payout on the opening journal against Opening
     * Balance Equity (Dr AP / Cr OBE).
     */
    @Transactional
    public VendorPayment postOpeningPayment(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                            BigDecimal exchangeRate, LocalDate date, String reference,
                                            Journal openingJournal, UUID openingEquityAccountId) {
        PartnerResponse vendor = partnerApplicationService.getPartner(partnerId);
        if (!companyId.equals(vendor.getCompanyId())) {
            throw new PurchaseDomainException("error.purchase.partnerCompanyMismatch", null, "Vendor belongs to another company");
        }
        return postPaymentEntry(companyId, vendor, openingJournal, openingEquityAccountId, "Opening balance",
                date.atStartOfDay(), scale(amount), currency.toUpperCase(), exchangeRate, reference, false, true);
    }

    @Transactional
    public VendorPayment postOpeningPayment(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                            LocalDate date, String reference,
                                            UUID openingJournalId, UUID openingEquityAccountId) {
        Journal openingJournal = journalRepository.findById(new JournalId(openingJournalId))
                .orElseThrow(() -> new PurchaseDomainException("Opening journal not found: " + openingJournalId));
        return postOpeningPayment(companyId, partnerId, amount, currency, null, date, reference,
                openingJournal, openingEquityAccountId);
    }

    /** Re-derive reconciliation tags around the given documents (e.g. after a credit note posts). */
    @Transactional
    public void syncForBills(Collection<UUID> billIds) {
        syncGraph(billIds, Set.of());
    }

    /** Re-derive every payable reconciliation tag of the company from its allocations. */
    @Transactional
    public void rebuildCompany(UUID companyId) {
        List<VendorBill> bills = billRepository.findByCompanyIdOrderByBillDateDescCreatedAtDesc(companyId);
        for (VendorBill b : bills) {
            ensureLinesLoaded(b);
        }
        List<VendorPayment> payments = paymentRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId);
        List<VendorPaymentAllocation> allocations = allocationRepository.findByCompanyId(companyId);
        applyGraph(bills, payments, allocations);
    }

    // ------------------------------------------------------------------ internals

    private VendorPayment postPaymentEntry(UUID companyId, PartnerResponse vendor, Journal journal,
                                           UUID counterAccount, String counterLabel, LocalDateTime paymentDate,
                                           BigDecimal amount, String currency, BigDecimal explicitRate,
                                           String reference, boolean refund, boolean opening) {
        if (amount.signum() <= 0) {
            throw new PurchaseDomainException("error.purchase.paymentAmountPositive", null, "Payment amount must be positive");
        }
        periodPostingGuard.assertDatePostable(companyId, paymentDate.toLocalDate());
        UUID payable = payableAccount(companyId, vendor);
        BigDecimal rate = resolveExchangeRate(companyId, currency, paymentDate.toLocalDate(), explicitRate);
        BigDecimal comp = CurrencyMath.convertAtRate(amount, rate);
        String apLabel = (refund ? "Refund" : "Payment") + (reference != null && !reference.isBlank() ? " " + reference : "");
        List<JournalItemCommand> items = new ArrayList<>();
        if (refund) {
            items.add(new JournalItemCommand(counterAccount, counterLabel, comp, BigDecimal.ZERO, currency, amount, null));
            items.add(new JournalItemCommand(payable, apLabel, BigDecimal.ZERO, comp, currency, amount.negate(), vendor.getId()));
        } else {
            items.add(new JournalItemCommand(payable, apLabel, comp, BigDecimal.ZERO, currency, amount, vendor.getId()));
            items.add(new JournalItemCommand(counterAccount, counterLabel, BigDecimal.ZERO, comp, currency, amount.negate(), null));
        }
        CreateJournalEntryResponse entry = journalEntryApplicationService.createJournalEntry(new CreateJournalEntryCommand(
                companyId, journal.getId().getId(), "", JournalEntryTiming.ensureTimed(paymentDate), currency,
                vendor.getId(), items));
        journalEntryApplicationService.postJournalEntry(entry.getJournalEntryId());
        String sequence = journalEntryApplicationService.getJournalEntry(entry.getJournalEntryId()).getSequenceNumber();

        Instant now = Instant.now();
        VendorPayment p = new VendorPayment();
        p.setId(UUID.randomUUID());
        p.setCompanyId(companyId);
        p.setVendorPartnerId(vendor.getId());
        p.setPaymentDate(paymentDate);
        p.setBankJournalId(journal.getId().getId());
        p.setAmount(amount);
        p.setCurrencyCode(currency);
        p.setExchangeRateToCompany(rate);
        p.setState(VendorPaymentState.POSTED);
        p.setPaymentKind(refund ? VendorPaymentKind.REFUND : VendorPaymentKind.PAYOUT);
        p.setJournalEntryId(entry.getJournalEntryId());
        p.setReference(reference != null && !reference.isBlank() ? reference : sequence);
        p.setOpeningBalance(opening);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return paymentRepository.save(p);
    }

    /** Caller must hold the payment lock and the document lock. */
    private VendorPaymentAllocation allocateLocked(VendorPayment payment, VendorBill docRef,
                                                   BigDecimal amount, LocalDate requestedDate) {
        return allocateLocked(payment, docRef, amount, null, requestedDate);
    }

    /**
     * {@code amount} is in the document's currency. When the payment is in another currency,
     * {@code explicitPaymentAmount} is the part of the payment used (otherwise it is derived from the
     * two exchange rates). Same-currency allocations use the same figure for both.
     */
    private VendorPaymentAllocation allocateLocked(VendorPayment payment, VendorBill docRef,
                                                   BigDecimal amount, BigDecimal explicitPaymentAmount,
                                                   LocalDate requestedDate) {
        VendorBill bill = billRepository.findById(docRef.getId())
                .orElseThrow(() -> new PurchaseDomainException(
                        "error.purchase.vendorBillNotFound", null, "Vendor bill not found"));
        ensureLinesLoaded(bill);
        boolean refund = payment.getPaymentKind() == VendorPaymentKind.REFUND;
        if (payment.getState() != VendorPaymentState.POSTED) {
            throw new PurchaseDomainException("error.purchase.allocationPaymentNotPosted", null,
                    "Only posted payments can be allocated");
        }
        if (!bill.getCompanyId().equals(payment.getCompanyId())) {
            throw new PurchaseDomainException("error.purchase.billCompanyMismatch", null, "Bill company mismatch");
        }
        if (bill.getState() != VendorBillState.POSTED || bill.getJournalEntryId() == null) {
            throw new PurchaseDomainException("error.purchase.billMustBePostedBeforePayment", null,
                    "Bill must be posted before payment");
        }
        if (!bill.getVendorPartnerId().equals(payment.getVendorPartnerId())) {
            throw new PurchaseDomainException("error.purchase.allocationPartnerMismatch", null,
                    "Payment and document belong to different vendors");
        }
        boolean sameCurrency = bill.getCurrencyCode().equalsIgnoreCase(payment.getCurrencyCode());
        if (refund != isCreditNote(bill)) {
            throw new PurchaseDomainException("error.purchase.allocationKindMismatch", null,
                    refund ? "Refunds can only settle credit notes" : "Payouts can only settle bills");
        }
        if (amount.signum() <= 0) {
            throw new PurchaseDomainException("error.purchase.allocationAmountPositive", null,
                    "Allocation amount must be positive");
        }

        BigDecimal paymentAmount = sameCurrency ? amount
                : explicitPaymentAmount != null ? scale(explicitPaymentAmount)
                : docToPayment(amount, bill, payment.getExchangeRateToCompany());
        if (paymentAmount.signum() <= 0) {
            throw new PurchaseDomainException("error.purchase.allocationAmountPositive", null,
                    "Allocation amount must be positive");
        }
        List<VendorPaymentAllocation> paymentAllocs = allocationRepository.findByPaymentIdIn(List.of(payment.getId()))
                .stream().filter(VendorPaymentAllocation::isActive).toList();
        BigDecimal unallocated = unallocated(payment, sum(paymentAllocs, VendorPaymentAllocation::getPaymentAmount));
        if (paymentAmount.compareTo(unallocated) > 0) {
            throw new PurchaseDomainException("error.purchase.allocationExceedsUnallocated",
                    new Object[] { MonetaryScale.toDisplayString(unallocated), payment.getCurrencyCode() },
                    "Allocation exceeds the payment's unallocated amount of "
                            + MonetaryScale.toDisplayString(unallocated) + " " + payment.getCurrencyCode());
        }
        if (refund) {
            // A credit note first cancels what we still owe; only money we actually paid on the
            // source bill can come back from the vendor.
            BigDecimal refundable = refundableOf(bill);
            if (amount.compareTo(refundable) > 0) {
                throw new PurchaseDomainException("error.purchase.refundExceedsVendorCredit",
                        new Object[] { MonetaryScale.toDisplayString(refundable), bill.getCurrencyCode() },
                        "Refund exceeds the vendor credit of " + MonetaryScale.toDisplayString(refundable)
                                + " " + bill.getCurrencyCode() + ": we have not paid that much on the bill");
            }
        }
        DocumentBalance balance = balanceOf(bill);
        if (balance.residual().signum() <= 0) {
            if (refund) {
                throw new PurchaseDomainException("error.purchase.creditNoteFullyRefunded", null,
                        "Credit note is already fully refunded");
            }
            throw new PurchaseDomainException("error.purchase.vendorBillFullyPaid", null, "Vendor bill is already fully paid");
        }
        if (amount.compareTo(balance.residual()) > 0) {
            throw new PurchaseDomainException("error.purchase.allocationExceedsOutstanding",
                    new Object[] { MonetaryScale.toDisplayString(balance.residual()), bill.getCurrencyCode() },
                    "Payment amount exceeds outstanding balance of "
                            + MonetaryScale.toDisplayString(balance.residual()) + " " + bill.getCurrencyCode());
        }
        LocalDate paymentDay = payment.getPaymentDate().toLocalDate();
        LocalDate latest = paymentDay.isAfter(bill.getBillDate()) ? paymentDay : bill.getBillDate();
        LocalDate allocationDate = requestedDate != null ? requestedDate : latest;
        if (allocationDate.isBefore(latest)) {
            throw new PurchaseDomainException("error.purchase.allocationDateTooEarly", null,
                    "Allocation date cannot be before the payment or document date");
        }

        TradeLine docLine = tradeLine(bill.getJournalEntryId(), bill.getVendorPartnerId());
        BigDecimal docSide;
        if (amount.compareTo(balance.residual()) == 0) {
            BigDecimal creditedComp = BigDecimal.ZERO;
            if (!refund) {
                for (VendorBill cn : billRepository.findByReversedBillIdIn(List.of(bill.getId()))) {
                    if (isPostedCreditNoteFor(cn, bill) && cn.getJournalEntryId() != null) {
                        creditedComp = creditedComp.add(tradeLine(cn.getJournalEntryId(), cn.getVendorPartnerId()).absAmount());
                    }
                }
            }
            BigDecimal priorComp = sum(allocationRepository.findByBillIdIn(List.of(bill.getId())).stream()
                    .filter(VendorPaymentAllocation::isActive).toList(), VendorPaymentAllocation::getAmountCompany);
            docSide = docLine.absAmount().subtract(creditedComp).subtract(priorComp);
        } else {
            docSide = CurrencyMath.convertAtRate(amount, bill.getExchangeRateToCompany());
        }
        BigDecimal paySide;
        if (paymentAmount.compareTo(unallocated) == 0) {
            paySide = tradeLine(payment.getJournalEntryId(), payment.getVendorPartnerId()).absAmount()
                    .subtract(sum(paymentAllocs, VendorPaymentAllocation::getPaymentAmountCompany));
        } else {
            paySide = CurrencyMath.convertAtRate(paymentAmount, payment.getExchangeRateToCompany());
        }
        // Payable left open by this pair: document side (credit for bills) − payment side.
        BigDecimal residualComp = (refund ? docSide.negate().add(paySide) : docSide.subtract(paySide))
                .setScale(4, RoundingMode.HALF_UP);

        UUID fxEntryId = null;
        if (residualComp.signum() != 0) {
            fxEntryId = postExchangeDifference(payment, bill, docLine.accountId(), residualComp, allocationDate);
        }
        Instant now = Instant.now();
        VendorPaymentAllocation a = new VendorPaymentAllocation();
        a.setId(UUID.randomUUID());
        a.setCompanyId(payment.getCompanyId());
        a.setPaymentId(payment.getId());
        a.setVendorBillId(bill.getId());
        a.setAmount(amount);
        a.setPaymentAmount(paymentAmount);
        a.setAmountCompany(docSide.setScale(4, RoundingMode.HALF_UP));
        a.setPaymentAmountCompany(paySide.setScale(4, RoundingMode.HALF_UP));
        a.setAllocationDate(allocationDate);
        a.setFxJournalEntryId(fxEntryId);
        a.setState(VendorPaymentAllocationState.ACTIVE);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        VendorPaymentAllocation saved = allocationRepository.save(a);
        activityLogger.log(bill.getCompanyId(), RecordActivityLogger.MODEL_VENDOR_BILL, bill.getId(),
                (refund ? "Refund registered: " : "Payment registered: ")
                        + MonetaryScale.toDisplayString(amount) + " " + payment.getCurrencyCode());
        return saved;
    }

    /**
     * Positive residual = payable still credit → debit AP against exchange gain.
     * Negative residual = payment overshot in company currency → credit AP against exchange loss.
     */
    private UUID postExchangeDifference(VendorPayment payment, VendorBill bill, UUID apAccount,
                                        BigDecimal residualComp, LocalDate date) {
        UUID companyId = payment.getCompanyId();
        periodPostingGuard.assertDatePostable(companyId, date);
        BigDecimal abs = residualComp.abs();
        String label = "Exchange difference " + (payment.getReference() != null ? payment.getReference() : "")
                + " / " + docLabel(bill);
        List<JournalItemCommand> items = new ArrayList<>();
        if (residualComp.signum() > 0) {
            items.add(new JournalItemCommand(apAccount, label, abs, BigDecimal.ZERO,
                    bill.getCurrencyCode(), BigDecimal.ZERO, bill.getVendorPartnerId()));
            items.add(new JournalItemCommand(accountByCode(companyId, EXCHANGE_GAIN_ACCOUNT_CODE,
                    "error.purchase.exchangeGainAccountNotFound"), "Exchange gain", BigDecimal.ZERO, abs, null, null, null));
        } else {
            items.add(new JournalItemCommand(accountByCode(companyId, EXCHANGE_LOSS_ACCOUNT_CODE,
                    "error.purchase.exchangeLossAccountNotFound"), "Exchange loss", abs, BigDecimal.ZERO, null, null, null));
            items.add(new JournalItemCommand(apAccount, label, BigDecimal.ZERO, abs,
                    bill.getCurrencyCode(), BigDecimal.ZERO, bill.getVendorPartnerId()));
        }
        Journal journal = exchangeJournal(companyId);
        CreateJournalEntryResponse entry = journalEntryApplicationService.createJournalEntry(new CreateJournalEntryCommand(
                companyId, journal.getId().getId(), "", JournalEntryTiming.ofBusinessDate(date),
                currencyConversionPort.baseCurrencyCode(companyId), bill.getVendorPartnerId(), items));
        journalEntryApplicationService.postJournalEntry(entry.getJournalEntryId());
        return entry.getJournalEntryId();
    }

    private void reverseAllocation(VendorPaymentAllocation a, String reason) {
        if (a.getFxJournalEntryId() != null) {
            var resp = journalEntryApplicationService.reverseJournalEntry(
                    new ReverseJournalEntryCommand(a.getFxJournalEntryId(), reason));
            a.setFxReversalJournalEntryId(resp.getReversalJournalEntryId());
        }
        a.setState(VendorPaymentAllocationState.REVERSED);
        a.setUpdatedAt(Instant.now());
        allocationRepository.save(a);
    }

    private void syncGraph(Collection<UUID> billSeeds, Collection<UUID> paymentSeeds) {
        Map<UUID, VendorBill> bills = new LinkedHashMap<>();
        Map<UUID, VendorPayment> payments = new LinkedHashMap<>();
        Map<UUID, VendorPaymentAllocation> allocations = new LinkedHashMap<>();
        Set<UUID> pendingBills = new LinkedHashSet<>(billSeeds);
        Set<UUID> pendingPayments = new LinkedHashSet<>(paymentSeeds);
        while (!pendingBills.isEmpty() || !pendingPayments.isEmpty()) {
            Set<UUID> billBatch = new LinkedHashSet<>(pendingBills);
            billBatch.removeAll(bills.keySet());
            pendingBills.clear();
            Set<UUID> payBatch = new LinkedHashSet<>(pendingPayments);
            payBatch.removeAll(payments.keySet());
            pendingPayments.clear();
            if (!billBatch.isEmpty()) {
                for (VendorBill bill : billRepository.findByIdIn(billBatch)) {
                    bills.put(bill.getId(), bill);
                    if (bill.getReversedBillId() != null && !bills.containsKey(bill.getReversedBillId())) {
                        pendingBills.add(bill.getReversedBillId());
                    }
                }
                for (VendorBill cn : billRepository.findByReversedBillIdIn(billBatch)) {
                    if (!bills.containsKey(cn.getId())) pendingBills.add(cn.getId());
                }
                for (VendorPaymentAllocation a : allocationRepository.findByBillIdIn(billBatch)) {
                    allocations.put(a.getId(), a);
                    if (a.isActive() && !payments.containsKey(a.getPaymentId())) pendingPayments.add(a.getPaymentId());
                }
            }
            if (!payBatch.isEmpty()) {
                for (VendorPayment p : paymentRepository.findByIdIn(payBatch)) {
                    payments.put(p.getId(), p);
                }
                for (VendorPaymentAllocation a : allocationRepository.findByPaymentIdIn(payBatch)) {
                    allocations.put(a.getId(), a);
                    if (a.isActive() && !bills.containsKey(a.getVendorBillId())) {
                        pendingBills.add(a.getVendorBillId());
                    }
                }
            }
        }
        applyGraph(bills.values(), payments.values(), allocations.values());
    }

    private void applyGraph(Collection<VendorBill> bills, Collection<VendorPayment> payments,
                            Collection<VendorPaymentAllocation> allocations) {
        Map<UUID, VendorBill> billById = bills.stream()
                .collect(Collectors.toMap(VendorBill::getId, Function.identity(), (a, b) -> a));
        Map<UUID, VendorPayment> paymentById = payments.stream()
                .collect(Collectors.toMap(VendorPayment::getId, Function.identity(), (a, b) -> a));
        List<UUID> cancelledEntries = bills.stream()
                .filter(b -> b.getState() == VendorBillState.CANCELLED && b.getJournalEntryId() != null)
                .map(VendorBill::getJournalEntryId)
                .toList();
        Map<UUID, UUID> reversals = journalItemPort.findPostedReversalEntryIds(cancelledEntries);

        Map<UUID, TradeReconciliationGraph> graphs = new LinkedHashMap<>();
        Function<UUID, TradeReconciliationGraph> graphFor = partner -> graphs.computeIfAbsent(partner, k -> new TradeReconciliationGraph());
        for (VendorBill bill : bills) {
            TradeReconciliationGraph g = graphFor.apply(bill.getVendorPartnerId());
            if (bill.getState() == VendorBillState.POSTED) {
                g.addNode(bill.getId(), listOfNonNull(bill.getJournalEntryId()));
                VendorBill source = bill.getReversedBillId() != null ? billById.get(bill.getReversedBillId()) : null;
                if (source != null && isPostedCreditNoteFor(bill, source)) {
                    g.addEdge(bill.getId(), source.getId());
                }
            } else if (bill.getState() == VendorBillState.CANCELLED && bill.getJournalEntryId() != null) {
                g.addStandalone(listOfNonNull(bill.getJournalEntryId(), reversals.get(bill.getJournalEntryId())));
            }
        }
        for (VendorPayment p : payments) {
            graphFor.apply(p.getVendorPartnerId())
                    .addNode(p.getId(), listOfNonNull(p.getJournalEntryId(), p.getReversalJournalEntryId()));
        }
        for (VendorPaymentAllocation a : allocations) {
            VendorPayment p = paymentById.get(a.getPaymentId());
            if (p == null) continue;
            TradeReconciliationGraph g = graphFor.apply(p.getVendorPartnerId());
            if (a.isActive()) {
                g.addNode(a.getId(), listOfNonNull(a.getFxJournalEntryId()));
                g.addEdge(a.getId(), p.getId());
                if (billById.containsKey(a.getVendorBillId())) {
                    g.addEdge(a.getId(), a.getVendorBillId());
                }
            } else if (a.getFxJournalEntryId() != null) {
                g.addStandalone(listOfNonNull(a.getFxJournalEntryId(), a.getFxReversalJournalEntryId()));
            }
        }
        graphs.forEach((partner, g) -> tradeReconciliationSync.apply(partner, AccountType.PAYABLE, g.components()));
    }

    private List<VendorPaymentResponse> toResponses(List<VendorPayment> payments,
                                                    Collection<VendorPaymentAllocation> allocations) {
        Map<UUID, List<VendorPaymentAllocation>> byPayment = new HashMap<>();
        for (VendorPaymentAllocation a : allocations) {
            byPayment.computeIfAbsent(a.getPaymentId(), k -> new ArrayList<>()).add(a);
        }
        Set<UUID> billIds = allocations.stream().map(VendorPaymentAllocation::getVendorBillId).collect(Collectors.toSet());
        Map<UUID, VendorBill> bills = billRepository.findByIdIn(billIds).stream()
                .collect(Collectors.toMap(VendorBill::getId, Function.identity()));
        List<VendorPaymentResponse> out = new ArrayList<>();
        for (VendorPayment p : payments) {
            List<VendorPaymentAllocation> list = byPayment.getOrDefault(p.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(VendorPaymentAllocation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            BigDecimal allocated = sum(list.stream().filter(VendorPaymentAllocation::isActive).toList(),
                    VendorPaymentAllocation::getPaymentAmount);
            VendorPaymentResponse r = new VendorPaymentResponse();
            r.setId(p.getId());
            r.setCompanyId(p.getCompanyId());
            r.setVendorPartnerId(p.getVendorPartnerId());
            r.setPaymentDate(p.getPaymentDate());
            r.setBankJournalId(p.getBankJournalId());
            r.setAmount(p.getAmount());
            r.setCurrencyCode(p.getCurrencyCode());
            r.setExchangeRateToCompany(p.getExchangeRateToCompany());
            r.setJournalEntryId(p.getJournalEntryId());
            r.setReversalJournalEntryId(p.getReversalJournalEntryId());
            r.setReference(p.getReference());
            r.setState(p.getState() != null ? p.getState().name() : VendorPaymentState.POSTED.name());
            r.setPaymentKind(p.getPaymentKind() != null ? p.getPaymentKind().name() : VendorPaymentKind.PAYOUT.name());
            r.setOpeningBalance(p.isOpeningBalance());
            r.setAllocatedAmount(allocated);
            r.setUnallocatedAmount(unallocated(p, allocated));
            r.setAllocations(list.stream().map(a -> toAllocationResponse(a, p, bills)).toList());
            out.add(r);
        }
        return out;
    }

    private VendorPaymentAllocationResponse toAllocationResponse(VendorPaymentAllocation a, VendorPayment p,
                                                                 Map<UUID, VendorBill> bills) {
        VendorPaymentAllocationResponse r = new VendorPaymentAllocationResponse();
        r.setId(a.getId());
        r.setPaymentId(a.getPaymentId());
        if (p != null) {
            r.setPaymentReference(p.getReference());
            r.setPaymentDate(p.getPaymentDate());
            r.setCurrencyCode(p.getCurrencyCode());
        }
        r.setVendorBillId(a.getVendorBillId());
        VendorBill bill = bills.get(a.getVendorBillId());
        if (bill != null) {
            r.setBillReference(bill.getReference());
            r.setBillMoveType((bill.getMoveType() != null ? bill.getMoveType() : VendorBillMoveType.BILL).name());
            r.setBillOpeningBalance(bill.isOpeningBalance());
            // `amount` is in the document's currency (the payment's own part is paymentAmount).
            r.setCurrencyCode(bill.getCurrencyCode());
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

    /** The payable line(s) of one entry for the partner; all trade lines share one account. */
    private TradeLine tradeLine(UUID journalEntryId, UUID partnerId) {
        List<ItemSnapshot> items = journalItemPort.findItemsByEntryIds(List.of(journalEntryId)).stream()
                .filter(i -> i.accountType() == AccountType.PAYABLE && Objects.equals(partnerId, i.partnerId()))
                .toList();
        if (items.isEmpty()) {
            throw new PurchaseDomainException("error.purchase.apLineNotFoundOnEntry", null,
                    "Could not find the payable line on the journal entry");
        }
        BigDecimal net = items.stream().map(ItemSnapshot::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TradeLine(items.get(0).accountId(), net.abs().setScale(4, RoundingMode.HALF_UP));
    }

    private VendorPayment loadPayment(UUID paymentId, boolean forUpdate) {
        VendorPayment p = (forUpdate ? paymentRepository.findByIdForUpdate(paymentId) : paymentRepository.findById(paymentId))
                .orElseThrow(() -> new PurchaseDomainException("error.purchase.vendorPaymentNotFound", null,
                        "Vendor payment not found"));
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        UUID companyId = ctx != null ? ctx.currentCompany().map(CompanyId::getId).orElse(null) : null;
        if (companyId != null && !p.getCompanyId().equals(companyId)) {
            throw new PurchaseDomainException("error.purchase.vendorPaymentNotFound", null, "Vendor payment not found");
        }
        return p;
    }

    private static BigDecimal unallocated(VendorPayment p, BigDecimal allocated) {
        if (p.getState() != VendorPaymentState.POSTED) {
            return BigDecimal.ZERO.setScale(4);
        }
        return p.getAmount().subtract(allocated).setScale(4, RoundingMode.HALF_UP);
    }

    /** Document-currency amount expressed in the payment currency, through the company currency. */
    private static BigDecimal docToPayment(BigDecimal docAmount, VendorBill bill, BigDecimal paymentRate) {
        BigDecimal docRate = bill.getExchangeRateToCompany() != null ? bill.getExchangeRateToCompany() : BigDecimal.ONE;
        BigDecimal payRate = paymentRate != null && paymentRate.signum() > 0 ? paymentRate : BigDecimal.ONE;
        return docAmount.multiply(docRate).divide(payRate, 4, RoundingMode.HALF_UP);
    }

    /** Payment-currency amount expressed in the document currency, through the company currency. */
    private static BigDecimal paymentToDoc(BigDecimal paymentAmount, VendorBill bill, BigDecimal paymentRate) {
        BigDecimal docRate = bill.getExchangeRateToCompany() != null && bill.getExchangeRateToCompany().signum() > 0
                ? bill.getExchangeRateToCompany() : BigDecimal.ONE;
        BigDecimal payRate = paymentRate != null && paymentRate.signum() > 0 ? paymentRate : BigDecimal.ONE;
        return paymentAmount.multiply(payRate).divide(docRate, 4, RoundingMode.HALF_UP);
    }

    private static boolean isCreditNote(VendorBill bill) {
        return bill.getMoveType() == VendorBillMoveType.CREDIT_NOTE;
    }

    private static boolean isPostedCreditNoteFor(VendorBill cn, VendorBill source) {
        return cn.getState() == VendorBillState.POSTED
                && cn.getMoveType() == VendorBillMoveType.CREDIT_NOTE
                && source.getId().equals(cn.getReversedBillId())
                && source.getCurrencyCode().equalsIgnoreCase(cn.getCurrencyCode());
    }

    private static String docLabel(VendorBill bill) {
        return bill.getReference() != null ? bill.getReference() : bill.getId().toString();
    }

    private static void ensureLinesLoaded(VendorBill bill) {
        bill.getLines().size();
        for (var line : bill.getLines()) {
            line.getTaxSnapshots().size();
        }
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

    private UUID payableAccount(UUID companyId, PartnerResponse vendor) {
        if (vendor.getPayableAccountId() != null) {
            return vendor.getPayableAccountId();
        }
        return accountByCode(companyId, DEFAULT_AP_ACCOUNT_CODE, "error.purchase.defaultApAccountNotFound");
    }

    private UUID accountByCode(UUID companyId, String code, String errorKey) {
        return accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), code)
                .orElseThrow(() -> new PurchaseDomainException(errorKey, null, "Account " + code + " not found"))
                .getId().getId();
    }

    private UUID resolveLiquidityAccount(UUID companyId, Journal j) {
        if (!j.getCompanyId().getId().equals(companyId)) {
            throw new PurchaseDomainException("error.purchase.journalCompanyMismatch", null, "Journal company mismatch");
        }
        if (j.getJournalType() != JournalType.CASH && j.getJournalType() != JournalType.BANK) {
            throw new PurchaseDomainException("error.purchase.paymentJournalCashOrBank", null, "Payment journal must be cash or bank");
        }
        return accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), j.getCode())
                .orElseThrow(() -> new PurchaseDomainException(
                        "error.purchase.liquidityAccountNotFoundForJournal",
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
    public List<VendorPaymentAllocation> allocationsForBills(Collection<UUID> billIds) {
        return allocationRepository.findByBillIdIn(billIds);
    }
}
