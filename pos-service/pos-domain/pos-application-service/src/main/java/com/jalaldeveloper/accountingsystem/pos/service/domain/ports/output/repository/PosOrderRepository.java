package com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosOrderState;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosOrder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PosOrderRepository {
    PosOrder save(PosOrder order);

    Optional<PosOrder> findById(UUID id);

    long countByCompanyId(UUID companyId);

    long countBySessionIdAndState(UUID sessionId, PosOrderState state);

    BigDecimal sumAmountTotalBySessionIdAndState(UUID sessionId, PosOrderState state);

    BigDecimal sumCashPaymentsBySessionId(UUID sessionId, PosOrderState state);
}
