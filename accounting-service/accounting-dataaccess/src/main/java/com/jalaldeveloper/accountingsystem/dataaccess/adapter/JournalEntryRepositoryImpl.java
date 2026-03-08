package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntryEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalItemEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.mapper.JournalEntryDataAccessMapper;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccountJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalEntryJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JournalEntryRepositoryImpl implements JournalEntryRepository {

    private final JournalEntryJpaRepository jpaRepository;
    private final JournalEntryDataAccessMapper mapper;
    private final JournalJpaRepository journalJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    public JournalEntryRepositoryImpl(JournalEntryJpaRepository jpaRepository,
                                      JournalEntryDataAccessMapper mapper,
                                      JournalJpaRepository journalJpaRepository,
                                      AccountJpaRepository accountJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.journalJpaRepository = journalJpaRepository;
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public JournalEntry save(JournalEntry journalEntry) {
        JournalEntity journalEntity = journalJpaRepository.findById(journalEntry.getJournalId().getId())
                .orElseThrow(() -> new IllegalStateException("Journal not found: " + journalEntry.getJournalId().getId()));
        JournalEntryEntity existing = jpaRepository.findById(journalEntry.getId().getId()).orElse(null);
        if (existing != null && existing.getStatus() == JournalEntryStatus.POSTED) {
            throw new AccountingDomainException("Cannot modify a posted journal entry. Use reversal instead.");
        }
        JournalEntryEntity entity = mapper.domainToEntity(journalEntry, existing, journalEntity);
        setAccountOnItems(entity, journalEntry);
        JournalEntryEntity saved = jpaRepository.save(entity);
        return mapper.entityToDomain(saved);
    }

    @Override
    public Optional<JournalEntry> findById(JournalEntryId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public List<JournalEntry> findByCompanyId(CompanyId companyId) {
        return jpaRepository.findByCompanyId(companyId.getId()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<JournalEntry> findByCompanyIdAndJournalIdAndDateBetween(
            CompanyId companyId, JournalId journalId, LocalDate from, LocalDate to) {
        return jpaRepository.findByCompanyIdAndJournalIdAndEntryDateBetween(
                companyId.getId(), journalId.getId(), from, to).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySequenceNumberAndCompanyIdAndJournalId(
            String sequenceNumber, CompanyId companyId, JournalId journalId) {
        return jpaRepository.existsBySequenceNumberAndCompanyIdAndJournal_Id(
                sequenceNumber, companyId.getId(), journalId.getId());
    }

    private void setAccountOnItems(JournalEntryEntity entity, JournalEntry domain) {
        if (entity.getItems() == null || domain.getItems() == null) return;
        if (entity.getItems().size() != domain.getItems().size()) return;
        for (int i = 0; i < domain.getItems().size(); i++) {
            JournalItemEntity itemEntity = entity.getItems().get(i);
            var accountId = domain.getItems().get(i).getAccountId().getId();
            AccountEntity accountEntity = accountJpaRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalStateException("Account not found: " + accountId));
            itemEntity.setAccount(accountEntity);
        }
    }
}
