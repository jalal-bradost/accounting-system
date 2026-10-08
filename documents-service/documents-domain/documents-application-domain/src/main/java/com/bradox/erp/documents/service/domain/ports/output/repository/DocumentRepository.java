package com.bradox.erp.documents.service.domain.ports.output.repository;

import com.bradox.erp.documents.domain.core.entity.Document;
import com.bradox.erp.documents.domain.core.entity.DocumentVersion;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

    /** Saves the document with its tags and links, and its pending version if there is one. */
    Document save(Document document);

    Optional<Document> findById(DocumentId id);

    DocumentPage search(DocumentSearchCriteria criteria);

    List<DocumentVersion> findVersions(DocumentId documentId);

    Optional<DocumentVersion> findVersion(DocumentId documentId, DocumentVersionId versionId);

    int countVersions(DocumentId documentId);

    /** An active document in the folder whose current version has the same SHA-256. */
    Optional<Document> findDuplicate(CompanyId companyId, FolderId folderId, String sha256, DocumentId exclude);

    /** Active documents linked to a record. */
    List<Document> findByRecord(CompanyId companyId, String modelName, UUID recordId);

    long sumCurrentSizes(CompanyId companyId);

    long countActive(CompanyId companyId);

    List<Document> findTrashed(CompanyId companyId);

    List<Document> findTrashedBefore(Instant cutoff, int limit);

    /** Removes the document, its versions, tags and links. Returns the storage keys that can now be deleted. */
    List<String> deleteHard(Document document);
}
