package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFolderCommand(UUID parentId, @NotBlank @Size(max = 255) String name, String linkedModel) {
}
