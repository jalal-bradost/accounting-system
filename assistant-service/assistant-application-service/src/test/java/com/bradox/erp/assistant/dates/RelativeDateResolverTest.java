package com.bradox.erp.assistant.dates;

import com.bradox.erp.assistant.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelativeDateResolverTest {

    private RelativeDateResolver resolver;

    @BeforeEach
    void setUp() {
        AiProperties props = new AiProperties();
        props.setMaxDateRangeDays(366);
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC);
        resolver = new RelativeDateResolver(props, clock);
    }

    private static RelativeDateResolver resolverOn(String isoDay) {
        AiProperties props = new AiProperties();
        props.setMaxDateRangeDays(366);
        Clock clock = Clock.fixed(Instant.parse(isoDay + "T12:00:00Z"), ZoneOffset.UTC);
        return new RelativeDateResolver(props, clock);
    }

    @Test
    void weekRunsSaturdayToFriday() {
        // Wednesday 7 Oct 2026: this week began on Saturday 3 Oct.
        RelativeDateResolver.DateRange thisWeek = resolverOn("2026-10-07").resolve("THIS_WEEK", null, null);
        assertThat(thisWeek.from()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(thisWeek.to()).isEqualTo(LocalDate.of(2026, 10, 7));

        RelativeDateResolver.DateRange lastWeek = resolverOn("2026-10-07").resolve("LAST_WEEK", null, null);
        assertThat(lastWeek.from()).isEqualTo(LocalDate.of(2026, 9, 26)); // Saturday
        assertThat(lastWeek.to()).isEqualTo(LocalDate.of(2026, 10, 2));   // Friday
    }

    @Test
    void weekBoundariesOnSaturdayAndFriday() {
        // On a Saturday a new week starts: this week is only today, last week ended yesterday (Friday).
        RelativeDateResolver saturday = resolverOn("2026-10-10");
        assertThat(saturday.resolve("THIS_WEEK", null, null).from()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(saturday.resolve("LAST_WEEK", null, null).from()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(saturday.resolve("LAST_WEEK", null, null).to()).isEqualTo(LocalDate.of(2026, 10, 9));

        // On a Friday the week is complete: seven days.
        RelativeDateResolver.DateRange friday = resolverOn("2026-10-09").resolve("THIS_WEEK", null, null);
        assertThat(friday.from()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(friday.to()).isEqualTo(LocalDate.of(2026, 10, 9));
    }

    @Test
    void resolvesLastMonth() {
        RelativeDateResolver.DateRange range = resolver.resolve("LAST_MONTH", null, null);
        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void resolvesThisMonthThroughToday() {
        RelativeDateResolver.DateRange range = resolver.resolve("THIS_MONTH", null, null);
        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    void acceptsExplicitRange() {
        RelativeDateResolver.DateRange range = resolver.resolve(
                null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void rejectsExcessiveRange() {
        assertThatThrownBy(() -> resolver.resolve(
                null, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 9, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void rejectsUnknownPeriod() {
        assertThatThrownBy(() -> resolver.resolve("next_tuesday", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsBlankPeriodToThisMonth() {
        RelativeDateResolver.DateRange range = resolver.resolve(null, null, null);
        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 9, 15));
    }
}
