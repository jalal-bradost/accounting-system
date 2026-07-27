package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.AttendanceResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.AttendanceSummaryResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.BulkCreateAttendanceCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateAttendanceCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.GenerateAttendanceFromScheduleCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateAttendanceCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AttendanceApplicationService {

    AttendanceResponse create(CreateAttendanceCommand cmd);

    AttendanceResponse update(UUID id, UpdateAttendanceCommand cmd);

    AttendanceResponse get(UUID id);

    Page<AttendanceSummaryResponse> search(CompanyId companyId,
                                           UUID employeeId,
                                           Instant from,
                                           Instant to,
                                           Pageable pageable);

    List<AttendanceSummaryResponse> gantt(CompanyId companyId,
                                          UUID employeeId,
                                          Instant from,
                                          Instant to);

    List<AttendanceResponse> bulkCreate(BulkCreateAttendanceCommand cmd);

    List<AttendanceResponse> generateFromSchedule(GenerateAttendanceFromScheduleCommand cmd);
}
