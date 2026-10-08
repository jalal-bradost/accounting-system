package com.bradox.erp.accounting.service.domain.create;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class JournalItemCommand {
    @NotNull
    private final UUID accountId;
    private final String label;
    @NotNull
    private final BigDecimal debit;
    @NotNull
    private final BigDecimal credit;
    private final String currencyCode;
    private final BigDecimal amountCurrency;
    /** Optional per-line partner override; when null, the entry-level partner applies. */
    private final UUID partnerId;
    /** Optional analytic dimension, for example {@code tsh.project} and the project id. */
    private final String analyticModel;
    private final UUID analyticId;

    public JournalItemCommand(UUID accountId, String label, BigDecimal debit, BigDecimal credit,
                              String currencyCode, BigDecimal amountCurrency) {
        this(accountId, label, debit, credit, currencyCode, amountCurrency, null);
    }

    public JournalItemCommand(UUID accountId, String label, BigDecimal debit, BigDecimal credit,
                              String currencyCode, BigDecimal amountCurrency, UUID partnerId) {
        this(accountId, label, debit, credit, currencyCode, amountCurrency, partnerId, null, null);
    }

    @JsonCreator
    public JournalItemCommand(@JsonProperty("accountId") UUID accountId,
                              @JsonProperty("label") String label,
                              @JsonProperty("debit") BigDecimal debit,
                              @JsonProperty("credit") BigDecimal credit,
                              @JsonProperty("currencyCode") String currencyCode,
                              @JsonProperty("amountCurrency") BigDecimal amountCurrency,
                              @JsonProperty("partnerId") UUID partnerId,
                              @JsonProperty("analyticModel") String analyticModel,
                              @JsonProperty("analyticId") UUID analyticId) {
        this.accountId = accountId;
        this.label = label;
        this.debit = debit != null ? debit : BigDecimal.ZERO;
        this.credit = credit != null ? credit : BigDecimal.ZERO;
        this.currencyCode = currencyCode;
        this.amountCurrency = amountCurrency;
        this.partnerId = partnerId;
        this.analyticModel = analyticModel == null || analyticModel.isBlank() ? null : analyticModel.trim();
        this.analyticId = this.analyticModel == null ? null : analyticId;
    }

    public UUID getAccountId() { return accountId; }
    public String getLabel() { return label; }
    public BigDecimal getDebit() { return debit; }
    public BigDecimal getCredit() { return credit; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getAmountCurrency() { return amountCurrency; }
    public UUID getPartnerId() { return partnerId; }
    public String getAnalyticModel() { return analyticModel; }
    public UUID getAnalyticId() { return analyticId; }
}
