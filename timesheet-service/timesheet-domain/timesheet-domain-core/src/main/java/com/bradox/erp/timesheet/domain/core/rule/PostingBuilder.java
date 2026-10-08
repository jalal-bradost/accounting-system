package com.bradox.erp.timesheet.domain.core.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the debit lines of one employee-week posting (TSH-10, BR-TSH-17): one line per project, each rounded to the
 * currency precision, with any rounding difference put on the largest line so debits equal the credit exactly.
 */
public final class PostingBuilder {

    public record CostLine(UUID projectId, UUID debitAccountId, int minutes, BigDecimal amount) {
    }

    public record Line(UUID projectId, UUID debitAccountId, BigDecimal amount, int minutes) {
    }

    public record Result(List<Line> lines, BigDecimal total) {
        public boolean isEmpty() {
            return lines.isEmpty();
        }
    }

    private PostingBuilder() {
    }

    public static Result build(List<CostLine> costs, int scale) {
        Map<UUID, BigDecimal> raw = new LinkedHashMap<>();
        Map<UUID, Integer> minutes = new LinkedHashMap<>();
        Map<UUID, UUID> accounts = new LinkedHashMap<>();
        BigDecimal rawTotal = BigDecimal.ZERO;
        for (CostLine c : costs) {
            if (c.amount() == null || c.amount().signum() <= 0) {
                continue;
            }
            raw.merge(c.projectId(), c.amount(), BigDecimal::add);
            minutes.merge(c.projectId(), c.minutes(), Integer::sum);
            accounts.putIfAbsent(c.projectId(), c.debitAccountId());
            rawTotal = rawTotal.add(c.amount());
        }
        if (raw.isEmpty()) {
            return new Result(List.of(), BigDecimal.ZERO.setScale(scale));
        }
        BigDecimal total = rawTotal.setScale(scale, RoundingMode.HALF_EVEN);
        List<Line> lines = new ArrayList<>();
        BigDecimal roundedSum = BigDecimal.ZERO;
        for (var e : raw.entrySet()) {
            BigDecimal rounded = e.getValue().setScale(scale, RoundingMode.HALF_EVEN);
            roundedSum = roundedSum.add(rounded);
            lines.add(new Line(e.getKey(), accounts.get(e.getKey()), rounded, minutes.get(e.getKey())));
        }
        BigDecimal diff = total.subtract(roundedSum);
        if (diff.signum() != 0) {
            int largest = 0;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).amount().compareTo(lines.get(largest).amount()) > 0) {
                    largest = i;
                }
            }
            Line l = lines.get(largest);
            lines.set(largest, new Line(l.projectId(), l.debitAccountId(), l.amount().add(diff), l.minutes()));
        }
        lines.removeIf(l -> l.amount().signum() <= 0);
        lines.sort(Comparator.comparing(l -> l.projectId().toString()));
        return new Result(lines, total);
    }
}
