package com.bradox.erp.documents.dataaccess.repository;

import com.bradox.erp.documents.dataaccess.entity.DocumentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {

    Optional<DocumentEntity> findFirstByCompanyIdAndFolderIdAndSha256AndStatus(
            UUID companyId, UUID folderId, String sha256, String status);

    Optional<DocumentEntity> findFirstByCompanyIdAndFolderIdAndSha256AndStatusAndIdNot(
            UUID companyId, UUID folderId, String sha256, String status, UUID id);

    @Query("select d from DocumentEntity d join d.links l "
            + "where d.companyId = :companyId and l.modelName = :modelName and l.recordId = :recordId "
            + "and d.status = 'ACTIVE' order by d.updatedAt desc")
    List<DocumentEntity> findByRecord(@Param("companyId") UUID companyId,
                                      @Param("modelName") String modelName,
                                      @Param("recordId") UUID recordId);

    @Query("select coalesce(sum(d.sizeBytes), 0) from DocumentEntity d where d.companyId = :companyId")
    long sumSizes(@Param("companyId") UUID companyId);

    long countByCompanyIdAndStatus(UUID companyId, String status);

    List<DocumentEntity> findByCompanyIdAndStatusOrderByTrashedAtDesc(UUID companyId, String status);

    List<DocumentEntity> findByStatusAndTrashedAtBefore(String status, Instant cutoff, Pageable pageable);
}
