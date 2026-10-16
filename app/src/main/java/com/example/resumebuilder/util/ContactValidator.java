package com.example.resumebuilder.util;

import java.util.regex.Pattern;

/** Format checks for optional contact fields. Blank values are always accepted. */
public final class ContactValidator {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_CHARS = Pattern.compile("^\\+?[0-9 ().\\-]+$");
    private static final int MIN_DIGITS = 7;
    private static final int MAX_DIGITS = 15;

    private ContactValidator() {
    }

    public static boolean isValidEmail(String value) {
        String text = value == null ? "" : value.trim();
        return text.isEmpty() || EMAIL.matcher(text).matches();
    }

    public static boolean isValidPhone(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return true;
        }
        if (!PHONE_CHARS.matcher(text).matches()) {
            return false;
        }
        int digits = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
            }
        }
        return digits >= MIN_DIGITS && digits <= MAX_DIGITS;
    }
}