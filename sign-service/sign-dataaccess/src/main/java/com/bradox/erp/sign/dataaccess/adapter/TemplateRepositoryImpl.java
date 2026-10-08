package com.bradox.erp.sign.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.dataaccess.entity.TemplateEntity;
import com.bradox.erp.sign.dataaccess.mapper.SignDataMapper;
import com.bradox.erp.sign.dataaccess.repository.RequestJpaRepository;
import com.bradox.erp.sign.dataaccess.repository.TemplateFieldJpaRepository;
import com.bradox.erp.sign.dataaccess.repository.TemplateJpaRepository;
import com.bradox.erp.sign.dataaccess.repository.TemplateRoleJpaRepository;
import com.bradox.erp.sign.domain.core.entity.Template;
import com.bradox.erp.sign.service.domain.ports.output.repository.TemplateRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TemplateRepositoryImpl implements TemplateRepository {

    private final TemplateJpaRepository templates;
    private final TemplateRoleJpaRepository roles;
    private final TemplateFieldJpaRepository fields;
    private final RequestJpaRepository requests;
    private final SignDataMapper mapper;

    public TemplateRepositoryImpl(TemplateJpaRepository templates, TemplateRoleJpaRepository roles, TemplateFieldJpaRepository fields,
                                  RequestJpaRepository requests, SignDataMapper mapper) {
        this.templates = templates;
        this.roles = roles;
        this.fields = fields;
        this.requests = requests;
        this.mapper = mapper;
    }

    @Override
    public Optional<Template> find(CompanyId companyId, UUID id) {
        return templates.findById(id).filter(e -> e.getCompanyId().equals(companyId.getId())).map(e -> load(List.of(e)).get(0));
    }

    @Override
    public List<Template> list(CompanyId companyId, boolean includeArchived) {
        List<TemplateEntity> rows = includeArchived ? templates.findByCompanyIdOrderByNameAsc(companyId.getId())
                : templates.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId.getId());
        return load(rows);
    }

    @Override
    public Template save(Template t) {
        templates.save(mapper.toEntity(t));
        roles.deleteByTemplateId(t.getId());
        fields.deleteByTemplateId(t.getId());
        roles.flush();
        roles.saveAll(t.getRoles().stream().map(r -> mapper.toEntity(t.getId(), r)).toList());
        fields.saveAll(t.getFields().stream().map(f -> mapper.toEntity(t.getId(), f)).toList());
        return t;
    }

    @Override
    public long countRequests(CompanyId companyId, UUID templateId) {
        return requests.countByTemplateId(templateId);
    }

    private List<Template> load(List<TemplateEntity> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = rows.stream().map(TemplateEntity::getId).collect(Collectors.toSet());
        var roleRows = roles.findByTemplateIdIn(ids).stream().collect(Collectors.groupingBy(r -> r.getTemplateId()));
        var fieldRows = fields.findByTemplateIdIn(ids).stream().collect(Collectors.groupingBy(f -> f.getTemplateId()));
        return rows.stream().map(e -> mapper.toDomain(e, roleRows.getOrDefault(e.getId(), List.of()),
                fieldRows.getOrDefault(e.getId(), List.of()))).toList();
    }
}
