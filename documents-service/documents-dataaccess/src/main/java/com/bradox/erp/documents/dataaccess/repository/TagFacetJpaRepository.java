package com.bradox.erp.documents.dataaccess.repository;

import com.bradox.erp.documents.dataaccess.entity.TagFacetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TagFacetJpaRepository extends JpaRepository<TagFacetEntity, UUID> {

    List<TagFacetEntity> findByCompanyId(UUID companyId);
}
