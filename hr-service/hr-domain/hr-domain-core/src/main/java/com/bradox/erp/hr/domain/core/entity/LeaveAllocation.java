package com.bradox.erp.hr.domain.core.entity;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.hr.domain.core.exception.HrDomainException;
import com.bradox.erp.hr.domain.core.valueobject.EmployeeId;
import com.bradox.erp.hr.domain.core.valueobject.LeaveAllocationId;
import com.bradox.erp.hr.domain.core.valueobject.TimeOffTypeId;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LeaveAllocation {

    public static final String STATE_CONFIRM = "confirm";
    public static final String STATE_VALIDATE = "validate";
    public static final String STATE_REFUSE = "refuse";

    private final LeaveAllocationId id;
    private final CompanyId companyId;
    private final EmployeeId employeeId;
    private final TimeOffTypeId timeOffTypeId;
    private final String name;
    private final BigDecimal numberOfDays;
    private final String allocationType;
    private final String state;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    private LeaveAllocation(Builder b) {
        this.id = b.id;
        this.companyId = b.companyId;
        this.employeeId = b.employeeId;
        this.timeOffTypeId = b.timeOffTypeId;
        this.name = b.name;
        this.numberOfDays = b.numberOfDays;
        this.allocationType = b.allocationType;
        this.state = b.state;
        this.dateFrom = b.dateFrom;
        this.dateTo = b.dateTo;
    }

    public void validate() {
        if (companyId == null) throw new HrDomainException("error.hr.companyIdRequired", null, "companyId required");
        if (employeeId == null) throw new HrDomainException("error.hr.employeeIdRequired", null, "employeeId required");
        if (timeOffTypeId == null) throw new HrDomainException("error.hr.timeOffTypeIdRequired", null, "timeOffTypeId required");
        if (name == null || name.isBlank()) throw new HrDomainException("error.hr.nameRequired", null, "name required");
        if (numberOfDays == null || numberOfDays.signum() <= 0) {
            throw new HrDomainException("error.hr.numberOfDaysPositive", null, "numberOfDays must be positive");
        }
        if (dateFrom == null || dateTo == null) throw new HrDomainException("error.hr.validityDatesRequired", null, "validity dates required");
        if (dateTo.isBefore(dateFrom)) throw new HrDomainException("error.hr.dateToOnOrAfterFrom", null, "dateTo must be on or after dateFrom");
    }

    public LeaveAllocation approve() {
        if (!STATE_CONFIRM.equals(state)) {
            throw new HrDomainException("error.hr.onlyAwaitingAllocationCanValidate", null, "Only allocations awaiting approval can be validated");
        }
        return toBuilder().state(STATE_VALIDATE).build();
    }

    public LeaveAllocation refuse() {
        if (STATE_REFUSE.equals(state)) {
            throw new HrDomainException("error.hr.allocationAlreadyRefused", null, "Allocation already refused");
        }
        return toBuilder().state(STATE_REFUSE).build();
    }

    public boolean isApproved() {
        return STATE_VALIDATE.equals(state);
    }

    private Builder toBuilder() {
        return builder()
                .id(id)
                .companyId(companyId)
                .employeeId(employeeId)
                .timeOffTypeId(timeOffTypeId)
                .name(name)
                .numberOfDays(numberOfDays)
                .allocationType(allocationType)
                .state(state)
                .dateFrom(dateFrom)
                .dateTo(dateTo);
    }

    public LeaveAllocationId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public EmployeeId getEmployeeId() { return employeeId; }
    public TimeOffTypeId getTimeOffTypeId() { return timeOffTypeId; }
    public String getName() { return name; }
    public BigDecimal getNumberOfDays() { return numberOfDays; }
    public String getAllocationType() { return allocationType; }
    public String getState() { return state; }
    public LocalDate getDateFrom() { return dateFrom; }
    public LocalDate getDateTo() { return dateTo; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private LeaveAllocationId id;
        private CompanyId companyId;
        private EmployeeId employeeId;
        private TimeOffTypeId timeOffTypeId;
        private String name;
        private BigDecimal numberOfDays;
        private String allocationType = "regular";
        private String state = STATE_CONFIRM;
        private LocalDate dateFrom;
        private LocalDate dateTo;

        public Builder id(LeaveAllocationId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder employeeId(EmployeeId v) { this.employeeId = v; return this; }
        public Builder timeOffTypeId(TimeOffTypeId v) { this.timeOffTypeId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder numberOfDays(BigDecimal v) { this.numberOfDays = v; return this; }
        public Builder allocationType(String v) { this.allocationType = v; return this; }
        public Builder state(String v) { this.state = v; return this; }
        public Builder dateFrom(LocalDate v) { this.dateFrom = v; return this; }
        public Builder dateTo(LocalDate v) { this.dateTo = v; return this; }
        public LeaveAllocation build() { return new LeaveAllocation(this); }
    }
}
