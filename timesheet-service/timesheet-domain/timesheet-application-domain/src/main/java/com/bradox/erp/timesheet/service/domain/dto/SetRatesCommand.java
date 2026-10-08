package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SetRatesCommand(@NotNull @Valid List<Rate> rates) {

    public record Rate(@NotNull UUID employeeId, @NotNull UUID saleLineId) {
    }
}
