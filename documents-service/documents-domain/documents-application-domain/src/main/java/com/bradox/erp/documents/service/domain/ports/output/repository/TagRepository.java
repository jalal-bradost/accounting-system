package com.bradox.erp.documents.service.domain.ports.output.repository;

import com.bradox.erp.documents.domain.core.entity.Tag;
import com.bradox.erp.documents.domain.core.entity.TagFacet;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository {

    List<TagFacet> findFacets(CompanyId companyId);

    Optional<TagFacet> findFacet(TagFacetId id);

    TagFacet saveFacet(TagFacet facet);

    /** Deletes the facet, its tags, and the tag assignments on documents. */
    void deleteFacet(TagFacetId id);

    List<Tag> findTags(CompanyId companyId);

    List<Tag> findTagsByIds(Collection<TagId> ids);

    Optional<Tag> findTag(TagId id);

    Tag saveTag(Tag tag);

    /** Deletes the tag and its assignments on documents. */
    void deleteTag(TagId id);
}
