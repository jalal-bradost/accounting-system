package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceAdjustmentCommand;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceCommand;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceLine;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceResponse;
import com.bradox.erp.accounting.service.domain.create.OpeningPartnerLine;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.input.service.OpeningBalanceApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.OpeningPayablePort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerInvoiceRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerPaymentRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalRepository;
import com.bradox.erp.domain.core.ValueObject.AccountId;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.core.ValueObject.CustomerPaymentState;
import com.bradox.erp.domain.core.ValueObject.JournalEntryStatus;
import com.bradox.erp.domain.core.entity.Account;
import com.bradox.erp.domain.core.entity.CustomerInvoice;
import com.bradox.erp.domain.core.entity.CustomerPayment;
import com.bradox.erp.domain.core.entity.Journal;
import com.bradox.erp.domain.core.entity.JournalEntry;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class OpeningBalanceApplicationServiceImpl implements OpeningBalanceApplicationService {

    private static final String OPENING_JOURNAL_CODE = "OPEN";
    private static final String OPENING_BALANCE_EQUITY_ACCOUNT_CODE = "430019";
    private static final String OPENING_BALANCE_ADJUSTMENT_ACCOUNT_CODE = "430025";
    private static final String OPENING_BALANCE_ADJUSTMENT_ACCOUNT_NAME = "Opening Balance Adjustment";
    private static final String LINE_LABEL = "Opening balance";
    private static final String ADJUSTMENT_REFERENCE_DEFAULT = "OB-ADJ";

    private final AccountRepository accountRepository;
    private final JournalRepository journalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final CustomerPaymentService customerPaymentService;
    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;
    private final OpeningPayablePort openingPayablePort;
    private final CompanyCurrencyRepository companyCurrencyRepository;

    OpeningBalanceApplicationServiceImpl(AccountRepository accountRepository,
                                         JournalRepository journalRepository,
                                         JournalEntryRepository journalEntryRepository,
                                         JournalEntryApplicationService journalEntryApplicationService,
                                         CustomerInvoiceRepository customerInvoiceRepository,
                                         CustomerPaymentRepository customerPaymentRepository,
                                         CustomerPaymentService customerPaymentService,
                                         CustomerInvoiceApplicationService customerInvoiceApplicationService,
                                         OpeningPayablePort openingPayablePort,
                                         CompanyCurrencyRepository companyCurrencyRepository) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.customerInvoiceRepository = customerInvoiceRepository;
        this.customerPaymentRepository = customerPaymentRepository;
        this.customerPaymentService = customerPaymentService;
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
        this.openingPayablePort = openingPayablePort;
        this.companyCurrencyRepository = companyCurrencyRepository;
    }

    @Override
    @Transactional
    public OpeningBalanceResponse setOpeningBalances(OpeningBalanceCommand command) {
        CompanyId companyId = new CompanyId(command.getCompanyId());

        Journal openingJournal = journalRepository.findByCompanyIdAndCode(companyId, OPENING_JOURNAL_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "Opening journal (code " + OPENING_JOURNAL_CODE + ") not found for company."));
        Account obeAccount = accountRepository.findByCompanyIdAndCode(companyId, OPENING_BALANCE_EQUITY_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "Opening Balance Equity account (code " + OPENING_BALANCE_EQUITY_ACCOUNT_CODE
                                + ") not found for company."));

        UUID openingJournalId = openingJournal.getId().getId();
        UUID obeAccountId = obeAccount.getId().getId();

        List<OpeningBalanceLine> glLines = new ArrayList<>();
        List<OpeningPartnerLine> customerLines = new ArrayList<>(command.getCustomerLines());
        List<OpeningPartnerLine> vendorLines = new ArrayList<>(command.getVendorLines());
        splitLegacyLines(command.getLines(), glLines, customerLines, vendorLines);

        if (glLines.isEmpty() && customerLines.isEmpty() && vendorLines.isEmpty()) {
            throw new AccountingDomainException(
                    "Opening balances need at least one non-zero GL or partner line.");
        }

        handleExistingOpening(companyId, openingJournalId, command.isReplace());

        String currency = resolveCurrencyCode(companyId, command.getCurrencyCode());
        LocalDate defaultDate = command.getDate();
        BigDecimal plug = BigDecimal.ZERO;
        UUID glJournalEntryId = null;

        if (!glLines.isEmpty()) {
            List<JournalItemCommand> items = new ArrayList<>();
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;

            for (OpeningBalanceLine line : glLines) {
                BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                Account account = accountRepository.findById(new AccountId(line.getAccountId()))
                        .orElseThrow(() -> new AccountingDomainException("Account not found: " + line.getAccountId()));
                if (isTradeAccount(account.getAccountType())) {
                    throw new AccountingDomainException(
                            "Receivable/payable accounts belong in the customer/vendor sections, not the GL section"
                                    + " (account " + account.getCode() + ").");
                }
                if (line.getPartnerId() != null) {
                    throw new AccountingDomainException(
                            "A partner may only be set on receivable/payable opening balances (account "
                                    + account.getCode() + ").");
                }

                BigDecimal debit = amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ZERO;
                BigDecimal credit = amount.compareTo(BigDecimal.ZERO) < 0 ? amount.negate() : BigDecimal.ZERO;
                totalDebit = totalDebit.add(debit);
                totalCredit = totalCredit.add(credit);
                items.add(new JournalItemCommand(line.getAccountId(), LINE_LABEL, debit, credit, currency, null, null));
            }

            plug = totalDebit.subtract(totalCredit);
            if (plug.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal obeDebit = plug.compareTo(BigDecimal.ZERO) < 0 ? plug.negate() : BigDecimal.ZERO;
                BigDecimal obeCredit = plug.compareTo(BigDecimal.ZERO) > 0 ? plug : BigDecimal.ZERO;
                items.add(new JournalItemCommand(obeAccountId, LINE_LABEL, obeDebit, obeCredit, currency, null, null));
            }

            if (items.size() < 2) {
                throw new AccountingDomainException(
                        "Opening balances need at least two non-zero lines (or one line plus the equity plug).");
            }

            CreateJournalEntryCommand createCommand = new CreateJournalEntryCommand(
                    command.getCompanyId(), openingJournalId, null,
                    JournalEntryTiming.ofBusinessDate(defaultDate), currency, items);
            CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(createCommand);
            journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());
            glJournalEntryId = created.getJournalEntryId();
        }

        for (OpeningPartnerLine line : customerLines) {
            postCustomerOpening(command.getCompanyId(), line, currency, defaultDate, openingJournal,
                    openingJournalId, obeAccountId);
        }
        for (OpeningPartnerLine line : vendorLines) {
            postVendorOpening(command.getCompanyId(), line, currency, defaultDate,
                    openingJournalId, obeAccountId);
        }

        return new OpeningBalanceResponse(glJournalEntryId, plug,
                "Opening balances posted successfully.");
    }

    @Override
    @Transactional
    public OpeningBalanceResponse postAdjustments(OpeningBalanceAdjustmentCommand command) {
        CompanyId companyId = new CompanyId(command.getCompanyId());
        Journal openingJournal = journalRepository.findByCompanyIdAndCode(companyId, OPENING_JOURNAL_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "Opening journal (code " + OPENING_JOURNAL_CODE + ") not found for company."));
        Account obaAccount = ensureOpeningBalanceAdjustmentAccount(companyId);
        UUID openingJournalId = openingJournal.getId().getId();
        UUID obaAccountId = obaAccount.getId().getId();

        List<OpeningPartnerLine> customerLines = command.getCustomerLines().stream()
                .filter(l -> l.getAmount() != null && l.getAmount().signum() != 0)
                .toList();
        List<OpeningPartnerLine> vendorLines = command.getVendorLines().stream()
                .filter(l -> l.getAmount() != null && l.getAmount().signum() != 0)
                .toList();
        if (customerLines.isEmpty() && vendorLines.isEmpty()) {
            throw new AccountingDomainException(
                    "Opening balance adjustments need at least one non-zero customer or vendor line.");
        }

        String currency = resolveCurrencyCode(companyId, command.getCurrencyCode());
        LocalDate defaultDate = command.getDate();

        for (OpeningPartnerLine line : customerLines) {
            postCustomerOpening(command.getCompanyId(), withDefaultReference(line), currency, defaultDate,
                    openingJournal, openingJournalId, obaAccountId);
        }
        for (OpeningPartnerLine line : vendorLines) {
            postVendorOpening(command.getCompanyId(), withDefaultReference(line), currency, defaultDate,
                    openingJournalId, obaAccountId);
        }

        return new OpeningBalanceResponse(null, BigDecimal.ZERO,
                "Opening balance adjustments posted successfully.");
    }

    private Account ensureOpeningBalanceAdjustmentAccount(CompanyId companyId) {
        return accountRepository.findByCompanyIdAndCode(companyId, OPENING_BALANCE_ADJUSTMENT_ACCOUNT_CODE)
                .orElseGet(() -> accountRepository.save(Account.builder()
                        .id(new AccountId(UUID.randomUUID()))
                        .companyId(companyId)
                        .code(OPENING_BALANCE_ADJUSTMENT_ACCOUNT_CODE)
                        .name(OPENING_BALANCE_ADJUSTMENT_ACCOUNT_NAME)
                        .accountType(AccountType.EQUITY)
                        .active(true)
                        .build()));
    }

    private static OpeningPartnerLine withDefaultReference(OpeningPartnerLine line) {
        String ref = line.getReference();
        if (ref != null && !ref.isBlank()) {
            return line;
        }
        return new OpeningPartnerLine(line.getPartnerId(), line.getAmount(), ADJUSTMENT_REFERENCE_DEFAULT,
                line.getDate(), line.getDueDate());
    }

    private void splitLegacyLines(List<OpeningBalanceLine> lines,
                                  List<OpeningBalanceLine> glLines,
                                  List<OpeningPartnerLine> customerLines,
                                  List<OpeningPartnerLine> vendorLines) {
        for (OpeningBalanceLine line : lines) {
            BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Account account = accountRepository.findById(new AccountId(line.getAccountId()))
                    .orElseThrow(() -> new AccountingDomainException("Account not found: " + line.getAccountId()));
            if (account.getAccountType() == AccountType.RECEIVABLE) {
                if (line.getPartnerId() == null) {
                    throw new AccountingDomainException(
                            "A partner is required for receivable opening balance on account " + account.getCode() + ".");
                }
                customerLines.add(new OpeningPartnerLine(line.getPartnerId(), amount, null, null, null));
            } else if (account.getAccountType() == AccountType.PAYABLE) {
                if (line.getPartnerId() == null) {
                    throw new AccountingDomainException(
                            "A partner is required for payable opening balance on account " + account.getCode() + ".");
                }
                // Legacy signed AP lines: negative amount = we owe (bill), positive = prepayment.
                vendorLines.add(new OpeningPartnerLine(line.getPartnerId(), amount.negate(), null, null, null));
            } else {
                glLines.add(line);
            }
        }
    }

    private void postCustomerOpening(UUID companyId, OpeningPartnerLine line, String currency, LocalDate defaultDate,
                                     Journal openingJournal, UUID openingJournalId, UUID obeAccountId) {
        BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        LocalDate date = line.getDate() != null ? line.getDate() : defaultDate;
        if (amount.signum() > 0) {
            customerInvoiceApplicationService.createOpeningCustomerInvoice(
                    companyId, line.getPartnerId(), amount, currency, date, line.getDueDate(),
                    line.getReference(), openingJournalId, obeAccountId);
        } else {
            customerPaymentService.postOpeningPayment(
                    companyId, line.getPartnerId(), amount.negate(), currency, null, date,
                    line.getReference(), openingJournal, obeAccountId);
        }
    }

    private void postVendorOpening(UUID companyId, OpeningPartnerLine line, String currency, LocalDate defaultDate,
                                   UUID openingJournalId, UUID obeAccountId) {
        BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        LocalDate date = line.getDate() != null ? line.getDate() : defaultDate;
        if (amount.signum() > 0) {
            openingPayablePort.createOpeningVendorBill(
                    companyId, line.getPartnerId(), amount, currency, date, line.getDueDate(),
                    line.getReference(), openingJournalId, obeAccountId);
        } else {
            openingPayablePort.postOpeningPayment(
                    companyId, line.getPartnerId(), amount.negate(), currency, date,
                    line.getReference(), openingJournalId, obeAccountId);
        }
    }

    private void handleExistingOpening(CompanyId companyId, UUID openingJournalId, boolean replace) {
        List<CustomerInvoice> openingInvoices = customerInvoiceRepository
                .findOpeningBalanceByCompanyId(companyId.getId()).stream()
                .filter(i -> i.getState() == CustomerInvoiceState.POSTED)
                .toList();
        List<CustomerPayment> openingPayments = customerPaymentRepository
                .findOpeningBalanceByCompanyId(companyId.getId()).stream()
                .filter(p -> p.getState() == CustomerPaymentState.POSTED)
                .toList();
        List<OpeningPayablePort.OpeningVendorBillRef> openingBills =
                openingPayablePort.findOpeningBills(companyId.getId());
        List<OpeningPayablePort.OpeningVendorPaymentRef> openingVendorPayments =
                openingPayablePort.findOpeningPayments(companyId.getId());

        List<UUID> openingDocJeIds = new ArrayList<>();
        openingInvoices.stream().map(CustomerInvoice::getJournalEntryId).filter(id -> id != null)
                .forEach(openingDocJeIds::add);
        openingPayments.stream().map(CustomerPayment::getJournalEntryId).filter(id -> id != null)
                .forEach(openingDocJeIds::add);
        openingBills.stream().map(OpeningPayablePort.OpeningVendorBillRef::journalEntryId).filter(id -> id != null)
                .forEach(openingDocJeIds::add);
        openingVendorPayments.stream().map(OpeningPayablePort.OpeningVendorPaymentRef::journalEntryId)
                .filter(id -> id != null).forEach(openingDocJeIds::add);

        List<JournalEntry> existingGl = journalEntryRepository.findByCompanyId(companyId).stream()
                .filter(e -> e.getJournalId() != null && openingJournalId.equals(e.getJournalId().getId()))
                .filter(e -> e.getStatus() == JournalEntryStatus.POSTED)
                .filter(e -> !openingDocJeIds.contains(e.getId().getId()))
                .toList();

        boolean anyExisting = !existingGl.isEmpty() || !openingInvoices.isEmpty() || !openingPayments.isEmpty()
                || !openingBills.isEmpty() || !openingVendorPayments.isEmpty();
        if (!anyExisting) {
            return;
        }
        if (!replace) {
            throw new AccountingDomainException(
                    "Opening balances already exist for this company. Enable replace to overwrite them.");
        }

        List<String> blockers = new ArrayList<>();
        List<UUID> invoiceIds = openingInvoices.stream().map(CustomerInvoice::getId).toList();
        List<UUID> paymentIds = openingPayments.stream().map(CustomerPayment::getId).toList();
        if (customerPaymentService.hasActiveAllocations(invoiceIds, paymentIds)) {
            blockers.addAll(openingInvoices.stream()
                    .map(i -> "customer invoice " + ref(i.getReference(), i.getId()))
                    .toList());
            blockers.addAll(openingPayments.stream()
                    .map(p -> "customer payment " + ref(p.getReference(), p.getId()))
                    .toList());
        }
        for (CustomerInvoice inv : openingInvoices) {
            boolean hasCn = customerInvoiceRepository.findByReversedInvoiceIdWithLines(inv.getId()).stream()
                    .anyMatch(cn -> cn.getState() != CustomerInvoiceState.CANCELLED);
            if (hasCn) {
                blockers.add("customer invoice " + ref(inv.getReference(), inv.getId()) + " (has credit note)");
            }
        }

        List<UUID> billIds = openingBills.stream().map(OpeningPayablePort.OpeningVendorBillRef::id).toList();
        List<UUID> vendorPaymentIds = openingVendorPayments.stream()
                .map(OpeningPayablePort.OpeningVendorPaymentRef::id).toList();
        if (openingPayablePort.hasActiveAllocations(billIds, vendorPaymentIds)) {
            blockers.addAll(openingBills.stream()
                    .map(b -> "vendor bill " + ref(b.reference(), b.id()))
                    .toList());
            blockers.addAll(openingVendorPayments.stream()
                    .map(p -> "vendor payment " + ref(p.reference(), p.id()))
                    .toList());
        }
        if (openingPayablePort.hasCreditNotes(billIds)) {
            blockers.addAll(openingBills.stream()
                    .map(b -> "vendor bill " + ref(b.reference(), b.id()) + " (has credit note)")
                    .toList());
        }

        if (!blockers.isEmpty()) {
            throw new AccountingDomainException(
                    "Cannot replace opening balances; the following documents block replace: "
                            + blockers.stream().distinct().collect(Collectors.joining(", ")));
        }

        for (JournalEntry entry : existingGl) {
            journalEntryApplicationService.reverseJournalEntry(
                    new ReverseJournalEntryCommand(entry.getId().getId(), "Opening balances replaced"));
        }
        for (CustomerInvoice inv : openingInvoices) {
            customerInvoiceApplicationService.cancelOpeningCustomerInvoice(inv.getId());
        }
        for (CustomerPayment payment : openingPayments) {
            customerPaymentService.reverse(payment.getId(), "Opening balances replaced");
        }
        for (OpeningPayablePort.OpeningVendorBillRef bill : openingBills) {
            openingPayablePort.cancelOpeningBill(bill.id());
        }
        for (OpeningPayablePort.OpeningVendorPaymentRef payment : openingVendorPayments) {
            openingPayablePort.reverseOpeningPayment(payment.id(), "Opening balances replaced");
        }
    }

    private static String ref(String reference, UUID id) {
        return reference != null && !reference.isBlank() ? reference : id.toString();
    }

    private boolean isTradeAccount(AccountType type) {
        return type == AccountType.RECEIVABLE || type == AccountType.PAYABLE;
    }

    /** Prefer the request currency; fall back to the company base currency (never invent USD). */
    private String resolveCurrencyCode(CompanyId companyId, String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim().toUpperCase();
        }
        return companyCurrencyRepository.findBaseCurrency(companyId)
                .map(CompanyCurrencyRepository.CurrencyRow::code)
                .orElseThrow(() -> new AccountingDomainException(
                        "No currency provided and company has no base currency configured."));
    }
}
