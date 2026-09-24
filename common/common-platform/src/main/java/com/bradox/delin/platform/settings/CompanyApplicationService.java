package com.bradox.delin.platform.settings;

import com.bradox.delin.platform.dataaccess.entity.CompanyEntity;
import com.bradox.delin.platform.dataaccess.entity.UserRoleEntity;
import com.bradox.delin.platform.dataaccess.repository.AppUserJpaRepository;
import com.bradox.delin.platform.dataaccess.repository.CompanyJpaRepository;
import com.bradox.delin.platform.dataaccess.repository.RoleJpaRepository;
import com.bradox.delin.platform.dataaccess.repository.UserRoleJpaRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompanyApplicationService {

    private final CompanyJpaRepository companyRepository;
    private final UserRoleJpaRepository userRoleRepository;
    private final RoleJpaRepository roleRepository;
    private final AppUserJpaRepository appUserRepository;
    private final CompanyRoleProvisioner companyRoleProvisioner;
    private final ObjectProvider<BaseCurrencyChangeHandler> baseCurrencyChangeHandler;
    private final CompanyLogoStorage logoStorage;

    public CompanyApplicationService(CompanyJpaRepository companyRepository,
                                     UserRoleJpaRepository userRoleRepository,
                                     RoleJpaRepository roleRepository,
                                     AppUserJpaRepository appUserRepository,
                                     CompanyRoleProvisioner companyRoleProvisioner,
                                     ObjectProvider<BaseCurrencyChangeHandler> baseCurrencyChangeHandler,
                                     CompanyLogoStorage logoStorage) {
        this.companyRepository = companyRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.appUserRepository = appUserRepository;
        this.companyRoleProvisioner = companyRoleProvisioner;
        this.baseCurrencyChangeHandler = baseCurrencyChangeHandler;
        this.logoStorage = logoStorage;
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
    public CompanyResponse create(CompanyWriteRequest req, UUID creatorUserId) {
        CompanyEntity c = new CompanyEntity();
        c.setId(UUID.randomUUID());
        applyWrite(c, req);
        companyRepository.save(c);
        companyRoleProvisioner.provisionRoles(c.getId());
        companyRoleProvisioner.grantAdminToUser(c.getId(), creatorUserId);
        return CompanyResponse.from(c);
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyWriteRequest req) {
        CompanyEntity c = load(id);
        String previousCurrency = c.getDefaultCurrency();
        String previousLogo = c.getLogoUrl();
        applyWrite(c, req);
        if (!Objects.equals(blankToNull(previousLogo), c.getLogoUrl())) {
            logoStorage.deleteIfPresent(previousLogo);
        }
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
    public CompanyResponse uploadLogo(UUID id, MultipartFile file) {
        CompanyEntity c = load(id);
        String previousLogo = c.getLogoUrl();
        CompanyLogoStorage.StoredImage stored;
        try {
            stored = logoStorage.store(id, file);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        logoStorage.deleteIfPresent(previousLogo);
        c.setLogoUrl(stored.publicUrl());
        companyRepository.save(c);
        return CompanyResponse.from(c);
    }

    @Transactional
    public CompanyResponse deleteLogo(UUID id) {
        CompanyEntity c = load(id);
        logoStorage.deleteIfPresent(c.getLogoUrl());
        c.setLogoUrl(null);
        companyRepository.save(c);
        return CompanyResponse.from(c);
    }

    @Transactional
    public CompanyResponse setPeriodLock(UUID id, LocalDate periodLockDate) {
        CompanyEntity c = load(id);
        if (periodLockDate != null || c.getPeriodLockDate() != null) {
            // Accounting close owns the floor; platform still stores the visible lock.
            // Clearing or lowering below closed periods is rejected by accounting settings when used.
            c.setPeriodLockDate(periodLockDate);
        } else {
            c.setPeriodLockDate(null);
        }
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
        if (r.allowBillWithoutReceipt() != null) {
            c.setAllowBillWithoutReceipt(r.allowBillWithoutReceipt());
        }
        if (r.allowInvoiceWithoutDelivery() != null) {
            c.setAllowInvoiceWithoutDelivery(r.allowInvoiceWithoutDelivery());
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
