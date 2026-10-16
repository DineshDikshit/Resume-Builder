package com.example.resumebuilder.util;

/** Builds safe PDF file names from user-entered text. */
public final class FileNames {

    private static final int MAX_BASE_CODE_POINTS = 60;

    private FileNames() {
    }

    /**
     * "Rahul Sharma" gives "Rahul_Sharma_Resume.pdf". Falls back to the resume title, then to
     * "Resume.pdf" when both are empty.
     */
    public static String forResume(String fullName, String title) {
        String base = safeBase(fullName);
        if (base.isEmpty()) {
            base = safeBase(title);
        } else {
            base = base + "_Resume";
        }
        if (base.isEmpty()) {
            base = "Resume";
        }
        return base + ".pdf";
    }

    /**
     * Keeps letters, digits and combining marks of any language and turns every other run of
     * characters (slashes, dots, quotes, emoji, spaces) into one underscore.
     */
    public static String safeBase(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean lastWasUnderscore = false;
        int count = 0;
        int index = 0;
        while (index < raw.length() && count < MAX_BASE_CODE_POINTS) {
            int codePoint = raw.codePointAt(index);
            index += Character.charCount(codePoint);
            if (isKept(codePoint)) {
                builder.appendCodePoint(codePoint);
                lastWasUnderscore = false;
                count++;
            } else if (builder.length() > 0 && !lastWasUnderscore) {
                builder.append('_');
                lastWasUnderscore = true;
                count++;
            }
        }
        int end = builder.length();
        while (end > 0 && builder.charAt(end - 1) == '_') {
            end--;
        }
        return builder.substring(0, end);
    }

    private static boolean isKept(int codePoint) {
        if (Character.isLetterOrDigit(codePoint)) {
            return true;
        }
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}