package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.PK> {

    List<UserRoleEntity> findByUserId(UUID userId);

    List<UserRoleEntity> findByUserIdIn(Collection<UUID> userIds);

    long countByRoleId(UUID roleId);

    void deleteByUserId(UUID userId);

    void deleteByRoleId(UUID roleId);
}
