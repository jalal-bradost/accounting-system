package com.jalaldeveloper.accountingsystem.platform.settings;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.CompanyEntity;

import java.time.LocalDate;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String legalName,
        String taxId,
        String email,
        String phone,
        String website,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String defaultCurrency,
        String locale,
        String dateFormat,
        String numberFormat,
        Integer fiscalYearStartMonth,
        LocalDate periodLockDate,
        String logoUrl,
        boolean active) {

    public static CompanyResponse from(CompanyEntity e) {
        return new CompanyResponse(
                e.getId(), e.getName(), e.getLegalName(), e.getTaxId(),
                e.getEmail(), e.getPhone(), e.getWebsite(),
                e.getAddressLine1(), e.getAddressLine2(),
                e.getCity(), e.getState(), e.getPostalCode(), e.getCountry(),
                e.getDefaultCurrency(), e.getLocale(), e.getDateFormat(),
                e.getNumberFormat(), e.getFiscalYearStartMonth(),
                e.getPeriodLockDate(), e.getLogoUrl(), e.isActive());
    }
}
