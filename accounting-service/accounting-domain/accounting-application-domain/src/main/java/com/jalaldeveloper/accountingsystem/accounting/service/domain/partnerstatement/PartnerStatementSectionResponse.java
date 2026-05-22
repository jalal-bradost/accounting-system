package com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Receivable (AR) or payable (AP) slice of a partner statement. */
public class PartnerStatementSectionResponse {

    private String currencyCode;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private List<PartnerStatementLineResponse> lines = new ArrayList<>();

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public List<PartnerStatementLineResponse> getLines() {
        return lines;
    }

    public void setLines(List<PartnerStatementLineResponse> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}
