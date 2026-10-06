package com.bradox.erp.accounting.service.domain.customerinvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AllocateCustomerPaymentCommand {

    @NotEmpty
    @Valid
    private List<CustomerPaymentAllocationLine> allocations = new ArrayList<>();
    /** Defaults to the later of the payment date and the document date. */
    private LocalDate allocationDate;

    public List<CustomerPaymentAllocationLine> getAllocations() { return allocations; }
    public void setAllocations(List<CustomerPaymentAllocationLine> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }
    public LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(LocalDate allocationDate) { this.allocationDate = allocationDate; }
}
