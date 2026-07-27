package com.jalaldeveloper.accountingsystem.hr.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.LeaveRequestId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.TimeOffTypeId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LeaveRequest {

    public static final String STATE_CONFIRM = "confirm";
    public static final String STATE_VALIDATE = "validate";
    public static final String STATE_REFUSE = "refuse";
    public static final String STATE_CANCEL = "cancel";

    private final LeaveRequestId id;
    private final CompanyId companyId;
    private final EmployeeId employeeId;
    private final TimeOffTypeId timeOffTypeId;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private final BigDecimal numberOfDays;
    private final String state;
    private final String description;

    private LeaveRequest(Builder b) {
        this.id = b.id;
        this.companyId = b.companyId;
        this.employeeId = b.employeeId;
        this.timeOffTypeId = b.timeOffTypeId;
        this.dateFrom = b.dateFrom;
        this.dateTo = b.dateTo;
        this.numberOfDays = b.numberOfDays;
        this.state = b.state;
        this.description = b.description;
    }

    public void validate() {
        if (companyId == null) throw new HrDomainException("companyId required");
        if (employeeId == null) throw new HrDomainException("employeeId required");
        if (timeOffTypeId == null) throw new HrDomainException("timeOffTypeId required");
        if (dateFrom == null || dateTo == null) throw new HrDomainException("dates required");
        if (dateTo.isBefore(dateFrom)) throw new HrDomainException("dateTo must be on or after dateFrom");
        if (numberOfDays == null || numberOfDays.signum() <= 0) {
            throw new HrDomainException("numberOfDays must be positive");
        }
    }

    public static BigDecimal computeDays(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return BigDecimal.valueOf(days);
    }

    public LeaveRequest approve() {
        if (!STATE_CONFIRM.equals(state)) {
            throw new HrDomainException("Only requests awaiting approval can be validated");
        }
        return toBuilder().state(STATE_VALIDATE).build();
    }

    public LeaveRequest refuse() {
        if (STATE_REFUSE.equals(state)) {
            throw new HrDomainException("Request already refused");
        }
        if (STATE_CANCEL.equals(state)) {
            throw new HrDomainException("Cancelled request cannot be refused");
        }
        return toBuilder().state(STATE_REFUSE).build();
    }

    public LeaveRequest cancel() {
        if (STATE_VALIDATE.equals(state)) {
            throw new HrDomainException("Validated request cannot be cancelled");
        }
        return toBuilder().state(STATE_CANCEL).build();
    }

    public boolean countsAsTaken() {
        return STATE_VALIDATE.equals(state);
    }

    private Builder toBuilder() {
        return builder()
                .id(id)
                .companyId(companyId)
                .employeeId(employeeId)
                .timeOffTypeId(timeOffTypeId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .numberOfDays(numberOfDays)
                .state(state)
                .description(description);
    }

    public LeaveRequestId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public EmployeeId getEmployeeId() { return employeeId; }
    public TimeOffTypeId getTimeOffTypeId() { return timeOffTypeId; }
    public LocalDate getDateFrom() { return dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public BigDecimal getNumberOfDays() { return numberOfDays; }
    public String getState() { return state; }
    public String getDescription() { return description; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private LeaveRequestId id;
        private CompanyId companyId;
        private EmployeeId employeeId;
        private TimeOffTypeId timeOffTypeId;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private BigDecimal numberOfDays;
        private String state = STATE_CONFIRM;
        private String description;

        public Builder id(LeaveRequestId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder employeeId(EmployeeId v) { this.employeeId = v; return this; }
        public Builder timeOffTypeId(TimeOffTypeId v) { this.timeOffTypeId = v; return this; }
        public Builder dateFrom(LocalDate v) { this.dateFrom = v; return this; }
        public Builder dateTo(LocalDate v) { this.dateTo = v; return this; }
        public Builder numberOfDays(BigDecimal v) { this.numberOfDays = v; return this; }
        public Builder state(String v) { this.state = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public LeaveRequest build() { return new LeaveRequest(this); }
    }
}
