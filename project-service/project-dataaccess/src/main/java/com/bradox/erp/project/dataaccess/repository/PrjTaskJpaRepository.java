package com.bradox.erp.project.dataaccess.repository;

import com.bradox.erp.project.dataaccess.entity.PrjTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrjTaskJpaRepository extends JpaRepository<PrjTaskEntity, UUID> {

    List<PrjTaskEntity> findByCompanyIdAndProjectIdOrderBySequenceAscCreatedAtAsc(UUID companyId, UUID projectId);

    Optional<PrjTaskEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    boolean existsByCompanyIdAndStageId(UUID companyId, UUID stageId);

    @Query("select t.projectId, count(t) from PrjTaskEntity t where t.companyId = :companyId group by t.projectId")
    List<Object[]> countByProject(@Param("companyId") UUID companyId);

    @Modifying
    @Query("delete from PrjTaskEntity t where t.companyId = :companyId and t.projectId = :projectId")
    void deleteByProject(@Param("companyId") UUID companyId, @Param("projectId") UUID projectId);
}
