package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Null fields are left unchanged. */
public record UpdateDocumentCommand(@Size(max = 255) String name, String description, UUID folderId) {
}
