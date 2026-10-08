package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.dto.StartTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.StopTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.TimerResponse;

import java.util.List;

public interface TimerApplicationService {

    /** The caller's running timer, or null. */
    TimerResponse current(CompanyId companyId);

    TimerResponse start(CompanyId companyId, StartTimerCommand command);

    /** Creates one entry per calendar date the run touched (TSH-04 #4). */
    List<EntryResponse> stop(CompanyId companyId, StopTimerCommand command);

    void discard(CompanyId companyId);
}
