package com.bradox.delin.expense.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class SaveExpenseTypeCommand {

    private UUID companyId;

    @NotBlank
    private String name;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
