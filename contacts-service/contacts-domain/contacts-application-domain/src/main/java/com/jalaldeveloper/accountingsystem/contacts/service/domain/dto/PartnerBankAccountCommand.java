package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

public class PartnerBankAccountCommand {
    @NotBlank private String iban;
    private String swift;
    @NotBlank private String accountHolderName;

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getSwift() { return swift; }
    public void setSwift(String swift) { this.swift = swift; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
}
