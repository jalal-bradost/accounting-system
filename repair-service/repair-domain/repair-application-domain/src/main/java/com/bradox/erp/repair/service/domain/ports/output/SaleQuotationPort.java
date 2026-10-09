package com.bradox.erp.repair.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Creates a draft sale quotation in Sales (implemented in infrastructure). */
public interface SaleQuotationPort {

    record Line(UUID productId, BigDecimal qty) {
    }

    record Quotation(UUID id, String name) {
    }

    Quotation create(CompanyId companyId, UUID customerPartnerId, String note, List<Line> lines);
}
