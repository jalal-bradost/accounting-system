package com.jalaldeveloper.accountingsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap defaults for {@link com.jalaldeveloper.accountingsystem.bootstrap.DefaultCompanyCurrencySeeder}.
 * Base currency is always USD in v1; additional rows (e.g. IQD) are configurable here.
 */
@ConfigurationProperties(prefix = "accounting.currencies")
public class AccountingCurrencyProperties {

    /** When true, inserts configured currencies for the demo company if none exist. */
    private boolean seed = true;

    private BaseCurrency base = new BaseCurrency();

    private List<ExtraCurrency> extras = new ArrayList<>();

    public boolean isSeed() {
        return seed;
    }

    public void setSeed(boolean seed) {
        this.seed = seed;
    }

    public BaseCurrency getBase() {
        return base;
    }

    public void setBase(BaseCurrency base) {
        this.base = base;
    }

    public List<ExtraCurrency> getExtras() {
        return extras;
    }

    public void setExtras(List<ExtraCurrency> extras) {
        this.extras = extras;
    }

    public static class BaseCurrency {
        private String code = "USD";
        private String symbol = "$";
        private String name = "United States dollar";

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ExtraCurrency {
        private String code;
        private String symbol;
        private String name;
        /** Units of this currency per 1 unit of base (USD). */
        private BigDecimal ratePerUsd = new BigDecimal("1310");
        private boolean active = true;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getRatePerUsd() {
            return ratePerUsd;
        }

        public void setRatePerUsd(BigDecimal ratePerUsd) {
            this.ratePerUsd = ratePerUsd;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
