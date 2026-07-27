package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.TimeOffType;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.TimeOffTypeId;

import java.util.List;
import java.util.Optional;

public interface TimeOffTypeRepository {
    TimeOffType save(TimeOffType type);
    Optional<TimeOffType> findById(TimeOffTypeId id);
    List<TimeOffType> findByCompany(CompanyId companyId);
    boolean existsByCompanyAndCode(CompanyId companyId, String code);
}
