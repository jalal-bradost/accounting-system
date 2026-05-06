package com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.Partner;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Output port for the {@link Partner} aggregate (hexagonal). */
public interface PartnerRepository {

    Partner save(Partner partner);

    Optional<Partner> findById(PartnerId id);

    /** Includes archived rows. */
    Optional<Partner> findByIdIncludingArchived(PartnerId id);

    Page<Partner> search(CompanyId companyId,
                         String query,
                         Boolean isCustomer,
                         Boolean isVendor,
                         boolean includeArchived,
                         Pageable pageable);
}
