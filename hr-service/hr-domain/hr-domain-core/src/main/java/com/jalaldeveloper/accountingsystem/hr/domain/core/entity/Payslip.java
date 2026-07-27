package com.jalaldeveloper.accountingsystem.hr.domain.core.entity;

import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Payslip {

    public static final String STATE_DRAFT = "draft";
    public static final String STATE_CONFIRMED = "confirmed";
    public static final String STATE_POSTED = "posted";

    private final UUID id;
    private final UUID payRunId;
    private final EmployeeId employeeId;
    private final UUID contractId;
    private final String currencyCode;
    private final String state;
    private final BigDecimal basic;
    private final BigDecimal allowances;
    private final BigDecimal deductions;
    private final BigDecimal net;
    private final BigDecimal workedDays;
    private final BigDecimal absenceDays;
    private final List<PayslipLine> lines;

    private Payslip(Builder b) {
        this.id = b.id != null ? b.id : UUID.randomUUID();
        this.payRunId = b.payRunId;
        this.employeeId = b.employeeId;
        this.contractId = b.contractId;
        this.currencyCode = b.currencyCode;
        this.state = b.state != null ? b.state : STATE_DRAFT;
        this.basic = b.basic != null ? b.basic : BigDecimal.ZERO;
        this.allowances = b.allowances != null ? b.allowances : BigDecimal.ZERO;
        this.deductions = b.deductions != null ? b.deductions : BigDecimal.ZERO;
        this.net = b.net != null ? b.net : BigDecimal.ZERO;
        this.workedDays = b.workedDays != null ? b.workedDays : BigDecimal.ZERO;
        this.absenceDays = b.absenceDays != null ? b.absenceDays : BigDecimal.ZERO;
        this.lines = b.lines != null ? List.copyOf(b.lines) : List.of();
    }

    public void validate() {
        if (payRunId == null) throw new HrDomainException("payRunId required");
        if (employeeId == null) throw new HrDomainException("employeeId required");
        if (contractId == null) throw new HrDomainException("contractId required");
        if (currencyCode == null || currencyCode.isBlank()) throw new HrDomainException("currencyCode required");
    }

    public Payslip markPosted() {
        return toBuilder().state(STATE_POSTED).build();
    }

    public Payslip markConfirmed() {
        return toBuilder().state(STATE_CONFIRMED).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .payRunId(payRunId)
                .employeeId(employeeId)
                .contractId(contractId)
                .currencyCode(currencyCode)
                .state(state)
                .basic(basic)
                .allowances(allowances)
                .deductions(deductions)
                .net(net)
                .workedDays(workedDays)
                .absenceDays(absenceDays)
                .lines(new ArrayList<>(lines));
    }

    public UUID getId() { return id; }
    public UUID getPayRunId() { return payRunId; }
    public EmployeeId getEmployeeId() { return employeeId; }
    public UUID getContractId() { return contractId; }
    public String getCurrencyCode() { return currencyCode; }
    public String getState() { return state; }
    public BigDecimal getBasic() { return basic; }
    public BigDecimal getAllowances() { return allowances; }
    public BigDecimal getDeductions() { return deductions; }
    public BigDecimal getNet() { return net; }
    public BigDecimal getWorkedDays() { return workedDays; }
    public BigDecimal getAbsenceDays() { return absenceDays; }
    public List<PayslipLine> getLines() { return Collections.unmodifiableList(lines); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID payRunId;
        private EmployeeId employeeId;
        private UUID contractId;
        private String currencyCode;
        private String state;
        private BigDecimal basic;
        private BigDecimal allowances;
        private BigDecimal deductions;
        private BigDecimal net;
        private BigDecimal workedDays;
        private BigDecimal absenceDays;
        private List<PayslipLine> lines = new ArrayList<>();

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder payRunId(UUID v) { this.payRunId = v; return this; }
        public Builder employeeId(EmployeeId v) { this.employeeId = v; return this; }
        public Builder contractId(UUID v) { this.contractId = v; return this; }
        public Builder currencyCode(String v) { this.currencyCode = v; return this; }
        public Builder state(String v) { this.state = v; return this; }
        public Builder basic(BigDecimal v) { this.basic = v; return this; }
        public Builder allowances(BigDecimal v) { this.allowances = v; return this; }
        public Builder deductions(BigDecimal v) { this.deductions = v; return this; }
        public Builder net(BigDecimal v) { this.net = v; return this; }
        public Builder workedDays(BigDecimal v) { this.workedDays = v; return this; }
        public Builder absenceDays(BigDecimal v) { this.absenceDays = v; return this; }
        public Builder lines(List<PayslipLine> v) { this.lines = v; return this; }
        public Payslip build() { return new Payslip(this); }
    }
}
