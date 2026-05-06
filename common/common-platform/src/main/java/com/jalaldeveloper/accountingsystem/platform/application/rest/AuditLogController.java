package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AuditLogEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AuditLogJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/audit-log", produces = "application/json")
public class AuditLogController {

    private final AuditLogJpaRepository repository;

    public AuditLogController(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @RequiresPermission("platform.audit.read")
    public ResponseEntity<Page<AuditLogEntity>> list(@RequestParam UUID companyId,
                                                     @RequestParam(required = false) String model,
                                                     @RequestParam(required = false) UUID recordId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size);
        if (model != null && recordId != null) {
            return ResponseEntity.ok(repository
                    .findByCompanyIdAndModelNameAndRecordIdOrderByOccurredAtDesc(companyId, model, recordId, pageable));
        }
        return ResponseEntity.ok(repository.findByCompanyIdOrderByOccurredAtDesc(companyId, pageable));
    }
}
