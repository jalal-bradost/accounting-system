package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.WeekPostingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeekPostingJpaRepository extends JpaRepository<WeekPostingEntity, UUID> {

    Optional<WeekPostingEntity> findFirstByWeekIdAndStatusNot(UUID weekId, String status);

    List<WeekPostingEntity> findByWeekIdInOrderByVersionDesc(Collection<UUID> weekIds);

    List<WeekPostingEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<WeekPostingEntity> findByCompanyIdAndStatusInOrderByCreatedAtDesc(UUID companyId, Collection<String> statuses);

    @Query("select coalesce(sum(p.totalAmount), 0) from WeekPostingEntity p where p.companyId = :companyId "
            + "and p.status = 'POSTED' and p.entryDate >= :from and p.entryDate <= :to")
    java.math.BigDecimal sumPosted(@Param("companyId") UUID companyId, @Param("from") java.time.LocalDate from,
                                   @Param("to") java.time.LocalDate to);

    Optional<WeekPostingEntity> findFirstByCompanyIdAndJournalEntryId(UUID companyId, UUID journalEntryId);

    Optional<WeekPostingEntity> findFirstByCompanyIdAndReversalEntryId(UUID companyId, UUID reversalEntryId);

    @Query("select coalesce(max(p.version), 0) from WeekPostingEntity p where p.weekId = :weekId")
    int maxVersion(@Param("weekId") UUID weekId);

    @Query("select p from WeekPostingEntity p where (p.status = 'FAILED' and p.attempts < 5) "
            + "or (p.status = 'PENDING' and p.createdAt < :staleBefore)")
    List<WeekPostingEntity> findRetryable(@Param("staleBefore") Instant staleBefore);

    @Query("select w.id from WeekEntity w where w.companyId = :companyId and w.status = 'APPROVED' and not exists "
            + "(select 1 from WeekPostingEntity p where p.weekId = w.id and p.status <> 'REVERSED') order by w.weekStart")
    List<UUID> approvedWeeksWithoutActivePosting(@Param("companyId") UUID companyId);
}
