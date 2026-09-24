package com.bradox.erp.pos.dataaccess.adapter;

import com.bradox.erp.pos.dataaccess.mapper.PosDataAccessMapper;
import com.bradox.erp.pos.dataaccess.repository.PosSessionJpaRepository;
import com.bradox.erp.pos.domain.core.PosSessionState;
import com.bradox.erp.pos.domain.core.entity.PosSession;
import com.bradox.erp.pos.service.domain.ports.output.repository.PosSessionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PosSessionRepositoryImpl implements PosSessionRepository {

    private final PosSessionJpaRepository jpa;
    private final PosDataAccessMapper mapper;

    public PosSessionRepositoryImpl(PosSessionJpaRepository jpa, PosDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PosSession save(PosSession session) {
        var existing = jpa.findById(session.getId()).orElse(null);
        return mapper.entityToDomain(jpa.save(mapper.domainToEntity(session, existing)));
    }

    @Override
    public Optional<PosSession> findById(UUID id) {
        return jpa.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public Optional<PosSession> findFirstByConfigIdAndStateOrderByOpenedAtDesc(UUID configId, PosSessionState state) {
        return jpa.findFirstByConfigIdAndStateOrderByOpenedAtDesc(configId, state).map(mapper::entityToDomain);
    }

    @Override
    public Optional<PosSession> findFirstByConfigIdAndStateOrderByClosedAtDesc(UUID configId, PosSessionState state) {
        return jpa.findFirstByConfigIdAndStateOrderByClosedAtDesc(configId, state).map(mapper::entityToDomain);
    }
}
