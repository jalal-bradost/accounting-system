package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_items")
public class JournalItemEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryEntity journalEntry;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;
    @Column(length = 500)
    private String label;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal debit;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal credit;
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
    @Column(name = "amount_currency", precision = 19, scale = 4)
    private BigDecimal amountCurrency;
    @Column(name = "reconciliation_id")
    private UUID reconciliationId;
    @Column(name = "partner_id")
    private UUID partnerId;
    @Column(name = "partner_name", length = 255)
    private String partnerName;

    public JournalItemEntity() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public JournalEntryEntity getJournalEntry() { return journalEntry; }
    public void setJournalEntry(JournalEntryEntity journalEntry) { this.journalEntry = journalEntry; }
    public AccountEntity getAccount() { return account; }
    public void setAccount(AccountEntity account) { this.account = account; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getAmountCurrency() { return amountCurrency; }
    public void setAmountCurrency(BigDecimal amountCurrency) { this.amountCurrency = amountCurrency; }
    public UUID getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(UUID reconciliationId) { this.reconciliationId = reconciliationId; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
}