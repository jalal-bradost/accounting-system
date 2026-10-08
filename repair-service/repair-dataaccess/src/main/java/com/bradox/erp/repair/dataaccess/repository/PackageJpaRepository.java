package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.PackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackageJpaRepository extends JpaRepository<PackageEntity, UUID> {

    List<PackageEntity> findByCompanyIdOrderByNameAsc(UUID companyId);
}
