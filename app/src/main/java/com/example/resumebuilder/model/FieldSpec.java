package com.example.resumebuilder.model;

/**
 * Describes one input field of a repeatable section entry (for example "Employer").
 * The UI builds its form from these descriptions, and the renderer reads values by key.
 */
public final class FieldSpec {

    public enum Kind {
        TEXT,
        MULTILINE,
        URL,
        DATE,
        CHECKBOX,
        EMAIL,
        PHONE
    }

    public static final String DATE_HINT = "YYYY-MM (for example 2024-06)";

    public final String key;
    public final String label;
    public final String hint;
    public final Kind kind;
    public final boolean required;

    private FieldSpec(String key, String label, String hint, Kind kind, boolean required) {
        this.key = key;
        this.label = label;
        this.hint = hint;
        this.kind = kind;
        this.required = required;
    }

    public static FieldSpec text(String key, String label, boolean required) {
        return new FieldSpec(key, label, "", Kind.TEXT, required);
    }

    public static FieldSpec multiline(String key, String label, String hint) {
        return new FieldSpec(key, label, hint, Kind.MULTILINE, false);
    }

    public static FieldSpec url(String key, String label) {
        return new FieldSpec(key, label, "", Kind.URL, false);
    }

    public static FieldSpec date(String key, String label) {
        return new FieldSpec(key, label, DATE_HINT, Kind.DATE, false);
    }

    public static FieldSpec checkbox(String key, String label) {
        return new FieldSpec(key, label, "", Kind.CHECKBOX, false);
    }

    public static FieldSpec email(String key, String label) {
        return new FieldSpec(key, label, "", Kind.EMAIL, false);
    }

    public static FieldSpec phone(String key, String label) {
        return new FieldSpec(key, label, "", Kind.PHONE, false);
    }
}