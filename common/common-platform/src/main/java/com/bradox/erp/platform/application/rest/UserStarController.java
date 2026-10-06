package com.bradox.erp.platform.application.rest;

import com.bradox.erp.platform.star.UserStarService;
import com.bradox.erp.platform.web.CompanyContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** A user's starred (favourite) records, per model. Needs no special permission: they are personal. */
@RestController
@RequestMapping(value = "/api/v1/platform/stars", produces = "application/json")
public class UserStarController {

    private final UserStarService service;
    private final CompanyContext companyContext;

    public UserStarController(UserStarService service, CompanyContext companyContext) {
        this.service = service;
        this.companyContext = companyContext;
    }

    public record StarredResponse(String model, List<UUID> recordIds) {}

    public record SetStarRequest(
            @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9._-]+") String model,
            @NotNull UUID recordId,
            boolean starred) {}

    @GetMapping
    public ResponseEntity<StarredResponse> starred(@RequestParam @Pattern(regexp = "[a-z0-9._-]{1,100}") String model) {
        UUID user = requireUser();
        UUID company = companyContext.requireCompany().getId();
        return ResponseEntity.ok(new StarredResponse(model, List.copyOf(service.starred(company, user, model))));
    }

    @PutMapping
    public ResponseEntity<SetStarRequest> set(@Valid @RequestBody SetStarRequest request) {
        UUID user = requireUser();
        UUID company = companyContext.requireCompany().getId();
        service.setStarred(company, user, request.model(), request.recordId(), request.starred());
        return ResponseEntity.ok(request);
    }

    private UUID requireUser() {
        return companyContext.currentUser().map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
