package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.GridLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GridLineJpaRepository extends JpaRepository<GridLineEntity, UUID> {

    List<GridLineEntity> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);

    Optional<GridLineEntity> findByEmployeeIdAndLineKey(UUID employeeId, String lineKey);
}
