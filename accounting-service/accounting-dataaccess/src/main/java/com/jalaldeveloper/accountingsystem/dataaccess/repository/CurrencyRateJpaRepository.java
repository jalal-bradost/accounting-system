package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.CurrencyRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrencyRateJpaRepository extends JpaRepository<CurrencyRateEntity, UUID> {

    /** Newest-first history for a currency. Used by the history UI. */
    List<CurrencyRateEntity> findByCurrencyIdOrderByEffectiveDateDesc(UUID currencyId);

    /** Latest line whose effective_date ≤ the given date — i.e. the rate that applies. */
    Optional<CurrencyRateEntity>
            findFirstByCurrencyIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                    UUID currencyId, LocalDate effectiveDate);

    /** Highest-dated line overall (may be in the future) — used for the "Last rate updated" cache. */
    Optional<CurrencyRateEntity> findFirstByCurrencyIdOrderByEffectiveDateDesc(UUID currencyId);

    Optional<CurrencyRateEntity> findByCurrencyIdAndEffectiveDate(UUID currencyId, LocalDate effectiveDate);
}
