package com.bradox.erp.repair.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Display names of partners and products, read from Contacts and Inventory (implemented in infrastructure). */
public interface RepairLookupPort {

    Map<UUID, String> partnerNames(CompanyId companyId, Collection<UUID> ids);

    Map<UUID, String> productNames(CompanyId companyId, Collection<UUID> ids);
}
