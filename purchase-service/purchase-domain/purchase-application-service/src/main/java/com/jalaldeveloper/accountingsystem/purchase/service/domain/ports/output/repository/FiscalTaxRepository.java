package com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.FiscalTax;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiscalTaxRepository {

    FiscalTax save(FiscalTax tax);

    Optional<FiscalTax> findById(UUID id);

    List<FiscalTax> findByCompanyIdAndActive(UUID companyId, boolean active);
}
