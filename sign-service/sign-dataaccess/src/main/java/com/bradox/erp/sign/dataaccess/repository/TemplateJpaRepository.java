package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TemplateJpaRepository extends JpaRepository<TemplateEntity, UUID> {

    List<TemplateEntity> findByCompanyIdOrderByNameAsc(UUID companyId);

    List<TemplateEntity> findByCompanyIdAndActiveTrueOrderByNameAsc(UUID companyId);
}
