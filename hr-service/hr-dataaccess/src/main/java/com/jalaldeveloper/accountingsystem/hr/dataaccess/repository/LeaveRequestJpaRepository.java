package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.LeaveRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestJpaRepository extends JpaRepository<LeaveRequestEntity, UUID> {

    @Query("""
        SELECT r FROM LeaveRequestEntity r
        WHERE r.companyId = :companyId
          AND (:employeeId IS NULL OR r.employeeId = :employeeId)
          AND (:state IS NULL OR r.state = :state)
          AND r.dateFrom <= :to
          AND r.dateTo >= :from
        ORDER BY r.dateFrom DESC
        """)
    List<LeaveRequestEntity> search(@Param("companyId") UUID companyId,
                                    @Param("employeeId") UUID employeeId,
                                    @Param("state") String state,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(r.numberOfDays), 0) FROM LeaveRequestEntity r
        WHERE r.companyId = :companyId
          AND r.employeeId = :employeeId
          AND r.timeOffTypeId = :typeId
          AND r.state = 'validate'
          AND r.dateFrom <= :dateTo
          AND r.dateTo >= :dateFrom
        """)
    java.math.BigDecimal sumValidatedDays(@Param("companyId") UUID companyId,
                                          @Param("employeeId") UUID employeeId,
                                          @Param("typeId") UUID typeId,
                                          @Param("dateFrom") LocalDate dateFrom,
                                          @Param("dateTo") LocalDate dateTo);

    long countByCompanyIdAndEmployeeIdAndState(UUID companyId, UUID employeeId, String state);
}
