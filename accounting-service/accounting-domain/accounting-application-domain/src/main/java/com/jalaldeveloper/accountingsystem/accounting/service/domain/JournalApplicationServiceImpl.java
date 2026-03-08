package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class JournalApplicationServiceImpl implements JournalApplicationService {

    private final CreateJournalCommandHandler createJournalCommandHandler;
    private final JournalRepository journalRepository;
    private final AccountingDataMapper mapper;

    JournalApplicationServiceImpl(CreateJournalCommandHandler createJournalCommandHandler,
                                  JournalRepository journalRepository,
                                  AccountingDataMapper mapper) {
        this.createJournalCommandHandler = createJournalCommandHandler;
        this.journalRepository = journalRepository;
        this.mapper = mapper;
    }

    @Override
    public CreateJournalResponse createJournal(CreateJournalCommand command) {
        return createJournalCommandHandler.createJournal(command);
    }

    @Override
    public JournalResponse getJournal(UUID journalId) {
        return journalRepository.findById(new JournalId(journalId))
                .map(mapper::journalToJournalResponse)
                .orElseThrow(() -> new IllegalArgumentException("Journal not found: " + journalId));
    }

    @Override
    public List<JournalResponse> listJournalsByCompany(UUID companyId) {
        return journalRepository.findByCompanyId(new CompanyId(companyId)).stream()
                .map(mapper::journalToJournalResponse)
                .collect(Collectors.toList());
    }
}
