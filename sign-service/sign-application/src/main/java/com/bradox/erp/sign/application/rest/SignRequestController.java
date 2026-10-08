package com.bradox.erp.sign.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.sign.service.domain.dto.CreateRequestCommand;
import com.bradox.erp.sign.service.domain.dto.DashboardResponse;
import com.bradox.erp.sign.service.domain.dto.EventResponse;
import com.bradox.erp.sign.service.domain.dto.ExtendCommand;
import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.ReasonCommand;
import com.bradox.erp.sign.service.domain.dto.ReminderTextResponse;
import com.bradox.erp.sign.service.domain.dto.RequestFilter;
import com.bradox.erp.sign.service.domain.dto.RequestResponse;
import com.bradox.erp.sign.service.domain.dto.RequestSummaryResponse;
import com.bradox.erp.sign.service.domain.dto.SendResultResponse;
import com.bradox.erp.sign.service.domain.dto.SignerInput;
import com.bradox.erp.sign.service.domain.dto.SignerLinkResponse;
import com.bradox.erp.sign.service.domain.dto.UpdateRequestCommand;
import com.bradox.erp.sign.service.domain.ports.input.RequestApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/sign/requests", produces = "application/json")
public class SignRequestController {

    /** Optional body of "send". */
    public record SendCommand(Integer validityDays) {
    }

    /** Optional body of "remind": which signer; the one whose turn it is when omitted. */
    public record RemindCommand(UUID signerId) {
    }

    private final RequestApplicationService requests;

    public SignRequestController(RequestApplicationService requests) {
        this.requests = requests;
    }

    @GetMapping
    @RequiresPermission("sign.request.view")
    public PageResponse<RequestSummaryResponse> list(@CurrentCompany CompanyId companyId,
                                                     @RequestParam(required = false) List<String> status,
                                                     @RequestParam(required = false) String signer,
                                                     @RequestParam(required = false) UUID template,
                                                     @RequestParam(required = false) String recordModel,
                                                     @RequestParam(required = false) UUID recordId,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(required = false) Instant from,
                                                     @RequestParam(required = false) Instant to,
                                                     @RequestParam(defaultValue = "false") boolean mine,
                                                     @RequestParam(defaultValue = "false") boolean needsAttention,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "25") int size) {
        return requests.list(companyId, new RequestFilter(status, signer, template, recordModel, recordId, q, from, to, mine,
                needsAttention, page, size));
    }

    @GetMapping("/dashboard")
    @RequiresPermission("sign.request.view")
    public DashboardResponse dashboard(@CurrentCompany CompanyId companyId) {
        return requests.dashboard(companyId);
    }

    @GetMapping("/by-record")
    @RequiresPermission("sign.request.view")
    public List<RequestSummaryResponse> byRecord(@CurrentCompany CompanyId companyId, @RequestParam String model, @RequestParam UUID recordId) {
        return requests.byRecord(companyId, model, recordId);
    }

    @GetMapping("/{id}")
    @RequiresPermission("sign.request.view")
    public RequestResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return requests.get(companyId, id);
    }

    @PostMapping
    @RequiresPermission("sign.request.create")
    public RequestResponse create(@CurrentCompany CompanyId companyId, @Valid @RequestBody CreateRequestCommand command) {
        return requests.create(companyId, command);
    }

    @PutMapping("/{id}")
    @RequiresPermission("sign.request.create")
    public RequestResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody UpdateRequestCommand command) {
        return requests.update(companyId, id, command);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sign.request.create")
    public void delete(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        requests.delete(companyId, id);
    }

    @PostMapping("/{id}/send")
    @RequiresPermission("sign.request.create")
    public SendResultResponse send(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                   @RequestBody(required = false) SendCommand command) {
        return requests.send(companyId, id, command == null ? null : command.validityDays());
    }

    @PostMapping("/{id}/signers/{signerId}/link")
    @RequiresPermission("sign.request.create")
    public SignerLinkResponse newLink(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @PathVariable UUID signerId) {
        return requests.newLink(companyId, id, signerId);
    }

    @PostMapping("/{id}/signers/{signerId}/replace")
    @RequiresPermission("sign.request.create")
    public RequestResponse replaceSigner(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @PathVariable UUID signerId,
                                         @RequestBody SignerInput signer) {
        return requests.replaceSigner(companyId, id, signerId, signer);
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("sign.request.cancel")
    public RequestResponse cancel(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody ReasonCommand command) {
        return requests.cancel(companyId, id, command);
    }

    @PostMapping("/{id}/extend")
    @RequiresPermission("sign.request.create")
    public RequestResponse extend(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody ExtendCommand command) {
        return requests.extend(companyId, id, command);
    }

    @PostMapping("/{id}/remind")
    @RequiresPermission("sign.request.create")
    public ReminderTextResponse remind(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                       @RequestBody(required = false) RemindCommand command) {
        return requests.remind(companyId, id, command == null ? null : command.signerId());
    }

    @GetMapping("/{id}/events")
    @RequiresPermission("sign.request.view")
    public List<EventResponse> events(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return requests.events(companyId, id);
    }

    @GetMapping(value = "/{id}/pages/{page}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @RequiresPermission("sign.request.view")
    public ResponseEntity<byte[]> page(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @PathVariable int page) {
        PageImage image = requests.page(companyId, id, page);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(image.png());
    }

    @GetMapping(value = "/{id}/final", produces = MediaType.APPLICATION_PDF_VALUE)
    @RequiresPermission("sign.request.view")
    public ResponseEntity<byte[]> finalDocument(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        FileContent f = requests.finalDocument(companyId, id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(f.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(f.bytes());
    }
}
