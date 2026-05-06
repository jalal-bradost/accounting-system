package com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PaymentTermsCommand;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PaymentTermsResponse;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface PaymentTermsApplicationService {

    PaymentTermsResponse create(@Valid PaymentTermsCommand command);

    PaymentTermsResponse update(UUID id, @Valid PaymentTermsCommand command);

    PaymentTermsResponse archive(UUID id);

    PaymentTermsResponse unarchive(UUID id);

    PaymentTermsResponse get(UUID id);

    List<PaymentTermsResponse> list(CompanyId companyId, boolean includeArchived);
}
