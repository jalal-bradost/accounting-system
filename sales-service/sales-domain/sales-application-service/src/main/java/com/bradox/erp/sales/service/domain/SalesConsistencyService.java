package com.bradox.erp.sales.service.domain;

import com.bradox.erp.sales.service.domain.ports.output.SalesConsistencyQueryPort;
import com.bradox.erp.sales.service.domain.ports.output.SalesConsistencyQueryPort.Issue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs every sales consistency check for a company (optionally one order). Used by tests after
 * each correction scenario and exposed read-only so production can be checked the same way.
 */
@Service
@Transactional(readOnly = true)
public class SalesConsistencyService {

    private final SalesConsistencyQueryPort queryPort;

    public SalesConsistencyService(SalesConsistencyQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<Issue> check(UUID companyId, UUID salesOrderId) {
        List<Issue> issues = new ArrayList<>();
        issues.addAll(queryPort.linesOrderedBelowDeliveredOrInvoiced(companyId, salesOrderId));
        issues.addAll(queryPort.linesInvoicedQtyMismatch(companyId, salesOrderId));
        issues.addAll(queryPort.linesOnSeveralOpenDeliveries(companyId, salesOrderId));
        issues.addAll(queryPort.postedDocumentsWithReversedEntry(companyId, salesOrderId));
        issues.addAll(queryPort.cancelledOrdersWithActivity(companyId, salesOrderId));
        if (salesOrderId == null) {
            issues.addAll(queryPort.unbalancedLedger(companyId));
        }
        return issues;
    }
}
