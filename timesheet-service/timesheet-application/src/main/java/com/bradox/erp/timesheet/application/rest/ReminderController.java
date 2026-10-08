package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.ReminderRunResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.ReminderApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/timesheet/reminders", produces = "application/json")
public class ReminderController {

    private final ReminderApplicationService reminders;

    public ReminderController(ReminderApplicationService reminders) {
        this.reminders = reminders;
    }

    /** "Send reminders now": once per person per week, whatever the configured weekday. */
    @PostMapping("/run")
    @RequiresPermission("tsh.settings.manage")
    public ReminderRunResponse run(@CurrentCompany CompanyId companyId) {
        return reminders.runNow(companyId);
    }
}
