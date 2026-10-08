package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.ReminderRunResponse;

public interface ReminderApplicationService {

    /**
     * Sends this week's reminders now, ignoring the configured weekday (needs {@code tsh.settings.manage}). Still
     * once per person per week, so pressing it twice does not remind anyone twice.
     */
    ReminderRunResponse runNow(CompanyId companyId);
}
