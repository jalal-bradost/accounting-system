package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Document;
import com.bradox.erp.documents.domain.core.entity.DocumentVersion;
import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.entity.Tag;
import com.bradox.erp.documents.domain.core.entity.TagFacet;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentStatus;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.documents.service.domain.ports.output.AccessSubjectPort;
import com.bradox.erp.documents.service.domain.ports.output.RecordLookupPort;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentPage;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentRepository;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentSearchCriteria;
import com.bradox.erp.documents.service.domain.ports.output.repository.FolderRepository;
import com.bradox.erp.documents.service.domain.ports.output.repository.TagRepository;
import com.bradox.erp.documents.service.domain.ports.output.storage.DocumentStoragePort;
import com.bradox.erp.documents.service.domain.ports.output.storage.StoredObject;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** In-memory ports so the services can be tested without a database. */
final class Fakes {

    private Fakes() {
    }

    static class FolderRepo implements FolderRepository {
        final Map<FolderId, Folder> folders = new LinkedHashMap<>();
        final Map<FolderId, List<FolderAccessGrant>> grants = new HashMap<>();

        @Override
        public Folder save(Folder folder) {
            folders.put(folder.getId(), folder);
            return folder;
        }

        @Override
        public Optional<Folder> findById(FolderId id) {
            return Optional.ofNullable(folders.get(id));
        }

        @Override
        public List<Folder> findAll(CompanyId companyId) {
            return folders.values().stream().filter(f -> f.getCompanyId().equals(companyId)).toList();
        }

        @Override
        public boolean existsAny(CompanyId companyId) {
            return !findAll(companyId).isEmpty();
        }

        @Override
        public Map<FolderId, List<FolderAccessGrant>> findGrants(CompanyId companyId) {
            return new HashMap<>(grants);
        }

        @Override
        public List<FolderAccessGrant> findGrants(FolderId folderId) {
            return grants.getOrDefault(folderId, List.of());
        }

        @Override
        public void replaceGrants(CompanyId companyId, FolderId folderId, List<FolderAccessGrant> list) {
            grants.put(folderId, new ArrayList<>(list));
        }
    }

    static class DocRepo implements DocumentRepository {
        final Map<DocumentId, Document> docs = new LinkedHashMap<>();
        final Map<DocumentId, List<DocumentVersion>> versions = new HashMap<>();
        boolean failOnSave;

        @Override
        public Document save(Document document) {
            if (failOnSave) {
                throw new IllegalStateException("database down");
            }
            DocumentVersion pending = document.takePendingVersion();
            if (pending != null) {
                versions.computeIfAbsent(document.getId(), k -> new ArrayList<>()).add(pending);
            }
            docs.put(document.getId(), document);
            return document;
        }

        @Override
        public Optional<Document> findById(DocumentId id) {
            return Optional.ofNullable(docs.get(id));
        }

        @Override
        public DocumentPage search(DocumentSearchCriteria c) {
            List<Document> hits = docs.values().stream()
                    .filter(d -> d.getCompanyId().equals(c.companyId()))
                    .filter(d -> d.getStatus() == c.status())
                    .filter(d -> c.folderIds() == null || c.folderIds().contains(d.getFolderId().getId()))
                    .filter(d -> c.textNormalized() == null || c.textNormalized().isEmpty()
                            || d.getNameNormalized().contains(c.textNormalized())
                            || c.textTagIds().stream().anyMatch(t -> d.getTagIds().contains(new TagId(t))))
                    .filter(d -> c.tagGroups().stream().allMatch(g ->
                            g.stream().anyMatch(t -> d.getTagIds().contains(new TagId(t)))))
                    .sorted(Comparator.comparing(Document::getNameNormalized))
                    .toList();
            int from = Math.min(c.page() * c.size(), hits.size());
            int to = Math.min(from + c.size(), hits.size());
            return new DocumentPage(hits.subList(from, to), hits.size());
        }

        @Override
        public List<DocumentVersion> findVersions(DocumentId documentId) {
            List<DocumentVersion> list = new ArrayList<>(versions.getOrDefault(documentId, List.of()));
            list.sort(Comparator.comparingInt(DocumentVersion::getVersionNo).reversed());
            return list;
        }

        @Override
        public Optional<DocumentVersion> findVersion(DocumentId documentId, DocumentVersionId versionId) {
            return versions.getOrDefault(documentId, List.of()).stream().filter(v -> v.getId().equals(versionId)).findFirst();
        }

        @Override
        public int countVersions(DocumentId documentId) {
            return versions.getOrDefault(documentId, List.of()).size();
        }

        @Override
        public Optional<Document> findDuplicate(CompanyId companyId, FolderId folderId, String sha256, DocumentId exclude) {
            return docs.values().stream()
                    .filter(d -> d.getStatus() == DocumentStatus.ACTIVE && d.getFolderId().equals(folderId)
                            && sha256.equals(d.getSha256()) && !d.getId().equals(exclude))
                    .findFirst();
        }

        @Override
        public List<Document> findByRecord(CompanyId companyId, String modelName, UUID recordId) {
            return docs.values().stream()
                    .filter(d -> d.getLinks().stream().anyMatch(l -> l.getModelName().equals(modelName) && l.getRecordId().equals(recordId)))
                    .toList();
        }

