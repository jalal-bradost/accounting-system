package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.chatter.AddFollowersCommand;
import com.jalaldeveloper.accountingsystem.platform.chatter.ChatterApplicationService;
import com.jalaldeveloper.accountingsystem.platform.chatter.ChatterAttachmentResponse;
import com.jalaldeveloper.accountingsystem.platform.chatter.ChatterFollowerResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/chatter", produces = "application/json")
public class ChatterController {

    private final ChatterApplicationService service;

    public ChatterController(ChatterApplicationService service) {
        this.service = service;
    }

    @GetMapping("/followers")
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<List<ChatterFollowerResponse>> followers(@RequestParam UUID companyId,
                                                                   @RequestParam String model,
                                                                   @RequestParam UUID recordId) {
        return ResponseEntity.ok(service.listFollowers(companyId, model, recordId));
    }

    @PostMapping("/followers")
    @RequiresPermission("platform.activity.write")
    public ResponseEntity<List<ChatterFollowerResponse>> addFollowers(@Valid @RequestBody AddFollowersCommand command) {
        return ResponseEntity.ok(service.addFollowers(command));
    }

    @DeleteMapping("/followers/{id}")
    @RequiresPermission("platform.activity.write")
    public ResponseEntity<Void> removeFollower(@PathVariable UUID id) {
        service.removeFollower(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attachments")
    @RequiresPermission("platform.activity.read")
    public ResponseEntity<List<ChatterAttachmentResponse>> attachments(@RequestParam UUID companyId,
                                                                       @RequestParam String model,
                                                                       @RequestParam UUID recordId) {
        return ResponseEntity.ok(service.listAttachments(companyId, model, recordId));
    }

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission("platform.activity.write")
    public ResponseEntity<ChatterAttachmentResponse> uploadAttachment(
            @RequestParam UUID companyId,
            @RequestParam String model,
            @RequestParam UUID recordId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadAttachment(companyId, model, recordId, file));
    }

    @DeleteMapping("/attachments/{id}")
    @RequiresPermission("platform.activity.write")
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID id) {
        service.deleteAttachment(id);
        return ResponseEntity.noContent().build();
    }
}
