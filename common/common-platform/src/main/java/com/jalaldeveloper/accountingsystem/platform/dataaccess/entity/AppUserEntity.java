package com.jalaldeveloper.accountingsystem.platform.dataaccess.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "platform_app_user", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "username"}),
        @UniqueConstraint(columnNames = {"company_id", "email"})
})
public class AppUserEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    /** Hashed password placeholder; populated only once Spring Security is wired. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    public AppUserEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
