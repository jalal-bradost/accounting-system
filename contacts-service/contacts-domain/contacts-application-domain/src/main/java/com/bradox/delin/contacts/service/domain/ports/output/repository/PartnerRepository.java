package com.bradox.delin.contacts.service.domain.ports.output.repository;

import com.bradox.delin.contacts.domain.core.entity.Partner;
import com.bradox.delin.contacts.domain.core.valueobject.PartnerId;
import com.bradox.delin.contacts.service.domain.dto.PartnerImageMeta;
import com.bradox.delin.domain.valueobject.CompanyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    Optional<PartnerImageMeta> findImageMeta(UUID partnerId);

    Map<UUID, PartnerImageMeta> findImageMetaByPartnerIds(Collection<UUID> partnerIds);

    void updateImage(UUID partnerId, String imageUrl, String contentType);

    void clearImage(UUID partnerId);
}
