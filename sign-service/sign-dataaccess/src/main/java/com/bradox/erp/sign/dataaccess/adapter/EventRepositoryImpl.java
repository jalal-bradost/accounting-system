package com.bradox.erp.sign.dataaccess.adapter;

import com.bradox.erp.sign.dataaccess.mapper.SignDataMapper;
import com.bradox.erp.sign.dataaccess.repository.EventJpaRepository;
import com.bradox.erp.sign.domain.core.entity.SignEvent;
import com.bradox.erp.sign.service.domain.ports.output.repository.EventRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EventRepositoryImpl implements EventRepository {

    private final EventJpaRepository events;
    private final SignDataMapper mapper;

    public EventRepositoryImpl(EventJpaRepository events, SignDataMapper mapper) {
        this.events = events;
        this.mapper = mapper;
    }

    @Override
    public void append(SignEvent event) {
        long next = events.findFirstByRequestIdOrderBySeqDesc(event.getRequestId()).map(e -> e.getSeq() + 1).orElse(1L);
        events.save(mapper.toEntity(event, next));
    }

    @Override
    public String lastHash(UUID requestId) {
        return events.findFirstByRequestIdOrderBySeqDesc(requestId).map(e -> e.getEventHash()).orElse(null);
    }

    @Override
    public List<SignEvent> list(UUID requestId) {
        return events.findByRequestIdOrderBySeqAsc(requestId).stream().map(mapper::toDomain).toList();
    }
}
