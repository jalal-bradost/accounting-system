package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.Optional;
import java.util.UUID;

public interface SignerDirectoryPort {

    record Person(UUID id, String name, String email, String phone) {
    }

    Optional<Person> partner(CompanyId companyId, UUID partnerId);

    Optional<Person> user(CompanyId companyId, UUID userId);
}
