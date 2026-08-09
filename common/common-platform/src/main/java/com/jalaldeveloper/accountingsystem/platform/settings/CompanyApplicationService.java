package com.jalaldeveloper.accountingsystem.platform.settings;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.CompanyEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.UserRoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.CompanyJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.UserRoleJpaRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompanyApplicationService {

    private final CompanyJpaRepository companyRepository;
    private final UserRoleJpaRepository userRoleRepository;
    private final RoleJpaRepository roleRepository;
    private final AppUserJpaRepository appUserRepository;
    private final ObjectProvider<BaseCurrencyChangeHandler> baseCurrencyChangeHandler;

    public CompanyApplicationService(CompanyJpaRepository companyRepository,
                                     UserRoleJpaRepository userRoleRepository,
                                     RoleJpaRepository roleRepository,
                                     AppUserJpaRepository appUserRepository,
                                     ObjectProvider<BaseCurrencyChangeHandler> baseCurrencyChangeHandler) {
        this.companyRepository = companyRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.appUserRepository = appUserRepository;
        this.baseCurrencyChangeHandler = baseCurrencyChangeHandler;
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(UUID id) {
        return CompanyResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listAll() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    /** Companies the given user has at least one role in. Returns all when {@code userId} is null. */
    @Transactional(readOnly = true)
    public List<CompanyResponse> listForUser(UUID userId) {
        if (userId == null) {
            return listAll();
        }
        Set<UUID> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            // Fall back to the user's own home company so the topbar always has at least one entry.
            return appUserRepository.findById(userId)
                    .map(u -> companyRepository.findById(u.getCompanyId())
                            .map(c -> List.of(CompanyResponse.from(c)))
                            .orElse(List.<CompanyResponse>of()))
                    .orElseGet(List::of);
        }
        Set<UUID> companyIds = roleRepository.findAllById(roleIds).stream()
                .map(r -> r.getCompanyId())
                .collect(Collectors.toSet());
        if (companyIds.isEmpty()) {
            return List.of();
        }
        return companyRepository.findAllByIdInOrderByNameAsc(List.copyOf(companyIds)).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional
    public CompanyResponse create(CompanyWriteRequest req) {
        CompanyEntity c = new CompanyEntity();
        c.setId(UUID.randomUUID());
        applyWrite(c, req);
        companyRepository.save(c);
        return CompanyResponse.from(c);
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyWriteRequest req) {
        CompanyEntity c = load(id);
        String previousCurrency = c.getDefaultCurrency();
        applyWrite(c, req);
        companyRepository.save(c);
        // Keep the accounting base currency in sync when the default currency actually
        // changes. Runs in the same transaction, so a rejection (transactions exist)
        // rolls back the default_currency change too.
        String newCurrency = c.getDefaultCurrency();
        if (newCurrency != null && !newCurrency.equalsIgnoreCase(previousCurrency)) {
            BaseCurrencyChangeHandler handler = baseCurrencyChangeHandler.getIfAvailable();
            if (handler != null) {
                handler.changeBaseCurrency(id, newCurrency);
            }
        }
        return CompanyResponse.from(c);
    }

    @Transactional
    public CompanyResponse setPeriodLock(UUID id, LocalDate periodLockDate) {
        CompanyEntity c = load(id);
        c.setPeriodLockDate(periodLockDate);
        companyRepository.save(c);
        return CompanyResponse.from(c);
    }

    private CompanyEntity load(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private static void applyWrite(CompanyEntity c, CompanyWriteRequest r) {
        if (r.name() == null || r.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name is required");
        }
        c.setName(r.name().trim());
        c.setLegalName(blankToNull(r.legalName()));
        c.setTaxId(blankToNull(r.taxId()));
        c.setEmail(blankToNull(r.email()));
        c.setPhone(blankToNull(r.phone()));
        c.setWebsite(blankToNull(r.website()));
        c.setAddressLine1(blankToNull(r.addressLine1()));
        c.setAddressLine2(blankToNull(r.addressLine2()));
        c.setCity(blankToNull(r.city()));
        c.setState(blankToNull(r.state()));
        c.setPostalCode(blankToNull(r.postalCode()));
        c.setCountry(blankToNull(r.country()));
        c.setDefaultCurrency(blankToNull(r.defaultCurrency()));
        c.setLocale(blankToNull(r.locale()));
        c.setDateFormat(blankToNull(r.dateFormat()));
        c.setNumberFormat(blankToNull(r.numberFormat()));
        c.setFiscalYearStartMonth(r.fiscalYearStartMonth());
        c.setLogoUrl(blankToNull(r.logoUrl()));
        if (r.periodLockDate() != null) {
            c.setPeriodLockDate(r.periodLockDate());
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
