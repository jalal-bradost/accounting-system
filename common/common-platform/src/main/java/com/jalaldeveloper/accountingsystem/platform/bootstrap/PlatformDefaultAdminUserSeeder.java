package com.jalaldeveloper.accountingsystem.platform.bootstrap;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.UserRoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.UserRoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.security.PlatformSecurityProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seeds a single company-scoped admin user linked to the ADMIN role for local development and demos.
 * Password is taken from {@code app.security.seed-default-admin-password}.
 */
@Component
@Order(50)
@ConditionalOnProperty(name = "platform.seed.admin-user", havingValue = "true", matchIfMissing = true)
public class PlatformDefaultAdminUserSeeder implements ApplicationRunner {

    public static final UUID DEFAULT_ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private final AppUserJpaRepository appUserJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformSecurityProperties securityProperties;

    public PlatformDefaultAdminUserSeeder(
            AppUserJpaRepository appUserJpaRepository,
            RoleJpaRepository roleJpaRepository,
            UserRoleJpaRepository userRoleJpaRepository,
            PasswordEncoder passwordEncoder,
            PlatformSecurityProperties securityProperties) {
        this.appUserJpaRepository = appUserJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.userRoleJpaRepository = userRoleJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserJpaRepository.existsById(DEFAULT_ADMIN_USER_ID)) {
            return;
        }
        RoleEntity admin = roleJpaRepository
                .findByCompanyIdAndCode(PlatformRbacSeeder.DEFAULT_COMPANY_ID, "ADMIN")
                .orElse(null);
        if (admin == null) {
            return;
        }
        AppUserEntity user = new AppUserEntity();
        user.setId(DEFAULT_ADMIN_USER_ID);
        user.setCompanyId(PlatformRbacSeeder.DEFAULT_COMPANY_ID);
        user.setUsername("admin");
        user.setEmail("admin@example.local");
        user.setDisplayName("Administrator");
        user.setPasswordHash(passwordEncoder.encode(securityProperties.getSeedDefaultAdminPassword()));
        appUserJpaRepository.save(user);
        userRoleJpaRepository.save(new UserRoleEntity(user.getId(), admin.getId()));
    }
}
