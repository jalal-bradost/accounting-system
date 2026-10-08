package com.bradox.erp.documents.service.domain.dto;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A file another module stores on its own behalf, with no signed-in user (for example a signed PDF finished by an
 * outside signer). It goes into a system workspace identified by {@code folderSystemKey}, created on first use and
 * limited to {@code managerRoles}. The caller owns and closes the stream.
 */
public record SystemDocumentCommand(
        String fileName,
        InputStream content,
        String source,
        String folderSystemKey,
        String folderName,
        List<String> managerRoles,
        String linkModel,
        UUID linkRecordId,
        LocalDate retentionUntil,
        String retentionReason,
        String createdBy) {
}
