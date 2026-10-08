package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sign_counter")
@IdClass(CounterEntity.Key.class)
public class CounterEntity {

    public static class Key implements Serializable {
        private UUID companyId;
        private int counterYear;

        public Key() {
        }

        public Key(UUID companyId, int counterYear) {
            this.companyId = companyId;
            this.counterYear = counterYear;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(companyId, k.companyId) && counterYear == k.counterYear;
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyId, counterYear);
        }
    }

    @Id
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Id
    @Column(name = "counter_year", nullable = false)
    private int counterYear;
    @Column(name = "last_value", nullable = false)
    private int lastValue;

    public CounterEntity() {
    }

    public CounterEntity(UUID companyId, int counterYear, int lastValue) {
        this.companyId = companyId;
        this.counterYear = counterYear;
        this.lastValue = lastValue;
    }

    public int getLastValue() { return lastValue; }
    public void setLastValue(int lastValue) { this.lastValue = lastValue; }
}
