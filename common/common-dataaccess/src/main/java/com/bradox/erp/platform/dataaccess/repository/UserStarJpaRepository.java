package com.bradox.erp.platform.dataaccess.repository;

import com.bradox.erp.platform.dataaccess.entity.UserStarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStarJpaRepository extends JpaRepository<UserStarEntity, UUID> {

    List<UserStarEntity> findByCompanyIdAndUserIdAndModelName(UUID companyId, UUID userId, String modelName);

    Optional<UserStarEntity> findByCompanyIdAndUserIdAndModelNameAndRecordId(
            UUID companyId, UUID userId, String modelName, UUID recordId);
}
