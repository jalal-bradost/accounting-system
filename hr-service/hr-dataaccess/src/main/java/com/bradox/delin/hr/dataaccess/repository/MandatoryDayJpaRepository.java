package com.bradox.delin.hr.dataaccess.repository;

import com.bradox.delin.hr.dataaccess.entity.MandatoryDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MandatoryDayJpaRepository extends JpaRepository<MandatoryDayEntity, UUID> {
    List<MandatoryDayEntity> findByCompanyIdAndMandatoryDateBetweenOrderByMandatoryDateAsc(
            UUID companyId, LocalDate from, LocalDate to);
}
