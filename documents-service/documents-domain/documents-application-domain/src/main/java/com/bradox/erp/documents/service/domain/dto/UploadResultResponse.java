package com.bradox.erp.documents.service.domain.dto;

/**
 * Result of an upload. CREATED means a document or version was stored. DUPLICATE means the same
 * bytes already exist and nothing was stored; {@code existing} points to the duplicate.
 */
public record UploadResultResponse(String status, DocumentResponse document, DocumentSummaryResponse existing) {

    public static final String CREATED = "CREATED";
    public static final String DUPLICATE = "DUPLICATE";
}
