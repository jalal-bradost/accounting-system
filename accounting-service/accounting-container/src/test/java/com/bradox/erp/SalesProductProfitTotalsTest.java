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

/** Totals must cover every product, not just the first page of ranked rows. */
class SalesProductProfitTotalsTest {

    private static final int PRODUCTS = 60;

    @Test
    void totalsAndRowsIncludeEveryProduct() {
        List<SalesProductProfitQueryPort.ConfirmedProductFact> confirmed = new ArrayList<>();
        List<SalesProductProfitQueryPort.DeliveredProductFact> delivered = new ArrayList<>();
        for (int i = 1; i <= PRODUCTS; i++) {
            UUID id = UUID.randomUUID();
            confirmed.add(new SalesProductProfitQueryPort.ConfirmedProductFact(
                    id, "P" + i, 1, BigDecimal.ONE, BigDecimal.valueOf(100L * i)));
            delivered.add(new SalesProductProfitQueryPort.DeliveredProductFact(
                    id, "P" + i, BigDecimal.ONE, BigDecimal.valueOf(100L * i), BigDecimal.valueOf(60L * i)));
        }
        SalesProductProfitService service = new SalesProductProfitService(new SalesProductProfitQueryPort() {
            @Override
            public List<ConfirmedProductFact> confirmedProductFacts(UUID companyId, LocalDate from, LocalDate to) {
                return confirmed;
            }

            @Override
            public List<DeliveredProductFact> deliveredProductFacts(UUID companyId, LocalDate from, LocalDate to) {
                return delivered;
            }

            @Override
            public Map<UUID, BigDecimal> currentValuationUnitCosts(UUID companyId, Collection<UUID> productIds) {
                return new HashMap<>();
            }
        });

        SalesProductProfitResponse response = service.getProductProfit(
                UUID.randomUUID(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        long sum = 100L * PRODUCTS * (PRODUCTS + 1) / 2;
        assertThat(response.getProducts()).hasSize(PRODUCTS);
        assertThat(response.getTotals().getRealized().getRevenue()).isEqualByComparingTo(BigDecimal.valueOf(sum));
        assertThat(response.getTotals().getRealized().getCost()).isEqualByComparingTo(BigDecimal.valueOf(sum * 6 / 10));
        assertThat(response.getTotals().getEstimated().getRevenue()).isEqualByComparingTo(BigDecimal.valueOf(sum));
    }
}
