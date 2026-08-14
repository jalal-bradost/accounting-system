package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "dataset_import_ref")
@IdClass(DatasetImportRefEntity.Key.class)
public class DatasetImportRefEntity {

    @Id
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Id
    @Column(name = "ref_type", nullable = false, length = 32)
    private String refType;

    @Id
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    public DatasetImportRefEntity() {}

    public DatasetImportRefEntity(UUID companyId, String refType, String code, UUID entityId) {
        this.companyId = companyId;
        this.refType = refType;
        this.code = code;
        this.entityId = entityId;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public static class Key implements Serializable {
        private UUID companyId;
        private String refType;
        private String code;

        public Key() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(companyId, key.companyId)
                    && Objects.equals(refType, key.refType)
                    && Objects.equals(code, key.code);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyId, refType, code);
        }
    }
}
