package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Column holder for {@code sign_template}; the adapter maps it to the domain model. */
@Entity
@Table(name = "sign_template")
public class SignTemplateEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "name", nullable = false)
    public String name;
    @Column(name = "document_id", nullable = false)
    public UUID documentId;
    @Column(name = "document_sha256", nullable = false)
    public String documentSha256;
    @Column(name = "page_count", nullable = false)
    public int pageCount;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", nullable = false)
    @OrderBy("sequence")
    public List<SignTemplateFieldEntity> fields = new ArrayList<>();
}
