package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.WeekEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeekJpaRepository extends JpaRepository<WeekEntity, UUID>, JpaSpecificationExecutor<WeekEntity> {

    Optional<WeekEntity> findByCompanyIdAndEmployeeIdAndWeekStart(UUID companyId, UUID employeeId, LocalDate weekStart);

    List<WeekEntity> findByCompanyIdAndWeekStartAndEmployeeIdIn(UUID companyId, LocalDate weekStart, Collection<UUID> employeeIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update WeekEntity w set w.locked = true where w.companyId = :companyId and w.locked = false and w.weekStart < :cutoff")
    int lockStartedBefore(@Param("companyId") UUID companyId, @Param("cutoff") LocalDate cutoff);
}
