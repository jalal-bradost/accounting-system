package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.EventEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Deliberately not a CrudRepository: only save, find and list exist, so the log cannot be changed or deleted (SIG-08 #5). */
public interface EventJpaRepository extends Repository<EventEntity, UUID> {

    EventEntity save(EventEntity entity);

    List<EventEntity> findByRequestIdOrderBySeqAsc(UUID requestId);

    Optional<EventEntity> findFirstByRequestIdOrderBySeqDesc(UUID requestId);
}
