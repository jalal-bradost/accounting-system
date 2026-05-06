package com.jalaldeveloper.accountingsystem.platform.activity;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Generic chatter / activity feed API; usable from any module on any record id. */
public interface ActivityApplicationService {

    ActivityResponse create(@Valid CreateActivityCommand command);

    ActivityResponse complete(UUID activityId);

    ActivityResponse get(UUID activityId);

    Page<ActivityResponse> feed(UUID companyId, String modelName, UUID recordId, Pageable pageable);

    Page<ActivityResponse> openTodos(UUID companyId, String assigneeId, Pageable pageable);
}
