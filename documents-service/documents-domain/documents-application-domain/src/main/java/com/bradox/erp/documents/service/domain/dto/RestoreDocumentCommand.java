package com.bradox.erp.documents.service.domain.dto;

import java.util.UUID;

/** {@code targetFolderId} is only needed when the original folder is archived. */
public record RestoreDocumentCommand(UUID targetFolderId) {
}
