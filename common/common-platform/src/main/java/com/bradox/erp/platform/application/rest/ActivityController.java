package com.bradox.erp.platform.application.rest;

import com.bradox.erp.platform.activity.ActivityApplicationService;
import com.bradox.erp.platform.activity.ActivityAssigneeResponse;
import com.bradox.erp.platform.activity.ActivityInboxBucket;
import com.bradox.erp.platform.activity.ActivityInboxStatus;
import com.bradox.erp.platform.activity.ActivityInboxSummaryResponse;
import com.bradox.erp.platform.activity.ActivityResponse;
import com.bradox.erp.platform.activity.CreateActivityCommand;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.platform.security.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/activities", produces = "application/json")
public class ActivityController {

    private final ActivityApplicationService service;

    public ActivityController(ActivityApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("platform.activity.write")
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody CreateActivityCommand command) {
        return ResponseEntity.ok(service.create(command));
    }

    @GetMapping
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<PageResponse<ActivityResponse>> feed(@RequestParam UUID companyId,
                                                       @RequestParam String model,
                                                       @RequestParam UUID recordId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(PageResponse.of(
                service.feed(companyId, model, recordId, PageRequest.of(page, size)),
                r -> r));
    }

    @GetMapping("/todos")
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<PageResponse<ActivityResponse>> todos(@RequestParam UUID companyId,
                                                        @RequestParam(required = false) String assigneeId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(PageResponse.of(
                service.openTodos(companyId, assigneeId, PageRequest.of(page, size)),
                r -> r));
    }

    @GetMapping("/inbox/summary")
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<ActivityInboxSummaryResponse> inboxSummary(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String assigneeId) {
        return ResponseEntity.ok(service.inboxSummary(companyId, assigneeId));
    }

    @GetMapping("/inbox")
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<PageResponse<ActivityResponse>> inbox(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        ActivityInboxBucket parsedBucket = null;
        if (bucket != null && !bucket.isBlank()) {
            parsedBucket = ActivityInboxBucket.parse(bucket);
        }
        ActivityInboxStatus parsedStatus = ActivityInboxStatus.parse(status);
        return ResponseEntity.ok(PageResponse.of(
                service.inbox(companyId, assigneeId, model, parsedBucket, parsedStatus, PageRequest.of(page, size)),
                r -> r));
    }

    @GetMapping("/assignees")
    @RequiresPermission("platform.activity.write")
    public ResponseEntity<List<ActivityAssigneeResponse>> assignees(@RequestParam UUID companyId,
                                                                    @RequestParam String model) {
        return ResponseEntity.ok(service.listAssignees(companyId, model));
    }

    @GetMapping("/{id}")
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<ActivityResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/complete")
    @RequiresPermission("platform.activity.write")
    public ResponseEntity<ActivityResponse> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(service.complete(id));
    }
}
