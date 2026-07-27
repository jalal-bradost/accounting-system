package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayWorkingScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayWorkingScheduleJpaRepository extends JpaRepository<PayWorkingScheduleEntity, UUID> {
    List<PayWorkingScheduleEntity> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);
    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
