package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.RequestFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RequestFieldJpaRepository extends JpaRepository<RequestFieldEntity, UUID> {

    List<RequestFieldEntity> findByRequestIdIn(Collection<UUID> requestIds);

    void deleteByRequestId(UUID requestId);
}
