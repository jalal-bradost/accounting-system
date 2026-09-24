package com.bradox.delin.accounting.service.domain.ports.output.contacts;

import com.bradox.delin.domain.core.ValueObject.PartnerRef;
import com.bradox.delin.domain.valueobject.CompanyId;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port that lets the accounting module look up a partner without depending on
 * the contacts module at the Maven level. Implemented by an adapter living in
 * {@code accounting-container}.
 */
public interface PartnerLookupPort {

    Optional<PartnerRef> findByCompanyAndId(CompanyId companyId, UUID partnerId);
}
