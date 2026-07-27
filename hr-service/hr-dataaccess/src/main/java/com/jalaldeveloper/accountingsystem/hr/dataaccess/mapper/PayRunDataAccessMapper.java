package com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayPayslipEntity;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayPayslipLineEntity;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayRunEntity;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.PayRun;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Payslip;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.PayslipLine;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PayRunDataAccessMapper {

    public PayRun entityToDomain(PayRunEntity e) {
        if (e == null) return null;
        List<Payslip> payslips = e.getPayslips().stream().map(this::payslipEntityToDomain).toList();
        return PayRun.builder()
                .id(e.getId())
                .companyId(new CompanyId(e.getCompanyId()))
                .name(e.getName())
                .periodStart(e.getPeriodStart())
                .periodEnd(e.getPeriodEnd())
                .state(e.getState())
                .journalEntryId(e.getJournalEntryId())
                .paymentJournalEntryId(e.getPaymentJournalEntryId())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .payslips(payslips)
                .build();
    }

    public Payslip payslipEntityToDomain(PayPayslipEntity e) {
        if (e == null) return null;
        List<PayslipLine> lines = e.getLines().stream().map(this::lineEntityToDomain).toList();
        return Payslip.builder()
                .id(e.getId())
                .payRunId(e.getPayRun() != null ? e.getPayRun().getId() : null)
                .employeeId(new EmployeeId(e.getEmployeeId()))
                .contractId(e.getContractId())
                .currencyCode(e.getCurrencyCode())
                .state(e.getState())
                .basic(e.getBasic())
                .allowances(e.getAllowances())
                .deductions(e.getDeductions())
                .net(e.getNet())
                .workedDays(e.getWorkedDays())
                .absenceDays(e.getAbsenceDays())
                .lines(lines)
                .build();
    }

    public PayslipLine lineEntityToDomain(PayPayslipLineEntity e) {
        if (e == null) return null;
        return new PayslipLine(e.getId(), e.getCode(), e.getName(), e.getCategory(), e.getAmount(), e.getAccountId());
    }

    public PayRunEntity domainToEntity(PayRun d, PayRunEntity existingOrNull) {
        if (d == null) return null;
        PayRunEntity e = existingOrNull != null ? existingOrNull : new PayRunEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setName(d.getName());
        e.setPeriodStart(d.getPeriodStart());
        e.setPeriodEnd(d.getPeriodEnd());
        e.setState(d.getState());
        e.setJournalEntryId(d.getJournalEntryId());
        e.setPaymentJournalEntryId(d.getPaymentJournalEntryId());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        e.getPayslips().clear();
        for (Payslip payslip : d.getPayslips()) {
            e.getPayslips().add(payslipDomainToEntity(payslip, e));
        }
        return e;
    }

    private PayPayslipEntity payslipDomainToEntity(Payslip d, PayRunEntity run) {
        PayPayslipEntity e = new PayPayslipEntity();
        e.setId(d.getId());
        e.setPayRun(run);
        e.setEmployeeId(d.getEmployeeId().getId());
        e.setContractId(d.getContractId());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setState(d.getState());
        e.setBasic(d.getBasic());
        e.setAllowances(d.getAllowances());
        e.setDeductions(d.getDeductions());
        e.setNet(d.getNet());
        e.setWorkedDays(d.getWorkedDays());
        e.setAbsenceDays(d.getAbsenceDays());
        e.setJournalEntryId(null);
        List<PayPayslipLineEntity> lines = new ArrayList<>();
        for (PayslipLine line : d.getLines()) {
            PayPayslipLineEntity le = new PayPayslipLineEntity();
            le.setId(line.getId());
            le.setPayslip(e);
            le.setCode(line.getCode());
            le.setName(line.getName());
            le.setCategory(line.getCategory());
            le.setAmount(line.getAmount());
            le.setAccountId(line.getAccountId());
            lines.add(le);
        }
        e.setLines(lines);
        return e;
    }
}
