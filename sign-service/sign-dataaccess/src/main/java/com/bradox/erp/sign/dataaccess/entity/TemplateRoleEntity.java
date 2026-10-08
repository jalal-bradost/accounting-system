package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sign_template_role")
public class TemplateRoleEntity {

    @Id
    private UUID id;
    @Column(name = "template_id", nullable = false)
    private UUID templateId;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "color", length = 20)
    private String color;
    @Column(name = "sequence", nullable = false)
    private int sequence;
    @Column(name = "approver_only", nullable = false)
    private boolean approverOnly;
    @Column(name = "default_partner_id")
    private UUID defaultPartnerId;

    public TemplateRoleEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public boolean isApproverOnly() { return approverOnly; }
    public void setApproverOnly(boolean approverOnly) { this.approverOnly = approverOnly; }
    public UUID getDefaultPartnerId() { return defaultPartnerId; }
    public void setDefaultPartnerId(UUID defaultPartnerId) { this.defaultPartnerId = defaultPartnerId; }
}
