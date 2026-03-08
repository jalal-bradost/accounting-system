package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import jakarta.persistence.*;
import java.util.UUID;

@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
@Entity
public class AccountEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(nullable = false, length = 20)
    private String code;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;
    private boolean active;

    public AccountEntity() {}
    public AccountEntity(UUID id, UUID companyId, String code, String name, AccountType type, boolean active) {
        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.type = type;
        this.active = active;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public static AccountEntityBuilder builder() { return new AccountEntityBuilder(); }
    public static final class AccountEntityBuilder {
        private UUID id;
        private UUID companyId;
        private String code;
        private String name;
        private AccountType type;
        private boolean active;
        public AccountEntityBuilder id(UUID id) { this.id = id; return this; }
        public AccountEntityBuilder companyId(UUID companyId) { this.companyId = companyId; return this; }
        public AccountEntityBuilder code(String code) { this.code = code; return this; }
        public AccountEntityBuilder name(String name) { this.name = name; return this; }
        public AccountEntityBuilder type(AccountType type) { this.type = type; return this; }
        public AccountEntityBuilder active(boolean active) { this.active = active; return this; }
        public AccountEntity build() { return new AccountEntity(id, companyId, code, name, type, active); }
    }
}