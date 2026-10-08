package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.service.domain.dto.BackfillResponse;
import com.bradox.erp.timesheet.service.domain.dto.PostingResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.PostingApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Labor cost postings to the ledger (TSH-10). Without {@code tsh.posting.manage} every call is 403. */
@RestController
@RequestMapping(value = "/api/v1/timesheet/postings", produces = "application/json")
public class PostingController {

    private final PostingApplicationService postings;

    public PostingController(PostingApplicationService postings) {
        this.postings = postings;
    }

    @GetMapping
    @RequiresPermission("tsh.posting.manage")
    public List<PostingResponse> list(@CurrentCompany CompanyId companyId, @RequestParam(required = false) List<PostingStatus> status) {
        return postings.list(companyId, status);
    }

    /** The posting behind a journal entry, or 204 when the entry is not from a timesheet. */
    @GetMapping("/by-entry/{journalEntryId}")
    @RequiresPermission("tsh.posting.manage")
    public org.springframework.http.ResponseEntity<PostingResponse> bySource(@CurrentCompany CompanyId companyId,
                                                                            @PathVariable UUID journalEntryId) {
        PostingResponse p = postings.bySource(companyId, journalEntryId);
        return p == null ? org.springframework.http.ResponseEntity.noContent().build() : org.springframework.http.ResponseEntity.ok(p);
    }

    @GetMapping("/integrity")
    @RequiresPermission("tsh.posting.manage")
    public com.bradox.erp.timesheet.service.domain.dto.IntegrityResponse integrity(@CurrentCompany CompanyId companyId) {
        return postings.integrity(companyId);
    }

    @PostMapping("/{id}/retry")
    @RequiresPermission("tsh.posting.manage")
    public PostingResponse retry(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return postings.retry(companyId, id);
    }

    @PostMapping("/backfill")
    @RequiresPermission("tsh.posting.manage")
    public BackfillResponse backfill(@CurrentCompany CompanyId companyId) {
        return postings.backfill(companyId);
    }
}
