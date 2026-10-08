package com.bradox.erp.documents.service.domain.ports.input;

import com.bradox.erp.documents.service.domain.dto.TagCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetResponse;
import com.bradox.erp.documents.service.domain.dto.TagResponse;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.List;
import java.util.UUID;

public interface TagApplicationService {

    List<TagFacetResponse> listFacets(CompanyId companyId);

    TagFacetResponse createFacet(CompanyId companyId, TagFacetCommand command);

    TagFacetResponse updateFacet(CompanyId companyId, UUID id, TagFacetCommand command);

    /** Deleting a facet deletes its tags and removes them from documents. */
    void deleteFacet(CompanyId companyId, UUID id);

    List<TagResponse> listTags(CompanyId companyId);

    TagResponse createTag(CompanyId companyId, TagCommand command);

    TagResponse updateTag(CompanyId companyId, UUID id, TagCommand command);

    void deleteTag(CompanyId companyId, UUID id);
}
