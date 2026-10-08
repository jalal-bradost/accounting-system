package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.dto.NeedsAttentionItem;
import com.bradox.erp.timesheet.service.domain.dto.RateResponse;
import com.bradox.erp.timesheet.service.domain.dto.SaleLineResponse;
import com.bradox.erp.timesheet.service.domain.dto.SetRatesCommand;

import java.util.List;
import java.util.UUID;

/** Billing hours to customers through sales order lines (TSH-06). Needs {@code tsh.billing.manage} unless noted. */
public interface BillingApplicationService {

    /** Sales lines that can be billed from timesheets, for the pickers. Any timesheet user may read them. */
    List<SaleLineResponse> eligibleLines(CompanyId companyId, UUID customerPartnerId);

    List<RateResponse> rates(CompanyId companyId, UUID projectId);

    List<RateResponse> setRates(CompanyId companyId, UUID projectId, SetRatesCommand command);

    List<NeedsAttentionItem> needsAttention(CompanyId companyId);

    /** One-click "choose order line" on a billable entry (TSH-06 #6). */
    EntryResponse assignLine(CompanyId companyId, UUID entryId, UUID saleLineId);
}
