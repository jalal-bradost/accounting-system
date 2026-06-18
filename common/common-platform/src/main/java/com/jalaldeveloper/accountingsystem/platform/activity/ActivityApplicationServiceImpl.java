package com.jalaldeveloper.accountingsystem.platform.activity;

import com.jalaldeveloper.accountingsystem.platform.activity.ports.ActivityMessageRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.UUID;

@Service
@Validated
class ActivityApplicationServiceImpl implements ActivityApplicationService {

    private final ActivityMessageRepository repository;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    ActivityApplicationServiceImpl(ActivityMessageRepository repository,
                                   ObjectProvider<CompanyContext> companyContextProvider) {
        this.repository = repository;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public ActivityResponse create(CreateActivityCommand command) {
        UUID companyId = command.getCompanyId() != null
                ? command.getCompanyId()
                : currentCompanyId();
        if (companyId == null) {
            throw new IllegalArgumentException("companyId required (header X-Company-Id, query param, or body)");
        }
        ActivityMessage message = new ActivityMessage();
        message.setId(UUID.randomUUID());
        message.setCompanyId(companyId);
        message.setModelName(command.getModelName());
        message.setRecordId(command.getRecordId());
        message.setKind(command.getKind());
        message.setSubject(command.getSubject());
        message.setBody(command.getBody());
        message.setAuthorId(currentUser());
        message.setAssigneeId(command.getAssigneeId());
        message.setDueDate(command.getDueDate());
        message.setCreatedAt(Instant.now());
        return toResponse(repository.save(message));
    }

    @Override
    @Transactional
    public ActivityResponse complete(UUID activityId) {
        ActivityMessage message = repository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));
        if (message.getKind() != ActivityKind.ACTIVITY_TODO) {
            throw new IllegalArgumentException("Only ACTIVITY_TODO entries can be completed");
        }
        if (message.getCompletedAt() == null) {
            message.setCompletedAt(Instant.now());
            message = repository.save(message);
        }
        return toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponse get(UUID activityId) {
        return repository.findById(activityId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> feed(UUID companyId, String modelName, UUID recordId, Pageable pageable) {
        return repository.findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
                companyId, modelName, recordId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> openTodos(UUID companyId, String assigneeId, Pageable pageable) {
        return repository.findByCompanyIdAndAssigneeIdAndCompletedAtIsNullOrderByDueDateAsc(
                companyId, assigneeId, pageable).map(this::toResponse);
    }

    private ActivityResponse toResponse(ActivityMessage e) {
        return new ActivityResponse(
                e.getId(), e.getCompanyId(), e.getModelName(), e.getRecordId(),
                e.getKind(), e.getSubject(), e.getBody(),
                e.getAuthorId(), e.getAssigneeId(), e.getDueDate(),
                e.getCompletedAt(), e.getCreatedAt());
    }

    private UUID currentCompanyId() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? null : ctx.currentCompany().map(c -> c.getId()).orElse(null);
    }

    private String currentUser() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? "system" : ctx.currentUserDisplay();
    }
}
