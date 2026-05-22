package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.config.AccountingCurrencyProperties;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.CompanyCurrencyEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.CurrencyRateEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.CompanyCurrencyJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.CurrencyRateJpaRepository;
import com.jalaldeveloper.accountingsystem.web.DashboardController;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Seeds the demo company with a USD base row and configured additional currencies (IQD by default),
 * driven by {@code accounting.currencies} in {@code application.yml}.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "accounting.currencies.seed", havingValue = "true", matchIfMissing = true)
public class DefaultCompanyCurrencySeeder implements ApplicationRunner {

    private static final UUID COMPANY_ID = DashboardController.DEFAULT_COMPANY_ID;

    private final CompanyCurrencyJpaRepository companyCurrencyJpaRepository;
    private final CurrencyRateJpaRepository currencyRateJpaRepository;
    private final AccountingCurrencyProperties properties;

    public DefaultCompanyCurrencySeeder(
            CompanyCurrencyJpaRepository companyCurrencyJpaRepository,
            CurrencyRateJpaRepository currencyRateJpaRepository,
            AccountingCurrencyProperties properties) {
        this.companyCurrencyJpaRepository = companyCurrencyJpaRepository;
        this.currencyRateJpaRepository = currencyRateJpaRepository;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (companyCurrencyJpaRepository.existsByCompanyId(COMPANY_ID)) {
            return;
        }
        AccountingCurrencyProperties.BaseCurrency base = properties.getBase();
        LocalDate anchor = LocalDate.of(2010, 1, 1);
        insert(
                UUID.randomUUID(),
                base.getCode().trim().toUpperCase(),
                base.getSymbol(),
                base.getName(),
                BigDecimal.ONE,
                true,
                true,
                anchor);
        for (AccountingCurrencyProperties.ExtraCurrency extra : properties.getExtras()) {
            if (extra.getCode() == null || extra.getCode().isBlank()) {
                continue;
            }
            String code = extra.getCode().trim().toUpperCase();
            if (code.equalsIgnoreCase(base.getCode())) {
                continue;
            }
            BigDecimal rate =
                    extra.getRatePerUsd() != null && extra.getRatePerUsd().compareTo(BigDecimal.ZERO) > 0
                            ? extra.getRatePerUsd()
                            : BigDecimal.ONE;
            insert(
                    UUID.randomUUID(),
                    code,
                    nvl(extra.getSymbol(), code),
                    nvl(extra.getName(), code),
                    rate,
                    false,
                    extra.isActive(),
                    anchor);
        }
    }

    private static String nvl(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }

    private void insert(
            UUID id,
            String code,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean baseCurrency,
            boolean active,
            LocalDate lastRateUpdated) {
        CompanyCurrencyEntity e = new CompanyCurrencyEntity();
        e.setId(id);
        e.setCompanyId(COMPANY_ID);
        e.setCode(code);
        e.setSymbol(symbol);
        e.setName(name);
        e.setRatePerBase(ratePerBase);
        e.setBaseCurrency(baseCurrency);
        e.setActive(active);
        e.setLastRateUpdated(lastRateUpdated);
        companyCurrencyJpaRepository.save(e);

        // Anchor the dated history so transactions on/after this date can resolve a rate.
        CurrencyRateEntity rateRow = new CurrencyRateEntity();
        rateRow.setId(UUID.randomUUID());
        rateRow.setCurrencyId(id);
        rateRow.setEffectiveDate(lastRateUpdated);
        rateRow.setRate(ratePerBase);
        currencyRateJpaRepository.save(rateRow);
    }
}
