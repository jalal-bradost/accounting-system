package com.bradox.erp.documents.application.rest;

import com.bradox.erp.documents.service.domain.dto.TagCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetResponse;
import com.bradox.erp.documents.service.domain.dto.TagResponse;
import com.bradox.erp.documents.service.domain.ports.input.TagApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/documents", produces = "application/json")
public class TagController {

    private final TagApplicationService service;

    public TagController(TagApplicationService service) {
        this.service = service;
    }

    @GetMapping("/tag-facets")
    @RequiresPermission("documents.document.read")
    public List<TagFacetResponse> facets(@CurrentCompany CompanyId companyId) {
        return service.listFacets(companyId);
    }

    @PostMapping("/tag-facets")
    @RequiresPermission("documents.tag.write")
    public TagFacetResponse createFacet(@CurrentCompany CompanyId companyId, @Valid @RequestBody TagFacetCommand command) {
        return service.createFacet(companyId, command);
    }

    @PutMapping("/tag-facets/{id}")
    @RequiresPermission("documents.tag.write")
    public TagFacetResponse updateFacet(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                        @Valid @RequestBody TagFacetCommand command) {
        return service.updateFacet(companyId, id, command);
    }

    @DeleteMapping("/tag-facets/{id}")
    @RequiresPermission("documents.tag.write")
    public ResponseEntity<Void> deleteFacet(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        service.deleteFacet(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    @RequiresPermission("documents.document.read")
    public List<TagResponse> tags(@CurrentCompany CompanyId companyId) {
        return service.listTags(companyId);
    }

    @PostMapping("/tags")
    @RequiresPermission("documents.tag.write")
    public TagResponse createTag(@CurrentCompany CompanyId companyId, @Valid @RequestBody TagCommand command) {
        return service.createTag(companyId, command);
    }

    @PutMapping("/tags/{id}")
    @RequiresPermission("documents.tag.write")
    public TagResponse updateTag(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                 @Valid @RequestBody TagCommand command) {
        return service.updateTag(companyId, id, command);
    }

    @DeleteMapping("/tags/{id}")
    @RequiresPermission("documents.tag.write")
    public ResponseEntity<Void> deleteTag(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        service.deleteTag(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
