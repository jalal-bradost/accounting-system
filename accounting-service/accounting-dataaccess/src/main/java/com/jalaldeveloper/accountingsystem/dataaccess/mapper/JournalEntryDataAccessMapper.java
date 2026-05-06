package com.jalaldeveloper.accountingsystem.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntryEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalItemEntity;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.*;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import java.time.Instant;

@Component
public class JournalEntryDataAccessMapper {

    public JournalEntry entityToDomain(JournalEntryEntity entity) {
        if (entity == null) return null;
        List<JournalItem> items = entity.getItems() == null
                ? Collections.emptyList()
                : entity.getItems().stream()
                .map(this::itemEntityToDomain)
                .collect(Collectors.toList());
        Currency currency = toCurrency(entity.getCurrencyCode());
        return JournalEntry.builder()
                .id(new JournalEntryId(entity.getId()))
                .companyId(new CompanyId(entity.getCompanyId()))
                .journalId(new JournalId(entity.getJournal().getId()))
                .sequenceNumber(entity.getSequenceNumber())
                .date(entity.getEntryDate())
                .currency(currency)
                .items(items)
                .reversalOfEntryId(entity.getReversalOfEntryId() != null ? new JournalEntryId(entity.getReversalOfEntryId()) : null)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .postedAt(entity.getPostedAt())
                .postedBy(entity.getPostedBy())
                .partnerRef(entity.getPartnerId() != null
                        ? new PartnerRef(entity.getPartnerId(), entity.getPartnerName())
                        : null)
                .build();
    }

    public JournalEntryEntity domainToEntity(JournalEntry domain, JournalEntryEntity existingOrNull, JournalEntity journalEntity) {
        if (domain == null) return null;
        JournalEntryEntity entity = existingOrNull != null ? existingOrNull : new JournalEntryEntity();
        if (entity.getItems() == null) entity.setItems(new ArrayList<>());
        entity.setId(domain.getId().getId());
        entity.setCompanyId(domain.getCompanyId().getId());
        entity.setJournal(journalEntity);
        entity.setSequenceNumber(domain.getSequenceNumber());
        entity.setEntryDate(domain.getDate());
        entity.setCurrencyCode(domain.getCurrency() != null ? domain.getCurrency().code() : null);
        entity.setStatus(domain.getStatus());
        entity.setReversalOfEntryId(domain.getReversalOfEntryId() != null ? domain.getReversalOfEntryId().getId() : null);
        if (domain.getPartnerRef() != null) {
            entity.setPartnerId(domain.getPartnerRef().id());
            entity.setPartnerName(domain.getPartnerRef().name());
        } else {
            entity.setPartnerId(null);
            entity.setPartnerName(null);
        }
        if (domain.getItems() != null) {
            entity.getItems().clear();
            for (JournalItem item : domain.getItems()) {
                entity.getItems().add(itemDomainToEntity(item, entity));
            }
        }
        return entity;
    }

    private JournalItem itemEntityToDomain(JournalItemEntity e) {
        Money amountCurrency = e.getAmountCurrency() != null
                ? new Money(e.getAmountCurrency())
                : Money.ZERO;
        Currency currency = toCurrency(e.getCurrencyCode());
        return JournalItem.builder()
                .id(new JournalItemId(e.getId()))
                .accountId(new AccountId(e.getAccount().getId()))
                .label(e.getLabel())
                .debit(e.getDebit() != null ? e.getDebit() : BigDecimal.ZERO)
                .credit(e.getCredit() != null ? e.getCredit() : BigDecimal.ZERO)
                .amountCurrency(amountCurrency)
                .currency(currency)
                .reconciliationId(e.getReconciliationId())
                .partnerRef(e.getPartnerId() != null
                        ? new PartnerRef(e.getPartnerId(), e.getPartnerName())
                        : null)
                .build();
    }

    private JournalItemEntity itemDomainToEntity(JournalItem domain, JournalEntryEntity journalEntry) {
        JournalItemEntity e = new JournalItemEntity();
        e.setId(domain.getId().getId());
        e.setJournalEntry(journalEntry);
        e.setLabel(domain.getLabel());
        e.setDebit(domain.getDebit());
        e.setCredit(domain.getCredit());
        e.setCurrencyCode(domain.getCurrency() != null ? domain.getCurrency().code() : null);
        e.setAmountCurrency(domain.getAmountCurrency() != null && domain.getAmountCurrency().getAmount() != null
                ? domain.getAmountCurrency().getAmount()
                : BigDecimal.ZERO);
        e.setReconciliationId(domain.getReconciliationId());
        if (domain.getPartnerRef() != null) {
            e.setPartnerId(domain.getPartnerRef().id());
            e.setPartnerName(domain.getPartnerRef().name());
        } else {
            e.setPartnerId(null);
            e.setPartnerName(null);
        }
        return e;
    }

    private static Currency toCurrency(String code) {
        if (code == null || code.isBlank()) return Currency.USD();
        return new Currency(code, "", 2);
    }
}
