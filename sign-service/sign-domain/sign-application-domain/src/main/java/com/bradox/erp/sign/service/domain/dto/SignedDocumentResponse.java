package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record SignedDocumentResponse(UUID id, UUID templateId, String templateName, String signerName, Instant signedAt,
                                     String signedBy, UUID documentId, String fileName) {
}
