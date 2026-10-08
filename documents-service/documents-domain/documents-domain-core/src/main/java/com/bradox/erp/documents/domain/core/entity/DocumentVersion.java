package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;

import java.time.Instant;

/** One immutable stored file of a document (BR-DOC-03). */
public final class DocumentVersion {

    private final DocumentVersionId id;
    private final DocumentId documentId;
    private final int versionNo;
    private final String storageKey;
    private final String originalFileName;
    private final String contentType;
    private final long fileSize;
    private final String sha256;
    private final String uploadedBy;
    private final Instant uploadedAt;
    private final String comment;

    public DocumentVersion(DocumentVersionId id, DocumentId documentId, int versionNo, String storageKey,
                           String originalFileName, String contentType, long fileSize, String sha256,
                           String uploadedBy, Instant uploadedAt, String comment) {
        this.id = id;
        this.documentId = documentId;
        this.versionNo = versionNo;
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.comment = comment;
    }

    public DocumentVersionId getId() { return id; }
    public DocumentId getDocumentId() { return documentId; }
    public int getVersionNo() { return versionNo; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public String getSha256() { return sha256; }
    public String getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public String getComment() { return comment; }
}
