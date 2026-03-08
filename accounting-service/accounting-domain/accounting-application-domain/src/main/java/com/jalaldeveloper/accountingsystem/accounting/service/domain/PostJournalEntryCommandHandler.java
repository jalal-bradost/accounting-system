package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.jalaldeveloper.accountingsystem.domain.core.AccountingDomainService;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class PostJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDomainService accountingDomainService;
    private final AccountingDataMapper mapper;
    private final CompanyLockDatePort companyLockDatePort;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    PostJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                  AccountingDomainService accountingDomainService,
                                  AccountingDataMapper mapper,
                                  CompanyLockDatePort companyLockDatePort,
                                  FiscalPeriodRepository fiscalPeriodRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountingDomainService = accountingDomainService;
        this.mapper = mapper;
        this.companyLockDatePort = companyLockDatePort;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    @Transactional
    CreateJournalEntryResponse postJournalEntry(UUID journalEntryId) {
        JournalEntry entry = journalEntryRepository.findById(new JournalEntryId(journalEntryId))
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + journalEntryId));
        if (entry.getStatus() == JournalEntryStatus.POSTED) {
            return mapper.journalEntryToCreateResponse(entry, "Journal entry already posted.");
        }
        companyLockDatePort.getPeriodLockDate(entry.getCompanyId()).ifPresent(lockDate -> {
            if (entry.getDate().isBefore(lockDate)) {
                throw new AccountingDomainException(
                    "Cannot post: entry date " + entry.getDate() + " is before period lock date " + lockDate + ".");
            }
        });
        fiscalPeriodRepository.findPeriodContaining(entry.getCompanyId(), entry.getDate()).ifPresent(period -> {
            if (!period.open()) {
                throw new AccountingDomainException(
                    "Cannot post: fiscal period " + period.startDate() + "–" + period.endDate() + " is closed.");
            }
        });
        accountingDomainService.postJournalEntry(entry);
        JournalEntry saved = journalEntryRepository.save(entry);
        return mapper.journalEntryToCreateResponse(saved, "Journal entry posted successfully.");
    }
}
