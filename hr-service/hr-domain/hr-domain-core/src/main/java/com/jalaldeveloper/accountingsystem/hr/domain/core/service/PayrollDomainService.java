package com.jalaldeveloper.accountingsystem.hr.domain.core.service;

import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Payslip;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.PayslipLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PayrollDomainService {

    public record SalaryRuleInput(
            UUID id,
            String code,
            String name,
            String category,
            String amountType,
            BigDecimal amount,
            boolean active,
            UUID accountId) {}

    public record PayslipComputationInput(
            UUID payRunId,
            UUID employeeId,
            UUID contractId,
            String currencyCode,
            BigDecimal contractWage,
            boolean attendanceBased,
            BigDecimal expectedWorkDays,
            BigDecimal workedDays,
            BigDecimal absenceDays,
            List<SalaryRuleInput> rules) {}

    public Payslip computePayslip(PayslipComputationInput input) {
        BigDecimal basicBase = input.contractWage();
        if (input.attendanceBased() && input.expectedWorkDays().signum() > 0) {
            BigDecimal ratio = input.workedDays().divide(input.expectedWorkDays(), 6, RoundingMode.HALF_UP);
            basicBase = input.contractWage().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        }

        List<PayslipLine> lines = new ArrayList<>();
        BigDecimal allowances = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;

        lines.add(new PayslipLine(null, "BASIC", "Basic Salary", "basic", basicBase, null));

        for (SalaryRuleInput rule : input.rules()) {
            if (!rule.active()
                    || "basic".equalsIgnoreCase(rule.category())
                    || "net".equalsIgnoreCase(rule.category())) {
                continue;
            }
            BigDecimal amount = computeRuleAmount(rule, basicBase);
            lines.add(new PayslipLine(null, rule.code(), rule.name(), rule.category(), amount, rule.accountId()));
            if ("allowance".equalsIgnoreCase(rule.category())) {
                allowances = allowances.add(amount);
            } else if ("deduction".equalsIgnoreCase(rule.category())) {
                deductions = deductions.add(amount);
            }
        }

        BigDecimal net = basicBase.add(allowances).subtract(deductions).setScale(2, RoundingMode.HALF_UP);
        lines.add(new PayslipLine(null, "NET", "Net Salary", "net", net, null));

        return Payslip.builder()
                .payRunId(input.payRunId())
                .employeeId(new com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId(input.employeeId()))
                .contractId(input.contractId())
                .currencyCode(input.currencyCode())
                .state(Payslip.STATE_CONFIRMED)
                .basic(basicBase)
                .allowances(allowances)
                .deductions(deductions)
                .net(net)
                .workedDays(input.workedDays())
                .absenceDays(input.absenceDays())
                .lines(lines)
                .build();
    }

    private BigDecimal computeRuleAmount(SalaryRuleInput rule, BigDecimal basic) {
        if ("percent".equalsIgnoreCase(rule.amountType())) {
            return basic.multiply(rule.amount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return rule.amount().setScale(2, RoundingMode.HALF_UP);
    }
}
