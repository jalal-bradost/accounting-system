package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestJpaRepository extends JpaRepository<RequestEntity, UUID>, JpaSpecificationExecutor<RequestEntity> {

    Optional<RequestEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<RequestEntity> findByCompanyIdAndRecordModelAndRecordIdOrderByCreatedAtDesc(UUID companyId, String recordModel, UUID recordId);

    List<RequestEntity> findByStatusInAndExpiresAtBefore(Collection<String> statuses, Instant before);

    List<RequestEntity> findByStatusInAndReminderEveryDaysNotNull(Collection<String> statuses);

    List<RequestEntity> findByStatusAndFinalDocumentIdIsNull(String status);

    Optional<RequestEntity> findFirstByFinalSha256(String sha256);

    long countByTemplateId(UUID templateId);

    @Query("select r from RequestEntity r where r.status = 'IN_PROGRESS' and r.finalDocumentId is null "
            + "and not exists (select 1 from SignerEntity s where s.requestId = r.id and s.status <> 'SIGNED')")
    List<RequestEntity> findAwaitingFinal();

    @Query("select count(s) from SignerEntity s, RequestEntity r where s.requestId = r.id and r.companyId = :companyId "
            + "and s.userId = :userId and s.status in ('PENDING', 'VIEWED') and r.status in ('SENT', 'IN_PROGRESS')")
    long countWaitingForUser(@Param("companyId") UUID companyId, @Param("userId") UUID userId);
}
