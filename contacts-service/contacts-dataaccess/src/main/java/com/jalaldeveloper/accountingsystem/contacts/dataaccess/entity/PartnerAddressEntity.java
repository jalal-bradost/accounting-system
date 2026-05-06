package com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.AddressType;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "contacts_partner_address")
public class PartnerAddressEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressType type;

    @Column(name = "default_for_type", nullable = false)
    private boolean defaultForType;

    @Column(length = 255) private String street1;
    @Column(length = 255) private String street2;
    @Column(length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(name = "postal_code", length = 20) private String postalCode;
    @Column(length = 100) private String country;

    public PartnerAddressEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PartnerEntity getPartner() { return partner; }
    public void setPartner(PartnerEntity partner) { this.partner = partner; }
    public AddressType getType() { return type; }
    public void setType(AddressType type) { this.type = type; }
    public boolean isDefaultForType() { return defaultForType; }
    public void setDefaultForType(boolean v) { this.defaultForType = v; }
    public String getStreet1() { return street1; }
    public void setStreet1(String v) { this.street1 = v; }
    public String getStreet2() { return street2; }
    public void setStreet2(String v) { this.street2 = v; }
    public String getCity() { return city; }
    public void setCity(String v) { this.city = v; }
    public String getState() { return state; }
    public void setState(String v) { this.state = v; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String v) { this.postalCode = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }
}
