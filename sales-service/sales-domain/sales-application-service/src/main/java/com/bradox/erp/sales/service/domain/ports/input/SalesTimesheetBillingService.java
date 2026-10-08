package com.bradox.erp.sales.service.domain.ports.input;

import com.bradox.erp.sales.service.domain.dto.SalesTimesheetLineResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What Timesheet may do to Sales (TSH-06): look up lines that are billed from timesheets and set their delivered
 * quantity. Called through an adapter in {@code infrastructure}.
 */
public interface SalesTimesheetBillingService {

    Optional<SalesTimesheetLineResponse> find(UUID companyId, UUID lineId);

    Map<UUID, SalesTimesheetLineResponse> findAll(UUID companyId, Collection<UUID> lineIds);

    /** Eligible lines of open, confirmed orders, optionally for one customer. */
    List<SalesTimesheetLineResponse> listEligible(UUID companyId, UUID customerPartnerId);

    /**
     * Sets (never increments) the delivered quantity. Refuses lines that are not timesheet-billed service lines,
     * and refuses a quantity below what is already invoiced (BR-TSH-14).
     */
    void setDeliveredFromTimesheet(UUID companyId, UUID lineId, BigDecimal qty, String source);
}
