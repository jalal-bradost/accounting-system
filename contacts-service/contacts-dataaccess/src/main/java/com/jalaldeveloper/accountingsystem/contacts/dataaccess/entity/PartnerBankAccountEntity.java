package com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "contacts_partner_bank_account")
public class PartnerBankAccountEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    @Column(nullable = false, length = 50)
    private String iban;

    @Column(length = 20)
    private String swift;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    private String accountHolderName;

    public PartnerBankAccountEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PartnerEntity getPartner() { return partner; }
    public void setPartner(PartnerEntity partner) { this.partner = partner; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getSwift() { return swift; }
    public void setSwift(String swift) { this.swift = swift; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
}
