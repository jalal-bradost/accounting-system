package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort.DayType;

import java.time.LocalDate;

public record GridDayResponse(LocalDate date, int expectedMinutes, DayType dayType, String dayLabel,
                              int loggedMinutes, int attendedMinutes) {
}
