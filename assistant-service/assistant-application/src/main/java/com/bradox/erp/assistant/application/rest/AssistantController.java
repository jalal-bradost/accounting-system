package com.bradox.erp.assistant.application.rest;

import com.bradox.erp.assistant.service.AssistantOrchestrator;
import com.bradox.erp.assistant.service.AssistantUnavailableException;
import com.bradox.erp.assistant.settings.AssistantSettingsResponse;
import com.bradox.erp.assistant.settings.AssistantSettingsService;
import com.bradox.erp.assistant.settings.AssistantSettingsUpdateRequest;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CompanyContext;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/assistant", produces = "application/json")
public class AssistantController {

    private final AssistantOrchestrator orchestrator;
    private final AssistantSettingsService settingsService;
    private final CompanyContext companyContext;

    public AssistantController(AssistantOrchestrator orchestrator,
                               AssistantSettingsService settingsService,
                               CompanyContext companyContext) {
        this.orchestrator = orchestrator;
        this.settingsService = settingsService;
        this.companyContext = companyContext;
    }

    @GetMapping("/settings")
    @RequiresPermission("platform.company.read")
    public ResponseEntity<AssistantSettingsResponse> getSettings(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(settingsService.getResponse(companyId));
    }

    /**
     * Lightweight enablement check for users who can chat but may not read full company settings.
     * Used by the shell to hide the assistant affordance when the company flag is off.
     */
    @GetMapping("/status")
    @RequiresPermission("platform.assistant.use")
    public ResponseEntity<AssistantStatusResponse> getStatus(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(new AssistantStatusResponse(
                settingsService.resolve(companyId).assistantEnabled()));
    }

    @PutMapping("/settings")
    @RequiresPermission("platform.company.write")
    public ResponseEntity<AssistantSettingsResponse> updateSettings(
            @CurrentCompany CompanyId companyId,
            @RequestBody AssistantSettingsUpdateRequest request) {
        return ResponseEntity.ok(settingsService.update(companyId, request));
    }

    @PostMapping("/chat")
    @RequiresPermission("platform.assistant.use")
    public ResponseEntity<?> chat(@CurrentCompany CompanyId companyId,
                                  @Valid @RequestBody ChatRequest request) {
        UserId userId = companyContext.currentUser().orElse(null);
        AssistantOrchestrator.ChatResponse response = orchestrator.chat(
                companyId, userId, request.conversationId(), request.message());
        return ResponseEntity.ok(new ChatResponseBody(
                response.conversationId(),
                response.reply(),
                response.artifacts(),
                response.toolTrace()));
    }

    @DeleteMapping("/conversations/{id}")
    @RequiresPermission("platform.assistant.use")
    public ResponseEntity<Void> clear(@PathVariable UUID id) {
        UserId userId = companyContext.currentUser().orElse(null);
        orchestrator.clearConversation(id, userId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(AssistantUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage()
                        : "The AI service is temporarily unavailable. Please try again."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    public record ChatRequest(UUID conversationId, @NotBlank String message) {}

    public record AssistantStatusResponse(boolean assistantEnabled) {}

    public record ChatResponseBody(
            UUID conversationId,
            String reply,
            List<Map<String, Object>> artifacts,
            List<Map<String, Object>> toolTrace
    ) {}
}
