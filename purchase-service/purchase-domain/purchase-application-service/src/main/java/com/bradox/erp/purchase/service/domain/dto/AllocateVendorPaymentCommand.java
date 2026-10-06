package com.bradox.erp.purchase.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AllocateVendorPaymentCommand {

    @NotEmpty
    @Valid
    private List<VendorPaymentAllocationLine> allocations = new ArrayList<>();
    /** Defaults to the later of the payment date and the document date. */
    private LocalDate allocationDate;

    public List<VendorPaymentAllocationLine> getAllocations() { return allocations; }
    public void setAllocations(List<VendorPaymentAllocationLine> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }
    public LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(LocalDate allocationDate) { this.allocationDate = allocationDate; }
}
