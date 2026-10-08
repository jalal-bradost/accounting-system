package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateFolderAccessCommand(boolean inheritAccess, @Valid List<GrantCommand> grants) {
}
