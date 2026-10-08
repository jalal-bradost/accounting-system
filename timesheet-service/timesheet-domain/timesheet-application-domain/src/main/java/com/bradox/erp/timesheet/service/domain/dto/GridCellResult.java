package com.bradox.erp.timesheet.service.domain.dto;

import java.time.LocalDate;

public record GridCellResult(LocalDate workDate, int minutes, int entryCount, int dayTotalMinutes, String warning) {
}
