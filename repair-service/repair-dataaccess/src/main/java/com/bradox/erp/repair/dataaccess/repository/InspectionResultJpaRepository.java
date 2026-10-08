package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.InspectionResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionResultJpaRepository extends JpaRepository<InspectionResultEntity, UUID> {

    List<InspectionResultEntity> findByInspectionIdOrderBySequenceAsc(UUID inspectionId);

    void deleteByInspectionId(UUID inspectionId);
}
