package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementLineResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.PartnerStatementApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CustomerInvoiceRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CustomerPaymentRepository;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PartnerResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoice;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoiceLine;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoiceLineTax;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerPayment;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.PayableStatementPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
class PartnerStatementApplicationServiceImpl implements PartnerStatementApplicationService {

    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final PartnerApplicationService partnerApplicationService;
    private final PayableStatementPort payableStatementPort;

    PartnerStatementApplicationServiceImpl(CustomerInvoiceRepository customerInvoiceRepository,
                                           CustomerPaymentRepository customerPaymentRepository,
                                           PartnerApplicationService partnerApplicationService,
                                           PayableStatementPort payableStatementPort) {
        this.customerInvoiceRepository = customerInvoiceRepository;
        this.customerPaymentRepository = customerPaymentRepository;
        this.partnerApplicationService = partnerApplicationService;
        this.payableStatementPort = payableStatementPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerStatementResponse partnerStatement(UUID companyId, UUID partnerId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Statement end date must be on or after start date");
        }
        PartnerResponse partner = partnerApplicationService.getPartner(partnerId);
        if (!partner.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Partner belongs to another company");
        }
        if (!partner.isCustomer() && !partner.isVendor()) {
            throw new IllegalArgumentException("Partner is not a customer or vendor");
        }

        PartnerStatementResponse response = new PartnerStatementResponse();
        response.setPartnerId(partnerId);
        response.setPartnerDisplayName(partner.getDisplayName());
        response.setFromDate(from);
        response.setToDate(to);

        if (partner.isCustomer()) {
            response.setReceivableSections(buildReceivableSections(companyId, partnerId, from, to));
        }
        if (partner.isVendor()) {
            response.setPayableSections(payableStatementPort.payableStatement(companyId, partnerId, from, to));
        }
        return response;
    }

    private List<PartnerStatementSectionResponse> buildReceivableSections(
            UUID companyId, UUID partnerId, LocalDate from, LocalDate to) {
        List<CustomerInvoice> invoices = customerInvoiceRepository
                .findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(companyId, partnerId);
        List<CustomerPayment> payments = customerPaymentRepository
                .findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(companyId, partnerId);

        Set<String> currencies = new LinkedHashSet<>();
        for (CustomerInvoice inv : invoices) {
            if (inv.getState() == CustomerInvoiceState.POSTED && inv.getCurrencyCode() != null) {
                currencies.add(inv.getCurrencyCode().trim().toUpperCase());
            }
        }
        for (CustomerPayment p : payments) {
            if (p.getCurrencyCode() != null) {
                currencies.add(p.getCurrencyCode().trim().toUpperCase());
            }
        }

        List<PartnerStatementSectionResponse> sections = new ArrayList<>();
        for (String currency : currencies) {
            sections.add(buildReceivableSectionForCurrency(currency, from, to, invoices, payments));
        }
        return sections;
    }

