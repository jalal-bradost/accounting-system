package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Hours the employee was present, from HR check-ins (TSH-03 #11). Presence is not work done (D16, BR-TSH-18): this is
 * only ever shown as a hint or used by an explicit "Fill from attendance" action.
 */
public interface AttendanceLookupPort {

    /** Minutes present per calendar date in {@code zone}; a stay past midnight is split. Open check-ins are ignored. */
    Map<LocalDate, Integer> attendedMinutes(CompanyId companyId, UUID employeeId, LocalDate from, LocalDate to, ZoneId zone);
}
