package com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Primary input port for everything Partner-shaped. */
public interface PartnerApplicationService {

    PartnerResponse createPartner(@Valid CreatePartnerCommand command);

    PartnerResponse updatePartner(UUID partnerId, @Valid UpdatePartnerCommand command);

    PartnerResponse archive(UUID partnerId);

    PartnerResponse unarchive(UUID partnerId);

    PartnerResponse getPartner(UUID partnerId);

    Optional<PartnerRefResponse> findRef(CompanyId companyId, UUID partnerId);

    Page<PartnerResponse> search(CompanyId companyId,
                                 String query,
                                 Boolean isCustomer,
                                 Boolean isVendor,
                                 boolean includeArchived,
                                 Pageable pageable);

    PartnerResponse.AddressResponse addAddress(UUID partnerId, @Valid PartnerAddressCommand command);

    PartnerResponse.AddressResponse updateAddress(UUID partnerId, UUID addressId, @Valid PartnerAddressCommand command);

    void removeAddress(UUID partnerId, UUID addressId);

    PartnerResponse.BankAccountResponse addBankAccount(UUID partnerId, @Valid PartnerBankAccountCommand command);

    void removeBankAccount(UUID partnerId, UUID bankAccountId);

    CreditStatusResponse creditStatus(UUID partnerId);
}
