package com.bradox.erp.repair.domain.core.rule;

import com.bradox.erp.repair.domain.core.model.GuideEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Orders labor-guide entries for a vehicle: exact make and model first, then make only, then generic ones (REP-03 #9). */
public final class GuideRanking {

    private GuideRanking() {
    }

    /** 3 = make, model (and year) match; 2 = make matches and the entry names no model; 1 = generic; 0 = does not apply. */
    public static int score(GuideEntry e, String make, String model, Integer year) {
        boolean noMake = isBlank(e.make());
        if (noMake) {
            return 1;
        }
        if (!same(e.make(), make)) {
            return 0;
        }
        if (year != null && ((e.yearFrom() != null && year < e.yearFrom()) || (e.yearTo() != null && year > e.yearTo()))) {
            return 0;
        }
        if (isBlank(e.model())) {
            return 2;
        }
        return same(e.model(), model) ? 3 : 0;
    }

    /** Entries that apply to the vehicle, best match first, then by code. With no vehicle given, all entries by code. */
    public static List<GuideEntry> rank(List<GuideEntry> entries, String make, String model, Integer year) {
        if (isBlank(make)) {
            return entries.stream().sorted(Comparator.comparing(e -> e.code().toLowerCase(Locale.ROOT))).toList();
        }
        return entries.stream().filter(e -> score(e, make, model, year) > 0)
                .sorted(Comparator.comparingInt((GuideEntry e) -> score(e, make, model, year)).reversed()
                        .thenComparing(e -> e.code().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static boolean same(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
