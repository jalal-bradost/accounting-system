package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.FiscalPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiscalPeriodJpaRepository extends JpaRepository<FiscalPeriodEntity, UUID> {

    @Query("SELECT p FROM FiscalPeriodEntity p WHERE p.companyId = :companyId AND :date BETWEEN p.startDate AND p.endDate")
    Optional<FiscalPeriodEntity> findByCompanyIdAndDateBetweenStartAndEnd(
            @Param("companyId") UUID companyId, @Param("date") LocalDate date);

    List<FiscalPeriodEntity> findByCompanyIdOrderByStartDateDesc(UUID companyId);
}
