package com.bradox.delin.expense.service.domain;

import com.bradox.delin.accounting.service.domain.JournalEntryTiming;
import com.bradox.delin.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.delin.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.delin.accounting.service.domain.create.JournalItemCommand;
import com.bradox.delin.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.delin.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.delin.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.delin.domain.core.ValueObject.JournalType;
import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.expense.domain.core.entity.Expense;
import com.bradox.delin.expense.domain.core.entity.ExpenseState;
import com.bradox.delin.expense.domain.core.entity.ReimbursementType;
import com.bradox.delin.expense.domain.core.exception.ExpenseDomainException;
import com.bradox.delin.expense.domain.core.valueobject.ExpenseId;
import com.bradox.delin.expense.service.domain.dto.ExpenseResponse;
import com.bradox.delin.expense.service.domain.dto.ExpenseSummaryResponse;
import com.bradox.delin.expense.service.domain.dto.RegisterExpensePaymentCommand;
import com.bradox.delin.expense.service.domain.dto.SaveExpenseCommand;
import com.bradox.delin.expense.service.domain.ports.input.ExpenseApplicationService;
import com.bradox.delin.expense.service.domain.ports.output.repository.ExpenseRepository;
import com.bradox.delin.expense.service.domain.ports.output.repository.ExpenseTypeRepository;
import com.bradox.delin.expense.domain.core.valueobject.ExpenseTypeId;
import com.bradox.delin.hr.service.domain.dto.EmployeeResponse;
import com.bradox.delin.hr.service.domain.ports.input.EmployeeApplicationService;
import com.bradox.delin.inventory.service.domain.dto.ProductResponse;
import com.bradox.delin.inventory.service.domain.ports.input.ProductApplicationService;
import com.bradox.delin.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class ExpenseApplicationServiceImpl implements ExpenseApplicationService {

    private static final String DEFAULT_AP_ACCOUNT_CODE = "430004";
    private static final String DEFAULT_EXPENSE_ACCOUNT_CODE = "430021";
    private static final String EXPENSE_JOURNAL_CODE = "EXP";

    private final ExpenseRepository expenseRepository;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final EmployeeApplicationService employeeApplicationService;
    private final ProductApplicationService productApplicationService;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final AccountingReferenceLookupPort accountingReferenceLookupPort;
    private final CurrencyConversionPort currencyConversionPort;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    ExpenseApplicationServiceImpl(ExpenseRepository expenseRepository,
                                  ExpenseTypeRepository expenseTypeRepository,
                                  EmployeeApplicationService employeeApplicationService,
                                  ProductApplicationService productApplicationService,
                                  JournalEntryApplicationService journalEntryApplicationService,
                                  AccountingReferenceLookupPort accountingReferenceLookupPort,
                                  CurrencyConversionPort currencyConversionPort,
                                  ObjectProvider<CompanyContext> companyContextProvider) {
        this.expenseRepository = expenseRepository;
        this.expenseTypeRepository = expenseTypeRepository;
        this.employeeApplicationService = employeeApplicationService;
        this.productApplicationService = productApplicationService;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.accountingReferenceLookupPort = accountingReferenceLookupPort;
        this.currencyConversionPort = currencyConversionPort;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(CompanyId companyId, UUID employeeId, ExpenseState state) {
        return expenseRepository.search(requireCompany(companyId), employeeId, state).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResponse summary(CompanyId companyId, UUID employeeId) {
        CompanyId cid = requireCompany(companyId);
        BigDecimal toSubmit = expenseRepository.sumTotalByStates(cid, employeeId, List.of(ExpenseState.DRAFT));
        BigDecimal waitingApproval = expenseRepository.sumTotalByStates(cid, employeeId, List.of(ExpenseState.SUBMITTED));
        BigDecimal waitingReimbursement = expenseRepository.sumTotalByStates(
                cid, employeeId, List.of(ExpenseState.APPROVED, ExpenseState.POSTED));
        return new ExpenseSummaryResponse(toSubmit, waitingApproval, waitingReimbursement);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID id) {
        return toResponse(requireExpense(id));
    }

    @Override
    @Transactional
    public ExpenseResponse create(SaveExpenseCommand command) {
        CompanyId companyId = requireCompany(command.getCompanyId() != null
                ? new CompanyId(command.getCompanyId())
                : null);
        EmployeeResponse employee = employeeApplicationService.get(command.getEmployeeId());
        if (!companyId.getId().equals(employee.companyId())) {
            throw new ExpenseDomainException("error.expense.employeeCompanyMismatch", null, "Employee does not belong to company");
        }
        UUID managerId = command.getManagerEmployeeId() != null
                ? command.getManagerEmployeeId()
                : employee.managerId();
        UUID accountId = resolveAccountId(companyId.getId(), command.getAccountId(), command.getProductId());
        Instant now = Instant.now();
        Expense expense = Expense.builder()
                .id(new ExpenseId(UUID.randomUUID()))
                .companyId(companyId)
                .description(command.getDescription())
                .productId(command.getProductId())
                .accountId(accountId)
                .expenseTypeId(command.getExpenseTypeId())
                .employeeId(command.getEmployeeId())
                .managerEmployeeId(managerId)
                .expenseDate(command.getExpenseDate())
                .total(nz(command.getTotal()))
                .taxAmount(nz(command.getTaxAmount()))
                .currencyCode(command.getCurrencyCode() != null && !command.getCurrencyCode().isBlank()
                        ? command.getCurrencyCode().trim().toUpperCase()
                        : currencyConversionPort.baseCurrencyCode(companyId.getId()))
                .reimbursement(command.getReimbursement() != null
                        ? command.getReimbursement()
                        : ReimbursementType.EMPLOYEE)
                .notes(command.getNotes())
                .state(ExpenseState.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        expense.validate();
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse update(UUID id, SaveExpenseCommand command) {
        Expense expense = requireExpense(id);
        CompanyId companyId = expense.getCompanyId();
        EmployeeResponse employee = employeeApplicationService.get(command.getEmployeeId());
        if (!companyId.getId().equals(employee.companyId())) {
            throw new ExpenseDomainException("error.expense.employeeCompanyMismatch", null, "Employee does not belong to company");
        }
        UUID managerId = command.getManagerEmployeeId() != null
                ? command.getManagerEmployeeId()
                : employee.managerId();
        UUID accountId = resolveAccountId(companyId.getId(), command.getAccountId(), command.getProductId());
        expense.applyUpdate(
                command.getDescription(),
                command.getProductId(),
                accountId,
                command.getExpenseTypeId(),
                command.getEmployeeId(),
                managerId,
                command.getExpenseDate(),
                nz(command.getTotal()),
                nz(command.getTaxAmount()),
                command.getCurrencyCode() != null && !command.getCurrencyCode().isBlank()
                        ? command.getCurrencyCode()
                        : expense.getCurrencyCode(),
                command.getReimbursement() != null ? command.getReimbursement() : expense.getReimbursement(),
                command.getNotes());
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse submit(UUID id) {
        Expense expense = requireExpense(id);
        expense.submit();
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse approve(UUID id) {
        Expense expense = requireExpense(id);
        expense.approve();
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse refuse(UUID id) {
        Expense expense = requireExpense(id);
        expense.refuse();
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse post(UUID id) {
        Expense expense = requireExpense(id);
        if (expense.getState() != ExpenseState.DRAFT
                && expense.getState() != ExpenseState.SUBMITTED
                && expense.getState() != ExpenseState.APPROVED) {
            throw new ExpenseDomainException(
                    "error.expense.onlyApprovedCanPost", null, "Only draft or approved expenses can be posted");
        }
        if (expense.getAccountId() == null) {
            UUID resolved = resolveAccountId(expense.getCompanyId().getId(), null, expense.getProductId());
            if (resolved != null) {
                expense.setAccountId(resolved);
            }
        }
        if (expense.getAccountId() == null) {
            throw new ExpenseDomainException("error.expense.accountIdRequiredToPost", null, "accountId required to post");
        }

        UUID payableAccount = accountingReferenceLookupPort.resolveAccountIdByCode(
                expense.getCompanyId().getId(), DEFAULT_AP_ACCOUNT_CODE);
        UUID expenseJournalId = resolveExpenseJournalId(expense.getCompanyId().getId());

        BigDecimal total = expense.getTotal().setScale(4, RoundingMode.HALF_UP);
        if (total.signum() <= 0) {
            throw new ExpenseDomainException("error.expense.totalPositiveToPost", null, "total must be positive to post");
        }

        List<JournalItemCommand> items = new ArrayList<>();
        // Tax is included in the expense debit to keep posting simple (no separate tax account).
        items.add(new JournalItemCommand(
                expense.getAccountId(),
                expense.getDescription(),
                total,
                BigDecimal.ZERO,
                expense.getCurrencyCode(),
                total,
                null));
        items.add(new JournalItemCommand(
                payableAccount,
                "Expense payable",
                BigDecimal.ZERO,
                total,
                expense.getCurrencyCode(),
                total.negate(),
                null));

        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                expense.getCompanyId().getId(),
                expenseJournalId,
                "",
                JournalEntryTiming.ofBusinessDate(expense.getExpenseDate()),
                expense.getCurrencyCode(),
                null,
                items);
        CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());
        expense.markPosted(created.getJournalEntryId());
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse registerPayment(UUID id, RegisterExpensePaymentCommand command) {
        Expense expense = requireExpense(id);
        if (expense.getState() != ExpenseState.POSTED) {
            throw new ExpenseDomainException("error.expense.onlyPostedCanBePaid", null, "Only posted expenses can be paid");
        }
        if (expense.getJournalEntryId() == null) {
            throw new ExpenseDomainException("error.expense.mustBePostedToGlBeforePayment", null, "Expense must be posted to the GL before payment");
        }

        UUID companyId = expense.getCompanyId().getId();
        BigDecimal amount = command.getAmount().setScale(4, RoundingMode.HALF_UP);
        BigDecimal due = expense.getAmountDue();
        if (amount.compareTo(due) > 0) {
            throw new ExpenseDomainException("error.expense.paymentExceedsAmountDueWithDetail", new Object[] { due }, "payment amount exceeds amount due (" + due + ")");
        }

        UUID payableAccount = accountingReferenceLookupPort.resolveAccountIdByCode(
                companyId, DEFAULT_AP_ACCOUNT_CODE);
        JournalType paymentJournalType = accountingReferenceLookupPort
                .resolveJournalType(companyId, command.getBankJournalId());
        if (paymentJournalType != JournalType.CASH && paymentJournalType != JournalType.BANK) {
            throw new ExpenseDomainException("error.expense.paymentJournalCashOrBank", null, "Payment journal must be cash or bank");
        }
        UUID liquidityAccountId = accountingReferenceLookupPort
                .resolveLiquidityAccountIdForJournal(companyId, command.getBankJournalId());
        String liquidityLabel = paymentJournalType == JournalType.CASH ? "Cash payment" : "Bank payment";
        String paymentCurrency = command.getCurrencyCode() != null && !command.getCurrencyCode().isBlank()
                ? command.getCurrencyCode()
                : expense.getCurrencyCode();
        String reference = command.getReference() != null && !command.getReference().isBlank()
                ? command.getReference()
                : "Expense payment: " + expense.getDescription();

        List<JournalItemCommand> items = new ArrayList<>();
        // Clear payable (debit AP) and credit bank/cash — same effect as vendor bill payment.
        items.add(new JournalItemCommand(
                payableAccount,
                reference,
                amount,
                BigDecimal.ZERO,
                paymentCurrency,
                amount,
                null));
        items.add(new JournalItemCommand(
                liquidityAccountId,
                liquidityLabel,
                BigDecimal.ZERO,
                amount,
                paymentCurrency,
                amount.negate(),
                null));

        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                command.getBankJournalId(),
                "",
                JournalEntryTiming.ofBusinessDate(command.getPaymentDate()),
                paymentCurrency,
                null,
                items);
        CreateJournalEntryResponse payEntry = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(payEntry.getJournalEntryId());

        expense.registerPayment(
                payEntry.getJournalEntryId(),
                command.getBankJournalId(),
                amount,
                command.getPaymentDate(),
                reference);
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse cancel(UUID id) {
        Expense expense = requireExpense(id);
        expense.cancel();
        return toResponse(expenseRepository.save(expense));
    }

    private UUID resolveExpenseJournalId(UUID companyId) {
        try {
            return accountingReferenceLookupPort.resolveJournalIdByCode(companyId, EXPENSE_JOURNAL_CODE);
        } catch (RuntimeException ex) {
            return accountingReferenceLookupPort.resolveJournalIdByType(companyId, JournalType.MISC);
        }
    }

    private UUID resolveAccountId(UUID companyId, UUID accountId, UUID productId) {
        if (accountId != null) {
            return accountId;
        }
        if (productId != null) {
            try {
                ProductResponse product = productApplicationService.getProduct(productId);
                if (product.getCogsAccountIdOverride() != null) {
                    return product.getCogsAccountIdOverride();
                }
            } catch (RuntimeException ignored) {
                // fall through to default expense account
            }
        }
        try {
            return accountingReferenceLookupPort.resolveAccountIdByCode(companyId, DEFAULT_EXPENSE_ACCOUNT_CODE);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Expense requireExpense(UUID id) {
        return expenseRepository.findById(new ExpenseId(id))
                .orElseThrow(() -> new ExpenseDomainException("Expense not found: " + id));
    }

    private CompanyId requireCompany(CompanyId companyId) {
        if (companyId != null && companyId.getId() != null) {
            return companyId;
        }
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany()
                    .orElseThrow(() -> new ExpenseDomainException("companyId required"));
        }
        throw new ExpenseDomainException("error.expense.companyIdRequired", null, "companyId required");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private ExpenseResponse toResponse(Expense e) {
        ExpenseResponse r = new ExpenseResponse();
        r.setId(e.getId().getId());
        r.setCompanyId(e.getCompanyId().getId());
        r.setDescription(e.getDescription());
        r.setProductId(e.getProductId());
        r.setAccountId(e.getAccountId());
        r.setExpenseTypeId(e.getExpenseTypeId());
        r.setEmployeeId(e.getEmployeeId());
        r.setManagerEmployeeId(e.getManagerEmployeeId());
        r.setExpenseDate(e.getExpenseDate());
        r.setTotal(e.getTotal());
        r.setTaxAmount(e.getTaxAmount());
        r.setCurrencyCode(e.getCurrencyCode());
        r.setReimbursement(e.getReimbursement());
        r.setNotes(e.getNotes());
        r.setState(e.getState());
        r.setJournalEntryId(e.getJournalEntryId());
        r.setPaymentJournalEntryId(e.getPaymentJournalEntryId());
        r.setPaymentJournalId(e.getPaymentJournalId());
        r.setAmountPaid(e.getAmountPaid());
        r.setAmountDue(e.getAmountDue());
        r.setPaymentDate(e.getPaymentDate());
        r.setPaymentReference(e.getPaymentReference());
        r.setRowVersion(e.getRowVersion());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        if (e.getExpenseTypeId() != null) {
            try {
                expenseTypeRepository.findById(new ExpenseTypeId(e.getExpenseTypeId()))
                        .ifPresent(t -> r.setExpenseTypeName(t.getName()));
            } catch (RuntimeException ignored) {
                // enrichment is best-effort
            }
        }
        try {
            EmployeeResponse emp = employeeApplicationService.get(e.getEmployeeId());
            r.setEmployeeName(emp.displayName());
            r.setEmployeeImageUrl(emp.imageUrl());
            if (e.getManagerEmployeeId() == null && emp.managerId() != null) {
                r.setManagerEmployeeId(emp.managerId());
            }
            if (emp.managerName() != null) {
                r.setManagerName(emp.managerName());
            }
        } catch (RuntimeException ignored) {
            // enrichment is best-effort
        }
        if (r.getManagerName() == null && r.getManagerEmployeeId() != null) {
            try {
                EmployeeResponse mgr = employeeApplicationService.get(r.getManagerEmployeeId());
                r.setManagerName(mgr.displayName());
            } catch (RuntimeException ignored) {
                // ignore
            }
        }
        return r;
    }
}
