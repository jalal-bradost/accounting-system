package com.jalaldeveloper.accountingsystem.platform.bootstrap;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.CompanyEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.PermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.CompanyJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.PermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.settings.CompanyRoleProvisioner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Idempotently inserts the canonical permission catalog and a baseline role set
 * for the demo company. Mirrors the existing {@code DefaultCompanyChartDataSeeder}
 * pattern in the accounting service.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "platform.seed.rbac", havingValue = "true", matchIfMissing = true)
public class PlatformRbacSeeder implements ApplicationRunner {

    /** Default demo company id; matches {@code DashboardController.DEFAULT_COMPANY_ID}. */
    public static final UUID DEFAULT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final PermissionJpaRepository permissionRepository;
    private final CompanyJpaRepository companyRepository;
    private final CompanyRoleProvisioner companyRoleProvisioner;

    public PlatformRbacSeeder(PermissionJpaRepository permissionRepository,
                              CompanyJpaRepository companyRepository,
                              CompanyRoleProvisioner companyRoleProvisioner) {
        this.permissionRepository = permissionRepository;
        this.companyRepository = companyRepository;
        this.companyRoleProvisioner = companyRoleProvisioner;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDefaultCompany();
        seedPermissions();
        companyRoleProvisioner.provisionRoles(DEFAULT_COMPANY_ID);
    }

    private void seedDefaultCompany() {
        if (companyRepository.existsById(DEFAULT_COMPANY_ID)) {
            return;
        }
        CompanyEntity c = new CompanyEntity();
        c.setId(DEFAULT_COMPANY_ID);
        c.setName("Demo Company");
        c.setLegalName("Demo Company LLC");
        c.setEmail("contact@demo.local");
        c.setCountry("IQ");
        c.setDefaultCurrency("IQD");
        c.setLocale("en-US");
        c.setDateFormat("yyyy-MM-dd");
        c.setNumberFormat("#,##0.00");
        c.setFiscalYearStartMonth(1);
        companyRepository.save(c);
    }

    private void seedPermissions() {
        for (String code : companyRoleProvisioner.permissionCatalog()) {
            if (!permissionRepository.existsByCode(code)) {
                PermissionEntity p = new PermissionEntity();
                p.setId(UUID.randomUUID());
                p.setCode(code);
                p.setDescription(code);
                permissionRepository.save(p);
            }
        }
    }
}
