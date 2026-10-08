package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.TemplateFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TemplateFieldJpaRepository extends JpaRepository<TemplateFieldEntity, UUID> {

    List<TemplateFieldEntity> findByTemplateIdIn(Collection<UUID> templateIds);

    void deleteByTemplateId(UUID templateId);
}
