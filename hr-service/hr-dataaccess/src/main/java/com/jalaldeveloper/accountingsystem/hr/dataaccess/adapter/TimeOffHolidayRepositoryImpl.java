package com.jalaldeveloper.accountingsystem.hr.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.MandatoryDayJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.PublicHolidayJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.TimeOffHolidayRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TimeOffHolidayRepositoryImpl implements TimeOffHolidayRepository {

    private final PublicHolidayJpaRepository publicHolidayRepository;
    private final MandatoryDayJpaRepository mandatoryDayRepository;

    public TimeOffHolidayRepositoryImpl(PublicHolidayJpaRepository publicHolidayRepository,
                                        MandatoryDayJpaRepository mandatoryDayRepository) {
        this.publicHolidayRepository = publicHolidayRepository;
        this.mandatoryDayRepository = mandatoryDayRepository;
    }

    @Override
    public List<HolidayRow> findPublicHolidays(CompanyId companyId, LocalDate from, LocalDate to) {
        return publicHolidayRepository
                .findByCompanyIdAndHolidayDateBetweenOrderByHolidayDateAsc(companyId.getId(), from, to)
                .stream()
                .map(h -> new HolidayRow(h.getName(), h.getHolidayDate(), h.getCountryCode()))
                .toList();
    }

    @Override
    public List<MandatoryDayRow> findMandatoryDays(CompanyId companyId, LocalDate from, LocalDate to) {
        return mandatoryDayRepository
                .findByCompanyIdAndMandatoryDateBetweenOrderByMandatoryDateAsc(companyId.getId(), from, to)
                .stream()
                .map(m -> new MandatoryDayRow(m.getName(), m.getMandatoryDate()))
                .toList();
    }
}
