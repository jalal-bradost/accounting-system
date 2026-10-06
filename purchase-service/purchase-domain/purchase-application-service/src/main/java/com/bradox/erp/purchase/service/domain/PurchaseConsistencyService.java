package com.bradox.erp.purchase.service.domain;

import com.bradox.erp.purchase.service.domain.ports.output.PurchaseConsistencyQueryPort;
import com.bradox.erp.purchase.service.domain.ports.output.PurchaseConsistencyQueryPort.Issue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs every purchase consistency check for a company (optionally one order). Used by tests after
 * each correction scenario and exposed read-only so production can be checked the same way.
 */
@Service
@Transactional(readOnly = true)
public class PurchaseConsistencyService {

    private final PurchaseConsistencyQueryPort queryPort;

    public PurchaseConsistencyService(PurchaseConsistencyQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<Issue> check(UUID companyId, UUID purchaseOrderId) {
        List<Issue> issues = new ArrayList<>();
        issues.addAll(queryPort.linesOrderedBelowReceivedOrBilled(companyId, purchaseOrderId));
        issues.addAll(queryPort.linesBilledQtyMismatch(companyId, purchaseOrderId));
        issues.addAll(queryPort.linesOnSeveralOpenReceipts(companyId, purchaseOrderId));
        issues.addAll(queryPort.postedDocumentsWithReversedEntry(companyId, purchaseOrderId));
        issues.addAll(queryPort.cancelledOrdersWithActivity(companyId, purchaseOrderId));
        return issues;
    }
}
