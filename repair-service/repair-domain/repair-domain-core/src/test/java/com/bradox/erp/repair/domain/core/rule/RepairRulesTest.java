package com.bradox.erp.repair.domain.core.rule;

import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.RepairLine;
import com.bradox.erp.repair.domain.core.valueobject.LineStatus;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepairRulesTest {

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @Test
    void flatRatePriceIsMinutesOverSixtyTimesRate() {
        assertEquals(d("37.50"), LaborPricing.flatRatePrice(45, d("50")));
        assertEquals(d("16.67"), LaborPricing.flatRatePrice(20, d("50")));
        assertNull(LaborPricing.flatRatePrice(45, null));
    }

    @Test
    void rateFallsBackToCompanyDefault() {
        assertEquals(d("40"), LaborPricing.resolveRate(d("40"), d("30")));
        assertEquals(d("30"), LaborPricing.resolveRate(null, d("30")));
        assertEquals(d("30"), LaborPricing.resolveRate(BigDecimal.ZERO, d("30")));
        assertNull(LaborPricing.resolveRate(null, null));
    }

    @Test
    void lineTotalAppliesDiscount() {
        assertEquals(d("90.00"), LaborPricing.lineTotal(d("2"), d("50"), d("10")));
        assertEquals(d("100.00"), LaborPricing.lineTotal(d("1"), d("100"), null));
    }

    @Test
    void customerPartCannotCarryAPrice() {
        assertThrows(RepairDomainException.class, () -> line(LineType.CUSTOMER_PART, d("5")));
        line(LineType.CUSTOMER_PART, BigDecimal.ZERO);
    }

    @Test
    void csvReportsBadRowsWithLineNumbers() {
        String csv = "code,description_en,description_ar,description_ku,category,standard_minutes,make,model,year_from,year_to\n"
                + "OIL,Oil change,,,Mechanical,30,,,,\n"
                + "BAD,No minutes,,,Mechanical,abc,,,,\n"
                + "\"BRK,1\",\"Brake pads, front\",,,Mechanical,90,Toyota,Camry,2015,2020\n"
                + "OIL,Duplicate,,,Mechanical,30,,,,\n"
                + "MODEL,Model without make,,,Mechanical,30,,Camry,,\n";
        LaborGuideCsv.Result r = LaborGuideCsv.parse(csv);
        assertEquals(2, r.rows().size());
        assertEquals("BRK,1", r.rows().get(1).code());
        assertEquals(List.of(3, 5, 6), r.errors().stream().map(LaborGuideCsv.RowError::line).toList());
    }

    @Test
    void guideRankingPutsExactMatchFirstAndDropsOtherMakes() {
        GuideEntry generic = entry("A-GENERIC", null, null, null, null);
        GuideEntry make = entry("B-MAKE", "Toyota", null, null, null);
        GuideEntry exact = entry("C-EXACT", "Toyota", "Camry", 2015, 2020);
        GuideEntry otherModel = entry("D-OTHER", "Toyota", "Corolla", null, null);
        GuideEntry otherMake = entry("E-HONDA", "Honda", null, null, null);
        GuideEntry wrongYear = entry("F-OLD", "Toyota", "Camry", 2000, 2010);
        List<GuideEntry> ranked = GuideRanking.rank(List.of(generic, make, exact, otherModel, otherMake, wrongYear), "toyota", "CAMRY", 2018);
        assertEquals(List.of("C-EXACT", "B-MAKE", "A-GENERIC"), ranked.stream().map(GuideEntry::code).toList());
        assertTrue(GuideRanking.rank(List.of(generic, otherMake), null, null, null).size() == 2);
    }

    private static GuideEntry entry(String code, String make, String model, Integer from, Integer to) {
        return new GuideEntry(UUID.randomUUID(), UUID.randomUUID(), code, code, null, null, null, 30, make, model, from, to, true);
    }

    private static RepairLine line(LineType type, BigDecimal price) {
        return new RepairLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), type, null, 1, null, "x", BigDecimal.ONE, price,
                BigDecimal.ZERO, LineStatus.DRAFT, false, null, null, null, null, null, null, null, null, null, null, null,
                Instant.now(), "t");
    }
}
