package com.bradox.delin.integration;

import com.bradox.delin.accounting.service.domain.ports.output.contacts.PartnerLookupPort;
import com.bradox.delin.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.delin.domain.core.ValueObject.PartnerRef;
import com.bradox.delin.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure-side adapter that bridges the accounting module's {@link PartnerLookupPort}
 * to the contacts module's {@link PartnerApplicationService}. Lives in infrastructure
 * so neither business module depends on the other at the Maven level.
 */
@Component
public class PartnerLookupAdapter implements PartnerLookupPort {

    private final PartnerApplicationService partnerService;

    public PartnerLookupAdapter(PartnerApplicationService partnerService) {
        this.partnerService = partnerService;
    }

    @Override
    public Optional<PartnerRef> findByCompanyAndId(CompanyId companyId, UUID partnerId) {
        return partnerService.findRef(companyId, partnerId)
                .map(ref -> new PartnerRef(ref.id(), ref.displayName()));
    }
}
