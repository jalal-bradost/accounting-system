package com.bradox.erp.documents.service.domain.dto;

import java.util.UUID;

/** A folder as seen by the current user. {@code level} is the user's effective access level. */
public record FolderResponse(
        UUID id,
        UUID parentId,
        String name,
        int sequence,
        boolean archived,
        boolean inheritAccess,
        boolean restricted,
        String linkedModel,
        String systemKey,
        String level) {
}
