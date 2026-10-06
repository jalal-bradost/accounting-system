package com.bradox.erp.assistant.dates;

import com.bradox.erp.assistant.config.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * Resolves relative date phrases on the server. The model must not invent dates silently.
 */
@Component
public class RelativeDateResolver {

    private final AiProperties properties;
    private final Clock clock;

    @Autowired
    public RelativeDateResolver(AiProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    RelativeDateResolver(AiProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public DateRange resolve(String period, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return validate(new DateRange(from, to));
        }
        // Small models often omit period; default to THIS_MONTH so tools still answer.
        if (period == null || period.isBlank()) {
            period = "THIS_MONTH";
        }
        String key = period.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        LocalDate today = LocalDate.now(clock);
        DateRange range = switch (key) {
            case "TODAY" -> new DateRange(today, today);
            case "YESTERDAY" -> {
                LocalDate y = today.minusDays(1);
                yield new DateRange(y, y);
            }
            case "THIS_WEEK" -> {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                yield new DateRange(start, today);
            }
            case "LAST_WEEK" -> {
                LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate start = thisMonday.minusWeeks(1);
                yield new DateRange(start, start.plusDays(6));
            }
            case "THIS_MONTH" -> new DateRange(today.withDayOfMonth(1), today);
            case "LAST_MONTH" -> {
                YearMonth prev = YearMonth.from(today).minusMonths(1);
                yield new DateRange(prev.atDay(1), prev.atEndOfMonth());
            }
            case "THIS_QUARTER" -> {
                int q = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate start = LocalDate.of(today.getYear(), q, 1);
                yield new DateRange(start, today);
            }
            case "LAST_QUARTER" -> {
                LocalDate firstOfThisQ = LocalDate.of(today.getYear(), ((today.getMonthValue() - 1) / 3) * 3 + 1, 1);
                LocalDate end = firstOfThisQ.minusDays(1);
                LocalDate start = end.withDayOfMonth(1).minusMonths(2).withDayOfMonth(1);
                // align to quarter start
                int qMonth = ((end.getMonthValue() - 1) / 3) * 3 + 1;
                start = LocalDate.of(end.getYear(), qMonth, 1);
                yield new DateRange(start, end);
            }
            case "THIS_YEAR" -> new DateRange(LocalDate.of(today.getYear(), 1, 1), today);
            case "LAST_YEAR" -> {
                int y = today.getYear() - 1;
                yield new DateRange(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31));
            }
            case "LAST_30_DAYS" -> new DateRange(today.minusDays(29), today);
            case "LAST_90_DAYS" -> new DateRange(today.minusDays(89), today);
            default -> throw new IllegalArgumentException(
                    "Unknown period '" + period + "'. Use THIS_MONTH, LAST_MONTH, LAST_30_DAYS, THIS_YEAR, or explicit from/to.");
        };
        return validate(range);
    }

    private DateRange validate(DateRange range) {
        if (range.to().isBefore(range.from())) {
            throw new IllegalArgumentException("Date range end must not be before start.");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
        int max = Math.max(1, properties.getMaxDateRangeDays());
        if (days > max) {
            throw new IllegalArgumentException("Date range exceeds maximum of " + max + " days.");
        }
        return range;
    }

    public record DateRange(LocalDate from, LocalDate to) {}
}
