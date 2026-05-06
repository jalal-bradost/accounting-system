package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, RolePermissionEntity.PK> {

    List<RolePermissionEntity> findByRoleIdIn(List<UUID> roleIds);
}
