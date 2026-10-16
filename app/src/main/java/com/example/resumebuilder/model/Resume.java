package com.example.resumebuilder.model;

import java.util.ArrayList;
import java.util.List;

/** A resume: personal details, settings and all section entries. */
public class Resume {

    public static final String DEFAULT_TEMPLATE_ID = "classic";

    public long id;
    /** Name shown in the resume list (for example "Android Developer 2026"). */
    public String title = "";
    public String templateId = DEFAULT_TEMPLATE_ID;

    public String fullName = "";
    public String professionalTitle = "";
    public String email = "";
    public String phone = "";
    public String city = "";
    public String region = "";
    public String country = "";
    public String website = "";
    public String profileLink = "";

    public String summary = "";

    public long createdAt;
    public long updatedAt;

    /** All entries of all sections, ordered by section type and position. */
    public final List<SectionItem> items = new ArrayList<>();

    /** Returns the entries of one section in display order. */
    public List<SectionItem> itemsOf(SectionType type) {
        List<SectionItem> result = new ArrayList<>();
        for (SectionItem item : items) {
            if (item.type == type) {
                result.add(item);
            }
        }
        return result;
    }

    /** "City, Region, Country" without empty parts. */
    public String locationLine() {
        return joinNonBlank(", ", city, region, country);
    }

    /** Contact details in display order, skipping blanks. */
    public List<String> contactParts() {
        List<String> parts = new ArrayList<>();
        addIfNotBlank(parts, email);
        addIfNotBlank(parts, phone);
        addIfNotBlank(parts, locationLine());
        addIfNotBlank(parts, website);
        addIfNotBlank(parts, profileLink);
        return parts;
    }

    private static void addIfNotBlank(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(value.trim());
        }
    }

    private static String joinNonBlank(String separator, String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }
}