package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.GeneralLedgerRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.PartnerLedgerRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.BalanceSheetReport;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.GeneralLedgerLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.PartnerLedgerMovementLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.PartnerLedgerReport;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.PartnerLedgerSummaryRow;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.ProfitAndLossReport;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportingApplicationServiceImpl implements ReportingApplicationService {

    private static final List<AccountType> PERMANENT_ACCOUNT_TYPES = AccountType.typesIn(
            AccountType.Category.ASSET, AccountType.Category.LIABILITY, AccountType.Category.EQUITY);

    private static final List<AccountType> TRADE_ACCOUNT_TYPES = List.of(AccountType.RECEIVABLE, AccountType.PAYABLE);

    private final AccountBalanceRepository accountBalanceRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final PartnerLedgerRepository partnerLedgerRepository;
    private final PartnerApplicationService partnerApplicationService;
    private final AccountApplicationService accountApplicationService;

    public ReportingApplicationServiceImpl(AccountBalanceRepository accountBalanceRepository,
                                           GeneralLedgerRepository generalLedgerRepository,
                                           PartnerLedgerRepository partnerLedgerRepository,
                                           PartnerApplicationService partnerApplicationService,
                                           AccountApplicationService accountApplicationService) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.generalLedgerRepository = generalLedgerRepository;
        this.partnerLedgerRepository = partnerLedgerRepository;
        this.partnerApplicationService = partnerApplicationService;
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

    @Override
    public PartnerLedgerReport getPartnerLedger(UUID companyId, LocalDate from, LocalDate to, UUID partnerId) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Partner ledger end date must be on or after start date");
        }
        CompanyId cid = new CompanyId(companyId);
        boolean drafts = partnerLedgerRepository.hasDraftJournalEntriesThrough(cid, to);
        Map<UUID, BigDecimal> opening = partnerLedgerRepository.sumOpeningBalanceByPartnerBefore(
                cid, from, TRADE_ACCOUNT_TYPES);
        Map<UUID, PartnerLedgerRepository.PeriodDebitCredit> periodAgg =
                partnerLedgerRepository.sumPeriodDebitCreditByPartner(cid, from, to, TRADE_ACCOUNT_TYPES);

        Set<UUID> partnerIds = new HashSet<>();
        partnerIds.addAll(opening.keySet());
        partnerIds.addAll(periodAgg.keySet());
        if (partnerId != null) {
            partnerIds.add(partnerId);
        }

        List<PartnerLedgerSummaryRow> summaries = new ArrayList<>();
        for (UUID pid : partnerIds) {
            BigDecimal o = scaleMoney(opening.getOrDefault(pid, BigDecimal.ZERO));
            PartnerLedgerRepository.PeriodDebitCredit pdc = periodAgg.getOrDefault(
                    pid, new PartnerLedgerRepository.PeriodDebitCredit(BigDecimal.ZERO, BigDecimal.ZERO));
            BigDecimal pd = scaleMoney(pdc.debit());
            BigDecimal pc = scaleMoney(pdc.credit());
            BigDecimal closing = o.add(pd).subtract(pc);
            if (partnerId == null && o.signum() == 0 && pd.signum() == 0 && pc.signum() == 0) {
                continue;
            }
            summaries.add(new PartnerLedgerSummaryRow(
                    pid, resolvePartnerDisplayName(pid), o, pd, pc, scaleMoney(closing)));
        }
        summaries.sort(Comparator.comparing(
                PartnerLedgerSummaryRow::partnerDisplayName, String.CASE_INSENSITIVE_ORDER));

        List<PartnerLedgerMovementLine> lines = List.of();
        if (partnerId != null) {
            var pr = partnerApplicationService.getPartner(partnerId);
            if (!pr.getCompanyId().equals(companyId)) {
                throw new IllegalArgumentException("Partner belongs to another company");
            }
            BigDecimal running = scaleMoney(opening.getOrDefault(partnerId, BigDecimal.ZERO));
            List<PartnerLedgerRepository.PartnerLedgerMovementRaw> raw =
                    partnerLedgerRepository.listMovementsInPeriod(cid, from, to, partnerId, TRADE_ACCOUNT_TYPES);
            lines = new ArrayList<>(raw.size());
            for (PartnerLedgerRepository.PartnerLedgerMovementRaw r : raw) {
                running = running.add(scaleMoney(r.debit())).subtract(scaleMoney(r.credit()));
                lines.add(new PartnerLedgerMovementLine(
                        r.entryDate(),
                        r.journalEntryId(),
                        r.journalCode(),
                        r.sequenceNumber(),
                        r.accountCode(),
                        r.accountName(),
                        r.label(),
                        scaleMoney(r.debit()),
                        scaleMoney(r.credit()),
                        scaleMoney(running),
                        r.reconciliationId()));
            }
            Optional<PartnerLedgerSummaryRow> one = summaries.stream()
                    .filter(s -> s.partnerId().equals(partnerId))
                    .findFirst();
            if (one.isEmpty()) {
                BigDecimal o0 = scaleMoney(opening.getOrDefault(partnerId, BigDecimal.ZERO));
                PartnerLedgerRepository.PeriodDebitCredit p0 = periodAgg.getOrDefault(
                        partnerId, new PartnerLedgerRepository.PeriodDebitCredit(BigDecimal.ZERO, BigDecimal.ZERO));
                BigDecimal pd0 = scaleMoney(p0.debit());
                BigDecimal pc0 = scaleMoney(p0.credit());
                summaries = List.of(new PartnerLedgerSummaryRow(
                        partnerId, resolvePartnerDisplayName(partnerId), o0, pd0, pc0, o0.add(pd0).subtract(pc0)));
            } else {
                summaries = List.of(one.get());
            }
        }

        return new PartnerLedgerReport(from, to, partnerId, drafts, summaries, lines);
    }

    private String resolvePartnerDisplayName(UUID partnerId) {
        try {
            return partnerApplicationService.getPartner(partnerId).getDisplayName();
        } catch (RuntimeException ignored) {
            return "Unknown partner";
        }
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        return v.setScale(4, RoundingMode.HALF_UP);
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
