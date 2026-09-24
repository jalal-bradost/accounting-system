package com.bradox.erp.platform.dataaccess.repository;

import com.bradox.erp.platform.dataaccess.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.PK> {

    List<UserRoleEntity> findByUserId(UUID userId);

    List<UserRoleEntity> findByUserIdIn(Collection<UUID> userIds);

    List<UserRoleEntity> findByRoleIdIn(Collection<UUID> roleIds);

    long countByRoleId(UUID roleId);

    void deleteByUserId(UUID userId);

    void deleteByRoleId(UUID roleId);
}
