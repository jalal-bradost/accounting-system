package com.jalaldeveloper.accountingsystem.platform.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.platform.activity.ActivityMessage;
import com.jalaldeveloper.accountingsystem.platform.activity.ports.ActivityMessageRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.mapper.ActivityMessageDataAccessMapper;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.ActivityMessageJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
    public Page<ActivityMessage> findByCompanyIdAndAssigneeIdAndCompletedAtIsNullOrderByDueDateAsc(
            UUID companyId, String assigneeId, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndAssigneeIdAndCompletedAtIsNullOrderByDueDateAsc(
                companyId, assigneeId, pageable).map(mapper::entityToDomain);
    }
}
