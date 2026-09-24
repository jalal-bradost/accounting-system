package com.bradox.erp.platform.activity;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Generic chatter / activity feed API; usable from any module on any record id. */
public interface ActivityApplicationService {

    ActivityResponse create(@Valid CreateActivityCommand command);

    ActivityResponse complete(UUID activityId);

    ActivityResponse get(UUID activityId);

    Page<ActivityResponse> feed(UUID companyId, String modelName, UUID recordId, Pageable pageable);

    Page<ActivityResponse> openTodos(UUID companyId, String assigneeId, Pageable pageable);

    ActivityInboxSummaryResponse inboxSummary(UUID companyId, String assigneeId);

    Page<ActivityResponse> inbox(UUID companyId,
                                 String assigneeId,
                                 String modelName,
                                 ActivityInboxBucket bucket,
                                 ActivityInboxStatus status,
                                 Pageable pageable);

    /** Active users who may be assigned an activity on the given model. */
    List<ActivityAssigneeResponse> listAssignees(UUID companyId, String modelName);
}
