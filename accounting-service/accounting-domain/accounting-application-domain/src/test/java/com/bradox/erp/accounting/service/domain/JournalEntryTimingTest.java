package com.bradox.erp.accounting.service.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryTimingTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Baghdad");
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-21T14:36:45Z"), ZONE);

    @Test
    void ofBusinessDate_stampsCurrentTimeOnBusinessDay() {
        LocalDateTime result = JournalEntryTiming.ofBusinessDate(LocalDate.of(2026, 9, 21), FIXED);
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.now(FIXED));
        assertThat(result.toLocalTime()).isNotEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    void ensureTimed_replacesMidnightWithClockTime() {
        LocalDateTime midnight = LocalDate.of(2026, 9, 21).atStartOfDay();
        LocalDateTime result = JournalEntryTiming.ensureTimed(midnight, FIXED);
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.now(FIXED));
    }

    @Test
    void ensureTimed_keepsNonMidnightTime() {
        LocalDateTime timed = LocalDateTime.of(2026, 9, 21, 17, 36, 0);
        assertThat(JournalEntryTiming.ensureTimed(timed, FIXED)).isEqualTo(timed);
    }
}
