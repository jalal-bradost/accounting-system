package com.bradox.erp.hr.dataaccess.repository;

import com.bradox.erp.hr.dataaccess.entity.PayPayslipLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayPayslipLineJpaRepository extends JpaRepository<PayPayslipLineEntity, UUID> {}
