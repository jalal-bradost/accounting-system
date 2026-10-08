package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.InspectionTemplateItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionTemplateItemJpaRepository extends JpaRepository<InspectionTemplateItemEntity, UUID> {

    List<InspectionTemplateItemEntity> findByTemplateIdInOrderBySequenceAsc(Collection<UUID> templateIds);

    void deleteByTemplateId(UUID templateId);
}
