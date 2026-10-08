package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Reads Sales order lines that are billed from timesheets (TSH-06). Implemented in {@code infrastructure}. */
public interface SalesLineLookupPort {

    /**
     * {@code unit} is HOURS or DAYS. {@code eligible}: a service line with the timesheet policy.
     * {@code closed}: the order is canceled or locked, so no quantity may change.
     */
    record SaleLine(UUID lineId, UUID orderId, String orderName, UUID customerPartnerId, String lineName, String unit,
                    boolean eligible, boolean closed, BigDecimal qtyOrdered, BigDecimal qtyDelivered, BigDecimal qtyInvoiced) {

        public String label() {
            return orderName + " · " + lineName;
        }
    }

    Optional<SaleLine> find(CompanyId companyId, UUID lineId);

    Map<UUID, SaleLine> findAll(CompanyId companyId, Collection<UUID> lineIds);

    List<SaleLine> listEligible(CompanyId companyId, UUID customerPartnerId);
}
