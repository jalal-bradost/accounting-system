package com.bradox.erp.documents.dataaccess.adapter;

import com.bradox.erp.documents.dataaccess.entity.DocumentEntity;
import com.bradox.erp.documents.dataaccess.entity.DocumentVersionEntity;
import com.bradox.erp.documents.dataaccess.mapper.DocumentsDataAccessMapper;
import com.bradox.erp.documents.dataaccess.repository.DocumentJpaRepository;
import com.bradox.erp.documents.dataaccess.repository.DocumentVersionJpaRepository;
import com.bradox.erp.documents.domain.core.entity.Document;
import com.bradox.erp.documents.domain.core.entity.DocumentVersion;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentStatus;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentPage;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentRepository;
import com.bradox.erp.documents.service.domain.ports.output.repository.DocumentSearchCriteria;
import com.bradox.erp.domain.valueobject.CompanyId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentJpaRepository documents;
    private final DocumentVersionJpaRepository versions;
    private final DocumentsDataAccessMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public DocumentRepositoryImpl(DocumentJpaRepository documents, DocumentVersionJpaRepository versions,
                                  DocumentsDataAccessMapper mapper) {
        this.documents = documents;
        this.versions = versions;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        DocumentEntity entity = documents.findById(document.getId().getId()).orElseGet(DocumentEntity::new);
        mapper.apply(document, entity);
        DocumentEntity saved = documents.saveAndFlush(entity);
        DocumentVersion pending = document.takePendingVersion();
        if (pending != null) {
            versions.saveAndFlush(mapper.toEntity(document.getCompanyId().getId(), pending));
        }
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Document> findById(DocumentId id) {
        return documents.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public List<DocumentVersion> findVersions(DocumentId documentId) {
        return versions.findByDocumentIdOrderByVersionNoDesc(documentId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<DocumentVersion> findVersion(DocumentId documentId, DocumentVersionId versionId) {
        return versions.findByIdAndDocumentId(versionId.getId(), documentId.getId()).map(mapper::toDomain);
    }

    @Override
    public int countVersions(DocumentId documentId) {
        return versions.countByDocumentId(documentId.getId());
    }

    @Override
    public Optional<Document> findDuplicate(CompanyId companyId, FolderId folderId, String sha256, DocumentId exclude) {
        String active = DocumentStatus.ACTIVE.name();
        Optional<DocumentEntity> found = exclude == null
                ? documents.findFirstByCompanyIdAndFolderIdAndSha256AndStatus(companyId.getId(), folderId.getId(), sha256, active)
                : documents.findFirstByCompanyIdAndFolderIdAndSha256AndStatusAndIdNot(
                companyId.getId(), folderId.getId(), sha256, active, exclude.getId());
        return found.map(mapper::toDomain);
    }

    @Override
    public List<Document> findByRecord(CompanyId companyId, String modelName, UUID recordId) {
        return documents.findByRecord(companyId.getId(), modelName, recordId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long sumCurrentSizes(CompanyId companyId) {
        return documents.sumSizes(companyId.getId());
    }

    @Override
    public long countActive(CompanyId companyId) {
        return documents.countByCompanyIdAndStatus(companyId.getId(), DocumentStatus.ACTIVE.name());
    }

    @Override
    public List<Document> findTrashed(CompanyId companyId) {
        return documents.findByCompanyIdAndStatusOrderByTrashedAtDesc(companyId.getId(), DocumentStatus.TRASHED.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Document> findTrashedBefore(Instant cutoff, int limit) {
        return documents.findByStatusAndTrashedAtBefore(DocumentStatus.TRASHED.name(), cutoff, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<String> deleteHard(Document document) {
        UUID id = document.getId().getId();
        List<String> keys = versions.findByDocumentIdOrderByVersionNoDesc(id).stream()
                .map(DocumentVersionEntity::getStorageKey).toList();
        versions.deleteAllForDocument(id);
        documents.findById(id).ifPresent(documents::delete);
        documents.flush();
        return keys;
    }

    // ------------------------------------------------------------ search

    @Override
    public DocumentPage search(DocumentSearchCriteria c) {
        if (c.folderIds() != null && c.folderIds().isEmpty()) {
            return new DocumentPage(List.of(), 0);
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<DocumentEntity> countRoot = countQuery.from(DocumentEntity.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countQuery, countRoot, c).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();
        if (total == 0) {
            return new DocumentPage(List.of(), 0);
        }

        CriteriaQuery<DocumentEntity> query = cb.createQuery(DocumentEntity.class);
        Root<DocumentEntity> root = query.from(DocumentEntity.class);
        query.select(root)
                .where(predicates(cb, query, root, c).toArray(new Predicate[0]))
                .orderBy(orders(cb, root, c));
        TypedQuery<DocumentEntity> typed = em.createQuery(query);
        typed.setFirstResult(c.page() * c.size());
        typed.setMaxResults(c.size());
        List<Document> content = typed.getResultList().stream().map(mapper::toDomain).toList();
        return new DocumentPage(content, total);
    }

    private List<Predicate> predicates(CriteriaBuilder cb, AbstractQuery<?> query, Root<DocumentEntity> root,
                                       DocumentSearchCriteria c) {
        List<Predicate> preds = new ArrayList<>();
        preds.add(cb.equal(root.get("companyId"), c.companyId().getId()));
        preds.add(cb.equal(root.get("status"), c.status().name()));

        if (c.folderIds() != null) {
            preds.add(root.get("folderId").in(c.folderIds()));
        }
        if (c.textNormalized() != null && !c.textNormalized().isEmpty()) {
            String pattern = "%" + escapeLike(c.textNormalized()) + "%";
            List<Predicate> any = new ArrayList<>();
            any.add(cb.like(root.<String>get("nameNormalized"), pattern, '\\'));
            any.add(cb.like(root.<String>get("fileNameNormalized"), pattern, '\\'));
            if (c.textTagIds() != null && !c.textTagIds().isEmpty()) {
                any.add(hasAnyTag(cb, query, root, c.textTagIds()));
            }
            preds.add(cb.or(any.toArray(new Predicate[0])));
        }
        if (c.tagGroups() != null) {
            for (Set<UUID> group : c.tagGroups()) {
                preds.add(hasAnyTag(cb, query, root, group));
            }
        }
        if (c.contentTypes() != null && !c.contentTypes().isEmpty()) {
            preds.add(root.get("contentType").in(c.contentTypes()));
        }
        if (c.contentTypePrefix() != null) {
            preds.add(cb.like(root.<String>get("contentType"), escapeLike(c.contentTypePrefix()) + "%", '\\'));
        }
        if (c.createdBy() != null) {
            preds.add(cb.equal(root.get("createdBy"), c.createdBy()));
        }
        if (c.from() != null) {
            preds.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), c.from()));
        }
        if (c.to() != null) {
            preds.add(cb.lessThan(root.<Instant>get("createdAt"), c.to()));
        }
        if (c.linkedModel() != null) {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<DocumentEntity> r = sub.from(DocumentEntity.class);
            Join<DocumentEntity, ?> link = r.join("links");
            sub.select(cb.literal(1)).where(cb.equal(r.get("id"), root.get("id")),
                    cb.equal(link.get("modelName"), c.linkedModel()));
            preds.add(cb.exists(sub));
        }
        return preds;
    }

    private Predicate hasAnyTag(CriteriaBuilder cb, AbstractQuery<?> query, Root<DocumentEntity> root, Set<UUID> tagIds) {
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<DocumentEntity> r = sub.from(DocumentEntity.class);
        Join<DocumentEntity, UUID> tag = r.join("tagIds");
        sub.select(cb.literal(1)).where(cb.equal(r.get("id"), root.get("id")), tag.in(tagIds));
        return cb.exists(sub);
    }

    private List<Order> orders(CriteriaBuilder cb, Root<DocumentEntity> root, DocumentSearchCriteria c) {
        String field = c.sortField() == null ? "" : c.sortField();
        Expression<?> sortExpr = switch (field) {
            case "name" -> root.get("nameNormalized");
            case "created" -> root.get("createdAt");
            case "size" -> root.get("sizeBytes");
            default -> root.get("updatedAt");
        };
        boolean desc = field.isEmpty() || c.sortDescending();
        List<Order> orders = new ArrayList<>();
        orders.add(desc ? cb.desc(sortExpr) : cb.asc(sortExpr));
        orders.add(cb.asc(root.get("id")));
        return orders;
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
