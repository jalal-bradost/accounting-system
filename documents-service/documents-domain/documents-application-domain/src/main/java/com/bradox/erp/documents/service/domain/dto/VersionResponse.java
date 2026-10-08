package com.bradox.erp.documents.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record VersionResponse(
        UUID id,
        int versionNo,
        String fileName,
        String contentType,
        long sizeBytes,
        String sha256,
        String uploadedBy,
        Instant uploadedAt,
        String comment,
        boolean current) {
}
