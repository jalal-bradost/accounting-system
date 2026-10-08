package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.util.UUID;

/** Sets the delivered quantity of a timesheet-billed sales line (TSH-06). The quantity is recomputed, never incremented. */
public interface SalesLineBillingPort {

    void setDelivered(CompanyId companyId, UUID lineId, BigDecimal quantity, String source);
}
