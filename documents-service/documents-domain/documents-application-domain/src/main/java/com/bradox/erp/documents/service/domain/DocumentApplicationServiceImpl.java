package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Document;
import com.bradox.erp.documents.domain.core.entity.DocumentVersion;
import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.RecordLink;
import com.bradox.erp.documents.domain.core.entity.Tag;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.rule.FileTypePolicy;
import com.bradox.erp.documents.domain.core.rule.NameNormalizer;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentSource;
import com.bradox.erp.documents.domain.core.valueobject.DocumentStatus;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.documents.service.domain.DocumentAccessResolver.Access;
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
import com.bradox.erp.documents.service.domain.ports.input.DocumentApplicationService;
import com.bradox.erp.documents.service.domain.ports.output.RecordLookupPort;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentPage;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentRepository;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentSearchCriteria;
import com.bradox.erp.documents.service.domain.ports.output.repository.TagRepository;
import com.bradox.erp.documents.service.domain.ports.output.storage.DocumentStoragePort;
import com.bradox.erp.documents.service.domain.ports.output.storage.StoredObject;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
class DocumentApplicationServiceImpl implements DocumentApplicationService {

    static final String AUDIT_MODEL = "documents.document";
    static final int MAX_VERSIONS = 50;
    static final int TRASH_RETENTION_DAYS = 30;
    static final int PURGE_BATCH = 1000;

    private final DocumentRepository documents;
    private final TagRepository tags;
    private final DocumentStoragePort storage;
    private final RecordLookupPort records;
    private final DocumentAccessResolver access;
    private final AuditLogPort audit;
    private final CompanyContext context;
    private final Clock clock;
    private final long maxBytes;
    private final long quotaBytes;

    @Autowired
    DocumentApplicationServiceImpl(DocumentRepository documents, TagRepository tags, DocumentStoragePort storage,
                                   RecordLookupPort records, DocumentAccessResolver access, AuditLogPort audit,
                                   CompanyContext context, ObjectProvider<Clock> clockProvider,
                                   @Value("${app.documents.max-bytes:26214400}") long maxBytes,
                                   @Value("${app.documents.quota-bytes:0}") long quotaBytes) {
        this(documents, tags, storage, records, access, audit, context,
                clockProvider.getIfAvailable(Clock::systemUTC), maxBytes, quotaBytes);
    }

    DocumentApplicationServiceImpl(DocumentRepository documents, TagRepository tags, DocumentStoragePort storage,
                                   RecordLookupPort records, DocumentAccessResolver access, AuditLogPort audit,
                                   CompanyContext context, Clock clock, long maxBytes, long quotaBytes) {
        this.documents = documents;
        this.tags = tags;
        this.storage = storage;
        this.records = records;
        this.access = access;
        this.audit = audit;
        this.context = context;
        this.clock = clock;
        this.maxBytes = maxBytes;
        this.quotaBytes = quotaBytes;
    }

