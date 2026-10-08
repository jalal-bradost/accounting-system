package com.bradox.erp.documents.service.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Full document detail. {@code level} is the current user's effective access level on it. */
public record DocumentResponse(
        UUID id,
        UUID folderId,
        List<FolderRefResponse> folderPath,
        String name,
        String description,
        String fileName,
        String contentType,
        long sizeBytes,
        String sha256,
        int versionNo,
        String status,
        String source,
        List<UUID> tagIds,
        List<RecordLinkResponse> links,
        LocalDate retentionUntil,
        String retentionReason,
        Instant trashedAt,
        String trashedBy,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt,
        boolean previewable,
        String level) {
}
