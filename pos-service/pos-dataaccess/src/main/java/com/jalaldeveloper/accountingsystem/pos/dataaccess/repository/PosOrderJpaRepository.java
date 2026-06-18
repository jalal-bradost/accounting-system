package com.jalaldeveloper.accountingsystem.pos.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosOrderEntity;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosOrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PosOrderJpaRepository extends JpaRepository<PosOrderEntity, UUID> {
    @Query("""
            select distinct o from PosOrderEntity o
            left join fetch o.lines
            left join fetch o.payments
            where o.id = :id
            """)
    Optional<PosOrderEntity> findByIdWithLinesAndPayments(@Param("id") UUID id);

    long countByCompanyId(UUID companyId);

    long countBySessionIdAndState(UUID sessionId, PosOrderState state);

    @Query("""
            select coalesce(sum(o.amountTotal), 0) from PosOrderEntity o
            where o.sessionId = :sessionId and o.state = :state
            """)
    BigDecimal sumAmountTotalBySessionIdAndState(@Param("sessionId") UUID sessionId, @Param("state") PosOrderState state);

    @Query("""
            select coalesce(sum(p.amount), 0) from PosOrderEntity o
            join o.payments p
            where o.sessionId = :sessionId and o.state = :state and p.method = com.jalaldeveloper.accountingsystem.pos.domain.core.PosPaymentMethod.CASH
            """)
    BigDecimal sumCashPaymentsBySessionId(@Param("sessionId") UUID sessionId, @Param("state") PosOrderState state);
}
