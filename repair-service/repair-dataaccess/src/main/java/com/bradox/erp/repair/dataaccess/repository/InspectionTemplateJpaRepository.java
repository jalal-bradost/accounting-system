package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.InspectionTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionTemplateJpaRepository extends JpaRepository<InspectionTemplateEntity, UUID> {

    List<InspectionTemplateEntity> findByCompanyIdOrderByNameAsc(UUID companyId);
}
