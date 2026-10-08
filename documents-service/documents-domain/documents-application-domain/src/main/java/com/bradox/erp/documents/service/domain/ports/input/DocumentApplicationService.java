package com.bradox.erp.documents.service.domain.ports.input;

import com.bradox.erp.documents.service.domain.dto.AddLinkCommand;
import com.bradox.erp.documents.service.domain.dto.DocumentContent;
import com.bradox.erp.documents.service.domain.dto.DocumentResponse;
import com.bradox.erp.documents.service.domain.dto.DocumentSearchQuery;
import com.bradox.erp.documents.service.domain.dto.DocumentSummaryResponse;
import com.bradox.erp.documents.service.domain.dto.LockRetentionCommand;
import com.bradox.erp.documents.service.domain.dto.RestoreDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.SetTagsCommand;
import com.bradox.erp.documents.service.domain.dto.StorageStatsResponse;
import com.bradox.erp.documents.service.domain.dto.UpdateDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.UploadDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.UploadResultResponse;
import com.bradox.erp.documents.service.domain.dto.VersionResponse;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;

import java.util.List;
import java.util.UUID;

public interface DocumentApplicationService {

    PageResponse<DocumentSummaryResponse> search(CompanyId companyId, DocumentSearchQuery query);

    DocumentResponse get(CompanyId companyId, UUID id);

    UploadResultResponse upload(CompanyId companyId, UploadDocumentCommand command);

    UploadResultResponse uploadVersion(CompanyId companyId, UUID documentId, UploadDocumentCommand command);

    List<VersionResponse> versions(CompanyId companyId, UUID documentId);

    DocumentResponse restoreVersion(CompanyId companyId, UUID documentId, UUID versionId);

    /** Bytes of the current version, or of {@code versionId} when given. */
    DocumentContent content(CompanyId companyId, UUID documentId, UUID versionId);

    DocumentResponse update(CompanyId companyId, UUID id, UpdateDocumentCommand command);

    DocumentResponse setTags(CompanyId companyId, UUID id, SetTagsCommand command);

    DocumentResponse addLink(CompanyId companyId, UUID id, AddLinkCommand command);

    DocumentResponse removeLink(CompanyId companyId, UUID id, String modelName, UUID recordId);

    /** Documents linked to a business record. Visible through the record, not through folder access. */
    List<DocumentSummaryResponse> findByRecord(CompanyId companyId, String modelName, UUID recordId);

    void trash(CompanyId companyId, UUID id);

    DocumentResponse restore(CompanyId companyId, UUID id, RestoreDocumentCommand command);

    /** Permanently removes every trashed document that is not under retention. Returns how many. */
    int emptyTrash(CompanyId companyId);

    /** Scheduled purge of documents trashed more than 30 days ago, across companies. Returns how many. */
    int purgeExpiredTrash();

    /** Locks a document against deletion until a date. Used by other modules, for example signed PDFs. */
    DocumentResponse lockRetention(CompanyId companyId, UUID id, LockRetentionCommand command);

    StorageStatsResponse stats(CompanyId companyId);
}
