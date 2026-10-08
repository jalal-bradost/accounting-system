package com.bradox.erp.documents.domain.core.rule;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalizes names and search text so that Latin, Arabic and Kurdish spellings compare equal:
 * case folding, Arabic-Indic and Persian digits to 0-9, alef variants, yeh / kaf variants,
 * tatweel and diacritics removed, invisible direction marks removed, whitespace collapsed.
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String s = Normalizer.normalize(input, Normalizer.Form.NFKC);
        StringBuilder out = new StringBuilder(s.length());
        boolean lastSpace = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isIgnorable(c)) {
                continue;
            }
            c = mapChar(c);
            if (Character.isWhitespace(c)) {
                if (!lastSpace) {
                    out.append(' ');
                    lastSpace = true;
                }
                continue;
            }
            out.append(c);
            lastSpace = false;
        }
        int len = out.length();
        if (len > 0 && out.charAt(len - 1) == ' ') {
            out.setLength(len - 1);
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean isIgnorable(char c) {
        if (c == 'ـ') return true;                       // tatweel
        if (c >= 'ً' && c <= 'ٟ') return true;      // Arabic diacritics
        if (c == 'ٰ') return true;                       // superscript alef
        if (c == '‌' || c == '‍') return true;      // ZWNJ, ZWJ
        if (c == '‎' || c == '‏') return true;      // LRM, RLM
        if (c >= '‪' && c <= '‮') return true;      // bidi embeddings
        if (c >= '⁦' && c <= '⁩') return true;      // bidi isolates
        return Character.isISOControl(c) && !Character.isWhitespace(c);
    }

    private static char mapChar(char c) {
        if (c >= '٠' && c <= '٩') return (char) ('0' + (c - '٠'));   // Arabic-Indic digits
        if (c >= '۰' && c <= '۹') return (char) ('0' + (c - '۰'));   // Persian digits
        switch (c) {
            case 'أ': // alef with hamza above
            case 'إ': // alef with hamza below
            case 'آ': // alef with madda
            case 'ٱ': // alef wasla
                return 'ا';
            case 'ى': // alef maksura
            case 'ی': // Persian / Kurdish yeh
                return 'ي';
            case 'ک': // Persian / Kurdish kaf
                return 'ك';
            default:
                return c;
        }
    }
}
