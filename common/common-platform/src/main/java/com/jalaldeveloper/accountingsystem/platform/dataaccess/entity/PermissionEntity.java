package com.jalaldeveloper.accountingsystem.platform.dataaccess.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "platform_permission", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class PermissionEntity {

    @Id
    private UUID id;

    /** Permission code, e.g. {@code contacts.partner.write}. Globally unique. */
    @Column(nullable = false, length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    public PermissionEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
