package com.bradox.erp.documents.dataaccess.repository;

import com.bradox.erp.documents.dataaccess.entity.DocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

    Optional<DocumentVersionEntity> findByIdAndDocumentId(UUID id, UUID documentId);

    int countByDocumentId(UUID documentId);

    @Modifying(flushAutomatically = true)
    @Query("delete from DocumentVersionEntity v where v.documentId = :documentId")
    void deleteAllForDocument(@Param("documentId") UUID documentId);
}
