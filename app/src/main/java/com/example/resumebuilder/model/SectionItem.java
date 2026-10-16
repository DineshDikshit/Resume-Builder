package com.example.resumebuilder.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** One entry of a repeatable section, such as a single job or a single skill. */
public class SectionItem {

    public long id;
    public long resumeId;
    public final SectionType type;
    /** Zero-based order inside its section. */
    public int position;

    private final Map<String, String> values = new LinkedHashMap<>();

    public SectionItem(SectionType type) {
        this.type = type;
    }

    /** Returns the trimmed value for the key, or an empty string when it is not set. */
    public String get(String key) {
        String value = values.get(key);
        return value == null ? "" : value.trim();
    }

    public void put(String key, String value) {
        values.put(key, value == null ? "" : value);
    }

    public boolean isChecked(String key) {
        return "true".equals(values.get(key));
    }

    public void setChecked(String key, boolean checked) {
        values.put(key, checked ? "true" : "false");
    }

    /** Live view of the stored values, used by the database layer for JSON conversion. */
    public Map<String, String> getValues() {
        return values;
    }

    /** True when no field contains visible text. */
    public boolean isBlank() {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if ("true".equals(entry.getValue()) || "false".equals(entry.getValue())) {
                continue;
            }
            if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Creates an unsaved copy (id 0) with the same values, used when duplicating a resume. */
    public SectionItem copy() {
        SectionItem copy = new SectionItem(type);
        copy.position = position;
        copy.values.putAll(values);
        return copy;
    }
}