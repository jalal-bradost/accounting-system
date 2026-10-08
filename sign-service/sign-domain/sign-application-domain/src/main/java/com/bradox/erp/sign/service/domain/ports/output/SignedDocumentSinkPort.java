package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.UUID;

/** Stores the finished PDF in Documents, linked to the record, retention-locked (BR-SIG-12). */
public interface SignedDocumentSinkPort {

    UUID store(CompanyId companyId, String fileName, byte[] pdf, UUID requestId, String reference, String recordModel,
               UUID recordId, LocalDate retentionUntil);

    /** Bytes of a stored final document, read by the system so signers can download without an account. */
    byte[] read(CompanyId companyId, UUID documentId);
}
