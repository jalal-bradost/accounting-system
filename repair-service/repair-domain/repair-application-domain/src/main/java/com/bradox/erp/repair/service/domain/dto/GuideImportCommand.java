package com.bradox.erp.repair.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code allOrNothing}: when true, one bad row rejects the whole file; otherwise valid rows are imported. */
public record GuideImportCommand(@NotBlank String csv, boolean allOrNothing) {
}
