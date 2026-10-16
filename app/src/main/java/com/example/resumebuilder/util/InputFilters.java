package com.example.resumebuilder.util;

import android.text.InputFilter;

/** Input filters that stop unwanted characters while the user types or pastes. */
public final class InputFilters {

    public static final String PHONE_CHARS = "0123456789+-(). ";
    public static final String DATE_CHARS = "0123456789-";

    private InputFilters() {
    }

    /** Removes every typed or pasted character that is not in the allowed set. */
    public static InputFilter allowOnly(String allowed) {
        return (source, start, end, dest, destStart, destEnd) -> {
            StringBuilder kept = null;
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (allowed.indexOf(c) < 0) {
                    if (kept == null) {
                        kept = new StringBuilder();
                        kept.append(source, start, i);
                    }
                } else if (kept != null) {
                    kept.append(c);
                }
            }
            return kept == null ? null : kept.toString();
        };
    }
}