package com.bradox.erp.repair.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.service.domain.dto.UpdateSettingsCommand;

public interface SettingsApplicationService {

    Settings get(CompanyId companyId);

    Settings update(CompanyId companyId, UpdateSettingsCommand command);
}
