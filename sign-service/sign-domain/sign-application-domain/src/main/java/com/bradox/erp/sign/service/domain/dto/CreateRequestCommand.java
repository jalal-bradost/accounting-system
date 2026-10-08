package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/** From a template ({@code templateId}) or one-off from a document ({@code documentId} plus {@code fields}). */
public record CreateRequestCommand(@NotBlank String name, UUID templateId, UUID documentId, String signingOrder, String message,
                                   Integer reminderEveryDays, String recordModel, UUID recordId,
                                   List<SignerInput> signers, List<FieldDto> fields) {
}
