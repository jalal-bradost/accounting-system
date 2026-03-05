package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_items")
public class JournalItemEntity {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    private BigDecimal debit;
    private BigDecimal credit;
    private String currencyCode;
    private Money amountCurrency;
}