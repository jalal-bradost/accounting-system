package com.bradox.erp.timesheet.service.domain.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The daily accounting-integrity check (NFR): problems between approved weeks, their cost and their ledger postings. */
public record IntegrityResponse(boolean ok, List<Issue> issues) {

    /** {@code kind}: MISSING_POSTING, POSTED_NOT_APPROVED, LINES_MISMATCH or COST_MISMATCH. */
    public record Issue(String kind, UUID weekId, String employeeName, LocalDate weekStart, String detail) {
    }
}
