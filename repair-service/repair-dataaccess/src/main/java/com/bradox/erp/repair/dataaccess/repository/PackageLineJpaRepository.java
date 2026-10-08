package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.PackageLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackageLineJpaRepository extends JpaRepository<PackageLineEntity, UUID> {

    List<PackageLineEntity> findByPackageIdInOrderBySequenceAsc(Collection<UUID> packageIds);

    void deleteByPackageId(UUID packageId);
}
