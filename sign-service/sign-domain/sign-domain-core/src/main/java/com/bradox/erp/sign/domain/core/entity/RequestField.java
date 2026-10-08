package com.bradox.erp.sign.domain.core.entity;

import com.bradox.erp.sign.domain.core.rule.FieldGeometry;
import com.bradox.erp.sign.domain.core.valueobject.AutoFill;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;

import java.util.UUID;

/** A field copied from the template (or placed one-off) into a request, owned by exactly one signer (BR-SIG-14, D9). */
public class RequestField {

    private final UUID id;
    private final UUID signerItemId;
    private final FieldGeometry geometry;
    private final FieldType type;
    private final boolean required;
    private final String label;
    private final String placeholder;
    private final AutoFill autoFill;
    private String valueText;
    private Boolean valueBool;
    private byte[] valueImage;

    public RequestField(UUID id, UUID signerItemId, FieldGeometry geometry, FieldType type, boolean required, String label,
                        String placeholder, AutoFill autoFill) {
        this.id = id;
        this.signerItemId = signerItemId;
        this.geometry = geometry;
        this.type = type;
        this.required = required;
        this.label = label;
        this.placeholder = placeholder;
        this.autoFill = autoFill == null ? AutoFill.NONE : autoFill;
    }

    public void restoreValue(String text, Boolean bool, byte[] image) {
        this.valueText = text;
        this.valueBool = bool;
        this.valueImage = image;
    }

    void setValue(String text, Boolean bool, byte[] image) {
        restoreValue(text, bool, image);
    }

    public boolean isFilled() {
        return switch (type) {
            case SIGNATURE, INITIALS -> valueImage != null && valueImage.length > 0;
            case CHECKBOX -> valueBool != null;
            case TEXT, DATE -> valueText != null && !valueText.isBlank();
        };
    }

    public UUID getId() { return id; }
    public UUID getSignerItemId() { return signerItemId; }
    public FieldGeometry getGeometry() { return geometry; }
    public FieldType getType() { return type; }
    public boolean isRequired() { return required; }
    public String getLabel() { return label; }
    public String getPlaceholder() { return placeholder; }
    public AutoFill getAutoFill() { return autoFill; }
    public String getValueText() { return valueText; }
    public Boolean getValueBool() { return valueBool; }
    public byte[] getValueImage() { return valueImage; }
}
