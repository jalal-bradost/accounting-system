package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Null fields are left unchanged. {@code moveToRoot} moves the folder to the top level. */
public record UpdateFolderCommand(
        @Size(max = 255) String name,
        UUID parentId,
        Boolean moveToRoot,
        Integer sequence,
        String linkedModel) {
}
