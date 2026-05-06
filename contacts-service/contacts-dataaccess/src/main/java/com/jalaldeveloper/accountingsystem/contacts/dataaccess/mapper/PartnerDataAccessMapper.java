package com.jalaldeveloper.accountingsystem.contacts.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PartnerAddressEntity;
import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PartnerBankAccountEntity;
import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PartnerEntity;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.Partner;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PartnerAddress;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PartnerBankAccount;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerAddressId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerBankAccountId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PartnerDataAccessMapper {

    public Partner entityToDomain(PartnerEntity entity) {
        if (entity == null) return null;
        Partner.Builder builder = Partner.builder()
                .id(new PartnerId(entity.getId()))
                .companyId(new CompanyId(entity.getCompanyId()))
                .kind(entity.getKind())
                .displayName(entity.getDisplayName())
                .legalName(entity.getLegalName())
                .parentId(entity.getParentId() != null ? new PartnerId(entity.getParentId()) : null)
                .isCustomer(entity.isCustomer())
                .isVendor(entity.isVendor())
                .creditLimit(new Money(entity.getCreditLimit()))
                .paymentTermsId(entity.getPaymentTermsId() != null ? new PaymentTermsId(entity.getPaymentTermsId()) : null)
                .receivableAccountId(entity.getReceivableAccountId())
                .payableAccountId(entity.getPayableAccountId())
                .taxId(entity.getTaxId())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .website(entity.getWebsite())
                .language(entity.getLanguage())
                .currency(entity.getCurrencyCode() != null ? new Currency(entity.getCurrencyCode(), "", 2) : null);

        if (!entity.isActive()) {
            builder.archived(true)
                    .archivedAt(entity.getArchivedAt())
                    .archivedBy(entity.getArchivedBy());
        }

        Partner partner = builder.build();
        if (entity.getAddresses() != null) {
            for (PartnerAddressEntity a : entity.getAddresses()) {
                PartnerAddress address = PartnerAddress.builder()
                        .id(new PartnerAddressId(a.getId()))
                        .partnerId(partner.getId())
                        .type(a.getType())
                        .defaultForType(a.isDefaultForType())
                        .street1(a.getStreet1())
                        .street2(a.getStreet2())
                        .city(a.getCity())
                        .state(a.getState())
                        .postalCode(a.getPostalCode())
                        .country(a.getCountry())
                        .build();
                partner.addAddress(address);
            }
        }
        if (entity.getBankAccounts() != null) {
            for (PartnerBankAccountEntity b : entity.getBankAccounts()) {
                PartnerBankAccount account = PartnerBankAccount.builder()
                        .id(new PartnerBankAccountId(b.getId()))
                        .partnerId(partner.getId())
                        .iban(b.getIban())
                        .swift(b.getSwift())
                        .accountHolderName(b.getAccountHolderName())
                        .build();
                partner.addBankAccount(account);
            }
        }
        return partner;
    }

    public PartnerEntity domainToEntity(Partner domain, PartnerEntity existingOrNull) {
        if (domain == null) return null;
        PartnerEntity entity = existingOrNull != null ? existingOrNull : new PartnerEntity();
        entity.setId(domain.getId().getId());
        entity.setCompanyId(domain.getCompanyId().getId());
        entity.setKind(domain.getKind());
        entity.setDisplayName(domain.getDisplayName());
        entity.setLegalName(domain.getLegalName());
        entity.setParentId(domain.getParentId() != null ? domain.getParentId().getId() : null);
        entity.setCustomer(domain.isCustomer());
        entity.setVendor(domain.isVendor());
        entity.setCreditLimit(domain.getCreditLimit() != null ? domain.getCreditLimit().getAmount() : BigDecimal.ZERO);
        entity.setPaymentTermsId(domain.getPaymentTermsId() != null ? domain.getPaymentTermsId().getId() : null);
        entity.setReceivableAccountId(domain.getReceivableAccountId());
        entity.setPayableAccountId(domain.getPayableAccountId());
        entity.setTaxId(domain.getTaxId());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setWebsite(domain.getWebsite());
        entity.setLanguage(domain.getLanguage());
        entity.setCurrencyCode(domain.getCurrency() != null ? domain.getCurrency().code() : null);
        entity.setActive(domain.isActive());
        entity.setArchivedAt(domain.getArchivedAt());
        entity.setArchivedBy(domain.getArchivedBy());

        // Reconcile address collection in-place to preserve orphanRemoval semantics.
        if (entity.getAddresses() == null) entity.setAddresses(new ArrayList<>());
        Map<UUID, PartnerAddressEntity> existingAddresses = new HashMap<>();
        for (PartnerAddressEntity a : entity.getAddresses()) existingAddresses.put(a.getId(), a);
        entity.getAddresses().clear();
        for (PartnerAddress a : domain.getAddresses()) {
            UUID id = a.getId() != null ? a.getId().getId() : UUID.randomUUID();
            PartnerAddressEntity addrEntity = existingAddresses.getOrDefault(id, new PartnerAddressEntity());
            addrEntity.setId(id);
            addrEntity.setPartner(entity);
            addrEntity.setType(a.getType());
            addrEntity.setDefaultForType(a.isDefaultForType());
            addrEntity.setStreet1(a.getStreet1());
            addrEntity.setStreet2(a.getStreet2());
            addrEntity.setCity(a.getCity());
            addrEntity.setState(a.getState());
            addrEntity.setPostalCode(a.getPostalCode());
            addrEntity.setCountry(a.getCountry());
            entity.getAddresses().add(addrEntity);
        }

        if (entity.getBankAccounts() == null) entity.setBankAccounts(new ArrayList<>());
        Map<UUID, PartnerBankAccountEntity> existingAccounts = new HashMap<>();
        for (PartnerBankAccountEntity b : entity.getBankAccounts()) existingAccounts.put(b.getId(), b);
        entity.getBankAccounts().clear();
        for (PartnerBankAccount b : domain.getBankAccounts()) {
            UUID id = b.getId() != null ? b.getId().getId() : UUID.randomUUID();
            PartnerBankAccountEntity bankEntity = existingAccounts.getOrDefault(id, new PartnerBankAccountEntity());
            bankEntity.setId(id);
            bankEntity.setPartner(entity);
            bankEntity.setIban(b.getIban());
            bankEntity.setSwift(b.getSwift());
            bankEntity.setAccountHolderName(b.getAccountHolderName());
            entity.getBankAccounts().add(bankEntity);
        }
        return entity;
    }
}
