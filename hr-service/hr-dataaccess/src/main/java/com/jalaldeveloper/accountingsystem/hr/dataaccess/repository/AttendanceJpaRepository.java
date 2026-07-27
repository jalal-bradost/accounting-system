package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.AttendanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AttendanceJpaRepository extends JpaRepository<AttendanceEntity, UUID> {

    @Query("""
        SELECT a FROM AttendanceEntity a
        WHERE a.companyId = :companyId
          AND (:employeeId IS NULL OR a.employeeId = :employeeId)
          AND a.checkIn >= :from
          AND a.checkIn < :to
        ORDER BY a.checkIn DESC
        """)
    Page<AttendanceEntity> search(@Param("companyId") UUID companyId,
                                  @Param("employeeId") UUID employeeId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to,
                                  Pageable pageable);

    @Query("""
        SELECT a FROM AttendanceEntity a
        WHERE a.companyId = :companyId
          AND (:employeeId IS NULL OR a.employeeId = :employeeId)
          AND a.checkIn >= :from
          AND a.checkIn < :to
        ORDER BY a.checkIn ASC
        """)
    List<AttendanceEntity> findForGantt(@Param("companyId") UUID companyId,
                                        @Param("employeeId") UUID employeeId,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to);

    @Query(value = """
        SELECT COUNT(DISTINCT CAST(a.check_in AS date))
        FROM hr_attendance a
        WHERE a.employee_id = :employeeId
          AND a.check_in >= :from
          AND a.check_in < :to
          AND a.check_out IS NOT NULL
        """, nativeQuery = true)
    long countWorkedDays(@Param("employeeId") UUID employeeId,
                         @Param("from") Instant from,
                         @Param("to") Instant to);
}
