package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.service.domain.dto.TagCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetResponse;
import com.bradox.erp.documents.service.domain.dto.TagResponse;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagApplicationServiceTest extends ServiceTestBase {

    @Test
    void facetsAndTagsLifecycle() {
        TagFacetResponse facet = tagService.createFacet(COMPANY, new TagFacetCommand("Status", null));
        TagFacetResponse second = tagService.createFacet(COMPANY, new TagFacetCommand("Type", null));
        assertEquals(1, second.sequence());
        assertEquals("Kind", tagService.updateFacet(COMPANY, second.id(), new TagFacetCommand("Kind", 5)).name());
        assertEquals(2, tagService.listFacets(COMPANY).size());

        TagResponse tag = tagService.createTag(COMPANY, new TagCommand(facet.id(), "Draft", "#ff0000", null));
        assertEquals("#ff0000", tag.color());
        TagResponse updated = tagService.updateTag(COMPANY, tag.id(), new TagCommand(null, "Final", "#00ff00", 3));
        assertEquals("Final", updated.name());
        assertEquals(3, updated.sequence());
        assertEquals(1, tagService.listTags(COMPANY).size());

        tagService.deleteTag(COMPANY, tag.id());
        assertEquals(0, tagService.listTags(COMPANY).size());
        tagService.deleteFacet(COMPANY, facet.id());
        assertEquals(1, tagService.listFacets(COMPANY).size());
    }

    @Test
    void tagsNeedAnExistingFacetAndBelongToTheirCompany() {
        assertThrows(DocumentsDomainException.class, () -> tagService.createTag(COMPANY, new TagCommand(null, "X", null, null)));
        assertThrows(ResponseStatusException.class, () -> tagService.createTag(COMPANY, new TagCommand(UUID.randomUUID(), "X", null, null)));
        TagFacetResponse facet = tagService.createFacet(COMPANY, new TagFacetCommand("Status", null));
        CompanyId other = new CompanyId(UUID.randomUUID());
        assertThrows(ResponseStatusException.class, () -> tagService.updateFacet(other, facet.id(), new TagFacetCommand("Hack", null)));
        assertThrows(ResponseStatusException.class, () -> tagService.deleteTag(COMPANY, UUID.randomUUID()));
    }
}
