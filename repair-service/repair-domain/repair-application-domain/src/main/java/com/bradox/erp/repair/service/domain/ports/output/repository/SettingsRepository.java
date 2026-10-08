package com.bradox.erp.repair.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.Settings;

import java.util.Optional;

public interface SettingsRepository {

    Optional<Settings> find(CompanyId companyId);

    Settings save(Settings settings);
}
