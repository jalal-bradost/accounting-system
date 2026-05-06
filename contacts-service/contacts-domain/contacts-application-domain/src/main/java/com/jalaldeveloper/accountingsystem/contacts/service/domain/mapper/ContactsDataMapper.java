package com.jalaldeveloper.accountingsystem.contacts.service.domain.mapper;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.Partner;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PartnerAddress;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PartnerBankAccount;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PaymentTerms;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerAddressId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerBankAccountId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ContactsDataMapper {

    public Partner createCommandToPartner(CreatePartnerCommand cmd, UUID id, CompanyId companyId) {
        return Partner.builder()
                .id(new PartnerId(id))
                .companyId(companyId)
                .kind(cmd.getKind())
                .displayName(cmd.getDisplayName())
                .legalName(cmd.getLegalName())
                .parentId(cmd.getParentId() != null ? new PartnerId(cmd.getParentId()) : null)
                .isCustomer(cmd.isCustomer())
                .isVendor(cmd.isVendor())
                .creditLimit(cmd.getCreditLimit() != null ? new Money(cmd.getCreditLimit()) : Money.ZERO)
                .paymentTermsId(cmd.getPaymentTermsId() != null ? new PaymentTermsId(cmd.getPaymentTermsId()) : null)
                .receivableAccountId(cmd.getReceivableAccountId())
                .payableAccountId(cmd.getPayableAccountId())
                .taxId(cmd.getTaxId())
                .email(cmd.getEmail())
                .phone(cmd.getPhone())
                .website(cmd.getWebsite())
                .language(cmd.getLanguage())
                .currency(toCurrency(cmd.getCurrencyCode()))
                .build();
    }

    public PartnerAddress addressCommandToDomain(PartnerId partnerId, UUID id, PartnerAddressCommand cmd) {
        return PartnerAddress.builder()
                .id(new PartnerAddressId(id))
                .partnerId(partnerId)
                .type(cmd.getType())
                .defaultForType(cmd.isDefaultForType())
                .street1(cmd.getStreet1())
                .street2(cmd.getStreet2())
                .city(cmd.getCity())
                .state(cmd.getState())
                .postalCode(cmd.getPostalCode())
                .country(cmd.getCountry())
                .build();
    }

    public PartnerBankAccount bankAccountCommandToDomain(PartnerId partnerId, UUID id, PartnerBankAccountCommand cmd) {
        return PartnerBankAccount.builder()
                .id(new PartnerBankAccountId(id))
                .partnerId(partnerId)
                .iban(cmd.getIban())
                .swift(cmd.getSwift())
                .accountHolderName(cmd.getAccountHolderName())
                .build();
    }

    public PaymentTerms paymentTermsCommandToDomain(UUID id, CompanyId companyId, PaymentTermsCommand cmd) {
        return PaymentTerms.builder()
                .id(new PaymentTermsId(id))
                .companyId(companyId)
                .name(cmd.getName())
                .daysNet(cmd.getDaysNet())
                .discountDays(cmd.getDiscountDays())
                .discountPercent(cmd.getDiscountPercent())
                .build();
    }

    public PaymentTermsResponse paymentTermsToResponse(PaymentTerms terms) {
        if (terms == null) return null;
        return new PaymentTermsResponse(
                terms.getId().getId(),
                terms.getCompanyId().getId(),
                terms.getName(),
                terms.getDaysNet(),
                terms.getDiscountDays(),
                terms.getDiscountPercent(),
                terms.isActive(),
                terms.getArchivedAt(),
                terms.getArchivedBy());
    }

    public PartnerResponse partnerToResponse(Partner p) {
        if (p == null) return null;
        List<PartnerResponse.AddressResponse> addresses = p.getAddresses().stream()
                .map(this::addressToResponse)
                .collect(Collectors.toList());
        List<PartnerResponse.BankAccountResponse> bankAccounts = p.getBankAccounts().stream()
                .map(this::bankAccountToResponse)
                .collect(Collectors.toList());
        BigDecimal creditLimit = p.getCreditLimit() != null ? p.getCreditLimit().getAmount() : BigDecimal.ZERO;
        return new PartnerResponse(
                p.getId().getId(),
                p.getCompanyId().getId(),
                p.getKind(),
                p.getDisplayName(),
                p.getLegalName(),
                p.getParentId() != null ? p.getParentId().getId() : null,
                p.isCustomer(),
                p.isVendor(),
                creditLimit,
                p.getPaymentTermsId() != null ? p.getPaymentTermsId().getId() : null,
                p.getReceivableAccountId(),
                p.getPayableAccountId(),
                p.getTaxId(),
                p.getEmail(),
                p.getPhone(),
                p.getWebsite(),
                p.getLanguage(),
                p.getCurrency() != null ? p.getCurrency().code() : null,
                p.isActive(),
                p.getArchivedAt(),
                p.getArchivedBy(),
                addresses,
                bankAccounts);
    }

    public PartnerResponse.AddressResponse addressToResponse(PartnerAddress a) {
        return new PartnerResponse.AddressResponse(
                a.getId() != null ? a.getId().getId() : null,
                a.getType(),
                a.isDefaultForType(),
                a.getStreet1(), a.getStreet2(), a.getCity(),
                a.getState(), a.getPostalCode(), a.getCountry());
    }

    public PartnerResponse.BankAccountResponse bankAccountToResponse(PartnerBankAccount b) {
        return new PartnerResponse.BankAccountResponse(
                b.getId() != null ? b.getId().getId() : null,
                b.getIban(), b.getSwift(), b.getAccountHolderName());
    }

    public PartnerRefResponse partnerToRef(Partner p) {
        if (p == null) return null;
        return new PartnerRefResponse(p.getId().getId(), p.getDisplayName(), p.isActive(),
                p.isCustomer(), p.isVendor());
    }

    private static Currency toCurrency(String code) {
        if (code == null || code.isBlank()) return null;
        return new Currency(code, "", 2);
    }
}
