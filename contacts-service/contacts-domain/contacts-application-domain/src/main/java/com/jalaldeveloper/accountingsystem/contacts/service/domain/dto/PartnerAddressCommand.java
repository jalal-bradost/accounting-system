package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.AddressType;
import jakarta.validation.constraints.NotNull;

public class PartnerAddressCommand {
    @NotNull private AddressType type;
    private boolean defaultForType;
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

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
