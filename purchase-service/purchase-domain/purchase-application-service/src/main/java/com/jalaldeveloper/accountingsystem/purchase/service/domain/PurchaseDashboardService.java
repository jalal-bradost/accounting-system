package com.jalaldeveloper.accountingsystem.purchase.service.domain;

import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.PurchaseDashboardResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.PurchaseDashboardResponse.KpiMetric;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.PurchaseDashboardResponse.SeriesPoint;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.PurchaseDashboardQueryPort;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.PurchaseDashboardQueryPort.ConfirmedOrderFact;
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
public class PurchaseDashboardService {

    private static final int TOP_LIMIT = 8;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final PurchaseDashboardQueryPort queryPort;

    public PurchaseDashboardService(PurchaseDashboardQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public PurchaseDashboardResponse getDashboard(UUID companyId, LocalDate from, LocalDate to) {
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

        long rfqs = queryPort.countRfqs(companyId, from, to);
        long prevRfqs = queryPort.countRfqs(companyId, prevFrom, prevTo);
        long orders = queryPort.countConfirmedOrders(companyId, from, to);
        long prevOrders = queryPort.countConfirmedOrders(companyId, prevFrom, prevTo);
        BigDecimal spend = queryPort.sumConfirmedSpend(companyId, from, to);
        BigDecimal prevSpend = queryPort.sumConfirmedSpend(companyId, prevFrom, prevTo);
        BigDecimal averageOrder = average(spend, orders);
        BigDecimal prevAverageOrder = average(prevSpend, prevOrders);

        PurchaseDashboardResponse response = new PurchaseDashboardResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setGranularity(daily ? "DAY" : "MONTH");
        response.setRfqs(kpi(BigDecimal.valueOf(rfqs), BigDecimal.valueOf(prevRfqs)));
        response.setOrders(kpi(BigDecimal.valueOf(orders), BigDecimal.valueOf(prevOrders)));
        response.setSpend(kpi(spend, prevSpend));
        response.setAverageOrder(kpi(averageOrder, prevAverageOrder));
        response.setSeries(buildSeries(queryPort.listConfirmedOrderFacts(companyId, from, to), from, to, daily));
        response.setTopRfqs(queryPort.topRfqs(companyId, from, to, TOP_LIMIT));
        response.setTopOrders(queryPort.topConfirmedOrders(companyId, from, to, TOP_LIMIT));
        response.setTopProducts(queryPort.topProducts(companyId, from, to, TOP_LIMIT));
        response.setTopCategories(queryPort.topCategories(companyId, from, to, TOP_LIMIT));
        return response;
    }

    private static List<SeriesPoint> buildSeries(
            List<ConfirmedOrderFact> facts, LocalDate from, LocalDate to, boolean daily) {
        Map<LocalDate, SeriesPoint> buckets = new LinkedHashMap<>();
        if (daily) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                SeriesPoint point = new SeriesPoint();
                point.setPeriodStart(d);
                point.setPeriod(d.format(DAY_LABEL));
                point.setRevenue(BigDecimal.ZERO);
                point.setOrderCount(0);
                buckets.put(d, point);
            }
            for (ConfirmedOrderFact fact : facts) {
                SeriesPoint point = buckets.get(fact.confirmedDate());
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
                SeriesPoint point = new SeriesPoint();
                point.setPeriodStart(m);
                point.setPeriod(m.format(MONTH_LABEL));
                point.setRevenue(BigDecimal.ZERO);
                point.setOrderCount(0);
                buckets.put(m, point);
            }
            for (ConfirmedOrderFact fact : facts) {
                LocalDate month = fact.confirmedDate().withDayOfMonth(1);
                SeriesPoint point = buckets.get(month);
                if (point == null) {
                    continue;
                }
                point.setRevenue(point.getRevenue().add(fact.companyAmount()));
                point.setOrderCount(point.getOrderCount() + 1);
            }
        }
        return new ArrayList<>(buckets.values());
    }

    private static KpiMetric kpi(BigDecimal value, BigDecimal previousValue) {
        KpiMetric metric = new KpiMetric();
        metric.setValue(scale(value));
        metric.setPreviousValue(scale(previousValue));
        metric.setChangePercent(changePercent(value, previousValue));
        return metric;
    }

    private static BigDecimal average(BigDecimal spend, long orders) {
        if (orders <= 0) {
            return BigDecimal.ZERO;
        }
        return spend.divide(BigDecimal.valueOf(orders), 4, RoundingMode.HALF_UP);
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
