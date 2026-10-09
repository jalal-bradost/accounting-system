package com.bradox.erp.project.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectCommand(@NotBlank @Size(max = 255) String name, UUID customerPartnerId, String managerUsername,
                             LocalDate startDate, LocalDate endDate, Integer color) {

    public int colorOrNone() {
        return color == null ? 0 : color;
    }
}
