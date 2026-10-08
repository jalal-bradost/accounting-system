package com.bradox.erp.repair.domain.core.model;

import java.util.UUID;

/** One job of the labor-time guide: standard minutes, optionally limited to a make, model and year range. */
public record GuideEntry(UUID id, UUID companyId, String code, String descriptionEn, String descriptionAr, String descriptionKu,
                         UUID laborCategoryId, int standardMinutes, String make, String model, Integer yearFrom, Integer yearTo,
                         boolean active) {
}
