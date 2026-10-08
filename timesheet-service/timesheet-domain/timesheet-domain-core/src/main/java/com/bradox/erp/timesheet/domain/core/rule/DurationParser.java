package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns what a person types into whole minutes (D2). Accepts {@code 1:30}, {@code 1.5}, {@code 1,5},
 * {@code 90m}, {@code 1h30}, {@code 1h 30m}, {@code 2h}, and Arabic-Indic / Persian digits.
 * A bare number means hours. Returns 0 for blank input so a grid cell can be cleared.
 */
public final class DurationParser {

    private static final Pattern CLOCK = Pattern.compile("^(\\d+):(\\d{1,2})$");
    private static final Pattern HOURS_MINUTES = Pattern.compile("^(\\d+)h(?:(\\d+)m?)?$");
    private static final Pattern MINUTES = Pattern.compile("^(\\d+)m(?:in)?$");
    private static final Pattern DECIMAL_HOURS = Pattern.compile("^(\\d+(?:\\.\\d+)?)h?$");
    private static final Pattern DECIMAL_HOURS_LEADING_POINT = Pattern.compile("^(\\.\\d+)h?$");

    private DurationParser() {
    }

    public static int parse(String input) {
        if (input == null) {
            return 0;
        }
        String s = normalize(input);
        if (s.isEmpty()) {
            return 0;
        }
        Matcher m = CLOCK.matcher(s);
        if (m.matches()) {
            int minutes = Integer.parseInt(m.group(2));
            if (minutes > 59) {
                throw invalid(input);
            }
            return Math.addExact(Math.multiplyExact(Integer.parseInt(m.group(1)), 60), minutes);
        }
        m = HOURS_MINUTES.matcher(s);
        if (m.matches()) {
            int minutes = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            if (minutes > 59) {
                throw invalid(input);
            }
            return Math.addExact(Math.multiplyExact(Integer.parseInt(m.group(1)), 60), minutes);
        }
        m = MINUTES.matcher(s);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        m = DECIMAL_HOURS.matcher(s);
        if (!m.matches()) {
            m = DECIMAL_HOURS_LEADING_POINT.matcher(s);
        }
        if (m.matches()) {
            return (int) Math.round(Double.parseDouble(m.group(1)) * 60);
        }
        throw invalid(input);
    }

    /** Lower-cases, drops spaces, maps Arabic-Indic and Persian digits and decimal marks to ASCII. */
    static String normalize(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (char c : input.trim().toLowerCase().toCharArray()) {
            if (c >= '٠' && c <= '٩') {
                out.append((char) ('0' + (c - '٠')));
            } else if (c >= '۰' && c <= '۹') {
                out.append((char) ('0' + (c - '۰')));
            } else if (c == ',' || c == '٫' || c == '،') {
                out.append('.');
            } else if (c == '٬' || Character.isWhitespace(c)) {
                // thousands mark and spaces carry no meaning here
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static TimesheetDomainException invalid(String input) {
        return new TimesheetDomainException("error.timesheet.durationInvalid", new Object[]{input},
                "Cannot read the duration \"" + input + "\". Try 1:30, 1.5, 90m or 1h30.");
    }
}
