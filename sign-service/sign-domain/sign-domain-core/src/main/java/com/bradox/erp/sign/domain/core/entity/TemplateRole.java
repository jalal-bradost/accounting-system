package com.bradox.erp.sign.domain.core.entity;

import java.util.UUID;

/** A named signer slot in a template (Employee, Manager, Customer). */
public class TemplateRole {

    private final UUID id;
    private String name;
    private String color;
    private int sequence;
    private boolean approverOnly;
    private UUID defaultPartnerId;

    public TemplateRole(UUID id, String name, String color, int sequence, boolean approverOnly, UUID defaultPartnerId) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.sequence = sequence;
        this.approverOnly = approverOnly;
        this.defaultPartnerId = defaultPartnerId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public int getSequence() { return sequence; }
    public boolean isApproverOnly() { return approverOnly; }
    public UUID getDefaultPartnerId() { return defaultPartnerId; }
}
