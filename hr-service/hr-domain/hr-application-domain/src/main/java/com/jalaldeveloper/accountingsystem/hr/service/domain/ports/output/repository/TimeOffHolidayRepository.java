package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.List;

public interface TimeOffHolidayRepository {

    record HolidayRow(String name, LocalDate date, String countryCode) {}
    record MandatoryDayRow(String name, LocalDate date) {}

    List<HolidayRow> findPublicHolidays(CompanyId companyId, LocalDate from, LocalDate to);
    List<MandatoryDayRow> findMandatoryDays(CompanyId companyId, LocalDate from, LocalDate to);
}
