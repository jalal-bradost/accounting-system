package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.SignedDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignedDocumentJpaRepository extends JpaRepository<SignedDocumentEntity, UUID> {

    List<SignedDocumentEntity> findByCompanyIdAndTemplateIdOrderBySignedAtDesc(UUID companyId, UUID templateId);

    Optional<SignedDocumentEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    @Query("select d.templateId, count(d) from SignedDocumentEntity d where d.companyId = :companyId group by d.templateId")
    List<Object[]> countByTemplate(@Param("companyId") UUID companyId);

    @Modifying
    @Query("delete from SignedDocumentEntity d where d.companyId = :companyId and d.templateId = :templateId")
    void deleteByTemplate(@Param("companyId") UUID companyId, @Param("templateId") UUID templateId);
}
