package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pay_payslip")
public class PayPayslipEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_run_id", nullable = false)
    private PayRunEntity payRun;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode;

    @Column(nullable = false, length = 32)
    private String state;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal basic;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal allowances;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal deductions;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal net;

    @Column(name = "worked_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal workedDays;

    @Column(name = "absence_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal absenceDays;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @OneToMany(mappedBy = "payslip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("code ASC")
    private List<PayPayslipLineEntity> lines = new ArrayList<>();

    public PayPayslipEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PayRunEntity getPayRun() { return payRun; }
    public void setPayRun(PayRunEntity payRun) { this.payRun = payRun; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public BigDecimal getBasic() { return basic; }
    public void setBasic(BigDecimal basic) { this.basic = basic; }
    public BigDecimal getAllowances() { return allowances; }
    public void setAllowances(BigDecimal allowances) { this.allowances = allowances; }
    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal net) { this.net = net; }
    public BigDecimal getWorkedDays() { return workedDays; }
    public void setWorkedDays(BigDecimal workedDays) { this.workedDays = workedDays; }
    public BigDecimal getAbsenceDays() { return absenceDays; }
    public void setAbsenceDays(BigDecimal absenceDays) { this.absenceDays = absenceDays; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public List<PayPayslipLineEntity> getLines() { return lines; }
    public void setLines(List<PayPayslipLineEntity> lines) { this.lines = lines; }
}
