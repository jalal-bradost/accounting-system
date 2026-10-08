package com.bradox.erp.timesheet.service.domain.dto;

import java.time.LocalDate;

/** {@code entryCount > 1} means the cell is a sum and cannot be edited in place (TSH-03 #3). */
public record GridCellResponse(LocalDate date, int minutes, int entryCount) {
}
