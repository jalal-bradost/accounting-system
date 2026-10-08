package com.bradox.erp.documents.service.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DocumentSummaryResponse(
        UUID id,
        UUID folderId,
        String folderName,
        String name,
        String fileName,
        String contentType,
        long sizeBytes,
        int versionNo,
        String status,
        List<UUID> tagIds,
        int linkCount,
        LocalDate retentionUntil,
        Instant trashedAt,
        String trashedBy,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt,
        boolean previewable) {
}
