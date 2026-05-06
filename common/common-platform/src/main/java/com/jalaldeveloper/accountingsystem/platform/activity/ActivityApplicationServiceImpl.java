package com.jalaldeveloper.accountingsystem.platform.activity;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ActivityMessageEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.ActivityMessageJpaRepository;
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

    private final ActivityMessageJpaRepository repository;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    ActivityApplicationServiceImpl(ActivityMessageJpaRepository repository,
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
        ActivityMessageEntity entity = new ActivityMessageEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        entity.setModelName(command.getModelName());
        entity.setRecordId(command.getRecordId());
        entity.setKind(command.getKind());
        entity.setSubject(command.getSubject());
        entity.setBody(command.getBody());
        entity.setAuthorId(currentUser());
        entity.setAssigneeId(command.getAssigneeId());
        entity.setDueDate(command.getDueDate());
        entity.setCreatedAt(Instant.now());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ActivityResponse complete(UUID activityId) {
        ActivityMessageEntity entity = repository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));
        if (entity.getKind() != ActivityKind.ACTIVITY_TODO) {
            throw new IllegalArgumentException("Only ACTIVITY_TODO entries can be completed");
        }
        if (entity.getCompletedAt() == null) {
            entity.setCompletedAt(Instant.now());
        }
        return toResponse(entity);
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

    private ActivityResponse toResponse(ActivityMessageEntity e) {
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
