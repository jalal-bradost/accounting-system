package com.bradox.erp.platform.dataaccess.repository;

import com.bradox.erp.platform.dataaccess.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByCode(String code);

    List<PermissionEntity> findByCodeIn(List<String> codes);

    boolean existsByCode(String code);
}
