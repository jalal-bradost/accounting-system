package com.bradox.erp.sign.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.sign.service.domain.dto.CreateTemplateCommand;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.ReplaceDocumentCommand;
import com.bradox.erp.sign.service.domain.dto.TemplateResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateSummaryResponse;
import com.bradox.erp.sign.service.domain.dto.UpdateTemplateCommand;
import com.bradox.erp.sign.service.domain.ports.input.TemplateApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(value = "/api/v1/sign/templates", produces = "application/json")
public class SignTemplateController {

    private final TemplateApplicationService templates;

    public SignTemplateController(TemplateApplicationService templates) {
        this.templates = templates;
    }

    @GetMapping
    @RequiresPermission("sign.template.view")
    public List<TemplateSummaryResponse> list(@CurrentCompany CompanyId companyId, @RequestParam(defaultValue = "false") boolean archived) {
        return templates.list(companyId, archived);
    }

    @GetMapping("/{id}")
    @RequiresPermission("sign.template.view")
    public TemplateResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return templates.get(companyId, id);
    }

    @PostMapping
    @RequiresPermission("sign.template.manage")
    public TemplateResponse create(@CurrentCompany CompanyId companyId, @Valid @RequestBody CreateTemplateCommand command) {
        return templates.create(companyId, command);
    }

    @PutMapping("/{id}")
    @RequiresPermission("sign.template.manage")
    public TemplateResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody UpdateTemplateCommand command) {
        return templates.update(companyId, id, command);
    }

    @PostMapping("/{id}/document")
    @RequiresPermission("sign.template.manage")
    public TemplateResponse replaceDocument(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                            @Valid @RequestBody ReplaceDocumentCommand command) {
        return templates.replaceDocument(companyId, id, command);
    }

    @PostMapping("/{id}/duplicate")
    @RequiresPermission("sign.template.manage")
    public TemplateResponse duplicate(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return templates.duplicate(companyId, id);
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("sign.template.manage")
    public TemplateResponse archive(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return templates.archive(companyId, id);
    }

    @PostMapping("/{id}/restore")
    @RequiresPermission("sign.template.manage")
    public TemplateResponse restore(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return templates.restore(companyId, id);
    }

    @GetMapping(value = "/{id}/pages/{page}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @RequiresPermission("sign.template.view")
    public ResponseEntity<byte[]> page(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @PathVariable int page) {
        PageImage image = templates.page(companyId, id, page);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .header("X-Page-Width", String.valueOf(image.width())).header("X-Page-Height", String.valueOf(image.height()))
                .body(image.png());
    }
}
