package com.bradox.erp.timesheet.service.domain.dto;

/** {@code confirmLong} acknowledges a run over 12 hours (TSH-04 #5). */
public record StopTimerCommand(String description, Boolean confirmLong) {
}
