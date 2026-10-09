package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Column holder for {@code sign_template_field}. */
@Entity
@Table(name = "sign_template_field")
public class SignTemplateFieldEntity {

    @Id
    public UUID id;
    @Column(name = "sequence", nullable = false)
    public int sequence;
    @Column(name = "page", nullable = false)
    public int page;
    @Column(name = "x", nullable = false)
    public double x;
    @Column(name = "y", nullable = false)
    public double y;
    @Column(name = "width", nullable = false)
    public double width;
    @Column(name = "height", nullable = false)
    public double height;
    @Column(name = "field_type", nullable = false)
    public String fieldType;
}
