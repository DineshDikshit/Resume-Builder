package com.example.resumebuilder.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and formats the "YYYY-MM" month values stored in section entries. */
public final class DateFormatter {

    private static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final Pattern YEAR_MONTH = Pattern.compile("^(\\d{4})-(\\d{1,2})$");
    private static final String DASH = " \u2013 ";

    private DateFormatter() {
    }

    /** "2024-06" becomes "Jun 2024". Text that is not a valid month is returned unchanged. */
    public static String formatMonth(String value) {
        String text = value == null ? "" : value.trim();
        Matcher matcher = YEAR_MONTH.matcher(text);
        if (matcher.matches()) {
            int month = Integer.parseInt(matcher.group(2));
            if (month >= 1 && month <= 12) {
                return MONTHS[month - 1] + " " + matcher.group(1);
            }
        }
        return text;
    }

    /** Builds "Jan 2020 \u2013 Mar 2022", "Jan 2020 \u2013 Present" or a single date. */
    public static String range(String start, String end, boolean current) {
        String from = formatMonth(start);
        String to = current ? "Present" : formatMonth(end);
        if (from.isEmpty()) {
            return to;
        }
        if (to.isEmpty()) {
            return from;
        }
        return from + DASH + to;
    }

    /** True for an empty value or a valid "YYYY-MM" month. */
    public static boolean isValidOrBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return monthIndex(value) >= 0;
    }

    /** True unless both dates are valid months and the end is before the start. */
    public static boolean isOrdered(String start, String end) {
        int from = monthIndex(start);
        int to = monthIndex(end);
        return from < 0 || to < 0 || from <= to;
    }

    /**
     * True when the value is a valid month later than the given current month
     * (currentMonth is 1 to 12). Invalid or empty values are never "in the future".
     */
    public static boolean isFuture(String value, int currentYear, int currentMonth) {
        int index = monthIndex(value);
        return index >= 0 && index > currentYear * 12 + (currentMonth - 1);
    }

    private static int monthIndex(String value) {
        if (value == null) {
            return -1;
        }
        Matcher matcher = YEAR_MONTH.matcher(value.trim());
        if (!matcher.matches()) {
            return -1;
        }
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        if (month < 1 || month > 12) {
            return -1;
        }
        return year * 12 + (month - 1);
    }
}