        @Override
        public long sumCurrentSizes(CompanyId companyId) {
            return docs.values().stream().mapToLong(Document::getSizeBytes).sum();
        }

        @Override
        public long countActive(CompanyId companyId) {
            return docs.values().stream().filter(d -> d.getStatus() == DocumentStatus.ACTIVE).count();
        }

        @Override
        public List<Document> findTrashed(CompanyId companyId) {
            return docs.values().stream().filter(d -> d.getStatus() == DocumentStatus.TRASHED).toList();
        }

        @Override
        public List<Document> findTrashedBefore(Instant cutoff, int limit) {
            return docs.values().stream()
                    .filter(d -> d.getStatus() == DocumentStatus.TRASHED && d.getTrashedAt().isBefore(cutoff))
                    .limit(limit).toList();
        }

        @Override
        public List<String> deleteHard(Document document) {
            List<String> keys = versions.getOrDefault(document.getId(), List.of()).stream()
                    .map(DocumentVersion::getStorageKey).toList();
            versions.remove(document.getId());
            docs.remove(document.getId());
            return keys;
        }
    }

    static class TagRepo implements TagRepository {
        final Map<TagFacetId, TagFacet> facets = new LinkedHashMap<>();
        final Map<TagId, Tag> tags = new LinkedHashMap<>();

        @Override
        public List<TagFacet> findFacets(CompanyId companyId) {
            return new ArrayList<>(facets.values());
        }

        @Override
        public Optional<TagFacet> findFacet(TagFacetId id) {
            return Optional.ofNullable(facets.get(id));
        }

        @Override
        public TagFacet saveFacet(TagFacet facet) {
            facets.put(facet.getId(), facet);
            return facet;
        }

        @Override
        public void deleteFacet(TagFacetId id) {
            facets.remove(id);
            tags.values().removeIf(t -> t.getFacetId().equals(id));
        }

        @Override
        public List<Tag> findTags(CompanyId companyId) {
            return new ArrayList<>(tags.values());
        }

        @Override
        public List<Tag> findTagsByIds(Collection<TagId> ids) {
            return ids.stream().map(tags::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public Optional<Tag> findTag(TagId id) {
            return Optional.ofNullable(tags.get(id));
        }

        @Override
        public Tag saveTag(Tag tag) {
            tags.put(tag.getId(), tag);
            return tag;
        }

        @Override
        public void deleteTag(TagId id) {
            tags.remove(id);
        }
    }

    static class Storage implements DocumentStoragePort {
        final Map<String, byte[]> files = new HashMap<>();

        @Override
        public StoredObject store(InputStream content, long maxBytes) {
            try {
                byte[] bytes = content.readAllBytes();
                if (bytes.length == 0) {
                    throw new DocumentsDomainException("The file is empty");
                }
                if (bytes.length > maxBytes) {
                    throw new DocumentsDomainException("The file is too large");
                }
                String key = UUID.randomUUID().toString();
                files.put(key, bytes);
                byte[] head = new byte[Math.min(bytes.length, 8192)];
                System.arraycopy(bytes, 0, head, 0, head.length);
                String sha = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                return new StoredObject(key, bytes.length, sha, head, head.length);
            } catch (IOException | java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public InputStream open(String key) {
            return new ByteArrayInputStream(files.get(key));
        }

        @Override
        public String copy(String key) {
            String copy = UUID.randomUUID().toString();
            files.put(copy, files.get(key));
            return copy;
        }

        @Override
        public void delete(String key) {
            files.remove(key);
        }
    }

    static class Subjects implements AccessSubjectPort {
        final Map<UUID, Set<UUID>> rolesByUser = new HashMap<>();
        final List<AccessSubjectResponse> subjects = new ArrayList<>();

        @Override
        public Set<UUID> roleIdsOf(UserId userId, CompanyId companyId) {
            return rolesByUser.getOrDefault(userId.getId(), Set.of());
        }

        @Override
        public List<AccessSubjectResponse> listSubjects(CompanyId companyId) {
            return subjects;
        }

        @Override
        public Map<UUID, String> names(SubjectType type, Collection<UUID> ids) {
            return subjects.stream().filter(s -> s.type().equals(type.name()) && ids.contains(s.id()))
                    .collect(Collectors.toMap(AccessSubjectResponse::id, AccessSubjectResponse::name));
        }
    }

    static class Records implements RecordLookupPort {
        final Set<UUID> existing = new HashSet<>();

        @Override
        public boolean supports(String modelName) {
            return "contacts.partner".equals(modelName);
        }

        @Override
        public boolean exists(CompanyId companyId, String modelName, UUID recordId) {
            return existing.contains(recordId);
        }

        @Override
        public Optional<String> label(CompanyId companyId, String modelName, UUID recordId) {
            return existing.contains(recordId) ? Optional.of("Partner " + recordId.toString().substring(0, 4)) : Optional.empty();
        }

        @Override
        public Optional<String> readPermission(String modelName) {
            return "contacts.partner".equals(modelName) ? Optional.of("contacts.partner.read") : Optional.empty();
        }
    }
}
