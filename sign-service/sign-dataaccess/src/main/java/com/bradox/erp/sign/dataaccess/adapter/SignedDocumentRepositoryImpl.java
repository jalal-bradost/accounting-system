package com.bradox.erp.sign.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.dataaccess.entity.SignedDocumentEntity;
import com.bradox.erp.sign.dataaccess.repository.SignedDocumentJpaRepository;
import com.bradox.erp.sign.domain.core.model.SignedDocument;
import com.bradox.erp.sign.service.domain.ports.output.SignedDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SignedDocumentRepositoryImpl implements SignedDocumentRepository {

    private final SignedDocumentJpaRepository jpa;

    public SignedDocumentRepositoryImpl(SignedDocumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<SignedDocument> findByTemplate(CompanyId companyId, UUID templateId) {
        return jpa.findByCompanyIdAndTemplateIdOrderBySignedAtDesc(companyId.getId(), templateId).stream()
                .map(SignedDocumentRepositoryImpl::toModel).toList();
    }

    @Override
    public Optional<SignedDocument> find(CompanyId companyId, UUID id) {
        return jpa.findByCompanyIdAndId(companyId.getId(), id).map(SignedDocumentRepositoryImpl::toModel);
    }

    @Override
    public Map<UUID, Long> countByTemplate(CompanyId companyId) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : jpa.countByTemplate(companyId.getId())) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public SignedDocument save(SignedDocument d) {
        SignedDocumentEntity e = new SignedDocumentEntity();
        e.id = d.id();
        e.companyId = d.companyId();
        e.templateId = d.templateId();
        e.templateName = d.templateName();
        e.signerName = d.signerName();
        e.signedAt = d.signedAt();
        e.signedBy = d.signedBy();
        e.documentId = d.documentId();
        e.fileName = d.fileName();
        return toModel(jpa.save(e));
    }

    @Override
    public void deleteByTemplate(CompanyId companyId, UUID templateId) {
        jpa.deleteByTemplate(companyId.getId(), templateId);
    }

    private static SignedDocument toModel(SignedDocumentEntity e) {
        return new SignedDocument(e.id, e.companyId, e.templateId, e.templateName, e.signerName, e.signedAt, e.signedBy,
                e.documentId, e.fileName);
    }
}
