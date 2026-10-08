package com.bradox.erp.documents.service.domain.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Search filters. Tags in the same facet are OR-ed, different facets are AND-ed. {@code type} is
 * one of pdf, image, office, text, archive or a full content type. {@code status} is ACTIVE
 * (default) or TRASHED.
 */
public record DocumentSearchQuery(
        String q,
        UUID folderId,
        boolean includeSubfolders,
        List<UUID> tagIds,
        String type,
        String createdBy,
        LocalDate from,
        LocalDate to,
        String linkedModel,
        String status,
        String sort,
        int page,
        int size) {

    public static final int MAX_PAGE_SIZE = 200;

    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return size <= 0 ? 50 : Math.min(size, MAX_PAGE_SIZE);
    }
}
