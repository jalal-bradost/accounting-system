package com.bradox.delin.platform.activity.ports;

import com.bradox.delin.platform.activity.ActivityMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityMessageRepository {

    ActivityMessage save(ActivityMessage message);

    Optional<ActivityMessage> findById(UUID activityId);

    Page<ActivityMessage> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId, Pageable pageable);

    Page<ActivityMessage> findOpenTodosByAssignee(
            UUID companyId, Collection<String> assigneeIds, Pageable pageable);

    List<ActivityMessage> findOpenTodosByAssignee(UUID companyId, Collection<String> assigneeIds);

    Page<ActivityMessage> findTodosInbox(
            UUID companyId,
            Collection<String> assigneeIds,
            String modelName,
            LocalDate asOfDate,
            boolean includeOpen,
            boolean includeDone,
            boolean includeLate,
            boolean includeToday,
            boolean includeFuture,
            Pageable pageable);
}
