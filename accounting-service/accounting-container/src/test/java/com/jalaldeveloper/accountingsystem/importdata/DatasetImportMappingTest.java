package com.jalaldeveloper.accountingsystem.importdata;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetImportMappingTest {

    @Test
    void personKindMapsToIndividual() {
        assertThat(DatasetImportService.parseKind("PERSON")).isEqualTo(PartnerKind.INDIVIDUAL);
        assertThat(DatasetImportService.parseKind("INDIVIDUAL")).isEqualTo(PartnerKind.INDIVIDUAL);
        assertThat(DatasetImportService.parseKind("COMPANY")).isEqualTo(PartnerKind.COMPANY);
    }

    @Test
    void extraCommaBeforeCurrencyIsShifted() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("currency", "");
        row.put("is_vendor", "IQD");
        row.put("is_customer", "false");
        row.put("payable_account_code", "true");
        row.put("receivable_account_code", "");
        row.put("credit_limit", "430003");
        Map<String, String> aligned = DatasetImportService.alignPartnerCurrency(row);
        assertThat(aligned.get("currency")).isEqualTo("IQD");
        assertThat(aligned.get("is_vendor")).isEqualTo("false");
        assertThat(aligned.get("is_customer")).isEqualTo("true");
        assertThat(aligned.get("receivable_account_code")).isEqualTo("430003");
    }
}
