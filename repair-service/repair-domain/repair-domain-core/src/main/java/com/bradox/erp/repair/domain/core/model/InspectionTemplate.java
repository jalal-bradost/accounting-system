package com.bradox.erp.repair.domain.core.model;

import java.util.List;
import java.util.UUID;

public record InspectionTemplate(UUID id, UUID companyId, String name, String vehicleType, boolean active, List<Item> items) {

    public record Item(UUID id, String section, String label, int sequence) {
    }
}
