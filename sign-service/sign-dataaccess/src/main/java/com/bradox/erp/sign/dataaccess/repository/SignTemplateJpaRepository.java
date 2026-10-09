package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.SignTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignTemplateJpaRepository extends JpaRepository<SignTemplateEntity, UUID> {

    List<SignTemplateEntity> findByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<SignTemplateEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
