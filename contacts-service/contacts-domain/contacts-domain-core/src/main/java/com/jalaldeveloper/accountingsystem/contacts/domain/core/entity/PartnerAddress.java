package com.jalaldeveloper.accountingsystem.contacts.domain.core.entity;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.AddressType;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerAddressId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.domain.entity.BaseEntity;

/** Postal address attached to a {@link Partner}. Owned by the partner aggregate. */
public class PartnerAddress extends BaseEntity<PartnerAddressId> {

    private final PartnerId partnerId;
    private final AddressType type;
    private boolean defaultForType;
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private PartnerAddress(Builder b) {
        super.setId(b.id);
        this.partnerId = b.partnerId;
        this.type = b.type;
        this.defaultForType = b.defaultForType;
        this.street1 = b.street1;
        this.street2 = b.street2;
        this.city = b.city;
        this.state = b.state;
        this.postalCode = b.postalCode;
        this.country = b.country;
    }

    public PartnerId getPartnerId() { return partnerId; }
    public AddressType getType() { return type; }
    public boolean isDefaultForType() { return defaultForType; }
    public String getStreet1() { return street1; }
    public String getStreet2() { return street2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }

    void markDefault(boolean isDefault) {
        this.defaultForType = isDefault;
    }

    public void update(String street1, String street2, String city, String state, String postalCode, String country) {
        this.street1 = street1;
        this.street2 = street2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PartnerAddressId id;
        private PartnerId partnerId;
        private AddressType type;
        private boolean defaultForType;
        private String street1;
        private String street2;
        private String city;
        private String state;
        private String postalCode;
        private String country;

        public Builder id(PartnerAddressId v) { this.id = v; return this; }
        public Builder partnerId(PartnerId v) { this.partnerId = v; return this; }
        public Builder type(AddressType v) { this.type = v; return this; }
        public Builder defaultForType(boolean v) { this.defaultForType = v; return this; }
        public Builder street1(String v) { this.street1 = v; return this; }
        public Builder street2(String v) { this.street2 = v; return this; }
        public Builder city(String v) { this.city = v; return this; }
        public Builder state(String v) { this.state = v; return this; }
        public Builder postalCode(String v) { this.postalCode = v; return this; }
        public Builder country(String v) { this.country = v; return this; }
        public PartnerAddress build() { return new PartnerAddress(this); }
    }
}
