package com.bradox.delin.platform.dataaccess.repository;

import com.bradox.delin.platform.dataaccess.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCompanyIdAndCode(UUID companyId, String code);

    List<RoleEntity> findByCompanyId(UUID companyId);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
