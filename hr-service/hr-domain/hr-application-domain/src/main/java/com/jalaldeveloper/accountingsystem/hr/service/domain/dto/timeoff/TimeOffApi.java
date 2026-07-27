package com.jalaldeveloper.accountingsystem.hr.service.domain.dto.timeoff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TimeOffApi {

    private TimeOffApi() {}

    public record TimeOffTypeResponse(
            UUID id, String name, String code, String displayCode,
            String countryCode, String colorHex, int sortOrder, boolean active
    ) {}

    public record SaveTimeOffTypeCommand(
            UUID companyId, @NotBlank String name, @NotBlank String code,
            @NotBlank String displayCode, String countryCode, String colorHex,
            Integer sortOrder, Boolean active
    ) {}

    public record AllocationResponse(
            UUID id, UUID employeeId, String employeeName, String employeeImageUrl,
            UUID timeOffTypeId, String timeOffTypeName, String timeOffTypeColor,
            String name, BigDecimal numberOfDays, BigDecimal usedDays, BigDecimal remainingDays,
            String allocationType, String state, LocalDate dateFrom, LocalDate dateTo
    ) {}

    public record SaveAllocationCommand(
            UUID companyId, @NotNull UUID employeeId, @NotNull UUID timeOffTypeId,
            @NotBlank String name, @NotNull BigDecimal numberOfDays,
            String allocationType, @NotNull LocalDate dateFrom, @NotNull LocalDate dateTo
    ) {}

    public record LeaveRequestResponse(
            UUID id, UUID employeeId, String employeeName, String employeeImageUrl,
            UUID timeOffTypeId, String timeOffTypeName, String timeOffTypeColor, String timeOffTypeDisplay,
            LocalDate dateFrom, LocalDate dateTo, BigDecimal numberOfDays,
            String state, String description
    ) {}

    public record SaveLeaveRequestCommand(
            UUID companyId, @NotNull UUID employeeId, @NotNull UUID timeOffTypeId,
            @NotNull LocalDate dateFrom, @NotNull LocalDate dateTo,
            BigDecimal numberOfDays, String description
    ) {}

    public record DashboardSummaryResponse(
            UUID timeOffTypeId, String timeOffTypeName, String colorHex,
            BigDecimal allocatedDays, BigDecimal usedDays, BigDecimal availableDays,
            LocalDate validUntil
    ) {}

    public record DashboardCalendarDayResponse(
            LocalDate date, String kind, UUID requestId, String state,
            UUID timeOffTypeId, String timeOffTypeName, String colorHex
    ) {}

    public record DashboardResponse(
            int year, UUID employeeId, String employeeName,
            long pendingRequests,
            List<DashboardSummaryResponse> summaries,
            List<DashboardCalendarDayResponse> calendarDays,
            List<HolidayResponse> publicHolidays,
            List<MandatoryDayResponse> mandatoryDays,
            List<TimeOffTypeResponse> timeOffTypes
    ) {}

    public record HolidayResponse(String name, LocalDate date, String countryCode) {}
    public record MandatoryDayResponse(String name, LocalDate date) {}

    public record TeamSummaryItemResponse(
            UUID employeeId,
            String displayName,
            String imageUrl,
            boolean outToday,
            long pendingRequests,
            BigDecimal availableLeaveDays
    ) {}

    public record TeamSummaryResponse(
            LocalDate date,
            List<TeamSummaryItemResponse> members
    ) {}
}
