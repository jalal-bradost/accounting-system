package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

/** Used to refuse a week and to reopen one; both need a reason (BR-TSH-11). */
public record WeekReasonCommand(@NotBlank String reason) {
}
