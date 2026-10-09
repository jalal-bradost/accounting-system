package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.model.SignedDocument;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SignedDocumentRepository {

    List<SignedDocument> findByTemplate(CompanyId companyId, UUID templateId);

    Optional<SignedDocument> find(CompanyId companyId, UUID id);

    /** Number of signings per template id. */
    Map<UUID, Long> countByTemplate(CompanyId companyId);

    SignedDocument save(SignedDocument document);

    void deleteByTemplate(CompanyId companyId, UUID templateId);
}
