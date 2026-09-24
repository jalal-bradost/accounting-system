package com.bradox.erp.platform.activity;

import com.bradox.erp.platform.activity.ports.ActivityMessageRepository;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class ActivityApplicationServiceImpl implements ActivityApplicationService {

    private final ActivityMessageRepository repository;
    private final ActivityAssigneeService assigneeService;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    ActivityApplicationServiceImpl(ActivityMessageRepository repository,
                                   ActivityAssigneeService assigneeService,
                                   ObjectProvider<CompanyContext> companyContextProvider) {
        this.repository = repository;
        this.assigneeService = assigneeService;
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
        if (command.getKind() == ActivityKind.ACTIVITY_TODO) {
            assigneeService.validateAssignee(companyId, command.getModelName(), command.getAssigneeId());
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
        List<String> keys = assigneeService.resolveAssigneeLookupKeys(companyId, assigneeId);
        return repository.findOpenTodosByAssignee(companyId, keys, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityInboxSummaryResponse inboxSummary(UUID companyId, String assigneeId) {
        List<String> keys = assigneeService.resolveAssigneeLookupKeys(companyId, assigneeId);
        LocalDate asOf = LocalDate.now();
        List<ActivityMessage> open = repository.findOpenTodosByAssignee(companyId, keys);

        Map<String, ActivityInboxBucketCounts> byModel = new HashMap<>();
        ActivityInboxBucketCounts totals = ActivityInboxBucketCounts.empty();

        for (ActivityMessage message : open) {
            ActivityInboxBucket bucket = ActivityInboxBucket.of(message.getDueDate(), asOf);
            totals = totals.increment(bucket);
            String model = message.getModelName() != null ? message.getModelName() : "";
            byModel.merge(model, ActivityInboxBucketCounts.empty().increment(bucket), (a, b) ->
                    new ActivityInboxBucketCounts(
                            a.late() + b.late(),
                            a.today() + b.today(),
                            a.future() + b.future()));
        }

        List<ActivityInboxModelGroup> groups = new ArrayList<>();
        for (Map.Entry<String, ActivityInboxBucketCounts> entry : byModel.entrySet()) {
            ActivityInboxBucketCounts c = entry.getValue();
            groups.add(new ActivityInboxModelGroup(entry.getKey(), c.late(), c.today(), c.future()));
        }
        groups.sort(Comparator
                .comparingInt(ActivityInboxModelGroup::late).reversed()
                .thenComparing(ActivityInboxModelGroup::modelName, String.CASE_INSENSITIVE_ORDER));

        return new ActivityInboxSummaryResponse(asOf, totals, groups);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> inbox(UUID companyId,
                                        String assigneeId,
                                        String modelName,
                                        ActivityInboxBucket bucket,
                                        ActivityInboxStatus status,
                                        Pageable pageable) {
        List<String> keys = assigneeService.resolveAssigneeLookupKeys(companyId, assigneeId);
        LocalDate asOf = LocalDate.now();
        ActivityInboxStatus effective = status != null ? status : ActivityInboxStatus.ALL;
        boolean includeOpen = effective == ActivityInboxStatus.ALL || effective == ActivityInboxStatus.OPEN;
        boolean includeDone = effective == ActivityInboxStatus.ALL || effective == ActivityInboxStatus.DONE;
        boolean includeLate = bucket == null || bucket == ActivityInboxBucket.LATE;
        boolean includeToday = bucket == null || bucket == ActivityInboxBucket.TODAY;
        boolean includeFuture = bucket == null || bucket == ActivityInboxBucket.FUTURE;
        String modelFilter = (modelName == null || modelName.isBlank()) ? null : modelName.trim();
        return repository.findTodosInbox(
                        companyId, keys, modelFilter, asOf,
                        includeOpen, includeDone,
                        includeLate, includeToday, includeFuture, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityAssigneeResponse> listAssignees(UUID companyId, String modelName) {
        return assigneeService.listAssignees(companyId, modelName);
    }

    private ActivityResponse toResponse(ActivityMessage e) {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        String author = e.getAuthorId();
        if (ctx != null) {
            author = ctx.resolveUserDisplay(author);
        }
        return new ActivityResponse(
                e.getId(), e.getCompanyId(), e.getModelName(), e.getRecordId(),
                e.getKind(), e.getSubject(), e.getBody(),
                author, e.getAssigneeId(), e.getDueDate(),
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
