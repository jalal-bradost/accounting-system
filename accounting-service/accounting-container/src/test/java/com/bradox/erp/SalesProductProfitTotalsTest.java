package com.bradox.erp;

import com.bradox.erp.sales.service.domain.SalesProductProfitService;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SalesProductProfitTotalsTest {

    private static final int PRODUCTS = 60;

    /** Estimated totals must cover every product, not just the first page of ranked rows. */
    @Test
    void estimatedTotalsAndRowsIncludeEveryProduct() {
        List<SalesProductProfitQueryPort.ConfirmedProductFact> confirmed = new ArrayList<>();
        for (int i = 1; i <= PRODUCTS; i++) {
            confirmed.add(new SalesProductProfitQueryPort.ConfirmedProductFact(
                    UUID.randomUUID(), "P" + i, 1, BigDecimal.ONE, BigDecimal.valueOf(100L * i)));
        }
        SalesProductProfitService service = new SalesProductProfitService(new StubPort() {
            @Override
            public List<ConfirmedProductFact> confirmedProductFacts(UUID companyId, LocalDate from, LocalDate to) {
                return confirmed;
            }
        });

        SalesProductProfitResponse response = service.getProductProfit(
                UUID.randomUUID(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        long sum = 100L * PRODUCTS * (PRODUCTS + 1) / 2;
        assertThat(response.getProducts()).hasSize(PRODUCTS);
        assertThat(response.getTotals().getEstimated().getRevenue()).isEqualByComparingTo(BigDecimal.valueOf(sum));
    }

    /** Realized totals are the Profit & Loss figures; rows come from invoices and posted COGS. */
    @Test
    void realizedTotalsComeFromTheLedgerAndRowsFromInvoicesAndCogs() {
        UUID juice = UUID.randomUUID();
        SalesProductProfitService service = new SalesProductProfitService(new StubPort() {
            @Override
            public List<InvoicedProductFact> invoicedProductFacts(UUID companyId, LocalDate from, LocalDate to) {
                return List.of(
                        new InvoicedProductFact(juice, "Juice", new BigDecimal("10"), new BigDecimal("1000")),
                        // Invoice lines not from a sales order still count as sales.
                        new InvoicedProductFact(null, null, BigDecimal.ONE, new BigDecimal("50")));
            }

            @Override
            public List<CostFact> costOfSalesByProduct(UUID companyId, LocalDate from, LocalDate to) {
                return List.of(new CostFact(juice, new BigDecimal("600")));
            }

            @Override
            public LedgerProfitTotals ledgerProfitTotals(UUID companyId, LocalDate from, LocalDate to) {
                return new LedgerProfitTotals(new BigDecimal("1050"), new BigDecimal("600"));
            }
        });

        SalesProductProfitResponse response = service.getProductProfit(
                UUID.randomUUID(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        SalesProductProfitResponse.ProfitSlice totals = response.getTotals().getRealized();
        assertThat(totals.getRevenue()).isEqualByComparingTo("1050");
        assertThat(totals.getCost()).isEqualByComparingTo("600");
        assertThat(totals.getProfit()).isEqualByComparingTo("450");
        assertThat(totals.getMarginPercent()).isEqualByComparingTo("42.9");

        SalesProductProfitResponse.ProductProfitRow juiceRow = response.getProducts().stream()
                .filter(r -> juice.equals(r.getProductId())).findFirst().orElseThrow();
        assertThat(juiceRow.getQtyInvoiced()).isEqualByComparingTo("10");
        assertThat(juiceRow.getRealized().getProfit()).isEqualByComparingTo("400");
        assertThat(response.getProducts()).anyMatch(r -> r.getProductId() == null
                && r.getRealized().getRevenue().compareTo(new BigDecimal("50")) == 0);
    }

    /**
     * Realized means delivered and invoiced. Sales invoiced but not delivered come out of it, are
     * reported separately with the cost they will carry, and the Profit &amp; Loss is the sum of both.
     */
    @Test
    void realizedExcludesInvoicedButUndeliveredSalesAndReconcilesToTheProfitAndLoss() {
        UUID juice = UUID.randomUUID();
        UUID order = UUID.randomUUID();
        UUID ali = UUID.randomUUID();
        LocalDate invoiced = LocalDate.of(2026, 10, 3);
        SalesProductProfitService service = new SalesProductProfitService(new StubPort() {
            @Override
            public List<InvoicedProductFact> invoicedProductFacts(UUID companyId, LocalDate from, LocalDate to) {
                return List.of(new InvoicedProductFact(juice, "Juice", new BigDecimal("10"), new BigDecimal("1000")));
            }

            @Override
            public List<CostFact> costOfSalesByProduct(UUID companyId, LocalDate from, LocalDate to) {
                return List.of(new CostFact(juice, new BigDecimal("360")));
            }

            @Override
            public LedgerProfitTotals ledgerProfitTotals(UUID companyId, LocalDate from, LocalDate to) {
                return new LedgerProfitTotals(new BigDecimal("1000"), new BigDecimal("360"));
            }

            @Override
            public List<PendingDeliveryLine> pendingDeliveryLines(UUID companyId, LocalDate from, LocalDate to) {
                return List.of(new PendingDeliveryLine(order, "SO/1", "Acme", juice,
                        new BigDecimal("4"), new BigDecimal("400"), invoiced));
            }

            @Override
            public Map<UUID, BigDecimal> currentValuationUnitCosts(UUID companyId, Collection<UUID> productIds) {
                return Map.of(juice, new BigDecimal("60"));
            }

            @Override
            public PipelineFact quotationPipeline(UUID companyId, LocalDate from, LocalDate to) {
                return new PipelineFact(3, new BigDecimal("900"));
            }

            @Override
            public PipelineFact confirmedNotInvoiced(UUID companyId, LocalDate from, LocalDate to) {
                return new PipelineFact(1, new BigDecimal("50"));
            }
        });

        SalesProductProfitResponse.ProfitTotals totals = service.getProductProfit(
                UUID.randomUUID(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).getTotals();

        // Profit & Loss as booked: all invoiced sales, only the cost of what shipped.
        assertThat(totals.getProfitAndLoss().getRevenue()).isEqualByComparingTo("1000");
        assertThat(totals.getProfitAndLoss().getProfit()).isEqualByComparingTo("640");
        // Realized: delivered and invoiced only (600 of sales against its 360 cost).
        assertThat(totals.getRealized().getRevenue()).isEqualByComparingTo("600");
        assertThat(totals.getRealized().getProfit()).isEqualByComparingTo("240");
        assertThat(totals.getRealized().getMarginPercent()).isEqualByComparingTo("40.0");
        // Awaiting delivery: 4 units, 400 of sales, cost still to come 4 x 60.
        SalesProductProfitResponse.PendingDelivery pending = totals.getPendingDelivery();
        assertThat(pending.getUnits()).isEqualByComparingTo("4");
        assertThat(pending.getRevenue()).isEqualByComparingTo("400");
        assertThat(pending.getCost()).isEqualByComparingTo("240");
        assertThat(pending.getOrderCount()).isEqualTo(1);
        assertThat(pending.getOldestInvoiceDate()).isEqualTo(invoiced);
        assertThat(pending.getOrders()).extracting(SalesProductProfitResponse.WaitingOrder::getOrderName).containsExactly("SO/1");
        // The books are realized plus awaiting delivery.
        assertThat(totals.getRealized().getRevenue().add(pending.getRevenue()))
                .isEqualByComparingTo(totals.getProfitAndLoss().getRevenue());
        assertThat(totals.getRealized().getProfit().add(pending.getRevenue()))
                .isEqualByComparingTo(totals.getProfitAndLoss().getProfit());
        // Pipeline stages ahead of invoicing.
        assertThat(totals.getPipeline().getQuotations().getOrders()).isEqualTo(3);
        assertThat(totals.getPipeline().getConfirmedNotInvoiced().getAmount()).isEqualByComparingTo("50");
    }

    /** Port that reports nothing; tests override what they need. */
    private static class StubPort implements SalesProductProfitQueryPort {
        @Override
        public List<ConfirmedProductFact> confirmedProductFacts(UUID companyId, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public List<InvoicedProductFact> invoicedProductFacts(UUID companyId, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public List<CostFact> costOfSalesByProduct(UUID companyId, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public LedgerProfitTotals ledgerProfitTotals(UUID companyId, LocalDate from, LocalDate to) {
            return new LedgerProfitTotals(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Override
        public List<PendingDeliveryLine> pendingDeliveryLines(UUID companyId, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public PipelineFact quotationPipeline(UUID companyId, LocalDate from, LocalDate to) {
            return new PipelineFact(0, BigDecimal.ZERO);
        }

        @Override
        public PipelineFact confirmedNotInvoiced(UUID companyId, LocalDate from, LocalDate to) {
            return new PipelineFact(0, BigDecimal.ZERO);
        }

        @Override
        public Map<UUID, BigDecimal> currentValuationUnitCosts(UUID companyId, Collection<UUID> productIds) {
            return new HashMap<>();
        }
    }
}
