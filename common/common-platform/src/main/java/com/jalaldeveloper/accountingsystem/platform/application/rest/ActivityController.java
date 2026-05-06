package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.activity.ActivityApplicationService;
import com.jalaldeveloper.accountingsystem.platform.activity.ActivityResponse;
import com.jalaldeveloper.accountingsystem.platform.activity.CreateActivityCommand;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<Page<ActivityResponse>> feed(@RequestParam UUID companyId,
                                                       @RequestParam String model,
                                                       @RequestParam UUID recordId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.feed(companyId, model, recordId, PageRequest.of(page, size)));
    }

    @GetMapping("/todos")
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<Page<ActivityResponse>> todos(@RequestParam UUID companyId,
                                                        @RequestParam String assigneeId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.openTodos(companyId, assigneeId, PageRequest.of(page, size)));
    }
}
