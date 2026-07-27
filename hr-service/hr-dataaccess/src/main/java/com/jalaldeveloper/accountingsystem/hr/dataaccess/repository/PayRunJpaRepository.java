package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayRunJpaRepository extends JpaRepository<PayRunEntity, UUID> {

    List<PayRunEntity> findByCompanyIdOrderByPeriodStartDescCreatedAtDesc(UUID companyId);
}
