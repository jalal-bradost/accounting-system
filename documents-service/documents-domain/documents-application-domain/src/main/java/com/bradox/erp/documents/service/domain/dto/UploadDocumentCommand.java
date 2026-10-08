package com.bradox.erp.documents.service.domain.dto;

import java.io.InputStream;
import java.util.UUID;

/**
 * File to store. The caller owns and closes the stream. {@code folderId} is used for new
 * documents, {@code comment} for new versions.
 */
public record UploadDocumentCommand(
        UUID folderId,
        String fileName,
        InputStream content,
        boolean allowDuplicate,
        String comment) {
}
