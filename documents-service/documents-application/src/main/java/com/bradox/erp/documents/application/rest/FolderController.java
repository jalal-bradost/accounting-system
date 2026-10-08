package com.bradox.erp.documents.application.rest;

import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.documents.service.domain.dto.CreateFolderCommand;
import com.bradox.erp.documents.service.domain.dto.FolderAccessResponse;
import com.bradox.erp.documents.service.domain.dto.FolderResponse;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderAccessCommand;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderCommand;
import com.bradox.erp.documents.service.domain.ports.input.FolderApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/documents/folders", produces = "application/json")
public class FolderController {

    private final FolderApplicationService service;

    public FolderController(FolderApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("documents.document.read")
    public List<FolderResponse> list(@CurrentCompany CompanyId companyId,
                                     @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(companyId, includeArchived);
    }

    @PostMapping
    @RequiresPermission("documents.folder.write")
    public FolderResponse create(@CurrentCompany CompanyId companyId, @Valid @RequestBody CreateFolderCommand command) {
        return service.create(companyId, command);
    }

    @PutMapping("/{id}")
    @RequiresPermission("documents.folder.write")
    public FolderResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateFolderCommand command) {
        return service.update(companyId, id, command);
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("documents.folder.write")
    public FolderResponse archive(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.archive(companyId, id);
    }

    @PostMapping("/{id}/unarchive")
    @RequiresPermission("documents.folder.write")
    public FolderResponse unarchive(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.unarchive(companyId, id);
    }

    @GetMapping("/{id}/access")
    @RequiresPermission("documents.access.manage")
    public FolderAccessResponse getAccess(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.getAccess(companyId, id);
    }

    @PutMapping("/{id}/access")
    @RequiresPermission("documents.access.manage")
    public FolderAccessResponse updateAccess(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                             @Valid @RequestBody UpdateFolderAccessCommand command) {
        return service.updateAccess(companyId, id, command);
    }

    @GetMapping("/access-subjects")
    @RequiresPermission("documents.access.manage")
    public List<AccessSubjectResponse> accessSubjects(@CurrentCompany CompanyId companyId) {
        return service.listAccessSubjects(companyId);
    }
}
