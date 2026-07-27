package com.jalaldeveloper.accountingsystem.hr.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PayslipLine {

    private final UUID id;
    private final String code;
    private final String name;
    private final String category;
    private final BigDecimal amount;
    private final UUID accountId;

    public PayslipLine(UUID id, String code, String name, String category, BigDecimal amount, UUID accountId) {
        this.id = id != null ? id : UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.category = category;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.accountId = accountId;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public UUID getAccountId() { return accountId; }
}
