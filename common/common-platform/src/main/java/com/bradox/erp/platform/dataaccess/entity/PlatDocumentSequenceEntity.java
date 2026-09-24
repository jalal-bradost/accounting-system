package com.bradox.erp.platform.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "plat_document_sequence",
        uniqueConstraints = @UniqueConstraint(name = "uk_plat_doc_seq", columnNames = {"company_id", "doc_type", "seq_year"}))
public class PlatDocumentSequenceEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    /** Calendar year for PO/SO/BILL/INV; 0 for PAY (no year in format). Named seq_year — "year" is reserved in H2. */
    @Column(name = "seq_year", nullable = false)
    private int year;

    @Column(name = "last_value", nullable = false)
    private long lastValue;

    public PlatDocumentSequenceEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getLastValue() {
        return lastValue;
    }

    public void setLastValue(long lastValue) {
        this.lastValue = lastValue;
    }
}
