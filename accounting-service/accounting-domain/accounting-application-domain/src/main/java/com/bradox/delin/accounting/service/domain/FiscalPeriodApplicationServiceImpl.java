package com.bradox.delin.accounting.service.domain;

import com.bradox.delin.accounting.service.domain.ports.input.service.FiscalPeriodApplicationService;
import com.bradox.delin.accounting.service.domain.ports.output.repository.CloseChecklistQueryPort;
import com.bradox.delin.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.bradox.delin.accounting.service.domain.ports.output.repository.FiscalPeriodRepository.FiscalPeriodInfo;
import com.bradox.delin.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.bradox.delin.domain.core.exception.AccountingDomainException;
import com.bradox.delin.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
class FiscalPeriodApplicationServiceImpl implements FiscalPeriodApplicationService {

    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final CompanyLockDatePort companyLockDatePort;
    private final CloseChecklistQueryPort closeChecklistQueryPort;

    FiscalPeriodApplicationServiceImpl(FiscalPeriodRepository fiscalPeriodRepository,
                                       CompanyLockDatePort companyLockDatePort,
                                       CloseChecklistQueryPort closeChecklistQueryPort) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.companyLockDatePort = companyLockDatePort;
        this.closeChecklistQueryPort = closeChecklistQueryPort;
    }

    @Override
    @Transactional
    public FiscalPeriodInfo createPeriod(UUID companyId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new AccountingDomainException("Invalid fiscal period dates.");
        }
        CompanyId cid = new CompanyId(companyId);
        for (FiscalPeriodInfo existing : fiscalPeriodRepository.findByCompanyIdOrderByStartDateDesc(cid)) {
            boolean overlaps = !endDate.isBefore(existing.startDate()) && !startDate.isAfter(existing.endDate());
            if (overlaps) {
                throw new AccountingDomainException(
                        "Fiscal period overlaps existing period " + existing.startDate() + "–" + existing.endDate() + ".");
            }
        }
        return fiscalPeriodRepository.create(cid, startDate, endDate);
    }

    @Override
    public List<FiscalPeriodInfo> listPeriods(UUID companyId) {
        return fiscalPeriodRepository.findByCompanyIdOrderByStartDateDesc(new CompanyId(companyId));
    }

    @Override
    @Transactional
    public List<FiscalPeriodInfo> ensureMonthlyPeriods(UUID companyId, int year) {
        CompanyId cid = new CompanyId(companyId);
        int startMonth = companyLockDatePort.getFiscalYearStartMonth(cid);
        List<FiscalPeriodInfo> createdOrExisting = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int month = ((startMonth - 1 + i) % 12) + 1;
            int y = year + ((startMonth - 1 + i) / 12);
            YearMonth ym = YearMonth.of(y, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            FiscalPeriodInfo existing = fiscalPeriodRepository.findPeriodContaining(cid, start).orElse(null);
            if (existing != null
                    && existing.startDate().equals(start)
                    && existing.endDate().equals(end)) {
                createdOrExisting.add(existing);
            } else if (existing == null) {
                createdOrExisting.add(fiscalPeriodRepository.create(cid, start, end));
            } else {
                createdOrExisting.add(existing);
            }
        }
        return createdOrExisting;
    }

    @Override
    public CloseChecklist getCloseChecklist(UUID companyId, UUID periodId) {
        FiscalPeriodInfo period = requirePeriod(companyId, periodId);
        int draftJes = closeChecklistQueryPort.countDraftJournalEntries(
                companyId, period.startDate(), period.endDate());
        int draftInvoices = closeChecklistQueryPort.countDraftCustomerInvoices(
                companyId, period.startDate(), period.endDate());
        int draftBills = closeChecklistQueryPort.countDraftVendorBills(
                companyId, period.startDate(), period.endDate());
        return new CloseChecklist(period.id(), period.startDate(), period.endDate(), period.open(),
                draftJes, draftInvoices, draftBills,
                draftJes == 0 && draftInvoices == 0 && draftBills == 0);
    }

    @Override
    @Transactional
    public FiscalPeriodInfo closeMonth(UUID companyId, UUID periodId, UUID closedBy) {
        FiscalPeriodInfo period = requirePeriod(companyId, periodId);
        if (!period.open()) {
            return period;
        }
        CloseChecklist checklist = getCloseChecklist(companyId, periodId);
        if (!checklist.canClose()) {
            throw new AccountingDomainException(
                    "Cannot close period: resolve "
                            + checklist.draftJournalEntries() + " draft journal entries, "
                            + checklist.draftCustomerInvoices() + " draft invoices, "
                            + checklist.draftVendorBills() + " draft vendor bills first.");
        }
        FiscalPeriodInfo closed = fiscalPeriodRepository.close(
                new CompanyId(companyId), periodId, closedBy, Instant.now());
        advancePeriodLock(companyId, closed.startDate(), closed.endDate());
        return closed;
    }

    @Override
    @Transactional
    public List<FiscalPeriodInfo> closeYear(UUID companyId, int year, UUID closedBy) {
        ensureMonthlyPeriods(companyId, year);
        CompanyId cid = new CompanyId(companyId);
        int startMonth = companyLockDatePort.getFiscalYearStartMonth(cid);
        LocalDate fyStart = LocalDate.of(year, startMonth, 1);
        LocalDate fyEnd = fyStart.plusYears(1).minusDays(1);

        List<FiscalPeriodInfo> closed = new ArrayList<>();
        for (FiscalPeriodInfo period : fiscalPeriodRepository.findByCompanyIdOrderByStartDateDesc(cid)) {
            boolean inFy = !period.endDate().isBefore(fyStart) && !period.startDate().isAfter(fyEnd);
            if (!inFy) {
                continue;
            }
            if (period.open()) {
                CloseChecklist checklist = getCloseChecklist(companyId, period.id());
                if (!checklist.canClose()) {
                    throw new AccountingDomainException(
                            "Cannot close year: period " + period.startDate() + "–" + period.endDate()
                                    + " still has open drafts.");
                }
                closed.add(fiscalPeriodRepository.close(cid, period.id(), closedBy, Instant.now()));
            } else {
                closed.add(period);
            }
        }
        advancePeriodLock(companyId, fyStart, fyEnd);
        return closed;
    }

    /**
     * Extends the company period lock through the closed range when doing so would not
     * jump the lock into the future beyond today. Closed fiscal periods still block
     * posting via {@link PeriodPostingGuard} even when the lock date is not advanced.
     */
    private void advancePeriodLock(UUID companyId, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate today = LocalDate.now();
        if (periodStart.isAfter(today)) {
            // Closing a future-only period must not lock all historical/current dates.
            return;
        }
        LocalDate lastLockedDayInclusive = periodEnd.isAfter(today) ? today : periodEnd;
        CompanyId cid = new CompanyId(companyId);
        LocalDate current = companyLockDatePort.getPeriodLockDate(cid).orElse(null);
        if (current == null || current.isBefore(lastLockedDayInclusive)) {
            companyLockDatePort.setPeriodLockDate(cid, lastLockedDayInclusive);
        }
    }

    private FiscalPeriodInfo requirePeriod(UUID companyId, UUID periodId) {
        return fiscalPeriodRepository.findById(new CompanyId(companyId), periodId)
                .orElseThrow(() -> new AccountingDomainException("Fiscal period not found: " + periodId));
    }
}
