package com.bradox.erp.repair.domain.core.rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses the labor-guide CSV (REP-03 #8). Columns, in order, with a header row:
 * code, description_en, description_ar, description_ku, category, standard_minutes, make, model, year_from, year_to.
 * Invalid rows are reported with their line number; valid rows are returned so the caller can import all-or-nothing or
 * skip the bad ones.
 */
public final class LaborGuideCsv {

    public static final List<String> HEADER = List.of("code", "description_en", "description_ar", "description_ku", "category",
            "standard_minutes", "make", "model", "year_from", "year_to");

    public record Row(int line, String code, String descriptionEn, String descriptionAr, String descriptionKu, String category,
                      int standardMinutes, String make, String model, Integer yearFrom, Integer yearTo) {
    }

    public record RowError(int line, String reason) {
    }

    public record Result(List<Row> rows, List<RowError> errors) {
    }

    private LaborGuideCsv() {
    }

    public static Result parse(String csv) {
        List<Row> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            errors.add(new RowError(1, "The file is empty"));
            return new Result(rows, errors);
        }
        String[] lines = csv.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int start = 0;
        if (!lines[0].isEmpty() && lines[0].charAt(0) == '﻿') {
            lines[0] = lines[0].substring(1);
        }
        List<String> first = split(lines[0]);
        if (!first.isEmpty() && first.get(0).trim().equalsIgnoreCase("code")) {
            start = 1;
        }
        Set<String> seen = new HashSet<>();
        for (int i = start; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            int number = i + 1;
            List<String> c = split(lines[i]);
            if (c.size() < 6) {
                errors.add(new RowError(number, "Expected at least 6 columns (code to standard_minutes)"));
                continue;
            }
            String code = c.get(0).trim();
            if (code.isEmpty() || code.length() > 50) {
                errors.add(new RowError(number, "Code is required (max 50 characters)"));
                continue;
            }
            int minutes;
            try {
                minutes = Integer.parseInt(c.get(5).trim());
            } catch (NumberFormatException e) {
                errors.add(new RowError(number, "standard_minutes must be a whole number"));
                continue;
            }
            if (minutes <= 0 || minutes > 100_000) {
                errors.add(new RowError(number, "standard_minutes must be between 1 and 100000"));
                continue;
            }
            if (c.get(1).isBlank() && c.get(2).isBlank() && c.get(3).isBlank()) {
                errors.add(new RowError(number, "A description in at least one language is required"));
                continue;
            }
            Integer from = optionalYear(c, 8);
            Integer to = optionalYear(c, 9);
            if ((from != null && from == -1) || (to != null && to == -1)) {
                errors.add(new RowError(number, "Years must be four-digit numbers"));
                continue;
            }
            if (from != null && to != null && from > to) {
                errors.add(new RowError(number, "year_from is after year_to"));
                continue;
            }
            String make = blankToNull(get(c, 6));
            String model = blankToNull(get(c, 7));
            if (model != null && make == null) {
                errors.add(new RowError(number, "A model needs a make"));
                continue;
            }
            String key = (code + "|" + (make == null ? "" : make) + "|" + (model == null ? "" : model)).toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                errors.add(new RowError(number, "Duplicate code for the same make and model in this file"));
                continue;
            }
            rows.add(new Row(number, code, blankToNull(c.get(1)), blankToNull(c.get(2)), blankToNull(c.get(3)),
                    blankToNull(c.get(4)), minutes, make, model, from, to));
        }
        return new Result(rows, errors);
    }

    private static String get(List<String> c, int i) {
        return i < c.size() ? c.get(i) : "";
    }

    /** {@code null} when empty, -1 when present but not a plausible year. */
    private static Integer optionalYear(List<String> c, int i) {
        String v = get(c, i).trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            int y = Integer.parseInt(v);
            return y >= 1900 && y <= 2100 ? y : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Splits one CSV line; double quotes wrap values that contain commas, "" is a literal quote. */
    static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
