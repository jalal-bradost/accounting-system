package com.bradox.erp.documents.service.domain.dto;

import java.util.List;
import java.util.UUID;

/** Access settings of one folder. {@code governedBy} is the folder whose grants apply when this one inherits. */
public record FolderAccessResponse(
        boolean inheritAccess,
        boolean restricted,
        UUID governedByFolderId,
        String governedByFolderName,
        List<GrantResponse> grants) {
}
