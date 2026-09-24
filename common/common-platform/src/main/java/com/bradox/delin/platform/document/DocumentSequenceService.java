package com.bradox.delin.platform.document;

import com.bradox.delin.platform.dataaccess.entity.PlatDocumentSequenceEntity;
import com.bradox.delin.platform.dataaccess.repository.DocumentSequenceJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Component
public class DocumentSequenceService {

    private static final int SEQ_WIDTH = 5;
    private static final String PADDED = "%0" + SEQ_WIDTH + "d";

    private final DocumentSequenceJpaRepository sequenceRepository;

    public DocumentSequenceService(DocumentSequenceJpaRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String next(UUID companyId, String docType) {
        String type = docType.trim().toUpperCase(Locale.ROOT);
        int year = LocalDate.now().getYear();
        PlatDocumentSequenceEntity seq = sequenceRepository
                .findForUpdate(companyId, type, year)
                .orElseGet(() -> {
                    PlatDocumentSequenceEntity row = new PlatDocumentSequenceEntity();
                    row.setId(UUID.randomUUID());
                    row.setCompanyId(companyId);
                    row.setDocType(type);
                    row.setYear(year);
                    row.setLastValue(0L);
                    return row;
                });
        seq.setLastValue(seq.getLastValue() + 1);
        sequenceRepository.save(seq);
        String padded = String.format(PADDED, seq.getLastValue());
        return switch (type) {
            case "PO" -> "PO/" + year + "/" + padded;
            case "SO" -> "SO/" + year + "/" + padded;
            case "BILL" -> "BILL/" + year + "/" + padded;
            case "INV" -> "INV/" + year + "/" + padded;
            default -> type + "/" + year + "/" + padded;
        };
    }
}
