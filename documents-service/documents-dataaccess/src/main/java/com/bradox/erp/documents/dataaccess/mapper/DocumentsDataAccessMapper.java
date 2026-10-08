package com.bradox.erp.documents.dataaccess.mapper;

import com.bradox.erp.documents.dataaccess.entity.DocumentEntity;
import com.bradox.erp.documents.dataaccess.entity.DocumentVersionEntity;
import com.bradox.erp.documents.dataaccess.entity.FolderAccessEntity;
import com.bradox.erp.documents.dataaccess.entity.FolderEntity;
import com.bradox.erp.documents.dataaccess.entity.RecordLinkEmbeddable;
import com.bradox.erp.documents.dataaccess.entity.TagEntity;
import com.bradox.erp.documents.dataaccess.entity.TagFacetEntity;
import com.bradox.erp.documents.domain.core.entity.Document;
import com.bradox.erp.documents.domain.core.entity.DocumentVersion;
import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.entity.RecordLink;
import com.bradox.erp.documents.domain.core.entity.Tag;
import com.bradox.erp.documents.domain.core.entity.TagFacet;
import com.bradox.erp.documents.domain.core.rule.NameNormalizer;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentSource;
import com.bradox.erp.documents.domain.core.valueobject.DocumentStatus;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DocumentsDataAccessMapper {

    // ---- folders

    public Folder toDomain(FolderEntity e) {
        return Folder.restore(new FolderId(e.getId()), new CompanyId(e.getCompanyId()),
                e.getParentId() == null ? null : new FolderId(e.getParentId()), e.getName(), e.getNameNormalized(),
                e.getSequence(), e.isArchived(), e.isInheritAccess(), e.getLinkedModel(), e.getSystemKey(),
                e.getCreatedAt(), e.getCreatedBy());
    }

    public void apply(Folder f, FolderEntity e) {
        e.setId(f.getId().getId());
        e.setCompanyId(f.getCompanyId().getId());
        e.setParentId(f.getParentId() == null ? null : f.getParentId().getId());
        e.setName(f.getName());
        e.setNameNormalized(f.getNameNormalized());
        e.setSequence(f.getSequence());
        e.setArchived(f.isArchived());
        e.setInheritAccess(f.isInheritAccess());
        e.setLinkedModel(f.getLinkedModel());
        e.setSystemKey(f.getSystemKey());
        e.setCreatedAt(f.getCreatedAt());
        e.setCreatedBy(f.getCreatedBy());
    }

    public FolderAccessGrant toDomain(FolderAccessEntity e) {
        return new FolderAccessGrant(SubjectType.valueOf(e.getSubjectType()), e.getSubjectId(), AccessLevel.valueOf(e.getLevel()));
    }

    public FolderAccessEntity toEntity(UUID companyId, UUID folderId, FolderAccessGrant g) {
        FolderAccessEntity e = new FolderAccessEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(companyId);
        e.setFolderId(folderId);
        e.setSubjectType(g.getSubjectType().name());
        e.setSubjectId(g.getSubjectId());
        e.setLevel(g.getLevel().name());
        return e;
    }

    // ---- documents

    public Document toDomain(DocumentEntity e) {
        Set<TagId> tags = e.getTagIds().stream().map(TagId::new).collect(Collectors.toCollection(LinkedHashSet::new));
        return Document.restore(new DocumentId(e.getId()), new CompanyId(e.getCompanyId()), new FolderId(e.getFolderId()),
                e.getName(), e.getNameNormalized(), e.getDescription(), DocumentStatus.valueOf(e.getStatus()),
                DocumentSource.valueOf(e.getSource()), e.getOwnerUserId(),
                e.getCurrentVersionId() == null ? null : new DocumentVersionId(e.getCurrentVersionId()),
                e.getVersionNo(), e.getFileName(), e.getContentType(), e.getSizeBytes(), e.getSha256(),
                e.getTrashedAt(), e.getTrashedBy(), e.getRetentionUntil(), e.getRetentionReason(),
                e.getCreatedAt(), e.getCreatedBy(), e.getUpdatedAt(), e.getUpdatedBy(), tags,
                e.getLinks().stream().map(l -> new RecordLink(l.getModelName(), l.getRecordId(), l.getCreatedAt(), l.getCreatedBy())).toList());
    }

    public void apply(Document d, DocumentEntity e) {
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setFolderId(d.getFolderId().getId());
        e.setName(d.getName());
        e.setNameNormalized(d.getNameNormalized());
        e.setDescription(d.getDescription());
        e.setStatus(d.getStatus().name());
        e.setSource(d.getSource().name());
        e.setOwnerUserId(d.getOwnerUserId());
        e.setCurrentVersionId(d.getCurrentVersionId() == null ? null : d.getCurrentVersionId().getId());
        e.setVersionNo(d.getVersionNo());
        e.setFileName(d.getFileName());
        e.setFileNameNormalized(d.getFileName() == null ? null : NameNormalizer.normalize(d.getFileName()));
        e.setContentType(d.getContentType());
        e.setSizeBytes(d.getSizeBytes());
        e.setSha256(d.getSha256());
        e.setTrashedAt(d.getTrashedAt());
        e.setTrashedBy(d.getTrashedBy());
        e.setRetentionUntil(d.getRetentionUntil());
        e.setRetentionReason(d.getRetentionReason());
        e.setCreatedAt(d.getCreatedAt());
        e.setCreatedBy(d.getCreatedBy());
        e.setUpdatedAt(d.getUpdatedAt());
        e.setUpdatedBy(d.getUpdatedBy());

        Set<UUID> wantedTags = d.getTagIds().stream().map(TagId::getId).collect(Collectors.toSet());
        e.getTagIds().retainAll(wantedTags);
        e.getTagIds().addAll(wantedTags);

        Set<RecordLinkEmbeddable> wantedLinks = d.getLinks().stream()
                .map(l -> new RecordLinkEmbeddable(l.getModelName(), l.getRecordId(), l.getCreatedAt(), l.getCreatedBy()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        e.getLinks().retainAll(wantedLinks);
        e.getLinks().addAll(wantedLinks);
    }

    public DocumentVersion toDomain(DocumentVersionEntity e) {
        return new DocumentVersion(new DocumentVersionId(e.getId()), new DocumentId(e.getDocumentId()), e.getVersionNo(),
                e.getStorageKey(), e.getOriginalFileName(), e.getContentType(), e.getFileSize(), e.getSha256(),
                e.getUploadedBy(), e.getUploadedAt(), e.getComment());
    }

    public DocumentVersionEntity toEntity(UUID companyId, DocumentVersion v) {
        DocumentVersionEntity e = new DocumentVersionEntity();
        e.setId(v.getId().getId());
        e.setCompanyId(companyId);
        e.setDocumentId(v.getDocumentId().getId());
        e.setVersionNo(v.getVersionNo());
        e.setStorageKey(v.getStorageKey());
        e.setOriginalFileName(v.getOriginalFileName());
        e.setContentType(v.getContentType());
        e.setFileSize(v.getFileSize());
        e.setSha256(v.getSha256());
        e.setUploadedBy(v.getUploadedBy());
        e.setUploadedAt(v.getUploadedAt());
        e.setComment(v.getComment());
        return e;
    }

    // ---- tags

    public TagFacet toDomain(TagFacetEntity e) {
        return TagFacet.create(new TagFacetId(e.getId()), new CompanyId(e.getCompanyId()), e.getName(), e.getSequence());
    }

    public void apply(TagFacet f, TagFacetEntity e) {
        e.setId(f.getId().getId());
        e.setCompanyId(f.getCompanyId().getId());
        e.setName(f.getName());
        e.setSequence(f.getSequence());
    }

    public Tag toDomain(TagEntity e) {
        return Tag.create(new TagId(e.getId()), new CompanyId(e.getCompanyId()), new TagFacetId(e.getFacetId()),
                e.getName(), e.getColor(), e.getSequence());
    }

    public void apply(Tag t, TagEntity e) {
        e.setId(t.getId().getId());
        e.setCompanyId(t.getCompanyId().getId());
        e.setFacetId(t.getFacetId().getId());
        e.setName(t.getName());
        e.setColor(t.getColor());
        e.setSequence(t.getSequence());
    }
}
