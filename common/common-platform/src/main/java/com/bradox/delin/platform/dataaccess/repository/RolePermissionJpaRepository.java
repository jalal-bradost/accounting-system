package com.bradox.delin.platform.dataaccess.repository;

import com.bradox.delin.platform.dataaccess.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, RolePermissionEntity.PK> {

    List<RolePermissionEntity> findByRoleIdIn(Collection<UUID> roleIds);

    List<RolePermissionEntity> findByRoleId(UUID roleId);

    List<RolePermissionEntity> findByPermissionId(UUID permissionId);

    void deleteByRoleId(UUID roleId);
}