    // ---------------------------------------------------------------- search and read

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentSummaryResponse> search(CompanyId companyId, DocumentSearchQuery query) {
        Access acc = access.load(companyId);
        Set<UUID> folderIds = acc.visibleFolderIds();
        if (query.folderId() != null) {
            FolderId fid = new FolderId(query.folderId());
            acc.require(fid, AccessLevel.VIEW);
            Set<UUID> scope = new HashSet<>();
            if (query.includeSubfolders()) {
                acc.tree().subtreeIds(fid).forEach(f -> scope.add(f.getId()));
            } else {
                scope.add(query.folderId());
            }
            if (folderIds != null) {
                scope.retainAll(folderIds);
            }
            folderIds = scope;
        }

        String text = NameNormalizer.normalize(query.q());
        Set<UUID> textTagIds = new HashSet<>();
        if (!text.isEmpty()) {
            for (Tag t : tags.findTags(companyId)) {
                if (NameNormalizer.normalize(t.getName()).contains(text)) {
                    textTagIds.add(t.getId().getId());
                }
            }
        }
        List<Set<UUID>> groups = tagGroups(query.tagIds());
        TypeFilter type = typeFilter(query.type());
        String sort = query.sort() == null ? "" : query.sort();
        boolean desc = sort.startsWith("-");
        String sortField = desc ? sort.substring(1) : sort;
        DocumentStatus status = "TRASHED".equalsIgnoreCase(query.status()) ? DocumentStatus.TRASHED : DocumentStatus.ACTIVE;

        DocumentSearchCriteria criteria = new DocumentSearchCriteria(
                companyId, status, text, textTagIds, folderIds, groups, type.exact(), type.prefix(),
                blankToNull(query.createdBy()),
                query.from() == null ? null : query.from().atStartOfDay(ZoneOffset.UTC).toInstant(),
                query.to() == null ? null : query.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                blankToNull(query.linkedModel()), sortField, desc, query.safePage(), query.safeSize());
        DocumentPage page = documents.search(criteria);
        List<DocumentSummaryResponse> content = page.content().stream()
                .map(d -> DocumentsMapper.summary(d, acc.tree())).toList();
        int totalPages = (int) Math.ceil(page.totalElements() / (double) query.safeSize());
        return new PageResponse<>(content, query.safePage(), query.safeSize(), page.totalElements(), totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse get(CompanyId companyId, UUID id) {
        Access acc = access.load(companyId);
        Document doc = loadVisible(acc, companyId, id);
        return detail(doc, acc, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionResponse> versions(CompanyId companyId, UUID documentId) {
        Access acc = access.load(companyId);
        Document doc = loadVisible(acc, companyId, documentId);
        return documents.findVersions(doc.getId()).stream()
                .map(v -> DocumentsMapper.version(v, v.getId().equals(doc.getCurrentVersionId())))
                .toList();
    }

    @Override
    @Transactional
    public DocumentContent content(CompanyId companyId, UUID documentId, UUID versionId) {
        Access acc = access.load(companyId);
        Document doc = loadVisible(acc, companyId, documentId);
        DocumentVersionId vid = versionId != null ? new DocumentVersionId(versionId) : doc.getCurrentVersionId();
        DocumentVersion version = documents.findVersion(doc.getId(), vid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, documentId, "Downloaded version " + version.getVersionNo(),
                Map.of("fileName", version.getOriginalFileName()));
        return new DocumentContent(storage.open(version.getStorageKey()), version.getOriginalFileName(),
                version.getContentType(), version.getFileSize(), FileTypePolicy.isInlineSafe(version.getContentType()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> findByRecord(CompanyId companyId, String modelName, UUID recordId) {
        Access acc = access.load(companyId);
        if (!records.supports(modelName)) {
            throw new DocumentsDomainException("error.documents.modelNotSupported", new Object[]{modelName},
                    "Documents cannot be linked to " + modelName);
        }
        Optional<String> permission = records.readPermission(modelName);
        if (permission.isPresent() && !access.hasPermission(acc.user(), permission.get())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot open this record");
        }
        return documents.findByRecord(companyId, modelName, recordId).stream()
                .map(d -> {
                    DocumentSummaryResponse s = DocumentsMapper.summary(d, acc.tree());
                    return acc.level(d.getFolderId()) == AccessLevel.NONE ? withoutFolderName(s) : s;
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StorageStatsResponse stats(CompanyId companyId) {
        return new StorageStatsResponse(documents.sumCurrentSizes(companyId), quotaBytes,
                documents.countActive(companyId), maxBytes);
    }

    // ---------------------------------------------------------------- upload and versions

    @Override
    @Transactional
    public UploadResultResponse upload(CompanyId companyId, UploadDocumentCommand command) {
        Access acc = access.load(companyId);
        if (command.folderId() == null) {
            throw new DocumentsDomainException("error.documents.folderRequired", null, "A folder is required");
        }
        FolderId folderId = new FolderId(command.folderId());
        Folder folder = acc.tree().get(folderId);
        acc.require(folderId, AccessLevel.EDIT);
        if (folder.isArchived()) {
            throw new DocumentsDomainException("error.documents.folderArchived", null, "The folder is archived");
        }
        checkQuota(companyId);
        Verified verified = storeVerified(command);
        try {
            if (!command.allowDuplicate()) {
                Optional<Document> duplicate = documents.findDuplicate(companyId, folderId, verified.stored().sha256(), null);
                if (duplicate.isPresent()) {
                    storage.delete(verified.stored().key());
                    return new UploadResultResponse(UploadResultResponse.DUPLICATE, null,
                            DocumentsMapper.summary(duplicate.get(), acc.tree()));
                }
            }
            Instant now = Instant.now(clock);
            String user = context.currentUserDisplay();
            Document doc = Document.create(new DocumentId(UUID.randomUUID()), companyId, folderId, verified.fileName(),
                    null, DocumentSource.UPLOAD, context.currentUser().map(u -> u.getId()).orElse(null), user, now);
            doc.addVersion(newVersion(doc, verified, command.comment(), user, now), user, now);
            Document saved = documents.save(doc);
            deleteFileOnRollback(verified.stored().key());
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, saved.getId().getId(), "Document uploaded",
                    Map.of("fileName", verified.fileName(), "size", verified.stored().size()));
            return new UploadResultResponse(UploadResultResponse.CREATED, detail(saved, acc, companyId), null);
        } catch (RuntimeException e) {
            storage.delete(verified.stored().key());
            throw e;
        }
    }

    @Override
    @Transactional
    public UploadResultResponse uploadVersion(CompanyId companyId, UUID documentId, UploadDocumentCommand command) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, documentId);
        if (documents.countVersions(doc.getId()) >= MAX_VERSIONS) {
            throw new DocumentsDomainException("error.documents.tooManyVersions", new Object[]{MAX_VERSIONS},
                    "A document can have at most " + MAX_VERSIONS + " versions");
        }
        checkQuota(companyId);
        Verified verified = storeVerified(command);
        try {
            if (!command.allowDuplicate() && verified.stored().sha256().equals(doc.getSha256())) {
                storage.delete(verified.stored().key());
                return new UploadResultResponse(UploadResultResponse.DUPLICATE, null,
                        DocumentsMapper.summary(doc, acc.tree()));
            }
            Instant now = Instant.now(clock);
            String user = context.currentUserDisplay();
            doc.addVersion(newVersion(doc, verified, command.comment(), user, now), user, now);
            Document saved = documents.save(doc);
            deleteFileOnRollback(verified.stored().key());
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, documentId, "New version " + saved.getVersionNo(),
                    Map.of("fileName", verified.fileName(), "size", verified.stored().size()));
            return new UploadResultResponse(UploadResultResponse.CREATED, detail(saved, acc, companyId), null);
        } catch (RuntimeException e) {
            storage.delete(verified.stored().key());
            throw e;
        }
    }

    @Override
    @Transactional
    public DocumentResponse restoreVersion(CompanyId companyId, UUID documentId, UUID versionId) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, documentId);
        DocumentVersion old = documents.findVersion(doc.getId(), new DocumentVersionId(versionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
        if (documents.countVersions(doc.getId()) >= MAX_VERSIONS) {
            throw new DocumentsDomainException("error.documents.tooManyVersions", new Object[]{MAX_VERSIONS},
                    "A document can have at most " + MAX_VERSIONS + " versions");
        }
        checkQuota(companyId);
        Instant now = Instant.now(clock);
        String user = context.currentUserDisplay();
        String newKey = storage.copy(old.getStorageKey());
        try {
            DocumentVersion restored = new DocumentVersion(new DocumentVersionId(UUID.randomUUID()), doc.getId(),
                    doc.nextVersionNo(), newKey, old.getOriginalFileName(), old.getContentType(), old.getFileSize(),
                    old.getSha256(), user, now, "Restored from version " + old.getVersionNo());
            doc.addVersion(restored, user, now);
            Document saved = documents.save(doc);
            deleteFileOnRollback(newKey);
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, documentId,
                    "Restored version " + old.getVersionNo() + " as version " + saved.getVersionNo(), Map.of());
            return detail(saved, acc, companyId);
        } catch (RuntimeException e) {
            storage.delete(newKey);
            throw e;
        }
    }

    // ---------------------------------------------------------------- edit

    @Override
    @Transactional
    public DocumentResponse update(CompanyId companyId, UUID id, UpdateDocumentCommand command) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, id);
        Instant now = Instant.now(clock);
        String user = context.currentUserDisplay();
        Map<String, Object> changes = new LinkedHashMap<>();
        if (command.name() != null && !command.name().equals(doc.getName())) {
            doc.rename(command.name(), user, now);
            changes.put("name", command.name());
        }
        if (command.description() != null) {
            doc.describe(command.description(), user, now);
            changes.put("description", "changed");
        }
        if (command.folderId() != null && !command.folderId().equals(doc.getFolderId().getId())) {
            FolderId target = new FolderId(command.folderId());
            acc.require(target, AccessLevel.EDIT);
            if (acc.tree().get(target).isArchived()) {
                throw new DocumentsDomainException("error.documents.folderArchived", null, "The folder is archived");
            }
            doc.moveTo(target, user, now);
            changes.put("folderId", command.folderId());
        }
        Document saved = documents.save(doc);
        if (!changes.isEmpty()) {
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Document updated", changes);
        }
        return detail(saved, acc, companyId);
    }

    @Override
    @Transactional
    public DocumentResponse setTags(CompanyId companyId, UUID id, SetTagsCommand command) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, id);
        Set<TagId> wanted = new HashSet<>();
        command.tagIds().forEach(t -> wanted.add(new TagId(t)));
        List<Tag> found = tags.findTagsByIds(wanted);
        if (found.size() != wanted.size() || found.stream().anyMatch(t -> !t.getCompanyId().equals(companyId))) {
            throw new DocumentsDomainException("error.documents.tagNotFound", null, "Unknown tag");
        }
        doc.replaceTags(wanted, context.currentUserDisplay(), Instant.now(clock));
        Document saved = documents.save(doc);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Tags changed", Map.of("tags", wanted.size()));
        return detail(saved, acc, companyId);
    }

    @Override
    @Transactional
    public DocumentResponse addLink(CompanyId companyId, UUID id, AddLinkCommand command) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, id);
        if (!records.supports(command.modelName())) {
            throw new DocumentsDomainException("error.documents.modelNotSupported", new Object[]{command.modelName()},
                    "Documents cannot be linked to " + command.modelName());
        }
        if (!records.exists(companyId, command.modelName(), command.recordId())) {
            throw new DocumentsDomainException("error.documents.recordNotFound", null, "The record does not exist");
        }
        Instant now = Instant.now(clock);
        String user = context.currentUserDisplay();
        if (doc.addLink(new RecordLink(command.modelName(), command.recordId(), now, user), user, now)) {
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Linked to " + command.modelName(),
                    Map.of("recordId", command.recordId()));
        }
        return detail(documents.save(doc), acc, companyId);
    }

    @Override
    @Transactional
    public DocumentResponse removeLink(CompanyId companyId, UUID id, String modelName, UUID recordId) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, id);
        if (doc.removeLink(modelName, recordId, context.currentUserDisplay(), Instant.now(clock))) {
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Unlinked from " + modelName,
                    Map.of("recordId", recordId));
        }
        return detail(documents.save(doc), acc, companyId);
    }

    // ---------------------------------------------------------------- trash and retention

    @Override
    @Transactional
    public void trash(CompanyId companyId, UUID id) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, id);
        doc.trash(context.currentUserDisplay(), Instant.now(clock), LocalDate.now(clock));
        documents.save(doc);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Moved to trash", Map.of("name", doc.getName()));
    }

    @Override
    @Transactional
    public DocumentResponse restore(CompanyId companyId, UUID id, RestoreDocumentCommand command) {
        Access acc = access.load(companyId);
        Document doc = loadVisible(acc, companyId, id);
        if (doc.getStatus() != DocumentStatus.TRASHED) {
            throw new DocumentsDomainException("error.documents.notTrashed", null, "Document is not in the trash");
        }
        FolderId target = command != null && command.targetFolderId() != null
                ? new FolderId(command.targetFolderId()) : doc.getFolderId();
        acc.require(target, AccessLevel.EDIT);
        if (acc.tree().get(target).isArchived()) {
            throw new DocumentsDomainException("error.documents.folderArchived", null,
                    "The original folder is archived. Choose another folder");
        }
        Instant now = Instant.now(clock);
        String user = context.currentUserDisplay();
        doc.restoreFromTrash(user, now);
        if (!target.equals(doc.getFolderId())) {
            doc.moveTo(target, user, now);
        }
        Document saved = documents.save(doc);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Restored from trash", Map.of("name", doc.getName()));
        return detail(saved, acc, companyId);
    }

    @Override
    @Transactional
    public int emptyTrash(CompanyId companyId) {
        LocalDate today = LocalDate.now(clock);
        int purged = 0;
        for (Document doc : documents.findTrashed(companyId)) {
            if (doc.canBePurged(today)) {
                purge(doc);
                purged++;
            }
        }
        return purged;
    }

    @Override
    @Transactional
    public int purgeExpiredTrash() {
        LocalDate today = LocalDate.now(clock);
        Instant cutoff = Instant.now(clock).minus(Duration.ofDays(TRASH_RETENTION_DAYS));
        int purged = 0;
        for (Document doc : documents.findTrashedBefore(cutoff, PURGE_BATCH)) {
            if (doc.canBePurged(today)) {
                purge(doc);
                purged++;
            }
        }
        return purged;
    }

    @Override
    @Transactional
    public DocumentResponse lockRetention(CompanyId companyId, UUID id, LockRetentionCommand command) {
        Access acc = access.load(companyId);
        Document doc = loadForWrite(acc, companyId, id);
        doc.lockUntil(command.until(), command.reason(), context.currentUserDisplay(), Instant.now(clock));
        Document saved = documents.save(doc);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Retention lock until " + command.until(),
                Map.of("reason", command.reason() == null ? "" : command.reason()));
        return detail(saved, acc, companyId);
    }

    private void purge(Document doc) {
        List<String> keys = documents.deleteHard(doc);
        runAfterCommit(() -> keys.forEach(storage::delete));
        audit.recordBusinessEvent(doc.getCompanyId(), AUDIT_MODEL, doc.getId().getId(), "Permanently deleted",
                Map.of("name", doc.getName()));
    }

    // ---------------------------------------------------------------- helpers

    private Document loadVisible(Access acc, CompanyId companyId, UUID id) {
        Document doc = documents.findById(new DocumentId(id))
                .filter(d -> d.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (documentLevel(acc, doc) == AccessLevel.NONE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return doc;
    }

    /** Loads the document and requires EDIT on its folder. Access through a record link is read-only. */
    private Document loadForWrite(Access acc, CompanyId companyId, UUID id) {
        Document doc = loadVisible(acc, companyId, id);
        if (!acc.level(doc.getFolderId()).atLeast(AccessLevel.EDIT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can view this document but not change it");
        }
        return doc;
    }

    /** Folder level, or VIEW when the user can open a record the document is linked to. */
    private AccessLevel documentLevel(Access acc, Document doc) {
        AccessLevel level = acc.level(doc.getFolderId());
        if (level.atLeast(AccessLevel.VIEW)) {
            return level;
        }
        for (RecordLink link : doc.getLinks()) {
            Optional<String> permission = records.readPermission(link.getModelName());
            if (permission.isPresent() && access.hasPermission(acc.user(), permission.get())) {
                return AccessLevel.VIEW;
            }
        }
        return AccessLevel.NONE;
    }

    private DocumentResponse detail(Document doc, Access acc, CompanyId companyId) {
        Map<String, String> labels = new HashMap<>();
        for (RecordLink link : doc.getLinks()) {
            records.label(companyId, link.getModelName(), link.getRecordId())
                    .ifPresent(l -> labels.put(DocumentsMapper.linkKey(link.getModelName(), link.getRecordId()), l));
        }
        return DocumentsMapper.detail(doc, acc.tree(), documentLevel(acc, doc), labels);
    }

    private DocumentSummaryResponse withoutFolderName(DocumentSummaryResponse s) {
        return new DocumentSummaryResponse(s.id(), s.folderId(), null, s.name(), s.fileName(), s.contentType(),
                s.sizeBytes(), s.versionNo(), s.status(), s.tagIds(), s.linkCount(), s.retentionUntil(),
                s.trashedAt(), s.trashedBy(), s.createdBy(), s.createdAt(), s.updatedBy(), s.updatedAt(), s.previewable());
    }

    private void checkQuota(CompanyId companyId) {
        if (quotaBytes > 0 && documents.sumCurrentSizes(companyId) >= quotaBytes) {
            throw new DocumentsDomainException("error.documents.quotaReached", null,
                    "The document storage quota has been reached");
        }
    }

    private record Verified(StoredObject stored, String contentType, String fileName) {
    }

    private Verified storeVerified(UploadDocumentCommand command) {
        String fileName = baseName(command.fileName());
        if (fileName.isBlank()) {
            throw new DocumentsDomainException("error.documents.fileNameRequired", null, "File name is required");
        }
        if (command.content() == null) {
            throw new DocumentsDomainException("error.documents.fileRequired", null, "File is required");
        }
        StoredObject stored = storage.store(command.content(), maxBytes);
        Optional<String> type = FileTypePolicy.detect(stored.head(), stored.headLength(), fileName);
        if (type.isEmpty()) {
            storage.delete(stored.key());
            throw new DocumentsDomainException("error.documents.fileTypeNotAllowed", null,
                    "This file type is not allowed, or the content does not match its extension");
        }
        return new Verified(stored, type.get(), fileName);
    }

    private DocumentVersion newVersion(Document doc, Verified v, String comment, String user, Instant now) {
        return new DocumentVersion(new DocumentVersionId(UUID.randomUUID()), doc.getId(), doc.nextVersionNo(),
                v.stored().key(), v.fileName(), v.contentType(), v.stored().size(), v.stored().sha256(),
                user, now, blankToNull(comment));
    }

    private void deleteFileOnRollback(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        storage.delete(key);
                    }
                }
            });
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private List<Set<UUID>> tagGroups(List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<TagId> ids = tagIds.stream().map(TagId::new).toList();
        Map<UUID, Set<UUID>> byFacet = new HashMap<>();
        for (Tag t : tags.findTagsByIds(ids)) {
            byFacet.computeIfAbsent(t.getFacetId().getId(), k -> new HashSet<>()).add(t.getId().getId());
        }
        return new ArrayList<>(byFacet.values());
    }

    private record TypeFilter(Set<String> exact, String prefix) {
    }

    private static TypeFilter typeFilter(String type) {
        if (type == null || type.isBlank()) {
            return new TypeFilter(null, null);
        }
        return switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "pdf" -> new TypeFilter(Set.of(FileTypePolicy.PDF), null);
            case "image" -> new TypeFilter(null, "image/");
            case "office" -> new TypeFilter(Set.of(FileTypePolicy.DOCX, FileTypePolicy.XLSX, FileTypePolicy.PPTX,
                    FileTypePolicy.DOC, FileTypePolicy.XLS, FileTypePolicy.PPT), null);
            case "text" -> new TypeFilter(Set.of(FileTypePolicy.TXT, FileTypePolicy.CSV, FileTypePolicy.XML,
                    FileTypePolicy.JSON), null);
            case "archive" -> new TypeFilter(Set.of(FileTypePolicy.ZIP), null);
            default -> new TypeFilter(Set.of(type.toLowerCase(java.util.Locale.ROOT)), null);
        };
    }

    static String baseName(String fileName) {
        if (fileName == null) {
            return "";
        }
        String s = fileName.replace('\\', '/');
        int slash = s.lastIndexOf('/');
        return (slash >= 0 ? s.substring(slash + 1) : s).trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
