package com.jalaldeveloper.accountingsystem.pos.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.mapper.PosDataAccessMapper;
import com.jalaldeveloper.accountingsystem.pos.dataaccess.repository.PosOrderJpaRepository;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosOrderState;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosOrder;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.output.repository.PosOrderRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class PosOrderRepositoryImpl implements PosOrderRepository {

    private final PosOrderJpaRepository jpa;
    private final PosDataAccessMapper mapper;

    public PosOrderRepositoryImpl(PosOrderJpaRepository jpa, PosDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PosOrder save(PosOrder order) {
        var existing = jpa.findByIdWithLinesAndPayments(order.getId()).orElse(null);
        return mapper.entityToDomain(jpa.save(mapper.domainToEntity(order, existing)));
    }

    @Override
    public Optional<PosOrder> findById(UUID id) {
        return jpa.findByIdWithLinesAndPayments(id).map(mapper::entityToDomain);
    }

    @Override
    public long countByCompanyId(UUID companyId) {
        return jpa.countByCompanyId(companyId);
    }

    @Override
    public long countBySessionIdAndState(UUID sessionId, PosOrderState state) {
        return jpa.countBySessionIdAndState(sessionId, state);
    }

    @Override
    public BigDecimal sumAmountTotalBySessionIdAndState(UUID sessionId, PosOrderState state) {
        return jpa.sumAmountTotalBySessionIdAndState(sessionId, state);
    }

    @Override
    public BigDecimal sumCashPaymentsBySessionId(UUID sessionId, PosOrderState state) {
        return jpa.sumCashPaymentsBySessionId(sessionId, state);
    }
}
