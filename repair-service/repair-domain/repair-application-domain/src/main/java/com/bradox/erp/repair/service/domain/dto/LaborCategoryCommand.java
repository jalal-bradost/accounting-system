package com.bradox.erp.repair.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record LaborCategoryCommand(@NotBlank String name, BigDecimal hourlyRate, boolean active) {
}
