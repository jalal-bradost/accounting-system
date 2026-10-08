package com.bradox.erp.documents.domain.core.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameNormalizerTest {

    @Test
    void lowercasesTrimsAndCollapsesSpaces() {
        assertEquals("annual report 2026", NameNormalizer.normalize("  Annual   REPORT\t2026 "));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", NameNormalizer.normalize(null));
    }

    @Test
    void mapsArabicIndicAndPersianDigits() {
        assertEquals("invoice 2026", NameNormalizer.normalize("invoice ٢٠٢٦"));
        assertEquals("invoice 2026", NameNormalizer.normalize("invoice ۲۰۲۶"));
    }

    @Test
    void unifiesAlefVariantsYehKafAndStripsTatweelAndDiacritics() {
        // أحمد, إحمد and احمد compare equal
        assertEquals(NameNormalizer.normalize("أحمد"), NameNormalizer.normalize("احمد"));
        assertEquals(NameNormalizer.normalize("إحمد"), NameNormalizer.normalize("احمد"));
        // Persian yeh and kaf equal the Arabic ones
        assertEquals(NameNormalizer.normalize("كتاب"), NameNormalizer.normalize("کتاب"));
        assertEquals(NameNormalizer.normalize("علي"), NameNormalizer.normalize("علی"));
        assertEquals(NameNormalizer.normalize("على"), NameNormalizer.normalize("علي"));
        // tatweel and diacritics vanish
        assertEquals("محمد", NameNormalizer.normalize("مـحمَّد"));
    }

    @Test
    void dropsInvisibleDirectionMarksAndControlCharacters() {
        assertEquals("abc", NameNormalizer.normalize("a‎b‏c‪"));
        assertEquals("ab", NameNormalizer.normalize("a\u0001b"));
    }
}
