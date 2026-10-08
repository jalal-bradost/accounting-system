package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequestSummaryResponse(UUID id, String reference, String name, String status, int signedCount, int signerCount,
                                     String currentSigner, Instant expiresAt, Instant sentAt, Instant completedAt,
                                     boolean needsAttention, boolean reminderDue, String recordModel, UUID recordId,
                                     String createdBy, Instant createdAt) {
}
