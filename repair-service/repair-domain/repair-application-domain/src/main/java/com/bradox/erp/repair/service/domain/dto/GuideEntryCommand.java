package com.bradox.erp.repair.service.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record GuideEntryCommand(@NotBlank String code, String descriptionEn, String descriptionAr, String descriptionKu,
                                UUID laborCategoryId, @Min(1) int standardMinutes, String make, String model, Integer yearFrom,
                                Integer yearTo, boolean active) {
}
