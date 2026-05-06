package com.jalaldeveloper.accountingsystem.sales.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateSalesOrderCommand {

    private UUID companyId;
    @NotNull
    private UUID customerPartnerId;
    private String name;
    @NotNull
    private String currencyCode;
    private UUID warehouseId;
    private UUID pricelistId;
    private UUID paymentTermsId;
    private LocalDate orderDate;
    private LocalDate validityDate;
    private String incoterm;
    private String notes;
    private BigDecimal exchangeRateToCompany;
    @NotEmpty
    @Valid
    private List<SalesOrderLineCommand> lines = new ArrayList<>();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getPricelistId() { return pricelistId; }
    public void setPricelistId(UUID pricelistId) { this.pricelistId = pricelistId; }
    public UUID getPaymentTermsId() { return paymentTermsId; }
    public void setPaymentTermsId(UUID paymentTermsId) { this.paymentTermsId = paymentTermsId; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalDate getValidityDate() { return validityDate; }
    public void setValidityDate(LocalDate validityDate) { this.validityDate = validityDate; }
    public String getIncoterm() { return incoterm; }
    public void setIncoterm(String incoterm) { this.incoterm = incoterm; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public List<SalesOrderLineCommand> getLines() { return lines; }
    public void setLines(List<SalesOrderLineCommand> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
}
