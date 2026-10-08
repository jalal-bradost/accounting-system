package com.bradox.erp.repair.domain.core.model;

import com.bradox.erp.repair.domain.core.valueobject.InspectionResultCode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The inspection of one order. Once signed off its results are read-only (REP-02 #9). */
public record Inspection(UUID id, UUID companyId, UUID orderId, UUID templateId, String performedBy, Instant startedAt,
                         Instant signedOffAt, String signedOffBy, Integer odometerKm, String notes, List<Result> results) {

    public record Result(UUID id, int sequence, String section, String itemLabel, InspectionResultCode result, String note,
                         UUID photoDocumentId, UUID recommendedLineId) {
    }

    public boolean isSignedOff() {
        return signedOffAt != null;
    }

    public int count(InspectionResultCode code) {
        return (int) results.stream().filter(r -> r.result() == code).count();
    }
}
