package com.bradox.erp.timesheet.service.domain.dto;

/** How many people one reminder run notified (TSH-05 #8). Zero everywhere is normal when nothing is missing. */
public record ReminderRunResponse(int employeesReminded, int managersNotified) {
}
