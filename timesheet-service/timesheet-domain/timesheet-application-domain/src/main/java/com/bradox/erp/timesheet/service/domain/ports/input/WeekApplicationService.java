package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.BulkApproveCommand;
import com.bradox.erp.timesheet.service.domain.dto.BulkApproveResponse;
import com.bradox.erp.timesheet.service.domain.dto.SubmitWeekCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekReasonCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekSummaryResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WeekApplicationService {

    /** Scope for {@link #list}: weeks I can approve, my team's, everyone's, or mine. */
    enum Scope { APPROVE, TEAM, ALL, MINE }

    List<WeekSummaryResponse> list(CompanyId companyId, Scope scope, WeekStatus status, LocalDate from, LocalDate to);

    WeekSummaryResponse get(CompanyId companyId, UUID weekId);

    WeekSummaryResponse submit(CompanyId companyId, SubmitWeekCommand command);

    WeekSummaryResponse approve(CompanyId companyId, UUID weekId);

    WeekSummaryResponse refuse(CompanyId companyId, UUID weekId, WeekReasonCommand command);

    WeekSummaryResponse reopen(CompanyId companyId, UUID weekId, WeekReasonCommand command);

    BulkApproveResponse bulkApprove(CompanyId companyId, BulkApproveCommand command);
}
