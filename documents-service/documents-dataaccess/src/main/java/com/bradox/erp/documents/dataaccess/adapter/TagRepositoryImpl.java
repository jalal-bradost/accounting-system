package com.bradox.erp.documents.dataaccess.adapter;

import com.bradox.erp.documents.dataaccess.entity.TagEntity;
import com.bradox.erp.documents.dataaccess.entity.TagFacetEntity;
import com.bradox.erp.documents.dataaccess.mapper.DocumentsDataAccessMapper;
import com.bradox.erp.documents.dataaccess.repository.TagFacetJpaRepository;
import com.bradox.erp.documents.dataaccess.repository.TagJpaRepository;
import com.bradox.erp.documents.domain.core.entity.Tag;
import com.bradox.erp.documents.domain.core.entity.TagFacet;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.documents.service.domain.ports.output.repository.TagRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TagRepositoryImpl implements TagRepository {

    private final TagFacetJpaRepository facets;
    private final TagJpaRepository tags;
    private final DocumentsDataAccessMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public TagRepositoryImpl(TagFacetJpaRepository facets, TagJpaRepository tags, DocumentsDataAccessMapper mapper) {
        this.facets = facets;
        this.tags = tags;
        this.mapper = mapper;
    }

    @Override
    public List<TagFacet> findFacets(CompanyId companyId) {
        return facets.findByCompanyId(companyId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<TagFacet> findFacet(TagFacetId id) {
        return facets.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public TagFacet saveFacet(TagFacet facet) {
        TagFacetEntity entity = facets.findById(facet.getId().getId()).orElseGet(TagFacetEntity::new);
        mapper.apply(facet, entity);
        return mapper.toDomain(facets.saveAndFlush(entity));
    }

    @Override
    public void deleteFacet(TagFacetId id) {
        em.createNativeQuery("delete from doc_document_tag where tag_id in (select t.id from doc_tag t where t.facet_id = ?1)")
                .setParameter(1, id.getId()).executeUpdate();
        tags.deleteAllForFacet(id.getId());
        facets.deleteById(id.getId());
        facets.flush();
    }

    @Override
    public List<Tag> findTags(CompanyId companyId) {
        return tags.findByCompanyId(companyId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Tag> findTagsByIds(Collection<TagId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<UUID> raw = ids.stream().map(TagId::getId).toList();
        return tags.findAllById(raw).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Tag> findTag(TagId id) {
        return tags.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public Tag saveTag(Tag tag) {
        TagEntity entity = tags.findById(tag.getId().getId()).orElseGet(TagEntity::new);
        mapper.apply(tag, entity);
        return mapper.toDomain(tags.saveAndFlush(entity));
    }

    @Override
    public void deleteTag(TagId id) {
        em.createNativeQuery("delete from doc_document_tag where tag_id = ?1").setParameter(1, id.getId()).executeUpdate();
        tags.deleteById(id.getId());
        tags.flush();
    }
}
