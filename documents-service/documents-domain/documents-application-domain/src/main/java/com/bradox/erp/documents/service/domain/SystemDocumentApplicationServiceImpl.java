package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Document;
import com.bradox.erp.documents.domain.core.entity.DocumentVersion;
import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.entity.RecordLink;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.rule.FileTypePolicy;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentSource;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.documents.service.domain.dto.DocumentContent;
import com.bradox.erp.documents.service.domain.dto.SystemDocumentCommand;
import com.bradox.erp.documents.service.domain.ports.input.SystemDocumentApplicationService;
import com.bradox.erp.documents.service.domain.ports.output.AccessSubjectPort;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentRepository;
import com.bradox.erp.documents.service.domain.ports.output.repository.FolderRepository;
import com.bradox.erp.documents.service.domain.ports.output.storage.DocumentStoragePort;
import com.bradox.erp.documents.service.domain.ports.output.storage.StoredObject;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class SystemDocumentApplicationServiceImpl implements SystemDocumentApplicationService {

    private final DocumentRepository documents;
    private final FolderRepository folders;
    private final DocumentStoragePort storage;
    private final AccessSubjectPort subjects;
    private final AuditLogPort audit;
    private final Clock clock;
    private final long maxBytes;

    @Autowired
    SystemDocumentApplicationServiceImpl(DocumentRepository documents, FolderRepository folders, DocumentStoragePort storage,
                                         AccessSubjectPort subjects, AuditLogPort audit, ObjectProvider<Clock> clock,
                                         @Value("${app.documents.max-bytes:26214400}") long maxBytes) {
        this.documents = documents;
        this.folders = folders;
        this.storage = storage;
        this.subjects = subjects;
        this.audit = audit;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
        this.maxBytes = maxBytes;
    }

    @Override
    @Transactional
    public UUID store(CompanyId companyId, SystemDocumentCommand c) {
        FolderId folderId = ensureFolder(companyId, c.folderSystemKey(), c.folderName(), c.managerRoles());
        String fileName = DocumentApplicationServiceImpl.baseName(c.fileName());
        if (fileName.isBlank() || c.content() == null) {
            throw new DocumentsDomainException("error.documents.fileRequired", null, "File is required");
        }
        StoredObject stored = storage.store(c.content(), maxBytes);
        try {
            String type = FileTypePolicy.detect(stored.head(), stored.headLength(), fileName).orElseThrow(
                    () -> new DocumentsDomainException("error.documents.fileTypeNotAllowed", null,
                            "This file type is not allowed, or the content does not match its extension"));
            Instant now = Instant.now(clock);
            String by = c.createdBy() == null ? "system" : c.createdBy();
            Document doc = Document.create(new DocumentId(UUID.randomUUID()), companyId, folderId, fileName, null,
                    DocumentSource.valueOf(c.source()), null, by, now);
            doc.addVersion(new DocumentVersion(new DocumentVersionId(UUID.randomUUID()), doc.getId(), doc.nextVersionNo(), stored.key(),
                    fileName, type, stored.size(), stored.sha256(), by, now, null), by, now);
            if (c.linkModel() != null && c.linkRecordId() != null) {
                doc.addLink(new RecordLink(c.linkModel(), c.linkRecordId(), now, by), by, now);
            }
            if (c.retentionUntil() != null) {
                doc.lockUntil(c.retentionUntil(), c.retentionReason(), by, now);
            }
            Document saved = documents.save(doc);
            audit.recordBusinessEvent(companyId, DocumentApplicationServiceImpl.AUDIT_MODEL, saved.getId().getId(), "Stored by system",
                    Map.of("fileName", fileName, "source", c.source()));
            return saved.getId().getId();
        } catch (RuntimeException e) {
            storage.delete(stored.key());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentContent read(CompanyId companyId, UUID documentId, UUID versionId) {
        Document doc = documents.findById(new DocumentId(documentId)).filter(d -> d.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        DocumentVersionId vid = versionId != null ? new DocumentVersionId(versionId) : doc.getCurrentVersionId();
        DocumentVersion v = documents.findVersion(doc.getId(), vid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
        return new DocumentContent(storage.open(v.getStorageKey()), v.getOriginalFileName(), v.getContentType(), v.getFileSize(),
                FileTypePolicy.isInlineSafe(v.getContentType()));
    }

    private FolderId ensureFolder(CompanyId companyId, String systemKey, String name, List<String> managerRoles) {
        List<Folder> all = folders.findAll(companyId);
        for (Folder f : all) {
            if (systemKey.equals(f.getSystemKey())) {
                return f.getId();
            }
        }
        int sequence = all.stream().mapToInt(Folder::getSequence).max().orElse(0) + 1;
        Folder folder = Folder.create(new FolderId(UUID.randomUUID()), companyId, null, name, sequence, null, systemKey, "system",
                Instant.now(clock));
        List<FolderAccessGrant> grants = new ArrayList<>();
        Map<String, UUID> roles = new java.util.HashMap<>();
        for (AccessSubjectResponse s : subjects.listSubjects(companyId)) {
            if ("ROLE".equals(s.type())) {
                roles.put(s.name(), s.id());
            }
        }
        for (String role : managerRoles == null ? List.<String>of() : managerRoles) {
            UUID id = roles.get(role);
            if (id != null) {
                grants.add(new FolderAccessGrant(SubjectType.ROLE, id, AccessLevel.MANAGE));
            }
        }
        if (!grants.isEmpty()) {
            folder.changeInheritAccess(false);
        }
        Folder saved = folders.save(folder);
        if (!grants.isEmpty()) {
            folders.replaceGrants(companyId, saved.getId(), grants);
        }
        return saved.getId();
    }
}
