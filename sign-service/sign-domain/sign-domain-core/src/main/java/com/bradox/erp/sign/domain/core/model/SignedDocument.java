package com.bradox.erp.sign.domain.core.model;

import java.time.Instant;
import java.util.UUID;

/** One signing of a template: who signed, when, and the signed PDF stored in Documents. */
public record SignedDocument(UUID id, UUID companyId, UUID templateId, String templateName, String signerName,
                             Instant signedAt, String signedBy, UUID documentId, String fileName) {
}
