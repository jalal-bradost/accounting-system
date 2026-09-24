package com.bradox.delin.purchase.dataaccess.adapter;

import com.bradox.delin.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.bradox.delin.accounting.service.domain.ports.output.PayableStatementPort;
import com.bradox.delin.purchase.service.domain.ports.input.PurchaseApplicationService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class PayableStatementPortAdapter implements PayableStatementPort {

    private final PurchaseApplicationService purchaseApplicationService;

    public PayableStatementPortAdapter(PurchaseApplicationService purchaseApplicationService) {
        this.purchaseApplicationService = purchaseApplicationService;
    }

    @Override
    public List<PartnerStatementSectionResponse> payableStatement(
            UUID companyId, UUID partnerId, LocalDate from, LocalDate to) {
        return purchaseApplicationService.payableStatement(companyId, partnerId, from, to);
    }
}
