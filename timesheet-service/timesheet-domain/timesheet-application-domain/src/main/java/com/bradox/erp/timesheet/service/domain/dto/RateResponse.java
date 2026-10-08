package com.bradox.erp.timesheet.service.domain.dto;

import java.util.UUID;

public record RateResponse(UUID employeeId, String employeeName, UUID saleLineId, String saleLineLabel) {
}
