package com.bradox.delin.pos.service.domain.ports.output.repository;

import com.bradox.delin.pos.domain.core.PosOrderState;
import com.bradox.delin.pos.domain.core.entity.PosOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosOrderRepository {
    PosOrder save(PosOrder order);

    Optional<PosOrder> findById(UUID id);

    List<PosOrder> findBySessionIdAndStateOrderByCreatedAtDesc(UUID sessionId, PosOrderState state);

    long countByCompanyId(UUID companyId);

    long countBySessionIdAndState(UUID sessionId, PosOrderState state);

    BigDecimal sumAmountTotalBySessionIdAndState(UUID sessionId, PosOrderState state);

    BigDecimal sumCashPaymentsBySessionId(UUID sessionId, PosOrderState state);
}
