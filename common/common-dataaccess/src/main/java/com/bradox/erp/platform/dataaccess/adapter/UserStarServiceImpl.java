package com.bradox.erp.platform.dataaccess.adapter;

import com.bradox.erp.platform.dataaccess.entity.UserStarEntity;
import com.bradox.erp.platform.dataaccess.repository.UserStarJpaRepository;
import com.bradox.erp.platform.star.UserStarService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserStarServiceImpl implements UserStarService {

    private final UserStarJpaRepository repository;

    public UserStarServiceImpl(UserStarJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> starred(UUID companyId, UUID userId, String model) {
        return repository.findByCompanyIdAndUserIdAndModelName(companyId, userId, model).stream()
                .map(UserStarEntity::getRecordId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void setStarred(UUID companyId, UUID userId, String model, UUID recordId, boolean starred) {
        var existing = repository.findByCompanyIdAndUserIdAndModelNameAndRecordId(companyId, userId, model, recordId);
        if (starred && existing.isEmpty()) {
            UserStarEntity e = new UserStarEntity();
            e.setId(UUID.randomUUID());
            e.setCompanyId(companyId);
            e.setUserId(userId);
            e.setModelName(model);
            e.setRecordId(recordId);
            e.setCreatedAt(Instant.now());
            repository.save(e);
        } else if (!starred) {
            existing.ifPresent(repository::delete);
        }
    }
}
