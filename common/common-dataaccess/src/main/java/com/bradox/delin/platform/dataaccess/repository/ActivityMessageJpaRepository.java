package com.bradox.delin.platform.dataaccess.repository;

import com.bradox.delin.platform.activity.ActivityKind;
import com.bradox.delin.platform.dataaccess.entity.ActivityMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ActivityMessageJpaRepository extends JpaRepository<ActivityMessageEntity, UUID> {

    Page<ActivityMessageEntity> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId, Pageable pageable);

    Page<ActivityMessageEntity> findByCompanyIdAndAssigneeIdInAndKindAndCompletedAtIsNullOrderByDueDateAsc(
            UUID companyId, Collection<String> assigneeIds, ActivityKind kind, Pageable pageable);

    List<ActivityMessageEntity> findByCompanyIdAndAssigneeIdInAndKindAndCompletedAtIsNull(
            UUID companyId, Collection<String> assigneeIds, ActivityKind kind);

    @Query("""
            SELECT e FROM ActivityMessageEntity e
            WHERE e.companyId = :companyId
              AND e.assigneeId IN :assigneeIds
              AND e.kind = :kind
              AND (
                    (:includeOpen = true AND e.completedAt IS NULL)
                 OR (:includeDone = true AND e.completedAt IS NOT NULL)
              )
              AND (:modelName IS NULL OR e.modelName = :modelName)
              AND (
                    (:includeLate = true AND e.dueDate IS NOT NULL AND e.dueDate < :asOfDate)
                 OR (:includeToday = true AND e.dueDate IS NOT NULL AND e.dueDate = :asOfDate)
                 OR (:includeFuture = true AND (e.dueDate IS NULL OR e.dueDate > :asOfDate))
              )
            ORDER BY CASE WHEN e.completedAt IS NULL THEN 0 ELSE 1 END,
                     e.dueDate ASC,
                     e.createdAt ASC
            """)
    Page<ActivityMessageEntity> findTodosInbox(
            @Param("companyId") UUID companyId,
            @Param("assigneeIds") Collection<String> assigneeIds,
            @Param("kind") ActivityKind kind,
            @Param("modelName") String modelName,
            @Param("asOfDate") LocalDate asOfDate,
            @Param("includeOpen") boolean includeOpen,
            @Param("includeDone") boolean includeDone,
            @Param("includeLate") boolean includeLate,
            @Param("includeToday") boolean includeToday,
            @Param("includeFuture") boolean includeFuture,
            Pageable pageable);
}
