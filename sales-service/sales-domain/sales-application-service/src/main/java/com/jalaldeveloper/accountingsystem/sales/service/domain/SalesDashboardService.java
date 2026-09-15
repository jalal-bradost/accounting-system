package com.jalaldeveloper.accountingsystem.sales.service.domain;

import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesDashboardResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesDashboardResponse.SalesKpiMetric;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesDashboardResponse.SalesSeriesPoint;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.SalesDashboardQueryPort;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.SalesDashboardQueryPort.ConfirmedOrderFact;
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
public class SalesDashboardService {

    private static final int TOP_LIMIT = 8;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final SalesDashboardQueryPort queryPort;

    public SalesDashboardService(SalesDashboardQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SalesDashboardResponse getDashboard(UUID companyId, LocalDate from, LocalDate to) {
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

        long quotations = queryPort.countQuotations(companyId, from, to);
        long prevQuotations = queryPort.countQuotations(companyId, prevFrom, prevTo);
        long orders = queryPort.countConfirmedOrders(companyId, from, to);
        long prevOrders = queryPort.countConfirmedOrders(companyId, prevFrom, prevTo);
        BigDecimal revenue = queryPort.sumConfirmedRevenue(companyId, from, to);
        BigDecimal prevRevenue = queryPort.sumConfirmedRevenue(companyId, prevFrom, prevTo);
        BigDecimal averageOrder = average(revenue, orders);
        BigDecimal prevAverageOrder = average(prevRevenue, prevOrders);

        SalesDashboardResponse response = new SalesDashboardResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setGranularity(daily ? "DAY" : "MONTH");
        response.setQuotations(kpi(BigDecimal.valueOf(quotations), BigDecimal.valueOf(prevQuotations)));
        response.setOrders(kpi(BigDecimal.valueOf(orders), BigDecimal.valueOf(prevOrders)));
        response.setRevenue(kpi(revenue, prevRevenue));
        response.setAverageOrder(kpi(averageOrder, prevAverageOrder));
        response.setSeries(buildSeries(queryPort.listConfirmedOrderFacts(companyId, from, to), from, to, daily));
        response.setTopQuotations(queryPort.topQuotations(companyId, from, to, TOP_LIMIT));
        response.setTopOrders(queryPort.topConfirmedOrders(companyId, from, to, TOP_LIMIT));
        response.setTopProducts(queryPort.topProducts(companyId, from, to, TOP_LIMIT));
        response.setTopCategories(queryPort.topCategories(companyId, from, to, TOP_LIMIT));
        response.setChannels(queryPort.channelSplit(companyId, from, to));
        response.setPaymentMethods(queryPort.paymentMethodSplit(companyId, from, to));
        return response;
    }

    private static List<SalesSeriesPoint> buildSeries(
            List<ConfirmedOrderFact> facts, LocalDate from, LocalDate to, boolean daily) {
        Map<LocalDate, SalesSeriesPoint> buckets = new LinkedHashMap<>();
        if (daily) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                SalesSeriesPoint point = new SalesSeriesPoint();
                point.setPeriodStart(d);
                point.setPeriod(d.format(DAY_LABEL));
                point.setRevenue(BigDecimal.ZERO);
                point.setOrderCount(0);
                buckets.put(d, point);
            }
            for (ConfirmedOrderFact fact : facts) {
                SalesSeriesPoint point = buckets.get(fact.confirmedDate());
                if (point == null) {
                    continue;
                }
                point.setRevenue(point.getRevenue().add(fact.companyAmount()));
                point.setOrderCount(point.getOrderCount() + 1);
            }
        } else {
            LocalDate cursor = from.withDayOfMonth(1);
            LocalDate endMonth = to.withDayOfMonth(1);
            for (LocalDate m = cursor; !m.isAfter(endMonth); m = m.plusMonths(1)) {
                SalesSeriesPoint point = new SalesSeriesPoint();
                point.setPeriodStart(m);
                point.setPeriod(m.format(MONTH_LABEL));
                point.setRevenue(BigDecimal.ZERO);
                point.setOrderCount(0);
                buckets.put(m, point);
            }
            for (ConfirmedOrderFact fact : facts) {
                LocalDate month = fact.confirmedDate().withDayOfMonth(1);
                SalesSeriesPoint point = buckets.get(month);
                if (point == null) {
                    continue;
                }
                point.setRevenue(point.getRevenue().add(fact.companyAmount()));
                point.setOrderCount(point.getOrderCount() + 1);
            }
        }
        return new ArrayList<>(buckets.values());
    }

    private static SalesKpiMetric kpi(BigDecimal value, BigDecimal previousValue) {
        SalesKpiMetric metric = new SalesKpiMetric();
        metric.setValue(scale(value));
        metric.setPreviousValue(scale(previousValue));
        metric.setChangePercent(changePercent(value, previousValue));
        return metric;
    }

    private static BigDecimal average(BigDecimal revenue, long orders) {
        if (orders <= 0) {
            return BigDecimal.ZERO;
        }
        return revenue.divide(BigDecimal.valueOf(orders), 4, RoundingMode.HALF_UP);
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
