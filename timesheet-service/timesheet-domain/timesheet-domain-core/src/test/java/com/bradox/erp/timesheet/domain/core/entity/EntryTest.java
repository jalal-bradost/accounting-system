package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntryTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    private static Entry entry(int minutes) {
        return Entry.create(new EntryId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()), UUID.randomUUID(),
                LocalDate.of(2026, 10, 8), minutes, new ProjectId(UUID.randomUUID()), null, "  work ", true,
                new WeekId(UUID.randomUUID()), EntrySource.MANUAL, NOW, "t");
    }

    @Test
    void minutesBoundaries() {
        assertEquals(1, entry(1).getMinutes());
        assertEquals(1440, entry(1440).getMinutes());
        assertThrows(TimesheetDomainException.class, () -> entry(0));
        assertThrows(TimesheetDomainException.class, () -> entry(1441));
    }

    @Test
    void descriptionIsTrimmedAndBlankBecomesNull() {
        assertEquals("work", entry(60).getDescription());
        Entry e = entry(60);
        e.changeMinutes(30, NOW.plusSeconds(1));
        assertEquals(30, e.getMinutes());
        assertThrows(TimesheetDomainException.class, () -> e.changeMinutes(0, NOW));
    }
}
