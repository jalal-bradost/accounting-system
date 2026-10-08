package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Expected working time per day from the HR working schedule, minus time off and public holidays
 * (D9, TSH-03 #2). Read only; nothing here is stored as a timesheet entry.
 */
public interface ExpectedHoursPort {

    enum DayType { WORKING, NON_WORKING, HOLIDAY, TIME_OFF, NO_SCHEDULE }

    record ExpectedDay(int minutes, DayType type, String label) {
    }

    /** Every date from {@code from} to {@code to} inclusive is present in the result. */
    Map<LocalDate, ExpectedDay> expected(CompanyId companyId, UUID employeeId, LocalDate from, LocalDate to);
}
