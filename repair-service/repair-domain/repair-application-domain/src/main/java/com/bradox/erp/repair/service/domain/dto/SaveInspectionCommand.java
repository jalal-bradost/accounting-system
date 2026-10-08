package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.valueobject.InspectionResultCode;

import java.util.List;
import java.util.UUID;

public record SaveInspectionCommand(Integer odometerKm, String notes, List<Result> results) {

    public record Result(String section, String itemLabel, InspectionResultCode result, String note, UUID photoDocumentId) {
    }
}
