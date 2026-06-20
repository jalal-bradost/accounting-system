package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, UUID> {

    List<DepartmentEntity> findByCompanyIdAndActiveTrueOrderByNameAsc(UUID companyId);

    List<DepartmentEntity> findByCompanyIdOrderByNameAsc(UUID companyId);

    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
