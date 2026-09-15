package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.dashboard.AccountingDashboardResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.dashboard.AccountingDashboardResponse.KpiMetric;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.dashboard.AccountingDashboardResponse.SeriesPoint;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.AccountingDashboardQueryPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.AccountingDashboardQueryPort.DocumentFact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountingDashboardService {

    private static final int TOP_LIMIT = 8;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final AccountingDashboardQueryPort queryPort;

    public AccountingDashboardService(AccountingDashboardQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public AccountingDashboardResponse getDashboard(UUID companyId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must be on or after from");
        }

        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);
        boolean daily = days <= 31;

        long invoices = queryPort.countPostedInvoices(companyId, from, to);
        long prevInvoices = queryPort.countPostedInvoices(companyId, prevFrom, prevTo);
        long bills = queryPort.countPostedBills(companyId, from, to);
        long prevBills = queryPort.countPostedBills(companyId, prevFrom, prevTo);
        BigDecimal income = queryPort.sumPostedIncome(companyId, from, to);
        BigDecimal prevIncome = queryPort.sumPostedIncome(companyId, prevFrom, prevTo);
        BigDecimal spend = queryPort.sumPostedSpend(companyId, from, to);
        BigDecimal prevSpend = queryPort.sumPostedSpend(companyId, prevFrom, prevTo);

        AccountingDashboardResponse response = new AccountingDashboardResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setGranularity(daily ? "DAY" : "MONTH");
        response.setInvoices(kpi(BigDecimal.valueOf(invoices), BigDecimal.valueOf(prevInvoices)));
        response.setBills(kpi(BigDecimal.valueOf(bills), BigDecimal.valueOf(prevBills)));
        response.setIncome(kpi(income, prevIncome));
        response.setSpend(kpi(spend, prevSpend));
        response.setSeries(buildSeries(
                queryPort.listInvoiceFacts(companyId, from, to),
                queryPort.listBillFacts(companyId, from, to),
                from,
                to,
                daily));
        response.setTopInvoices(queryPort.topInvoices(companyId, from, to, TOP_LIMIT));
        response.setTopBills(queryPort.topBills(companyId, from, to, TOP_LIMIT));
        return response;
    }

    private static List<SeriesPoint> buildSeries(
            List<DocumentFact> invoices,
            List<DocumentFact> bills,
            LocalDate from,
            LocalDate to,
            boolean daily) {
        Map<LocalDate, SeriesPoint> buckets = new LinkedHashMap<>();
        if (daily) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                buckets.put(d, emptyPoint(d, d.format(DAY_LABEL)));
            }
            for (DocumentFact fact : invoices) {
                SeriesPoint point = buckets.get(fact.documentDate());
                if (point == null) {
                    continue;
                }
                point.setRevenue(point.getRevenue().add(fact.companyAmount()));
                point.setOrderCount(point.getOrderCount() + 1);
            }
            for (DocumentFact fact : bills) {
                SeriesPoint point = buckets.get(fact.documentDate());
                if (point == null) {
                    continue;
                }
                point.setSpend(point.getSpend().add(fact.companyAmount()));
            }
        } else {
            LocalDate cursor = from.withDayOfMonth(1);
            LocalDate endMonth = to.withDayOfMonth(1);
            for (LocalDate m = cursor; !m.isAfter(endMonth); m = m.plusMonths(1)) {
                buckets.put(m, emptyPoint(m, m.format(MONTH_LABEL)));
            }
            for (DocumentFact fact : invoices) {
                LocalDate month = fact.documentDate().withDayOfMonth(1);
                SeriesPoint point = buckets.get(month);
                if (point == null) {
                    continue;
                }
                point.setRevenue(point.getRevenue().add(fact.companyAmount()));
                point.setOrderCount(point.getOrderCount() + 1);
            }
            for (DocumentFact fact : bills) {
                LocalDate month = fact.documentDate().withDayOfMonth(1);
                SeriesPoint point = buckets.get(month);
                if (point == null) {
                    continue;
                }
                point.setSpend(point.getSpend().add(fact.companyAmount()));
            }
        }
        return new ArrayList<>(buckets.values());
    }

    private static SeriesPoint emptyPoint(LocalDate start, String label) {
        SeriesPoint point = new SeriesPoint();
        point.setPeriodStart(start);
        point.setPeriod(label);
        point.setRevenue(BigDecimal.ZERO);
        point.setSpend(BigDecimal.ZERO);
        point.setOrderCount(0);
        return point;
    }

    private static KpiMetric kpi(BigDecimal value, BigDecimal previousValue) {
        KpiMetric metric = new KpiMetric();
        metric.setValue(scale(value));
        metric.setPreviousValue(scale(previousValue));
        metric.setChangePercent(changePercent(value, previousValue));
        return metric;
    }

    private static BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
