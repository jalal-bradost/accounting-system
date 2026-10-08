package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.rule.FolderTreeRules;
import com.bradox.erp.documents.domain.core.rule.NameNormalizer;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentSource;
import com.bradox.erp.documents.domain.core.valueobject.DocumentStatus;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A logical file with a current version. Rename, move, tags and links belong to the document,
 * file bytes belong to its versions. The current version's details are kept on the document so
 * lists and searches do not need to load versions.
 */
public class Document extends AggregateRoot<DocumentId> {

    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    private CompanyId companyId;
    private FolderId folderId;
    private String name;
    private String nameNormalized;
    private String description;
    private DocumentStatus status;
    private DocumentSource source;
    private UUID ownerUserId;

    private DocumentVersionId currentVersionId;
    private int versionNo;
    private String fileName;
    private String contentType;
    private long sizeBytes;
    private String sha256;

    private Instant trashedAt;
    private String trashedBy;
    private LocalDate retentionUntil;
    private String retentionReason;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    private Set<TagId> tagIds = new LinkedHashSet<>();
    private List<RecordLink> links = new ArrayList<>();
    private DocumentVersion pendingVersion;

    private Document() {
    }

    public static Document create(DocumentId id, CompanyId companyId, FolderId folderId, String name,
                                  String description, DocumentSource source, UUID ownerUserId,
                                  String createdBy, Instant now) {
        if (companyId == null || folderId == null) {
            throw new DocumentsDomainException("error.documents.folderRequired", null, "A folder is required");
        }
        FolderTreeRules.validateName(name);
        Document d = new Document();
        d.setId(id);
        d.companyId = companyId;
        d.folderId = folderId;
        d.name = name.trim();
        d.nameNormalized = NameNormalizer.normalize(name);
        d.description = checkDescription(description);
        d.status = DocumentStatus.ACTIVE;
        d.source = source == null ? DocumentSource.UPLOAD : source;
        d.ownerUserId = ownerUserId;
        d.createdAt = now;
        d.createdBy = createdBy;
        d.updatedAt = now;
        d.updatedBy = createdBy;
        return d;
    }

