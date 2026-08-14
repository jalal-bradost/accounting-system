package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.DatasetImportRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetImportRefJpaRepository extends JpaRepository<DatasetImportRefEntity, DatasetImportRefEntity.Key> {

    Optional<DatasetImportRefEntity> findByCompanyIdAndRefTypeAndCode(UUID companyId, String refType, String code);

    List<DatasetImportRefEntity> findByCompanyIdAndRefType(UUID companyId, String refType);
}
