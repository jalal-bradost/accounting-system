package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.erp.dataaccess.entity.AccountEntity;
import com.bradox.erp.dataaccess.entity.JournalEntryEntity;
import com.bradox.erp.dataaccess.entity.JournalEntity;
import com.bradox.erp.dataaccess.entity.JournalItemEntity;
import com.bradox.erp.dataaccess.mapper.JournalEntryDataAccessMapper;
import com.bradox.erp.dataaccess.repository.AccountJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalEntryJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalJpaRepository;
import com.bradox.erp.domain.core.ValueObject.JournalEntryId;
import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.core.ValueObject.JournalEntryStatus;
import com.bradox.erp.domain.core.entity.JournalEntry;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    @Transactional
    public JournalEntry save(JournalEntry journalEntry) {
        JournalEntity journalEntity = journalJpaRepository.findById(journalEntry.getJournalId().getId())
                .orElseThrow(() -> new IllegalStateException("Journal not found: " + journalEntry.getJournalId().getId()));
        JournalEntryEntity existing = jpaRepository.findByIdWithDetails(journalEntry.getId().getId()).orElse(null);
        if (existing != null && existing.getStatus() == JournalEntryStatus.POSTED) {
            throw new AccountingDomainException("error.accounting.cannotModifyPostedEntry", null, "Cannot modify a posted journal entry. Use reversal instead.");
        }
        JournalEntryEntity entity = mapper.domainToEntity(journalEntry, existing, journalEntity);
        Instant now = Instant.now();
        if (existing == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        if (journalEntry.getStatus() == JournalEntryStatus.POSTED && entity.getPostedAt() == null) {
            entity.setPostedAt(now);
        }
        setAccountOnItems(entity, journalEntry);
        JournalEntryEntity saved = jpaRepository.save(entity);
        // Re-load with associations — save() may return a managed entity whose
        // items/account proxies are not initialized when open-in-view is false.
        return jpaRepository.findByIdWithDetails(saved.getId())
                .map(mapper::entityToDomain)
                .orElseGet(() -> mapper.entityToDomain(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalEntry> findById(JournalEntryId id) {
        return jpaRepository.findByIdWithDetails(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntry> findByCompanyId(CompanyId companyId) {
        return jpaRepository.findByCompanyId(companyId.getId()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<JournalEntry> searchByCompanyId(
            CompanyId companyId, org.springframework.data.domain.Pageable pageable) {
        var idPage = jpaRepository.findIdsByCompanyId(companyId.getId(), pageable);
        if (idPage.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        Map<java.util.UUID, JournalEntry> byId = jpaRepository.findByIdInWithDetails(idPage.getContent()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toMap(e -> e.getId().getId(), java.util.function.Function.identity(), (a, b) -> a));
        List<JournalEntry> ordered = new java.util.ArrayList<>(idPage.getContent().size());
        for (java.util.UUID id : idPage.getContent()) {
            JournalEntry e = byId.get(id);
            if (e != null) {
                ordered.add(e);
            }
        }
        return new org.springframework.data.domain.PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntry> findByCompanyIdAndJournalIdAndDateBetween(
            CompanyId companyId, JournalId journalId, LocalDate from, LocalDate to) {
        return jpaRepository.findByCompanyIdAndJournalIdAndEntryDateBetween(
                companyId.getId(),
                journalId.getId(),
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay().minusNanos(1)).stream()
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
