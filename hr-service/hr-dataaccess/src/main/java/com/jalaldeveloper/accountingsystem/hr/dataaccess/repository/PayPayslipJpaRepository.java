package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayPayslipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayPayslipJpaRepository extends JpaRepository<PayPayslipEntity, UUID> {}
