package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.bradox.erp.dataaccess.entity.JournalEntity;
import com.bradox.erp.dataaccess.entity.JournalEntrySequenceEntity;
import com.bradox.erp.dataaccess.repository.JournalEntrySequenceJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalJpaRepository;
import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Component
public class SequenceGeneratorAdapter implements SequenceGeneratorPort {

    private static final int SEQ_WIDTH = 5;
    private static final String FORMAT = "%0" + SEQ_WIDTH + "d";

    private final JournalEntrySequenceJpaRepository sequenceRepository;
    private final JournalJpaRepository journalRepository;

    public SequenceGeneratorAdapter(JournalEntrySequenceJpaRepository sequenceRepository,
                                    JournalJpaRepository journalRepository) {
        this.sequenceRepository = sequenceRepository;
        this.journalRepository = journalRepository;
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

        String prefix = resolvePrefix(journalUuid);
        return prefix + "/" + periodKey + "/" + String.format(FORMAT, seq.getLastNumber());
    }

    private String resolvePrefix(UUID journalUuid) {
        return journalRepository.findById(journalUuid)
                .map(this::prefixFromJournal)
                .orElse("JOU");
    }

    private String prefixFromJournal(JournalEntity journal) {
        String name = journal.getName();
        if (name != null && !name.isBlank()) {
            return sanitizePrefix(name);
        }
        String code = journal.getCode();
        if (code != null && !code.isBlank()) {
            return sanitizePrefix(code);
        }
        return "JOU";
    }

    /** e.g. "Cash", "Bank", "Inventory Valuation" → "InventoryValuation" */
    private static String sanitizePrefix(String raw) {
        String cleaned = raw.trim().replaceAll("\\s+", "");
        if (cleaned.isEmpty()) {
            return "JOU";
        }
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
    }
}
