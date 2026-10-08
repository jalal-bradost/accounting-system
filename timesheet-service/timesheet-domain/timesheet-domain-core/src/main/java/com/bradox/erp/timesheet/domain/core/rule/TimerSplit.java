package com.bradox.erp.timesheet.domain.core.rule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Splits a timer run into one piece per calendar date in the company time zone (TSH-04 #4, D3). */
public final class TimerSplit {

    public record Piece(LocalDate date, int minutes) {
    }

    private TimerSplit() {
    }

    public static List<Piece> split(Instant start, Instant stop, ZoneId zone) {
        List<Piece> pieces = new ArrayList<>();
        Instant cursor = start;
        while (cursor.isBefore(stop)) {
            LocalDate date = cursor.atZone(zone).toLocalDate();
            Instant endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant();
            Instant segmentEnd = endOfDay.isBefore(stop) ? endOfDay : stop;
            long seconds = segmentEnd.getEpochSecond() - cursor.getEpochSecond();
            int minutes = (int) ((seconds + 30) / 60);
            if (minutes > 0) {
                pieces.add(new Piece(date, minutes));
            }
            cursor = segmentEnd;
        }
        return pieces;
    }
}
