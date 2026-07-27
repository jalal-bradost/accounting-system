package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PublicHolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PublicHolidayJpaRepository extends JpaRepository<PublicHolidayEntity, UUID> {
    List<PublicHolidayEntity> findByCompanyIdAndHolidayDateBetweenOrderByHolidayDateAsc(
            UUID companyId, LocalDate from, LocalDate to);
}
