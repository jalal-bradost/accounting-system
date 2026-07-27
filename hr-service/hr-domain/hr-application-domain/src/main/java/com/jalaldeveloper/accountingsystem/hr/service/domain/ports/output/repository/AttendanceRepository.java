package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Attendance;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.AttendanceId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository {

    Attendance save(Attendance attendance);

    Optional<Attendance> findById(AttendanceId id);

    org.springframework.data.domain.Page<Attendance> search(CompanyId companyId,
                                                            UUID employeeId,
                                                            Instant from,
                                                            Instant to,
                                                            org.springframework.data.domain.Pageable pageable);

    List<Attendance> findForGantt(CompanyId companyId,
                                  UUID employeeId,
                                  Instant from,
                                  Instant to);
}
