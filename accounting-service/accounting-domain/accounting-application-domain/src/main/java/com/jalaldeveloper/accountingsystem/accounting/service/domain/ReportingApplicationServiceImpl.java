package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.GeneralLedgerRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.BalanceSheetReport;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.GeneralLedgerLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.ProfitAndLossReport;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportingApplicationServiceImpl implements ReportingApplicationService {

    private static final List<AccountType> PERMANENT_ACCOUNT_TYPES = AccountType.typesIn(
            AccountType.Category.ASSET, AccountType.Category.LIABILITY, AccountType.Category.EQUITY);

    private final AccountBalanceRepository accountBalanceRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final AccountApplicationService accountApplicationService;

    public ReportingApplicationServiceImpl(AccountBalanceRepository accountBalanceRepository,
                                             GeneralLedgerRepository generalLedgerRepository,
                                             AccountApplicationService accountApplicationService) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.generalLedgerRepository = generalLedgerRepository;
        this.accountApplicationService = accountApplicationService;
    }

    @Override
    public List<AccountBalanceRepository.AccountBalanceLine> getTrialBalance(UUID companyId, LocalDate from, LocalDate to) {
        return accountBalanceRepository.getTrialBalance(new CompanyId(companyId), from, to);
    }

    @Override
    public BalanceSheetReport getBalanceSheet(UUID companyId, LocalDate asOf) {
        List<AccountBalanceRepository.AccountBalanceLine> raw = accountBalanceRepository.getBalancesUpTo(
                new CompanyId(companyId), asOf, PERMANENT_ACCOUNT_TYPES);
        Map<UUID, AccountResponse> accountMap = accountMap(companyId);
        List<BalanceSheetReport.AccountLine> assets = buildBalanceSheetSection(raw, accountMap, AccountType.Category.ASSET, b -> b);
        List<BalanceSheetReport.AccountLine> liabilities = buildBalanceSheetSection(
                raw, accountMap, AccountType.Category.LIABILITY, BigDecimal::negate);
        List<BalanceSheetReport.AccountLine> equity = buildBalanceSheetSection(
                raw, accountMap, AccountType.Category.EQUITY, BigDecimal::negate);
        BigDecimal totalAssets = sumAmounts(assets);
        BigDecimal totalLiabilities = sumAmounts(liabilities);
        BigDecimal totalEquity = sumAmounts(equity);
        return new BalanceSheetReport(asOf, assets, liabilities, equity, totalAssets, totalLiabilities, totalEquity);
    }

    @Override
    public ProfitAndLossReport getProfitAndLoss(UUID companyId, LocalDate from, LocalDate to) {
        List<AccountBalanceRepository.AccountBalanceLine> raw = getTrialBalance(companyId, from, to);
        Map<UUID, AccountResponse> accountMap = accountMap(companyId);
        List<ProfitAndLossReport.AccountLine> revenue = buildProfitAndLossSection(
                raw, accountMap, AccountType.Category.INCOME, BigDecimal::negate);
        List<ProfitAndLossReport.AccountLine> expenses = buildProfitAndLossSection(
                raw, accountMap, AccountType.Category.EXPENSE, b -> b);
        BigDecimal totalRevenue = sumProfitAndLossAmounts(revenue);
        BigDecimal totalExpenses = sumProfitAndLossAmounts(expenses);
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        return new ProfitAndLossReport(from, to, revenue, expenses, totalRevenue, totalExpenses, netIncome);
    }

    @Override
    public List<GeneralLedgerLine> getGeneralLedger(UUID companyId, LocalDate from, LocalDate to, UUID accountId) {
        List<GeneralLedgerRepository.GeneralLedgerRawLine> raw = generalLedgerRepository.listPostedLines(
                new CompanyId(companyId), from, to, accountId);
        List<GeneralLedgerLine> result = new ArrayList<>();
        UUID currentAccount = null;
        BigDecimal running = BigDecimal.ZERO;
        for (GeneralLedgerRepository.GeneralLedgerRawLine line : raw) {
            if (!Objects.equals(currentAccount, line.accountId())) {
                currentAccount = line.accountId();
                running = BigDecimal.ZERO;
            }
            running = running.add(line.debit()).subtract(line.credit());
            result.add(new GeneralLedgerLine(
                    line.accountId(),
                    line.journalEntryId(),
                    line.entryDate(),
                    line.journalCode(),
                    line.sequenceNumber(),
                    line.label(),
                    line.debit(),
                    line.credit(),
                    running));
        }
        return result;
    }

    private Map<UUID, AccountResponse> accountMap(UUID companyId) {
        return accountApplicationService.listAccountsByCompany(companyId).stream()
                .collect(Collectors.toMap(AccountResponse::getId, Function.identity()));
    }

    private static List<BalanceSheetReport.AccountLine> buildBalanceSheetSection(
            List<AccountBalanceRepository.AccountBalanceLine> raw,
            Map<UUID, AccountResponse> accountMap,
            AccountType.Category sectionCategory,
            Function<BigDecimal, BigDecimal> toStatementAmount) {
        return raw.stream()
                .filter(l -> {
                    AccountResponse a = accountMap.get(l.accountId());
                    return a != null && a.getAccountType().getCategory() == sectionCategory;
                })
                .map(l -> new BalanceSheetReport.AccountLine(
                        l.accountId(), toStatementAmount.apply(l.balance())))
                .sorted(Comparator.comparing(line -> accountMap.get(line.accountId()).getCode()))
                .collect(Collectors.toList());
    }

    private static List<ProfitAndLossReport.AccountLine> buildProfitAndLossSection(
            List<AccountBalanceRepository.AccountBalanceLine> raw,
            Map<UUID, AccountResponse> accountMap,
            AccountType.Category sectionCategory,
            Function<BigDecimal, BigDecimal> toStatementAmount) {
        return raw.stream()
                .filter(l -> {
                    AccountResponse a = accountMap.get(l.accountId());
                    return a != null && a.getAccountType().getCategory() == sectionCategory;
                })
                .map(l -> new ProfitAndLossReport.AccountLine(
                        l.accountId(), toStatementAmount.apply(l.balance())))
                .sorted(Comparator.comparing(line -> accountMap.get(line.accountId()).getCode()))
                .collect(Collectors.toList());
    }

    private static BigDecimal sumAmounts(List<BalanceSheetReport.AccountLine> lines) {
        return lines.stream()
                .map(BalanceSheetReport.AccountLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumProfitAndLossAmounts(List<ProfitAndLossReport.AccountLine> lines) {
        return lines.stream()
                .map(ProfitAndLossReport.AccountLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
