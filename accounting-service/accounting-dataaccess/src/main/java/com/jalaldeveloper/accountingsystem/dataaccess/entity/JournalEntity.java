package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import jakarta.persistence.*;
import java.util.UUID;

@Table(name = "journals", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
@Entity
public class JournalEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(nullable = false, length = 10)
    private String code;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalType type;

    public JournalEntity() {}
    public JournalEntity(UUID id, UUID companyId, String code, String name, JournalType type) {
        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public JournalType getType() { return type; }
    public void setType(JournalType type) { this.type = type; }

    public static JournalEntityBuilder builder() { return new JournalEntityBuilder(); }
    public static final class JournalEntityBuilder {
        private UUID id;
        private UUID companyId;
        private String code;
        private String name;
        private JournalType type;
        public JournalEntityBuilder id(UUID id) { this.id = id; return this; }
        public JournalEntityBuilder companyId(UUID companyId) { this.companyId = companyId; return this; }
        public JournalEntityBuilder code(String code) { this.code = code; return this; }
        public JournalEntityBuilder name(String name) { this.name = name; return this; }
        public JournalEntityBuilder type(JournalType type) { this.type = type; return this; }
        public JournalEntity build() { return new JournalEntity(id, companyId, code, name, type); }
    }
}