    private PartnerStatementSectionResponse buildReceivableSectionForCurrency(
            String currency,
            LocalDate from,
            LocalDate to,
            List<CustomerInvoice> invoices,
            List<CustomerPayment> payments) {
        BigDecimal opening = BigDecimal.ZERO;
        for (CustomerInvoice inv : invoices) {
            if (inv.getState() != CustomerInvoiceState.POSTED) {
                continue;
            }
            if (!currency.equalsIgnoreCase(inv.getCurrencyCode())) {
                continue;
            }
            if (inv.getInvoiceDate().isBefore(from)) {
                BigDecimal total = customerInvoiceTotalDocumentCurrency(inv);
                if (inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE) {
                    opening = opening.subtract(total);
                } else {
                    opening = opening.add(total);
                }
            }
        }
        for (CustomerPayment p : payments) {
            if (!currency.equalsIgnoreCase(p.getCurrencyCode())) {
                continue;
            }
            if (p.getPaymentDate().isBefore(from)) {
                opening = opening.subtract(p.getAmount());
            }
        }
        opening = opening.setScale(4, RoundingMode.HALF_UP);

        record ArEvt(LocalDate d, Instant created, String idKey, CustomerInvoice inv, CustomerPayment pay) {}
        List<ArEvt> period = new ArrayList<>();
        for (CustomerInvoice inv : invoices) {
            if (inv.getState() == CustomerInvoiceState.POSTED
                    && currency.equalsIgnoreCase(inv.getCurrencyCode())
                    && !inv.getInvoiceDate().isBefore(from)
                    && !inv.getInvoiceDate().isAfter(to)) {
                period.add(new ArEvt(inv.getInvoiceDate(), inv.getCreatedAt(), "I:" + inv.getId(), inv, null));
            }
        }
        for (CustomerPayment p : payments) {
            if (currency.equalsIgnoreCase(p.getCurrencyCode())
                    && !p.getPaymentDate().isBefore(from)
                    && !p.getPaymentDate().isAfter(to)) {
                period.add(new ArEvt(p.getPaymentDate(), p.getCreatedAt(), "M:" + p.getId(), null, p));
            }
        }
        period.sort(Comparator.comparing(ArEvt::d).thenComparing(ArEvt::created).thenComparing(ArEvt::idKey));

        BigDecimal running = opening;
        List<PartnerStatementLineResponse> lines = new ArrayList<>();
        BigDecimal zero = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        for (ArEvt e : period) {
            PartnerStatementLineResponse row = new PartnerStatementLineResponse();
            row.setEntryDate(e.d());
            if (e.inv() != null) {
                CustomerInvoice inv = e.inv();
                BigDecimal amount = customerInvoiceTotalDocumentCurrency(inv).setScale(4, RoundingMode.HALF_UP);
                boolean creditNote = inv.getMoveType() == CustomerInvoiceMoveType.CREDIT_NOTE;
                row.setLineType(creditNote ? "CUSTOMER_CREDIT_NOTE" : "CUSTOMER_INVOICE");
                row.setReference(inv.getReference() != null && !inv.getReference().isBlank() ? inv.getReference() : inv.getId().toString());
                row.setCurrencyCode(inv.getCurrencyCode());
                row.setCustomerInvoiceId(inv.getId());
                if (creditNote) {
                    row.setDebit(zero);
                    row.setCredit(amount);
                    running = running.subtract(amount);
                } else {
                    row.setDebit(amount);
                    row.setCredit(zero);
                    running = running.add(amount);
                }
            } else {
                CustomerPayment payment = e.pay();
                BigDecimal amount = payment.getAmount().setScale(4, RoundingMode.HALF_UP);
                row.setLineType("CUSTOMER_PAYMENT");
                row.setReference(payment.getReference() != null && !payment.getReference().isBlank() ? payment.getReference() : "Payment");
                row.setCurrencyCode(payment.getCurrencyCode());
                row.setCustomerInvoiceId(payment.getCustomerInvoiceId());
                row.setCustomerPaymentId(payment.getId());
                row.setDebit(zero);
                row.setCredit(amount);
                running = running.subtract(amount);
            }
            row.setBalance(running.setScale(4, RoundingMode.HALF_UP));
            lines.add(row);
        }

        PartnerStatementSectionResponse section = new PartnerStatementSectionResponse();
        section.setCurrencyCode(currency);
        section.setOpeningBalance(opening);
        section.setClosingBalance(running.setScale(4, RoundingMode.HALF_UP));
        section.setLines(lines);
        return section;
    }

    private BigDecimal customerInvoiceTotalDocumentCurrency(CustomerInvoice inv) {
        BigDecimal total = BigDecimal.ZERO;
        for (CustomerInvoiceLine line : inv.getLines()) {
            BigDecimal lineNet = customerLineNet(line.getQty(), line.getUnitPrice(), line.getDiscountPercent())
                    .setScale(4, RoundingMode.HALF_UP);
            total = total.add(lineNet);
            for (CustomerInvoiceLineTax ts : line.getTaxSnapshots()) {
                total = total.add(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }
        return total;
    }

    private static BigDecimal customerLineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal disc = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(
                disc.max(BigDecimal.ZERO).min(new BigDecimal("100"))
                        .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        return qty.multiply(unitPrice).multiply(factor);
    }
}
