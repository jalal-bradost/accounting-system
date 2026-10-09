package com.bradox.erp.sign.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.dataaccess.entity.SignTemplateEntity;
import com.bradox.erp.sign.dataaccess.entity.SignTemplateFieldEntity;
import com.bradox.erp.sign.dataaccess.repository.SignTemplateJpaRepository;
import com.bradox.erp.sign.domain.core.model.SignTemplate;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.service.domain.ports.output.SignTemplateRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SignTemplateRepositoryImpl implements SignTemplateRepository {

    private final SignTemplateJpaRepository jpa;

    public SignTemplateRepositoryImpl(SignTemplateJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<SignTemplate> findAll(CompanyId companyId) {
        return jpa.findByCompanyIdOrderByNameAsc(companyId.getId()).stream().map(SignTemplateRepositoryImpl::toModel).toList();
    }

    @Override
    public Optional<SignTemplate> find(CompanyId companyId, UUID id) {
        return jpa.findByCompanyIdAndId(companyId.getId(), id).map(SignTemplateRepositoryImpl::toModel);
    }

    @Override
    public SignTemplate save(SignTemplate t) {
        SignTemplateEntity e = jpa.findById(t.id()).orElseGet(SignTemplateEntity::new);
        e.id = t.id();
        e.companyId = t.companyId();
        e.name = t.name();
        e.documentId = t.documentId();
        e.documentSha256 = t.documentSha256();
        e.pageCount = t.pageCount();
        e.createdAt = t.createdAt();
        e.fields.clear();
        int seq = 0;
        for (SignTemplate.Field f : t.fields()) {
            SignTemplateFieldEntity fe = new SignTemplateFieldEntity();
            fe.id = f.id();
            fe.sequence = seq++;
            fe.page = f.page();
            fe.x = f.x();
            fe.y = f.y();
            fe.width = f.width();
            fe.height = f.height();
            fe.fieldType = f.type().name();
            e.fields.add(fe);
        }
        return toModel(jpa.save(e));
    }

    @Override
    public void delete(SignTemplate t) {
        jpa.deleteById(t.id());
    }

    private static SignTemplate toModel(SignTemplateEntity e) {
        List<SignTemplate.Field> fields = e.fields.stream()
                .map(f -> new SignTemplate.Field(f.id, f.page, f.x, f.y, f.width, f.height, FieldType.valueOf(f.fieldType))).toList();
        return new SignTemplate(e.id, e.companyId, e.name, e.documentId, e.documentSha256, e.pageCount, fields, e.createdAt);
    }
}
