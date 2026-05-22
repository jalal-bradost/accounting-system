package com.jalaldeveloper.accountingsystem.platform.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CompanyWriteRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String legalName,
        @Size(max = 100) String taxId,
        @Email @Size(max = 200) String email,
        @Size(max = 50) String phone,
        @Size(max = 200) String website,
        @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 30) String postalCode,
        @Size(min = 2, max = 2) String country,
        @Size(min = 3, max = 3) String defaultCurrency,
        @Size(max = 20) String locale,
        @Size(max = 30) String dateFormat,
        @Size(max = 30) String numberFormat,
        @Min(1) @Max(12) Integer fiscalYearStartMonth,
        LocalDate periodLockDate,
        @Size(max = 500) String logoUrl) {}
