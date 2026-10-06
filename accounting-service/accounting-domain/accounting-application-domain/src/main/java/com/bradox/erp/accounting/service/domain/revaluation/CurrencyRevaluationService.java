package com.bradox.erp.accounting.service.domain.revaluation;

import com.bradox.erp.accounting.service.domain.JournalEntryTiming;
import com.bradox.erp.accounting.service.domain.PeriodPostingGuard;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyRevaluationQueryPort;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyRevaluationQueryPort.OpenForeignBalance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Period-end revaluation of what customers owe and what is owed to vendors in foreign currencies.
 * Each open balance is carried at the rate it was booked at; this moves it to the rate in force on
 * the revaluation date, booking the difference as exchange gain or loss. The entry is reversed the
 * next day, so settling the balance later still books the full realised difference once and a
 * second revaluation starts from the original values.
 */
@Service
public class CurrencyRevaluationService {

    static final String EXCHANGE_GAIN_ACCOUNT_CODE = "430014";
    static final String EXCHANGE_LOSS_ACCOUNT_CODE = "430015";
    static final String EXCHANGE_JOURNAL_CODE = "EXCH";
    private static final BigDecimal MINIMUM = new BigDecimal("0.01");

    private final CurrencyRevaluationQueryPort queryPort;
    private final CurrencyConversionPort currencyConversionPort;
    private final AccountingReferenceLookupPort lookupPort;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final PeriodPostingGuard periodPostingGuard;

    public CurrencyRevaluationService(CurrencyRevaluationQueryPort queryPort,
                                      CurrencyConversionPort currencyConversionPort,
                                      AccountingReferenceLookupPort lookupPort,
                                      JournalEntryApplicationService journalEntryApplicationService,
                                      PeriodPostingGuard periodPostingGuard) {
        this.queryPort = queryPort;
        this.currencyConversionPort = currencyConversionPort;
        this.lookupPort = lookupPort;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.periodPostingGuard = periodPostingGuard;
    }

    /** What revaluing on {@code asOf} would change; nothing is posted. */
    @Transactional(readOnly = true)
    public CurrencyRevaluationResult preview(UUID companyId, LocalDate asOf) {
        CurrencyRevaluationResult result = compute(companyId, asOf);
        result.setPreview(true);
        return result;
    }

    /** Posts the revaluation entry dated {@code asOf} and its reversal on the next day. */
    @Transactional
    public CurrencyRevaluationResult post(UUID companyId, LocalDate asOf) {
        periodPostingGuard.assertDatePostable(companyId, asOf);
        periodPostingGuard.assertDatePostable(companyId, asOf.plusDays(1));
        CurrencyRevaluationResult result = compute(companyId, asOf);
        if (result.getLines().isEmpty()) {
            return result;
        }
        UUID gain = lookupPort.resolveAccountIdByCode(companyId, EXCHANGE_GAIN_ACCOUNT_CODE);
        UUID loss = lookupPort.resolveAccountIdByCode(companyId, EXCHANGE_LOSS_ACCOUNT_CODE);
        UUID journal = lookupPort.resolveJournalIdByCode(companyId, EXCHANGE_JOURNAL_CODE);
        String base = result.getBaseCurrency();

        result.setJournalEntryId(book(companyId, journal, asOf, base, result, gain, loss, false));
        result.setReversalJournalEntryId(book(companyId, journal, asOf.plusDays(1), base, result, gain, loss, true));
        return result;
    }

    private UUID book(UUID companyId, UUID journal, LocalDate date, String base,
                      CurrencyRevaluationResult result, UUID gain, UUID loss, boolean reverse) {
        String label = (reverse ? "Reversal of revaluation " : "Revaluation ") + result.getAsOfDate();
        List<JournalItemCommand> items = new ArrayList<>();
        for (CurrencyRevaluationResult.Line l : result.getLines()) {
            BigDecimal adj = reverse ? l.adjustment().negate() : l.adjustment();
            BigDecimal abs = adj.abs();
            // The receivable or payable line carries the document currency with no foreign amount, so
            // the foreign balance is unchanged and only the company-currency value moves.
            items.add(new JournalItemCommand(l.accountId(), label + " " + l.currencyCode(),
                    adj.signum() > 0 ? abs : BigDecimal.ZERO, adj.signum() < 0 ? abs : BigDecimal.ZERO,
                    l.currencyCode(), BigDecimal.ZERO, l.partnerId()));
        }
        // Gains are credits to the gain account; in a reversal the same account is debited.
        BigDecimal gainCredit = result.getTotalGain();
        BigDecimal lossDebit = result.getTotalLoss();
        if (!reverse) {
            if (gainCredit.signum() > 0) {
                items.add(new JournalItemCommand(gain, "Unrealised exchange gain", BigDecimal.ZERO, gainCredit, null, null, null));
            }
            if (lossDebit.signum() > 0) {
                items.add(new JournalItemCommand(loss, "Unrealised exchange loss", lossDebit, BigDecimal.ZERO, null, null, null));
            }
        } else {
            if (gainCredit.signum() > 0) {
                items.add(new JournalItemCommand(gain, "Reversal of unrealised exchange gain", gainCredit, BigDecimal.ZERO, null, null, null));
            }
            if (lossDebit.signum() > 0) {
                items.add(new JournalItemCommand(loss, "Reversal of unrealised exchange loss", BigDecimal.ZERO, lossDebit, null, null, null));
            }
        }
        CreateJournalEntryResponse entry = journalEntryApplicationService.createJournalEntry(new CreateJournalEntryCommand(
                companyId, journal, "", JournalEntryTiming.ofBusinessDate(date), base, items));
        journalEntryApplicationService.postJournalEntry(entry.getJournalEntryId());
        return entry.getJournalEntryId();
    }

    private CurrencyRevaluationResult compute(UUID companyId, LocalDate asOf) {
        String base = currencyConversionPort.baseCurrencyCode(companyId);
        CurrencyRevaluationResult result = new CurrencyRevaluationResult();
        result.setAsOfDate(asOf);
        result.setBaseCurrency(base);
        BigDecimal totalGain = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        for (OpenForeignBalance b : queryPort.findOpenForeignBalances(companyId, asOf, base)) {
            BigDecimal rate = currencyConversionPort.exchangeRateToCompany(companyId, b.currencyCode(), asOf);
            BigDecimal revalued = b.foreignBalance().multiply(rate).setScale(4, RoundingMode.HALF_UP);
            BigDecimal carried = b.companyBalance().setScale(4, RoundingMode.HALF_UP);
            BigDecimal adjustment = revalued.subtract(carried);
            if (adjustment.abs().compareTo(MINIMUM) < 0) {
                continue;
            }
            result.getLines().add(new CurrencyRevaluationResult.Line(
                    b.accountId(), b.accountCode(), b.accountName(), b.partnerId(), b.partnerName(),
                    b.currencyCode(), b.foreignBalance(), rate, carried, revalued, adjustment));
            if (adjustment.signum() > 0) {
                totalGain = totalGain.add(adjustment);
            } else {
                totalLoss = totalLoss.add(adjustment.negate());
            }
        }
        result.setTotalGain(totalGain);
        result.setTotalLoss(totalLoss);
        return result;
    }
}
