package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Hourly labor cost of an employee on a date (TSH-07), derived from the running contract. */
public interface LaborCostPort {

    record Rate(BigDecimal perHour, String currency) {
    }

    /** Empty when the employee has no running contract or the schedule has no hours. */
    Optional<Rate> hourlyRate(CompanyId companyId, UUID employeeId, LocalDate asOf);
}
