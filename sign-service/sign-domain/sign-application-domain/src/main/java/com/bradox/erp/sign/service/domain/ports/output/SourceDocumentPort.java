package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.UUID;

/** Reads PDFs from Documents. Sign never stores source files itself. */
public interface SourceDocumentPort {

    record SourceDoc(UUID documentId, UUID versionId, String sha256, String fileName, byte[] bytes) {
    }

    /** The current version, read as the signed-in user (Documents checks their access). */
    SourceDoc readCurrent(CompanyId companyId, UUID documentId);

    /** A pinned version, read by the system for public signing and final builds (no user is signed in). */
    SourceDoc readPinned(CompanyId companyId, UUID documentId, UUID versionId);
}
