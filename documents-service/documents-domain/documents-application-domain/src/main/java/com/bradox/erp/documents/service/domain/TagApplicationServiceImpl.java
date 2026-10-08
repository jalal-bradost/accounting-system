package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Tag;
import com.bradox.erp.documents.domain.core.entity.TagFacet;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.documents.service.domain.dto.TagCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetResponse;
import com.bradox.erp.documents.service.domain.dto.TagResponse;
import com.bradox.erp.documents.service.domain.ports.input.TagApplicationService;
import com.bradox.erp.documents.service.domain.ports.output.repository.TagRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class TagApplicationServiceImpl implements TagApplicationService {

    private final TagRepository tags;

    TagApplicationServiceImpl(TagRepository tags) {
        this.tags = tags;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagFacetResponse> listFacets(CompanyId companyId) {
        return tags.findFacets(companyId).stream()
                .sorted(Comparator.comparingInt(TagFacet::getSequence).thenComparing(TagFacet::getName))
                .map(TagApplicationServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public TagFacetResponse createFacet(CompanyId companyId, TagFacetCommand command) {
        int sequence = command.sequence() != null ? command.sequence()
                : tags.findFacets(companyId).stream().mapToInt(TagFacet::getSequence).max().orElse(-1) + 1;
        return toResponse(tags.saveFacet(TagFacet.create(new TagFacetId(UUID.randomUUID()), companyId, command.name(), sequence)));
    }

    @Override
    @Transactional
    public TagFacetResponse updateFacet(CompanyId companyId, UUID id, TagFacetCommand command) {
        TagFacet facet = loadFacet(companyId, id);
        facet.update(command.name(), command.sequence() != null ? command.sequence() : facet.getSequence());
        return toResponse(tags.saveFacet(facet));
    }

    @Override
    @Transactional
    public void deleteFacet(CompanyId companyId, UUID id) {
        loadFacet(companyId, id);
        tags.deleteFacet(new TagFacetId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> listTags(CompanyId companyId) {
        return tags.findTags(companyId).stream()
                .sorted(Comparator.comparingInt(Tag::getSequence).thenComparing(Tag::getName))
                .map(TagApplicationServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public TagResponse createTag(CompanyId companyId, TagCommand command) {
        if (command.facetId() == null) {
            throw new DocumentsDomainException("error.documents.facetRequired", null, "A tag category is required");
        }
        loadFacet(companyId, command.facetId());
        int sequence = command.sequence() != null ? command.sequence()
                : tags.findTags(companyId).stream().mapToInt(Tag::getSequence).max().orElse(-1) + 1;
        return toResponse(tags.saveTag(Tag.create(new TagId(UUID.randomUUID()), companyId,
                new TagFacetId(command.facetId()), command.name(), command.color(), sequence)));
    }

    @Override
    @Transactional
    public TagResponse updateTag(CompanyId companyId, UUID id, TagCommand command) {
        Tag tag = loadTag(companyId, id);
        tag.update(command.name(), command.color(), command.sequence() != null ? command.sequence() : tag.getSequence());
        return toResponse(tags.saveTag(tag));
    }

    @Override
    @Transactional
    public void deleteTag(CompanyId companyId, UUID id) {
        loadTag(companyId, id);
        tags.deleteTag(new TagId(id));
    }

    private TagFacet loadFacet(CompanyId companyId, UUID id) {
        return tags.findFacet(new TagFacetId(id))
                .filter(f -> f.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag category not found"));
    }

    private Tag loadTag(CompanyId companyId, UUID id) {
        return tags.findTag(new TagId(id))
                .filter(t -> t.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    private static TagFacetResponse toResponse(TagFacet f) {
        return new TagFacetResponse(f.getId().getId(), f.getName(), f.getSequence());
    }

    private static TagResponse toResponse(Tag t) {
        return new TagResponse(t.getId().getId(), t.getFacetId().getId(), t.getName(), t.getColor(), t.getSequence());
    }
}
