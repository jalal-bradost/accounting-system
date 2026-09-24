package com.bradox.erp.platform.dataaccess.repository;

import com.bradox.erp.platform.dataaccess.entity.PlatDocumentSequenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentSequenceJpaRepository extends JpaRepository<PlatDocumentSequenceEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PlatDocumentSequenceEntity s WHERE s.companyId = :companyId AND s.docType = :docType AND s.year = :year")
    Optional<PlatDocumentSequenceEntity> findForUpdate(
            @Param("companyId") UUID companyId,
            @Param("docType") String docType,
            @Param("year") int year);
}
