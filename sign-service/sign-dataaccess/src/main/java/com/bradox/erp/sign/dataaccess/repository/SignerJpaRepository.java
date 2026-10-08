package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.SignerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignerJpaRepository extends JpaRepository<SignerEntity, UUID> {

    List<SignerEntity> findByRequestIdIn(Collection<UUID> requestIds);

    Optional<SignerEntity> findByTokenHash(String tokenHash);

    void deleteByRequestId(UUID requestId);
}
