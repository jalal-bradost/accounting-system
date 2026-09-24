package com.bradox.delin.platform.dataaccess.adapter;

import com.bradox.delin.platform.activity.ActivityKind;
import com.bradox.delin.platform.activity.ActivityMessage;
import com.bradox.delin.platform.activity.ports.ActivityMessageRepository;
import com.bradox.delin.platform.dataaccess.mapper.ActivityMessageDataAccessMapper;
import com.bradox.delin.platform.dataaccess.repository.ActivityMessageJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ActivityMessageRepositoryImpl implements ActivityMessageRepository {

    private final ActivityMessageJpaRepository jpaRepository;
    private final ActivityMessageDataAccessMapper mapper;

    public ActivityMessageRepositoryImpl(ActivityMessageJpaRepository jpaRepository,
                                         ActivityMessageDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ActivityMessage save(ActivityMessage message) {
        if (message.getId() != null) {
            return jpaRepository.findById(message.getId())
                    .map(existing -> {
                        existing.setCompletedAt(message.getCompletedAt());
                        return mapper.entityToDomain(jpaRepository.save(existing));
                    })
                    .orElseGet(() -> mapper.entityToDomain(jpaRepository.save(mapper.domainToEntity(message))));
        }
        return mapper.entityToDomain(jpaRepository.save(mapper.domainToEntity(message)));
    }

    @Override
    public Optional<ActivityMessage> findById(UUID activityId) {
        return jpaRepository.findById(activityId).map(mapper::entityToDomain);
    }

    @Override
    public Page<ActivityMessage> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
                companyId, modelName, recordId, pageable).map(mapper::entityToDomain);
    }

    @Override
    public Page<ActivityMessage> findOpenTodosByAssignee(
            UUID companyId, Collection<String> assigneeIds, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndAssigneeIdInAndKindAndCompletedAtIsNullOrderByDueDateAsc(
                        companyId, assigneeIds, ActivityKind.ACTIVITY_TODO, pageable)
                .map(mapper::entityToDomain);
    }

    @Override
    public List<ActivityMessage> findOpenTodosByAssignee(UUID companyId, Collection<String> assigneeIds) {
        return jpaRepository.findByCompanyIdAndAssigneeIdInAndKindAndCompletedAtIsNull(
                        companyId, assigneeIds, ActivityKind.ACTIVITY_TODO)
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public Page<ActivityMessage> findTodosInbox(
            UUID companyId,
            Collection<String> assigneeIds,
            String modelName,
            LocalDate asOfDate,
            boolean includeOpen,
            boolean includeDone,
            boolean includeLate,
            boolean includeToday,
            boolean includeFuture,
            Pageable pageable) {
        return jpaRepository.findTodosInbox(
                        companyId,
                        assigneeIds,
                        ActivityKind.ACTIVITY_TODO,
                        modelName,
                        asOfDate,
                        includeOpen,
                        includeDone,
                        includeLate,
                        includeToday,
                        includeFuture,
                        pageable)
                .map(mapper::entityToDomain);
    }
}
