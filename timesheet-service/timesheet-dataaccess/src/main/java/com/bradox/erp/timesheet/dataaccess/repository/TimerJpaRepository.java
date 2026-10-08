package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.TimerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimerJpaRepository extends JpaRepository<TimerEntity, UUID> {

    Optional<TimerEntity> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);
}
