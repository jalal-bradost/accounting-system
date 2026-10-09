package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.model.SignTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignTemplateRepository {

    List<SignTemplate> findAll(CompanyId companyId);

    Optional<SignTemplate> find(CompanyId companyId, UUID id);

    SignTemplate save(SignTemplate template);

    void delete(SignTemplate template);
}
