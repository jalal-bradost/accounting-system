package com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PaymentTerms;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.util.List;
import java.util.Optional;

public interface PaymentTermsRepository {

    PaymentTerms save(PaymentTerms terms);

    Optional<PaymentTerms> findById(PaymentTermsId id);

    List<PaymentTerms> findByCompanyId(CompanyId companyId, boolean includeArchived);

    boolean existsByCompanyIdAndName(CompanyId companyId, String name);
}
