package com.bradox.erp.repair.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TemplateCommand(@NotBlank String name, String vehicleType, boolean active, List<Item> items) {

    public record Item(String section, String label) {
    }
}
