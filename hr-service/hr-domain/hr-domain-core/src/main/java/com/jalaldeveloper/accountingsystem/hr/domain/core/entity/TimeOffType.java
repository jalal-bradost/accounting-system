package com.jalaldeveloper.accountingsystem.hr.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.TimeOffTypeId;

import java.util.Locale;
import java.util.Set;

public class TimeOffType {

    public static final Set<String> COMPENSATORY_CODES = Set.of("LEAVE105", "CTO", "COMP", "COMPENSATORY");

    private final TimeOffTypeId id;
    private final CompanyId companyId;
    private final String name;
    private final String code;
    private final String displayCode;
    private final String countryCode;
    private final String colorHex;
    private final int sortOrder;
    private final boolean active;

    private TimeOffType(Builder b) {
        this.id = b.id;
        this.companyId = b.companyId;
        this.name = b.name;
        this.code = b.code;
        this.displayCode = b.displayCode;
        this.countryCode = b.countryCode;
        this.colorHex = b.colorHex;
        this.sortOrder = b.sortOrder;
        this.active = b.active;
    }

    public void validate() {
        if (companyId == null) throw new HrDomainException("companyId required");
        if (name == null || name.isBlank()) throw new HrDomainException("name required");
        if (code == null || code.isBlank()) throw new HrDomainException("code required");
        if (displayCode == null || displayCode.isBlank()) throw new HrDomainException("displayCode required");
        if (isCompensatory()) {
            throw new HrDomainException("Compensatory time off is not supported");
        }
    }

    public boolean isCompensatory() {
        if (code == null) return false;
        String normalized = code.toUpperCase(Locale.ROOT);
        return COMPENSATORY_CODES.contains(normalized)
                || name.toLowerCase(Locale.ROOT).contains("compensatory");
    }

    public TimeOffTypeId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDisplayCode() { return displayCode; }
    public String getCountryCode() { return countryCode; }
    public String getColorHex() { return colorHex; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private TimeOffTypeId id;
        private CompanyId companyId;
        private String name;
        private String code;
        private String displayCode;
        private String countryCode;
        private String colorHex = "#714B67";
        private int sortOrder;
        private boolean active = true;

        public Builder id(TimeOffTypeId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder code(String v) { this.code = v; return this; }
        public Builder displayCode(String v) { this.displayCode = v; return this; }
        public Builder countryCode(String v) { this.countryCode = v; return this; }
        public Builder colorHex(String v) { this.colorHex = v; return this; }
        public Builder sortOrder(int v) { this.sortOrder = v; return this; }
        public Builder active(boolean v) { this.active = v; return this; }
        public TimeOffType build() { return new TimeOffType(this); }
    }
}
