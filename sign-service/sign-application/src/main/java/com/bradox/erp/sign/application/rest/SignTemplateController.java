package com.bradox.erp.sign.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.SignCommand;
import com.bradox.erp.sign.service.domain.dto.SignedDocumentResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateCommand;
import com.bradox.erp.sign.service.domain.dto.TemplateResponse;
import com.bradox.erp.sign.service.domain.ports.input.SignTemplateApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/sign", produces = "application/json")
public class SignTemplateController {

    private final SignTemplateApplicationService templates;

    public SignTemplateController(SignTemplateApplicationService templates) {
        this.templates = templates;
    }

    @GetMapping("/templates")
    public List<TemplateResponse> list(@CurrentCompany CompanyId companyId) {
        return templates.list(companyId);
    }

    @GetMapping("/templates/{id}")
    public TemplateResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return templates.get(companyId, id);
    }

    @PostMapping(value = "/templates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@CurrentCompany CompanyId companyId, @RequestParam("file") MultipartFile file,
                                   @RequestParam(required = false) String name) throws IOException {
        return templates.create(companyId, name, file.getOriginalFilename(), file.getBytes());
    }

    @PutMapping("/templates/{id}")
    public TemplateResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody TemplateCommand c) {
        return templates.update(companyId, id, c);
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        templates.delete(companyId, id);
    }

    @GetMapping(value = "/templates/{id}/pages/{page}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> page(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @PathVariable int page) {
        PageImage image = templates.page(companyId, id, page);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .header("X-Page-Width", String.valueOf(image.width())).header("X-Page-Height", String.valueOf(image.height()))
                .body(image.png());
    }

    @PostMapping("/templates/{id}/sign")
    @ResponseStatus(HttpStatus.CREATED)
    public SignedDocumentResponse sign(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody SignCommand c) {
        return templates.sign(companyId, id, c);
    }

    @GetMapping("/templates/{id}/signed")
    public List<SignedDocumentResponse> signed(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return templates.signedDocuments(companyId, id);
    }

    @GetMapping(value = "/signed/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        FileContent file = templates.download(companyId, id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.bytes());
    }
}
