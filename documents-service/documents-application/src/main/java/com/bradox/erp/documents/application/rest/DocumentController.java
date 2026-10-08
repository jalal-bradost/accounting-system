package com.bradox.erp.documents.application.rest;

import com.bradox.erp.documents.service.domain.dto.AddLinkCommand;
import com.bradox.erp.documents.service.domain.dto.DocumentContent;
import com.bradox.erp.documents.service.domain.dto.DocumentResponse;
import com.bradox.erp.documents.service.domain.dto.DocumentSearchQuery;
import com.bradox.erp.documents.service.domain.dto.DocumentSummaryResponse;
import com.bradox.erp.documents.service.domain.dto.LockRetentionCommand;
import com.bradox.erp.documents.service.domain.dto.RestoreDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.SetTagsCommand;
import com.bradox.erp.documents.service.domain.dto.StorageStatsResponse;
import com.bradox.erp.documents.service.domain.dto.UpdateDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.UploadDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.UploadResultResponse;
import com.bradox.erp.documents.service.domain.dto.VersionResponse;
import com.bradox.erp.documents.service.domain.ports.input.DocumentApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/documents", produces = "application/json")
public class DocumentController {

    private final DocumentApplicationService service;

    public DocumentController(DocumentApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("documents.document.read")
    public PageResponse<DocumentSummaryResponse> search(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(defaultValue = "false") boolean includeSubfolders,
            @RequestParam(required = false) List<UUID> tagIds,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String linkedModel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.search(companyId, new DocumentSearchQuery(q, folderId, includeSubfolders, tagIds, type,
                createdBy, from, to, linkedModel, status, sort, page, size));
    }

    @GetMapping("/by-record")
    @RequiresPermission("documents.document.read")
    public List<DocumentSummaryResponse> byRecord(@CurrentCompany CompanyId companyId,
                                                  @RequestParam String model, @RequestParam UUID recordId) {
        return service.findByRecord(companyId, model, recordId);
    }

    @GetMapping("/stats")
    @RequiresPermission("documents.document.read")
    public StorageStatsResponse stats(@CurrentCompany CompanyId companyId) {
        return service.stats(companyId);
    }

    @GetMapping("/{id}")
    @RequiresPermission("documents.document.read")
    public DocumentResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.get(companyId, id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission("documents.document.write")
    public UploadResultResponse upload(@CurrentCompany CompanyId companyId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam UUID folderId,
                                       @RequestParam(defaultValue = "false") boolean allowDuplicate,
                                       @RequestParam(required = false) String comment) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return service.upload(companyId, new UploadDocumentCommand(folderId, file.getOriginalFilename(), in,
                    allowDuplicate, comment));
        }
    }

    @PatchMapping("/{id}")
    @RequiresPermission("documents.document.write")
    public DocumentResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                   @Valid @RequestBody UpdateDocumentCommand command) {
        return service.update(companyId, id, command);
    }

    @GetMapping("/{id}/content")
    @RequiresPermission("documents.document.read")
    public ResponseEntity<InputStreamResource> content(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                                         @RequestParam(required = false) UUID versionId,
                                                         @RequestParam(defaultValue = "false") boolean download) {
        DocumentContent content = service.content(companyId, id, versionId);
        boolean inline = !download && content.inlineSafe();
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(content.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                .body(new InputStreamResource(content.stream()));
    }

    @GetMapping("/{id}/versions")
    @RequiresPermission("documents.document.read")
    public List<VersionResponse> versions(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.versions(companyId, id);
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission("documents.document.write")
    public UploadResultResponse uploadVersion(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "false") boolean allowDuplicate,
                                              @RequestParam(required = false) String comment) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return service.uploadVersion(companyId, id, new UploadDocumentCommand(null, file.getOriginalFilename(), in,
                    allowDuplicate, comment));
        }
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    @RequiresPermission("documents.document.write")
    public DocumentResponse restoreVersion(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                           @PathVariable UUID versionId) {
        return service.restoreVersion(companyId, id, versionId);
    }

    @PutMapping("/{id}/tags")
    @RequiresPermission("documents.document.write")
    public DocumentResponse setTags(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                    @Valid @RequestBody SetTagsCommand command) {
        return service.setTags(companyId, id, command);
    }

    @PostMapping("/{id}/links")
    @RequiresPermission("documents.document.write")
    public DocumentResponse addLink(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                    @Valid @RequestBody AddLinkCommand command) {
        return service.addLink(companyId, id, command);
    }

    @DeleteMapping("/{id}/links")
    @RequiresPermission("documents.document.write")
    public DocumentResponse removeLink(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                       @RequestParam String model, @RequestParam UUID recordId) {
        return service.removeLink(companyId, id, model, recordId);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("documents.document.delete")
    public ResponseEntity<Void> trash(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        service.trash(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @RequiresPermission("documents.trash.manage")
    public DocumentResponse restore(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                    @RequestBody(required = false) RestoreDocumentCommand command) {
        return service.restore(companyId, id, command);
    }

    @DeleteMapping("/trash")
    @RequiresPermission("documents.trash.manage")
    public Map<String, Integer> emptyTrash(@CurrentCompany CompanyId companyId) {
        return Map.of("purged", service.emptyTrash(companyId));
    }

    @PostMapping("/{id}/retention")
    @RequiresPermission("documents.trash.manage")
    public DocumentResponse lockRetention(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                          @Valid @RequestBody LockRetentionCommand command) {
        return service.lockRetention(companyId, id, command);
    }
}
