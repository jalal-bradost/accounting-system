package com.jalaldeveloper.accountingsystem.pos.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PosReceiptJpaRepository extends JpaRepository<PosReceiptEntity, UUID> {
    long countByCompanyId(UUID companyId);
}
