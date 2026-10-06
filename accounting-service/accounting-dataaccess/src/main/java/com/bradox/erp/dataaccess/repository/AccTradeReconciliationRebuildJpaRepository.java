package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.AccTradeReconciliationRebuildEntity;
import com.bradox.erp.dataaccess.entity.TradeReconciliationRebuildId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccTradeReconciliationRebuildJpaRepository
        extends JpaRepository<AccTradeReconciliationRebuildEntity, TradeReconciliationRebuildId> {

    boolean existsByCompanyIdAndSide(UUID companyId, String side);
}
