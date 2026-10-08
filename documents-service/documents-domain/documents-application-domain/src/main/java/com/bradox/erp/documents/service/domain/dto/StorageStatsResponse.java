package com.bradox.erp.documents.service.domain.dto;

public record StorageStatsResponse(long usedBytes, long quotaBytes, long documentCount, long maxFileBytes) {
}
