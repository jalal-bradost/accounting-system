package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationParserTest {

    @ParameterizedTest
    @CsvSource({
            "1:30, 90", "1.5, 90", "'1,5', 90", "90m, 90", "1h30, 90", "1h 30m, 90", "1h30m, 90",
            "2h, 120", "2, 120", "0:45, 45", "0.25, 15", ".5, 30", "1.5h, 90", "8, 480", "24, 1440",
            "45min, 45", "' 2 H ', 120"
    })
    void parsesCommonShapes(String input, int expected) {
        assertEquals(expected, DurationParser.parse(input));
    }

    @Test
    void acceptsArabicIndicAndPersianDigits() {
        assertEquals(90, DurationParser.parse("١:٣٠"));
        assertEquals(90, DurationParser.parse("۱٫۵"));
        assertEquals(135, DurationParser.parse("۲:۱۵"));
    }

    @Test
    void blankMeansZeroSoACellCanBeCleared() {
        assertEquals(0, DurationParser.parse(null));
        assertEquals(0, DurationParser.parse("   "));
        assertEquals(0, DurationParser.parse("0"));
    }

    @ParameterizedTest
    @CsvSource({"abc", "1:75", "1h75", "-1", "1:2:3", "h", "1.2.3"})
    void rejectsGarbage(String input) {
        assertThrows(TimesheetDomainException.class, () -> DurationParser.parse(input));
    }
}
