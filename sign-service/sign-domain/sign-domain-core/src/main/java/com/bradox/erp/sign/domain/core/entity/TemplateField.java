package com.bradox.erp.sign.domain.core.entity;

import com.bradox.erp.sign.domain.core.rule.FieldGeometry;
import com.bradox.erp.sign.domain.core.valueobject.AutoFill;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;

import java.util.UUID;

public class TemplateField {

    private final UUID id;
    private final UUID roleId;
    private final FieldGeometry geometry;
    private final FieldType type;
    private final boolean required;
    private final String label;
    private final String placeholder;
    private final AutoFill autoFill;

    public TemplateField(UUID id, UUID roleId, FieldGeometry geometry, FieldType type, boolean required, String label,
                         String placeholder, AutoFill autoFill) {
        this.id = id;
        this.roleId = roleId;
        this.geometry = geometry;
        this.type = type;
        this.required = required;
        this.label = label;
        this.placeholder = placeholder;
        this.autoFill = autoFill == null ? AutoFill.NONE : autoFill;
    }

    public UUID getId() { return id; }
    public UUID getRoleId() { return roleId; }
    public FieldGeometry getGeometry() { return geometry; }
    public FieldType getType() { return type; }
    public boolean isRequired() { return required; }
    public String getLabel() { return label; }
    public String getPlaceholder() { return placeholder; }
    public AutoFill getAutoFill() { return autoFill; }
}
