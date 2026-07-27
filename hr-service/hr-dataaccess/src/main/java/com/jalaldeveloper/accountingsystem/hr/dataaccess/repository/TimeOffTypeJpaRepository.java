package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.TimeOffTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TimeOffTypeJpaRepository extends JpaRepository<TimeOffTypeEntity, UUID> {
    List<TimeOffTypeEntity> findByCompanyIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID companyId);
    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
