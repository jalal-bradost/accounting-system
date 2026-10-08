package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.TemplateRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TemplateRoleJpaRepository extends JpaRepository<TemplateRoleEntity, UUID> {

    List<TemplateRoleEntity> findByTemplateIdIn(Collection<UUID> templateIds);

    void deleteByTemplateId(UUID templateId);
}
