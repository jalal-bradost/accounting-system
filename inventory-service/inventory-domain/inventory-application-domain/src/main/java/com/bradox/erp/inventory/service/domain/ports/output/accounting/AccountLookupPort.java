package com.bradox.erp.inventory.service.domain.ports.output.accounting;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.Optional;
import java.util.UUID;

/** Resolves chart-of-accounts ids without a Maven dependency on accounting. */
public interface AccountLookupPort {

    Optional<UUID> findAccountId(CompanyId companyId, String code);
}
