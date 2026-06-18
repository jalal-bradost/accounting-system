package com.jalaldeveloper.accountingsystem.platform.activity.ports;

import com.jalaldeveloper.accountingsystem.platform.activity.ActivityMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ActivityMessageRepository {

    ActivityMessage save(ActivityMessage message);

    Optional<ActivityMessage> findById(UUID activityId);

    Page<ActivityMessage> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId, Pageable pageable);

    Page<ActivityMessage> findByCompanyIdAndAssigneeIdAndCompletedAtIsNullOrderByDueDateAsc(
            UUID companyId, String assigneeId, Pageable pageable);
}
