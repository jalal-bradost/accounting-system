package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sign_request_field")
public class RequestFieldEntity {

    @Id
    private UUID id;
    @Column(name = "request_id", nullable = false)
    private UUID requestId;
    @Column(name = "signer_item_id", nullable = false)
    private UUID signerItemId;
    @Column(name = "page", nullable = false)
    private int page;
    @Column(name = "x", nullable = false)
    private double x;
    @Column(name = "y", nullable = false)
    private double y;
    @Column(name = "width", nullable = false)
    private double width;
    @Column(name = "height", nullable = false)
    private double height;
    @Column(name = "field_type", nullable = false, length = 16)
    private String fieldType;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "label", length = 255)
    private String label;
    @Column(name = "placeholder", length = 255)
    private String placeholder;
    @Column(name = "auto_fill", nullable = false, length = 16)
    private String autoFill;
    @Column(name = "value_text", length = 1000)
    private String valueText;
    @Column(name = "value_bool")
    private Boolean valueBool;
    @Column(name = "value_image", columnDefinition = "TEXT")
    private String valueImage;

    public RequestFieldEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public UUID getSignerItemId() { return signerItemId; }
    public void setSignerItemId(UUID signerItemId) { this.signerItemId = signerItemId; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public String getAutoFill() { return autoFill; }
    public void setAutoFill(String autoFill) { this.autoFill = autoFill; }
    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }
    public Boolean getValueBool() { return valueBool; }
    public void setValueBool(Boolean valueBool) { this.valueBool = valueBool; }
    public String getValueImage() { return valueImage; }
    public void setValueImage(String valueImage) { this.valueImage = valueImage; }
}
