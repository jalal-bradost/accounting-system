package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Document;
import com.bradox.erp.documents.domain.core.entity.DocumentVersion;
import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.rule.FileTypePolicy;
import com.bradox.erp.documents.domain.core.rule.FolderTree;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.service.domain.dto.DocumentResponse;
import com.bradox.erp.documents.service.domain.dto.DocumentSummaryResponse;
import com.bradox.erp.documents.service.domain.dto.FolderRefResponse;
import com.bradox.erp.documents.service.domain.dto.FolderResponse;
import com.bradox.erp.documents.service.domain.dto.RecordLinkResponse;
import com.bradox.erp.documents.service.domain.dto.VersionResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class DocumentsMapper {

    private DocumentsMapper() {
    }

    static FolderResponse folder(Folder f, AccessLevel level, boolean restricted) {
        return new FolderResponse(f.getId().getId(),
                f.getParentId() == null ? null : f.getParentId().getId(),
                f.getName(), f.getSequence(), f.isArchived(), f.isInheritAccess(), restricted,
                f.getLinkedModel(), f.getSystemKey(), level.name());
    }

    static DocumentSummaryResponse summary(Document d, FolderTree tree) {
        Folder folder = tree.get(d.getFolderId());
        return new DocumentSummaryResponse(
                d.getId().getId(), d.getFolderId().getId(), folder == null ? null : folder.getName(),
                d.getName(), d.getFileName(), d.getContentType(), d.getSizeBytes(), d.getVersionNo(),
                d.getStatus().name(),
                d.getTagIds().stream().map(t -> t.getId()).toList(),
                d.getLinks().size(), d.getRetentionUntil(), d.getTrashedAt(), d.getTrashedBy(),
                d.getCreatedBy(), d.getCreatedAt(), d.getUpdatedBy(), d.getUpdatedAt(),
                FileTypePolicy.isInlineSafe(d.getContentType()));
    }

    static DocumentResponse detail(Document d, FolderTree tree, AccessLevel level, Map<String, String> linkLabels) {
        List<FolderRefResponse> path = new ArrayList<>();
        Folder folder = tree.get(d.getFolderId());
        if (folder != null) {
            for (Folder a : tree.ancestorsOf(folder.getId())) {
                path.add(new FolderRefResponse(a.getId().getId(), a.getName()));
            }
            Collections.reverse(path);
            path.add(new FolderRefResponse(folder.getId().getId(), folder.getName()));
        }
        List<RecordLinkResponse> links = d.getLinks().stream()
                .map(l -> new RecordLinkResponse(l.getModelName(), l.getRecordId(),
                        linkLabels.get(linkKey(l.getModelName(), l.getRecordId())), l.getCreatedAt(), l.getCreatedBy()))
                .toList();
        return new DocumentResponse(
                d.getId().getId(), d.getFolderId().getId(), path, d.getName(), d.getDescription(),
                d.getFileName(), d.getContentType(), d.getSizeBytes(), d.getSha256(), d.getVersionNo(),
                d.getStatus().name(), d.getSource().name(),
                d.getTagIds().stream().map(t -> t.getId()).toList(), links,
                d.getRetentionUntil(), d.getRetentionReason(), d.getTrashedAt(), d.getTrashedBy(),
                d.getCreatedBy(), d.getCreatedAt(), d.getUpdatedBy(), d.getUpdatedAt(),
                FileTypePolicy.isInlineSafe(d.getContentType()), level.name());
    }

    static VersionResponse version(DocumentVersion v, boolean current) {
        return new VersionResponse(v.getId().getId(), v.getVersionNo(), v.getOriginalFileName(), v.getContentType(),
                v.getFileSize(), v.getSha256(), v.getUploadedBy(), v.getUploadedAt(), v.getComment(), current);
    }

    static String linkKey(String model, UUID recordId) {
        return model + "|" + recordId;
    }
}
