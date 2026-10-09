package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code sign_document}. */
@Entity
@Table(name = "sign_document")
public class SignedDocumentEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "template_id", nullable = false)
    public UUID templateId;
    @Column(name = "template_name", nullable = false)
    public String templateName;
    @Column(name = "signer_name", nullable = false)
    public String signerName;
    @Column(name = "signed_at", nullable = false)
    public Instant signedAt;
    @Column(name = "signed_by")
    public String signedBy;
    @Column(name = "document_id", nullable = false)
    public UUID documentId;
    @Column(name = "file_name", nullable = false)
    public String fileName;
}
