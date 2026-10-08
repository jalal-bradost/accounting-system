package com.bradox.erp.documents.service.domain.ports.output.repository;

import com.bradox.erp.documents.domain.core.valueobject.DocumentStatus;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything already resolved by the service: access (folderIds), subfolders, tag text matches
 * and content type groups. The repository only translates it to a query.
 */
public record DocumentSearchCriteria(
        CompanyId companyId,
        DocumentStatus status,
        String textNormalized,
        Set<UUID> textTagIds,
        Set<UUID> folderIds,
        List<Set<UUID>> tagGroups,
        Set<String> contentTypes,
        String contentTypePrefix,
        String createdBy,
        Instant from,
        Instant to,
        String linkedModel,
        String sortField,
        boolean sortDescending,
        int page,
        int size) {
}
