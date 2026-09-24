package com.bradox.erp.contacts.service.domain.ports.output.repository;

import com.bradox.erp.contacts.domain.core.entity.PaymentTerms;
import com.bradox.erp.contacts.domain.core.valueobject.PaymentTermsId;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.List;
import java.util.Optional;

public interface PaymentTermsRepository {

    PaymentTerms save(PaymentTerms terms);

    Optional<PaymentTerms> findById(PaymentTermsId id);

    List<PaymentTerms> findByCompanyId(CompanyId companyId, boolean includeArchived);

    boolean existsByCompanyIdAndName(CompanyId companyId, String name);
}
