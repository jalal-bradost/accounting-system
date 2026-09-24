package com.bradox.erp.pos.dataaccess.repository;

import com.bradox.erp.pos.dataaccess.entity.PosReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PosReceiptJpaRepository extends JpaRepository<PosReceiptEntity, UUID> {
    long countByCompanyId(UUID companyId);
}
