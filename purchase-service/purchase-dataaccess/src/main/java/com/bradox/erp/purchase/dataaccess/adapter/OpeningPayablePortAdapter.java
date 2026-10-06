package com.bradox.erp.purchase.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.OpeningPayablePort;
import com.bradox.erp.purchase.service.domain.dto.VendorBillResponse;
import com.bradox.erp.purchase.service.domain.dto.VendorPaymentResponse;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class OpeningPayablePortAdapter implements OpeningPayablePort {

    private final PurchaseApplicationService purchaseApplicationService;

    public OpeningPayablePortAdapter(PurchaseApplicationService purchaseApplicationService) {
        this.purchaseApplicationService = purchaseApplicationService;
    }

    @Override
    public UUID createOpeningVendorBill(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                        LocalDate date, LocalDate dueDate, String reference,
                                        UUID openingJournalId, UUID openingEquityAccountId) {
        VendorBillResponse bill = purchaseApplicationService.createOpeningVendorBill(
                companyId, partnerId, amount, currency, date, dueDate, reference,
                openingJournalId, openingEquityAccountId);
        return bill.getId();
    }

    @Override
    public UUID postOpeningPayment(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                   LocalDate date, String reference,
                                   UUID openingJournalId, UUID openingEquityAccountId) {
        VendorPaymentResponse payment = purchaseApplicationService.postOpeningVendorPayment(
                companyId, partnerId, amount, currency, date, reference,
                openingJournalId, openingEquityAccountId);
        return payment.getId();
    }

    @Override
    public List<OpeningVendorBillRef> findOpeningBills(UUID companyId) {
        return purchaseApplicationService.listOpeningVendorBills(companyId).stream()
                .map(b -> new OpeningVendorBillRef(b.getId(), b.getReference(), b.getJournalEntryId()))
                .toList();
    }

    @Override
    public List<OpeningVendorPaymentRef> findOpeningPayments(UUID companyId) {
        return purchaseApplicationService.listOpeningVendorPayments(companyId).stream()
                .map(p -> new OpeningVendorPaymentRef(p.getId(), p.getReference(), p.getJournalEntryId()))
                .toList();
    }

    @Override
    public boolean hasActiveAllocations(Collection<UUID> billIds, Collection<UUID> paymentIds) {
        return purchaseApplicationService.hasActiveVendorAllocations(billIds, paymentIds);
    }

    @Override
    public boolean hasCreditNotes(Collection<UUID> billIds) {
        return purchaseApplicationService.hasVendorCreditNotes(billIds);
    }

    @Override
    public void cancelOpeningBill(UUID billId) {
        purchaseApplicationService.cancelOpeningVendorBill(billId);
    }

    @Override
    public void reverseOpeningPayment(UUID paymentId, String reason) {
        purchaseApplicationService.reverseVendorPayment(paymentId, reason);
    }
}
