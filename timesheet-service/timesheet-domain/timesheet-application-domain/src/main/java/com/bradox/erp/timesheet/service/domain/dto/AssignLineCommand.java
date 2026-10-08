package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignLineCommand(@NotNull UUID saleLineId) {
}
