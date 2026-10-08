package com.bradox.erp.documents.dataaccess.repository;

import com.bradox.erp.documents.dataaccess.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TagJpaRepository extends JpaRepository<TagEntity, UUID> {

    List<TagEntity> findByCompanyId(UUID companyId);

    List<TagEntity> findByFacetId(UUID facetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TagEntity t where t.facetId = :facetId")
    void deleteAllForFacet(@Param("facetId") UUID facetId);
}