    public static Document restore(DocumentId id, CompanyId companyId, FolderId folderId, String name,
                                   String nameNormalized, String description, DocumentStatus status,
                                   DocumentSource source, UUID ownerUserId, DocumentVersionId currentVersionId,
                                   int versionNo, String fileName, String contentType, long sizeBytes, String sha256,
                                   Instant trashedAt, String trashedBy, LocalDate retentionUntil,
                                   String retentionReason, Instant createdAt, String createdBy,
                                   Instant updatedAt, String updatedBy, Set<TagId> tagIds, List<RecordLink> links) {
        Document d = new Document();
        d.setId(id);
        d.companyId = companyId;
        d.folderId = folderId;
        d.name = name;
        d.nameNormalized = nameNormalized;
        d.description = description;
        d.status = status;
        d.source = source;
        d.ownerUserId = ownerUserId;
        d.currentVersionId = currentVersionId;
        d.versionNo = versionNo;
        d.fileName = fileName;
        d.contentType = contentType;
        d.sizeBytes = sizeBytes;
        d.sha256 = sha256;
        d.trashedAt = trashedAt;
        d.trashedBy = trashedBy;
        d.retentionUntil = retentionUntil;
        d.retentionReason = retentionReason;
        d.createdAt = createdAt;
        d.createdBy = createdBy;
        d.updatedAt = updatedAt;
        d.updatedBy = updatedBy;
        d.tagIds = tagIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tagIds);
        d.links = links == null ? new ArrayList<>() : new ArrayList<>(links);
        return d;
    }

    /** Next version number a new upload would get. */
    public int nextVersionNo() {
        return versionNo + 1;
    }

    public void addVersion(DocumentVersion version, String by, Instant now) {
        assertActive();
        if (version.getVersionNo() != nextVersionNo()) {
            throw new DocumentsDomainException("error.documents.versionOutOfOrder", null,
                    "Version number must be " + nextVersionNo());
        }
        this.currentVersionId = version.getId();
        this.versionNo = version.getVersionNo();
        this.fileName = version.getOriginalFileName();
        this.contentType = version.getContentType();
        this.sizeBytes = version.getFileSize();
        this.sha256 = version.getSha256();
        this.pendingVersion = version;
        touch(by, now);
    }

    /** The version added since the document was loaded, if any. Cleared once taken. */
    public DocumentVersion takePendingVersion() {
        DocumentVersion v = pendingVersion;
        pendingVersion = null;
        return v;
    }

    public void rename(String newName, String by, Instant now) {
        assertActive();
        FolderTreeRules.validateName(newName);
        this.name = newName.trim();
        this.nameNormalized = NameNormalizer.normalize(newName);
        touch(by, now);
    }

    public void moveTo(FolderId newFolderId, String by, Instant now) {
        assertActive();
        if (newFolderId == null) {
            throw new DocumentsDomainException("error.documents.folderRequired", null, "A folder is required");
        }
        this.folderId = newFolderId;
        touch(by, now);
    }

    public void describe(String text, String by, Instant now) {
        assertActive();
        this.description = checkDescription(text);
        touch(by, now);
    }

    public void trash(String by, Instant now, LocalDate today) {
        if (status == DocumentStatus.TRASHED) {
            throw new DocumentsDomainException("error.documents.alreadyTrashed", null, "Document is already in the trash");
        }
        if (isRetentionLocked(today)) {
            throw new DocumentsDomainException("error.documents.retentionLocked", new Object[]{retentionUntil},
                    "Document is under retention until " + retentionUntil);
        }
        this.status = DocumentStatus.TRASHED;
        this.trashedAt = now;
        this.trashedBy = by;
        touch(by, now);
    }

    public void restoreFromTrash(String by, Instant now) {
        if (status != DocumentStatus.TRASHED) {
            throw new DocumentsDomainException("error.documents.notTrashed", null, "Document is not in the trash");
        }
        this.status = DocumentStatus.ACTIVE;
        this.trashedAt = null;
        this.trashedBy = null;
        touch(by, now);
    }

    public boolean isRetentionLocked(LocalDate today) {
        return retentionUntil != null && retentionUntil.isAfter(today);
    }

    /** BR-DOC-07 and D9: purge only trashed documents that are not under retention. */
    public boolean canBePurged(LocalDate today) {
        return status == DocumentStatus.TRASHED && !isRetentionLocked(today);
    }

    /** A retention lock can be extended but never shortened. */
    public void lockUntil(LocalDate until, String reason, String by, Instant now) {
        if (until == null) {
            throw new DocumentsDomainException("error.documents.retentionDateRequired", null, "Retention date is required");
        }
        if (retentionUntil != null && until.isBefore(retentionUntil)) {
            throw new DocumentsDomainException("error.documents.retentionShorten", null,
                    "A retention lock cannot be shortened");
        }
        this.retentionUntil = until;
        this.retentionReason = reason;
        touch(by, now);
    }

    public void replaceTags(Set<TagId> newTags, String by, Instant now) {
        assertActive();
        this.tagIds = new LinkedHashSet<>(newTags);
        touch(by, now);
    }

    public boolean addLink(RecordLink link, String by, Instant now) {
        assertActive();
        if (links.contains(link)) {
            return false;
        }
        links.add(link);
        touch(by, now);
        return true;
    }

    public boolean removeLink(String modelName, UUID recordId, String by, Instant now) {
        assertActive();
        boolean removed = links.removeIf(l -> l.getModelName().equals(modelName) && l.getRecordId().equals(recordId));
        if (removed) {
            touch(by, now);
        }
        return removed;
    }

    private void assertActive() {
        if (status == DocumentStatus.TRASHED) {
            throw new DocumentsDomainException("error.documents.inTrash", null,
                    "Document is in the trash. Restore it first");
        }
    }

    private void touch(String by, Instant now) {
        this.updatedAt = now;
        this.updatedBy = by;
    }

    private static String checkDescription(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DocumentsDomainException("error.documents.descriptionTooLong",
                    new Object[]{MAX_DESCRIPTION_LENGTH}, "Description is too long");
        }
        return text.trim();
    }

    public CompanyId getCompanyId() { return companyId; }
    public FolderId getFolderId() { return folderId; }
    public String getName() { return name; }
    public String getNameNormalized() { return nameNormalized; }
    public String getDescription() { return description; }
    public DocumentStatus getStatus() { return status; }
    public DocumentSource getSource() { return source; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public DocumentVersionId getCurrentVersionId() { return currentVersionId; }
    public int getVersionNo() { return versionNo; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public Instant getTrashedAt() { return trashedAt; }
    public String getTrashedBy() { return trashedBy; }
    public LocalDate getRetentionUntil() { return retentionUntil; }
    public String getRetentionReason() { return retentionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Set<TagId> getTagIds() { return Set.copyOf(tagIds); }
    public List<RecordLink> getLinks() { return List.copyOf(links); }
}
