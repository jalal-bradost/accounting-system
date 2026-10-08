package com.bradox.erp.documents.dataaccess.repository;

import com.bradox.erp.documents.dataaccess.entity.FolderAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FolderAccessJpaRepository extends JpaRepository<FolderAccessEntity, UUID> {

    List<FolderAccessEntity> findByCompanyId(UUID companyId);

    List<FolderAccessEntity> findByFolderId(UUID folderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FolderAccessEntity a where a.folderId = :folderId")
    void deleteAllForFolder(@Param("folderId") UUID folderId);
}
