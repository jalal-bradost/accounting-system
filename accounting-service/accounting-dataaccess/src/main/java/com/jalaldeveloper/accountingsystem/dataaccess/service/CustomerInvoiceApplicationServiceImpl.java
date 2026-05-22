package com.jalaldeveloper.accountingsystem.dataaccess.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.SalesOrderInvoiceSyncPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.messaging.AccountingEventPublisher;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.event.CustomerInvoicePostedEvent;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PartnerResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceLineEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceLineTaxEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerPaymentEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccCustomerInvoiceJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccCustomerPaymentJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccountJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class CustomerInvoiceApplicationServiceImpl implements CustomerInvoiceApplicationService {

    private static final String DEFAULT_AR_ACCOUNT_CODE = "430003";
    private static final String DEFAULT_REVENUE_ACCOUNT_CODE = "430005";
    /** Sale journal code in {@link com.jalaldeveloper.accountingsystem.bootstrap.DefaultCompanyChartDataSeeder}. */
    private static final String SALE_JOURNAL_CODE = "430003";

    private final AccCustomerInvoiceJpaRepository invoiceRepository;
    private final AccCustomerPaymentJpaRepository paymentRepository;
    private final PartnerApplicationService partnerApplicationService;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final JournalJpaRepository journalJpaRepository;
    private final AccountJpaRepository accountJpaRepository;
    private final ReconciliationApplicationService reconciliationApplicationService;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final ObjectProvider<SalesOrderInvoiceSyncPort> salesOrderInvoiceSyncPortProvider;
    private final AccountingEventPublisher accountingEventPublisher;

    public CustomerInvoiceApplicationServiceImpl(AccCustomerInvoiceJpaRepository invoiceRepository,
                                                 AccCustomerPaymentJpaRepository paymentRepository,
                                                 PartnerApplicationService partnerApplicationService,
                                                 JournalEntryApplicationService journalEntryApplicationService,
                                                 JournalJpaRepository journalJpaRepository,
                                                 AccountJpaRepository accountJpaRepository,
                                                 ReconciliationApplicationService reconciliationApplicationService,
                                                 ObjectProvider<CompanyContext> companyContextProvider,
                                                 ObjectProvider<SalesOrderInvoiceSyncPort> salesOrderInvoiceSyncPortProvider,
                                                 AccountingEventPublisher accountingEventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.partnerApplicationService = partnerApplicationService;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.journalJpaRepository = journalJpaRepository;
        this.accountJpaRepository = accountJpaRepository;
        this.reconciliationApplicationService = reconciliationApplicationService;
        this.companyContextProvider = companyContextProvider;
        this.salesOrderInvoiceSyncPortProvider = salesOrderInvoiceSyncPortProvider;
        this.accountingEventPublisher = accountingEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPostedInvoiceForSalesOrder(UUID salesOrderId) {
        if (salesOrderId == null) {
            return false;
        }
        return invoiceRepository.existsBySalesOrderIdAndState(salesOrderId, CustomerInvoiceState.POSTED);
    }

    private UUID companyIdOrDefault(UUID fromCommand) {
        if (fromCommand != null) {
            return fromCommand;
        }
        return companyContextProvider.getObject().requireCompany().getId();
    }

    private AccountEntity resolveLiquidityAccountForPaymentJournal(UUID companyId, UUID journalId) {
        JournalEntity j = journalJpaRepository.findById(journalId)
                .orElseThrow(() -> new AccountingDomainException("Payment journal not found"));
        if (!j.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("Journal company mismatch");
        }
        if (j.getType() != JournalType.CASH && j.getType() != JournalType.BANK) {
            throw new AccountingDomainException("Payment journal must be cash or bank");
        }
        return accountJpaRepository.findByCompanyIdAndCode(companyId, j.getCode())
                .orElseThrow(() -> new AccountingDomainException(
                        "Liquidity account for journal code " + j.getCode() + " not found"));
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse createCustomerInvoice(CreateCustomerInvoiceCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        PartnerResponse customer = partnerApplicationService.getPartner(command.getCustomerPartnerId());
        if (!customer.isCustomer()) {
            throw new AccountingDomainException("Partner is not a customer");
        }
        if (!customer.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("Customer belongs to another company");
        }
        UUID defaultRevenue = accountJpaRepository.findByCompanyIdAndCode(companyId, DEFAULT_REVENUE_ACCOUNT_CODE)
                .map(AccountEntity::getId)
                .orElseThrow(() -> new AccountingDomainException("Default revenue account not found"));

        Instant now = Instant.now();
        AccCustomerInvoiceEntity inv = new AccCustomerInvoiceEntity();
        inv.setId(UUID.randomUUID());
        inv.setCompanyId(companyId);
        inv.setCustomerPartnerId(command.getCustomerPartnerId());
        inv.setInvoiceDate(command.getInvoiceDate());
        inv.setDueDate(command.getDueDate());
        inv.setReference(command.getReference());
        inv.setCurrencyCode(command.getCurrencyCode());
        inv.setSalesOrderId(command.getSalesOrderId());
        inv.setExchangeRateToCompany(command.getExchangeRateToCompany() != null
                ? command.getExchangeRateToCompany() : BigDecimal.ONE);
        inv.setState(CustomerInvoiceState.DRAFT);
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        inv.setRowVersion(0L);

        int seq = 0;
        for (CustomerInvoiceLineCommand lc : command.getLines()) {
            if (lc.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AccountingDomainException("Line unit price must be positive");
            }
            UUID revAcc = lc.getRevenueAccountId() != null ? lc.getRevenueAccountId() : defaultRevenue;
            AccCustomerInvoiceLineEntity line = new AccCustomerInvoiceLineEntity();
            line.setId(UUID.randomUUID());
            line.setInvoice(inv);
            line.setSequence(++seq);
            line.setName(lc.getName());
            line.setQty(lc.getQty().setScale(4, RoundingMode.HALF_UP));
            line.setUnitPrice(lc.getUnitPrice().setScale(4, RoundingMode.HALF_UP));
            line.setDiscountPercent(lc.getDiscountPercent() != null ? lc.getDiscountPercent() : BigDecimal.ZERO);
            line.setSalesOrderLineId(lc.getSalesOrderLineId());
            line.setRevenueAccountId(revAcc);
            line.setCreatedAt(now);
            line.setUpdatedAt(now);
            for (CustomerInvoiceLineTaxCommand ts : lc.getTaxSnapshots()) {
                AccCustomerInvoiceLineTaxEntity te = new AccCustomerInvoiceLineTaxEntity();
                te.setId(UUID.randomUUID());
                te.setLine(line);
                te.setTaxId(ts.getTaxId());
                te.setTaxName(ts.getTaxName());
                te.setTaxBase(ts.getTaxBase().setScale(4, RoundingMode.HALF_UP));
                te.setTaxAmount(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
                te.setAccountId(ts.getAccountId());
                line.getTaxSnapshots().add(te);
            }
            inv.getLines().add(line);
        }
        return toResponse(invoiceRepository.save(inv));
    }

    @Override
    @Transactional
    public CustomerInvoiceResponse postCustomerInvoice(UUID invoiceId) {
        AccCustomerInvoiceEntity inv = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
        if (inv.getState() != CustomerInvoiceState.DRAFT) {
            throw new AccountingDomainException("Invoice is not draft");
        }
        PartnerResponse customer = partnerApplicationService.getPartner(inv.getCustomerPartnerId());
        UUID receivableAccount = customer.getReceivableAccountId() != null
                ? customer.getReceivableAccountId()
                : accountJpaRepository.findByCompanyIdAndCode(inv.getCompanyId(), DEFAULT_AR_ACCOUNT_CODE)
                .map(AccountEntity::getId)
                .orElseThrow(() -> new AccountingDomainException("Default AR account not found"));

        JournalEntity saleJournal = journalJpaRepository.findByCompanyIdAndCode(inv.getCompanyId(), SALE_JOURNAL_CODE)
                .orElseThrow(() -> new AccountingDomainException("Sale journal not found"));

        BigDecimal rate = inv.getExchangeRateToCompany() != null && inv.getExchangeRateToCompany().signum() > 0
                ? inv.getExchangeRateToCompany() : BigDecimal.ONE;

        List<JournalItemCommand> items = new ArrayList<>();
        BigDecimal arTotalComp = BigDecimal.ZERO;
        BigDecimal arDoc = BigDecimal.ZERO;

        for (AccCustomerInvoiceLineEntity line : inv.getLines()) {
            BigDecimal disc = line.getDiscountPercent() != null ? line.getDiscountPercent() : BigDecimal.ZERO;
            BigDecimal lineNetDoc = lineNet(line.getQty(), line.getUnitPrice(), disc).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineNetComp = convertAtRate(lineNetDoc, rate);

            if (line.getTaxSnapshots().isEmpty()) {
                arTotalComp = arTotalComp.add(lineNetComp);
                arDoc = arDoc.add(lineNetDoc);
                items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), BigDecimal.ZERO, lineNetComp,
                        inv.getCurrencyCode(), lineNetDoc.negate(), null));
            } else {
                arTotalComp = arTotalComp.add(lineNetComp);
                arDoc = arDoc.add(lineNetDoc);
                items.add(new JournalItemCommand(line.getRevenueAccountId(), line.getName(), BigDecimal.ZERO, lineNetComp,
                        inv.getCurrencyCode(), lineNetDoc.negate(), null));
                for (AccCustomerInvoiceLineTaxEntity ts : line.getTaxSnapshots()) {
                    BigDecimal taxDoc = ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP);
                    BigDecimal taxComp = convertAtRate(taxDoc, rate);
                    arTotalComp = arTotalComp.add(taxComp);
                    arDoc = arDoc.add(taxDoc);
                    items.add(new JournalItemCommand(ts.getAccountId(), ts.getTaxName(), BigDecimal.ZERO, taxComp,
                            inv.getCurrencyCode(), taxDoc.negate(), null));
                }
            }
        }
        items.add(0, new JournalItemCommand(receivableAccount, "Accounts receivable", arTotalComp, BigDecimal.ZERO,
                inv.getCurrencyCode(), arDoc, inv.getCustomerPartnerId()));

        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                inv.getCompanyId(),
                saleJournal.getId(),
                "",
                inv.getInvoiceDate(),
                inv.getCurrencyCode(),
                inv.getCustomerPartnerId(),
                items);
        CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());

        inv.setJournalEntryId(created.getJournalEntryId());
        inv.setState(CustomerInvoiceState.POSTED);
        inv.setUpdatedAt(Instant.now());
        AccCustomerInvoiceEntity saved = invoiceRepository.save(inv);

        if (saved.getSalesOrderId() != null) {
            SalesOrderInvoiceSyncPort salesSync = salesOrderInvoiceSyncPortProvider.getIfAvailable();
            if (salesSync != null) {
                Map<UUID, BigDecimal> qtyByLine = new LinkedHashMap<>();
                for (AccCustomerInvoiceLineEntity line : saved.getLines()) {
                    if (line.getSalesOrderLineId() != null) {
                        qtyByLine.merge(line.getSalesOrderLineId(), line.getQty(), BigDecimal::add);
                    }
                }
                if (!qtyByLine.isEmpty()) {
                    salesSync.applyPostedInvoiceQuantities(saved.getSalesOrderId(), qtyByLine);
                }
            }
        }
        accountingEventPublisher.publishCustomerInvoicePosted(new CustomerInvoicePostedEvent(
                UUID.randomUUID(),
                Instant.now(),
                saved.getCompanyId(),
                saved.getId(),
                saved.getCustomerPartnerId()));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerInvoiceResponse getCustomerInvoice(UUID invoiceId) {
        return invoiceRepository.findByIdWithLines(invoiceId)
                .map(this::toResponse)
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerInvoiceResponse> listCustomerInvoices(UUID companyId) {
        UUID cid = companyIdOrDefault(companyId);
        return invoiceRepository.findByCompanyWithLines(cid).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerPaymentResponse> listCustomerPayments(UUID companyId) {
        UUID cid = companyIdOrDefault(companyId);
        return paymentRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(cid).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerPaymentResponse registerCustomerPayment(RegisterCustomerPaymentCommand command) {
        UUID companyId = companyIdOrDefault(command.getCompanyId());
        AccCustomerInvoiceEntity inv = invoiceRepository.findById(command.getCustomerInvoiceId())
                .orElseThrow(() -> new AccountingDomainException("Customer invoice not found"));
        if (!inv.getCompanyId().equals(companyId)) {
            throw new AccountingDomainException("Invoice company mismatch");
        }
        if (inv.getState() != CustomerInvoiceState.POSTED || inv.getJournalEntryId() == null) {
            throw new AccountingDomainException("Invoice must be posted before payment");
        }
        PartnerResponse customer = partnerApplicationService.getPartner(inv.getCustomerPartnerId());
        UUID receivableAccount = customer.getReceivableAccountId() != null
                ? customer.getReceivableAccountId()
                : accountJpaRepository.findByCompanyIdAndCode(companyId, DEFAULT_AR_ACCOUNT_CODE)
                .map(AccountEntity::getId)
                .orElseThrow(() -> new AccountingDomainException("Default AR account not found"));

        AccountEntity liquidity = resolveLiquidityAccountForPaymentJournal(companyId, command.getPaymentJournalId());
        JournalEntity paymentJournal = journalJpaRepository.findById(command.getPaymentJournalId())
                .orElseThrow(() -> new AccountingDomainException("Payment journal not found"));

        BigDecimal amt = command.getAmount().setScale(4, RoundingMode.HALF_UP);
        String liqLabel = paymentJournal.getType() == JournalType.CASH ? "Cash receipt" : "Bank receipt";
        List<JournalItemCommand> items = List.of(
                new JournalItemCommand(liquidity.getId(), liqLabel, amt, BigDecimal.ZERO,
                        command.getCurrencyCode(), amt, null),
                new JournalItemCommand(receivableAccount, "Payment " + (inv.getReference() != null ? inv.getReference() : ""),
                        BigDecimal.ZERO, amt, command.getCurrencyCode(), amt.negate(), inv.getCustomerPartnerId())
        );
        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                paymentJournal.getId(),
                "",
                command.getPaymentDate(),
                command.getCurrencyCode(),
                inv.getCustomerPartnerId(),
                items);
        CreateJournalEntryResponse payEntry = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(payEntry.getJournalEntryId());

        JournalEntryResponse invEntry = journalEntryApplicationService.getJournalEntry(inv.getJournalEntryId());
        UUID invArItem = invEntry.getItems().stream()
                .filter(i -> receivableAccount.equals(i.getAccountId()) && i.getDebit().compareTo(BigDecimal.ZERO) > 0)
                .map(JournalEntryResponse.JournalItemResponse::getId)
                .findFirst()
                .orElseThrow(() -> new AccountingDomainException("Could not find AR line on customer invoice entry"));
        JournalEntryResponse paymentEntry = journalEntryApplicationService.getJournalEntry(payEntry.getJournalEntryId());
        UUID payArItem = paymentEntry.getItems().stream()
                .filter(i -> receivableAccount.equals(i.getAccountId()) && i.getCredit().compareTo(BigDecimal.ZERO) > 0)
                .map(JournalEntryResponse.JournalItemResponse::getId)
                .findFirst()
                .orElseThrow(() -> new AccountingDomainException("Could not find AR line on payment entry"));

        UUID reconciliationId = UUID.randomUUID();
        reconciliationApplicationService.reconcile(
                new ReconciliationApplicationService.ReconcileCommand(
                        List.of(invArItem, payArItem), reconciliationId));

        Instant now = Instant.now();
        AccCustomerPaymentEntity p = new AccCustomerPaymentEntity();
        p.setId(UUID.randomUUID());
        p.setCompanyId(companyId);
        p.setCustomerPartnerId(inv.getCustomerPartnerId());
        p.setCustomerInvoiceId(inv.getId());
        p.setPaymentDate(command.getPaymentDate());
        p.setPaymentJournalId(command.getPaymentJournalId());
        p.setAmount(amt);
        p.setCurrencyCode(command.getCurrencyCode());
        p.setJournalEntryId(payEntry.getJournalEntryId());
        p.setReference(command.getReference());
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        paymentRepository.save(p);

        CustomerPaymentResponse r = new CustomerPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(companyId);
        r.setCustomerInvoiceId(inv.getId());
        r.setPaymentDate(command.getPaymentDate());
        r.setPaymentJournalId(command.getPaymentJournalId());
        r.setAmount(amt);
        r.setCurrencyCode(command.getCurrencyCode());
        r.setJournalEntryId(payEntry.getJournalEntryId());
        r.setReconciliationId(reconciliationId);
        return r;
    }

    private CustomerPaymentResponse toPaymentResponse(AccCustomerPaymentEntity p) {
        CustomerPaymentResponse r = new CustomerPaymentResponse();
        r.setId(p.getId());
        r.setCompanyId(p.getCompanyId());
        r.setCustomerInvoiceId(p.getCustomerInvoiceId());
        r.setPaymentDate(p.getPaymentDate());
        r.setPaymentJournalId(p.getPaymentJournalId());
        r.setAmount(p.getAmount());
        r.setCurrencyCode(p.getCurrencyCode());
        r.setJournalEntryId(p.getJournalEntryId());
        r.setReconciliationId(null);
        return r;
    }

    private CustomerInvoiceResponse toResponse(AccCustomerInvoiceEntity inv) {
        CustomerInvoiceResponse r = new CustomerInvoiceResponse();
        r.setId(inv.getId());
        r.setCompanyId(inv.getCompanyId());
        r.setCustomerPartnerId(inv.getCustomerPartnerId());
        r.setInvoiceDate(inv.getInvoiceDate());
        r.setDueDate(inv.getDueDate());
        r.setReference(inv.getReference());
        r.setCurrencyCode(inv.getCurrencyCode());
        r.setState(inv.getState());
        r.setJournalEntryId(inv.getJournalEntryId());
        List<CustomerInvoiceLineResponse> lines = new ArrayList<>();
        for (AccCustomerInvoiceLineEntity l : inv.getLines()) {
            CustomerInvoiceLineResponse lr = new CustomerInvoiceLineResponse();
            lr.setId(l.getId());
            lr.setSequence(l.getSequence());
            lr.setName(l.getName());
            lr.setQty(l.getQty());
            lr.setUnitPrice(l.getUnitPrice());
            lr.setDiscountPercent(l.getDiscountPercent());
            lr.setRevenueAccountId(l.getRevenueAccountId());
            lr.setSalesOrderLineId(l.getSalesOrderLineId());
            for (AccCustomerInvoiceLineTaxEntity t : l.getTaxSnapshots()) {
                CustomerInvoiceLineTaxResponse tr = new CustomerInvoiceLineTaxResponse();
                tr.setTaxId(t.getTaxId());
                tr.setTaxName(t.getTaxName());
                tr.setTaxBase(t.getTaxBase());
                tr.setTaxAmount(t.getTaxAmount());
                tr.setAccountId(t.getAccountId());
                lr.getTaxSnapshots().add(tr);
            }
            lines.add(lr);
        }
        r.setLines(lines);
        r.setSalesOrderId(inv.getSalesOrderId());
        r.setExchangeRateToCompany(inv.getExchangeRateToCompany());
        return r;
    }

    private static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal disc = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(
                disc.max(BigDecimal.ZERO).min(new BigDecimal("100"))
                        .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        return qty.multiply(unitPrice).multiply(factor);
    }

    private static BigDecimal convertAtRate(BigDecimal documentAmount, BigDecimal rateToCompany) {
        BigDecimal r = rateToCompany != null && rateToCompany.signum() > 0 ? rateToCompany : BigDecimal.ONE;
        return documentAmount.multiply(r).setScale(4, RoundingMode.HALF_UP);
    }
}
