package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.EmployeeRefResponse;
import com.bradox.erp.timesheet.service.domain.dto.MeResponse;
import com.bradox.erp.timesheet.service.domain.dto.SettingsCommand;
import com.bradox.erp.timesheet.service.domain.dto.SettingsResponse;

import java.util.List;

public interface SettingsApplicationService {

    SettingsResponse get(CompanyId companyId);

    SettingsResponse update(CompanyId companyId, SettingsCommand command);

    MeResponse me(CompanyId companyId);

    /** Active employees for the "on behalf" and team pickers. */
    List<EmployeeRefResponse> employees(CompanyId companyId);
}
