package com.bradox.erp.sign.service.domain.ports.output.repository;

import com.bradox.erp.sign.domain.core.entity.SignEvent;

import java.util.List;
import java.util.UUID;

/** Append only. There is deliberately no update or delete (SIG-08 #5). */
public interface EventRepository {

    void append(SignEvent event);

    /** Hash of the newest event, or null when there is none yet. */
    String lastHash(UUID requestId);

    List<SignEvent> list(UUID requestId);
}
