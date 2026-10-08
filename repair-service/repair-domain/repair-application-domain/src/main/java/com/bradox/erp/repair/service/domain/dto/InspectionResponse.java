package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.model.Inspection;

public record InspectionResponse(Inspection inspection, InspectionSummary summary) {
}
