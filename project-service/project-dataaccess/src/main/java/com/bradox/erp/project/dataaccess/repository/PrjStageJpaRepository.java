package com.bradox.erp.project.dataaccess.repository;

import com.bradox.erp.project.dataaccess.entity.PrjStageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrjStageJpaRepository extends JpaRepository<PrjStageEntity, UUID> {

    List<PrjStageEntity> findByCompanyIdAndProjectIdOrderBySequenceAsc(UUID companyId, UUID projectId);

    Optional<PrjStageEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    @Modifying
    @Query("delete from PrjStageEntity s where s.companyId = :companyId and s.projectId = :projectId")
    void deleteByProject(@Param("companyId") UUID companyId, @Param("projectId") UUID projectId);
}
