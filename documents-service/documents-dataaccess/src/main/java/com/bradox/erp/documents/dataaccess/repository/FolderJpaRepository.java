package com.bradox.erp.documents.dataaccess.repository;

import com.bradox.erp.documents.dataaccess.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FolderJpaRepository extends JpaRepository<FolderEntity, UUID> {

    List<FolderEntity> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
