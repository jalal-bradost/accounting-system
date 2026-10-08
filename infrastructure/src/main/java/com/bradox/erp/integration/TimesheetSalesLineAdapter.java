package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sales.service.domain.dto.SalesTimesheetLineResponse;
import com.bradox.erp.sales.service.domain.ports.input.SalesTimesheetBillingService;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineBillingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Bridges Timesheet billing (TSH-06) to Sales without either module importing the other. */
@Component
public class TimesheetSalesLineAdapter implements SalesLineLookupPort, SalesLineBillingPort {

    private final SalesTimesheetBillingService sales;

    public TimesheetSalesLineAdapter(SalesTimesheetBillingService sales) {
        this.sales = sales;
    }

    @Override
    public Optional<SaleLine> find(CompanyId companyId, UUID lineId) {
        return sales.find(companyId.getId(), lineId).map(TimesheetSalesLineAdapter::map);
    }

    @Override
    public Map<UUID, SaleLine> findAll(CompanyId companyId, Collection<UUID> lineIds) {
        Map<UUID, SaleLine> out = new LinkedHashMap<>();
        sales.findAll(companyId.getId(), lineIds).forEach((id, r) -> out.put(id, map(r)));
        return out;
    }

    @Override
    public List<SaleLine> listEligible(CompanyId companyId, UUID customerPartnerId) {
        return sales.listEligible(companyId.getId(), customerPartnerId).stream().map(TimesheetSalesLineAdapter::map).toList();
    }

    @Override
    public void setDelivered(CompanyId companyId, UUID lineId, BigDecimal quantity, String source) {
        sales.setDeliveredFromTimesheet(companyId.getId(), lineId, quantity, source);
    }

    private static SaleLine map(SalesTimesheetLineResponse r) {
        return new SaleLine(r.lineId(), r.orderId(), r.orderName(), r.customerPartnerId(), r.lineName(), r.unit(),
                r.eligible(), r.closed(), r.qtyOrdered(), r.qtyDelivered(), r.qtyInvoiced());
    }
}
