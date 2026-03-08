package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntrySequenceEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalEntrySequenceJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class SequenceGeneratorAdapter implements SequenceGeneratorPort {

    private static final int SEQ_WIDTH = 5;
    private static final String FORMAT = "%0" + SEQ_WIDTH + "d";

    private final JournalEntrySequenceJpaRepository sequenceRepository;

    public SequenceGeneratorAdapter(JournalEntrySequenceJpaRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Override
    @Transactional
    public String getNextSequenceNumber(CompanyId companyId, JournalId journalId, LocalDate forDate) {
        String periodKey = String.valueOf(forDate.getYear());
        UUID companyUuid = companyId.getId();
        UUID journalUuid = journalId.getId();

        JournalEntrySequenceEntity seq = sequenceRepository
                .findByCompanyIdAndJournalIdAndPeriodKeyForUpdate(companyUuid, journalUuid, periodKey)
                .orElseGet(() -> {
                    JournalEntrySequenceEntity newSeq = new JournalEntrySequenceEntity();
                    newSeq.setId(UUID.randomUUID());
                    newSeq.setCompanyId(companyUuid);
                    newSeq.setJournalId(journalUuid);
                    newSeq.setPeriodKey(periodKey);
                    newSeq.setLastNumber(0L);
                    return newSeq;
                });

        seq.setLastNumber(seq.getLastNumber() + 1);
        sequenceRepository.save(seq);

        return "JOU-" + periodKey + "-" + String.format(FORMAT, seq.getLastNumber());
    }
}
