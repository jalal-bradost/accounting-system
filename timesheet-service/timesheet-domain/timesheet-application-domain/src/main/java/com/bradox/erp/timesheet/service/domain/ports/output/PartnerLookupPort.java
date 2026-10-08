package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Customer names for projects, by id. Implemented in {@code infrastructure} over contacts. */
public interface PartnerLookupPort {

    boolean exists(CompanyId companyId, UUID partnerId);

    Map<UUID, String> names(CompanyId companyId, Collection<UUID> ids);
}
