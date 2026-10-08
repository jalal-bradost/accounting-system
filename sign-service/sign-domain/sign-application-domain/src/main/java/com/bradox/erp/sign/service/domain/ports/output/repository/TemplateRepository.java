package com.bradox.erp.sign.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.entity.Template;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository {

    Optional<Template> find(CompanyId companyId, UUID id);

    List<Template> list(CompanyId companyId, boolean includeArchived);

    Template save(Template template);

    /** Requests ever made from the template, for the "existing requests are unaffected" warning. */
    long countRequests(CompanyId companyId, UUID templateId);
}
