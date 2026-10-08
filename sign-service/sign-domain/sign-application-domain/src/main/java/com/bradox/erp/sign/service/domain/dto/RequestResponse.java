package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequestResponse(UUID id, String reference, String name, String status, String signingOrder, String message,
                              UUID templateId, UUID sourceDocumentId, String sourceSha256, int pageCount, Instant expiresAt,
                              Integer reminderEveryDays, int reminderPrompts, boolean reminderDue, String recordModel,
                              UUID recordId, UUID finalDocumentId, String finalSha256, Instant sentAt, Instant completedAt,
                              Instant canceledAt, String cancelReason, boolean needsAttention, String attentionReason,
                              String createdBy, Instant createdAt, int signedCount, List<SignerResponse> signers,
                              List<FieldDto> fields, boolean repeatedSigner, boolean canEdit, boolean canCancel) {
}
