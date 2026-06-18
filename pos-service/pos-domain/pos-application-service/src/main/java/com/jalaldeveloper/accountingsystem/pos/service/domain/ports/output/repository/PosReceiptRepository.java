package com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosReceipt;

import java.util.Optional;
import java.util.UUID;

public interface PosReceiptRepository {
    PosReceipt save(PosReceipt receipt);

    Optional<PosReceipt> findById(UUID id);

    long countByCompanyId(UUID companyId);
}
