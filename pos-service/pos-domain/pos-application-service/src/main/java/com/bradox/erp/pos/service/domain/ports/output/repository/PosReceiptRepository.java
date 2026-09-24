package com.bradox.erp.pos.service.domain.ports.output.repository;

import com.bradox.erp.pos.domain.core.entity.PosReceipt;

import java.util.Optional;
import java.util.UUID;

public interface PosReceiptRepository {
    PosReceipt save(PosReceipt receipt);

    Optional<PosReceipt> findById(UUID id);

    long countByCompanyId(UUID companyId);
}
