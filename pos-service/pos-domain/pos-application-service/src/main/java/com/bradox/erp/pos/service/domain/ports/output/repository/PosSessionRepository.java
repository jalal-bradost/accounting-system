package com.bradox.erp.pos.service.domain.ports.output.repository;

import com.bradox.erp.pos.domain.core.PosSessionState;
import com.bradox.erp.pos.domain.core.entity.PosSession;

import java.util.Optional;
import java.util.UUID;

public interface PosSessionRepository {
    PosSession save(PosSession session);

    Optional<PosSession> findById(UUID id);

    Optional<PosSession> findFirstByConfigIdAndStateOrderByOpenedAtDesc(UUID configId, PosSessionState state);

    Optional<PosSession> findFirstByConfigIdAndStateOrderByClosedAtDesc(UUID configId, PosSessionState state);
}
