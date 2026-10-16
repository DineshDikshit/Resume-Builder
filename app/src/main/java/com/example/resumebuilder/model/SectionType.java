package com.example.resumebuilder.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * All repeatable resume sections. The declaration order is the default order in which
 * sections appear on a resume. Field keys are stored in the database as JSON keys, so
 * never rename an existing key without a database migration.
 */
public enum SectionType {

    WORK_EXPERIENCE("work_experience", "Work experience", "position", "job_title", "employer",
            FieldSpec.text("job_title", "Job title", true),
            FieldSpec.text("employer", "Employer", true),
            FieldSpec.text("location", "Location", false),
            FieldSpec.date("start_date", "Start date"),
            FieldSpec.date("end_date", "End date"),
            FieldSpec.checkbox("current", "I currently work here"),
            FieldSpec.multiline("bullets", "Achievements and responsibilities",
                    "One bullet point per line")),

    EDUCATION("education", "Education", "education entry", "institution", "degree",
            FieldSpec.text("institution", "Institution", true),
            FieldSpec.text("degree", "Degree or qualification", true),
            FieldSpec.text("field_of_study", "Field of study", false),
            FieldSpec.text("location", "Location", false),
            FieldSpec.date("start_date", "Start date"),
            FieldSpec.date("end_date", "End date"),
            FieldSpec.text("grade", "Grade or GPA", false),
            FieldSpec.multiline("description", "Description", "")),

    SKILLS("skills", "Skills", "skill", "name", "",
            FieldSpec.text("name", "Skill", true)),

    PROJECTS("projects", "Projects", "project", "name", "role",
            FieldSpec.text("name", "Project name", true),
            FieldSpec.text("role", "Your role", false),
            FieldSpec.date("start_date", "Start date"),
            FieldSpec.date("end_date", "End date"),
            FieldSpec.url("link", "Link"),
            FieldSpec.multiline("description", "Description", "")),

    CERTIFICATIONS("certifications", "Certifications and licenses", "certification", "name",
            "issuer",
            FieldSpec.text("name", "Name", true),
            FieldSpec.text("issuer", "Issuing organization", false),
            FieldSpec.date("issue_date", "Issue date"),
            FieldSpec.date("expiry_date", "Expiry date"),
            FieldSpec.text("credential", "Credential ID or link", false)),

    LANGUAGES("languages", "Languages", "language", "language", "proficiency",
            FieldSpec.text("language", "Language", true),
            FieldSpec.text("proficiency", "Proficiency (for example Fluent)", false)),

    AWARDS("awards", "Awards and honors", "award", "title", "issuer",
            FieldSpec.text("title", "Title", true),
            FieldSpec.text("issuer", "Issued by", false),
            FieldSpec.date("date", "Date"),
            FieldSpec.multiline("description", "Description", "")),

    VOLUNTEER("volunteer", "Volunteer experience", "volunteer entry", "role", "organization",
            FieldSpec.text("role", "Role", true),
            FieldSpec.text("organization", "Organization", true),
            FieldSpec.text("location", "Location", false),
            FieldSpec.date("start_date", "Start date"),
            FieldSpec.date("end_date", "End date"),
            FieldSpec.checkbox("current", "I currently volunteer here"),
            FieldSpec.multiline("description", "Description", ""));

    /** Stable identifier stored in the database. */
    public final String key;
    /** Plural heading shown in the app and on the resume. */
    public final String title;
    /** Lower-case singular name used in messages such as "Add position". */
    public final String itemName;
    /** Field key shown as the first line of a list row. */
    public final String primaryKey;
    /** Field key shown as the second line of a list row (may be empty). */
    public final String secondaryKey;
    public final List<FieldSpec> fields;

    SectionType(String key, String title, String itemName, String primaryKey,
                String secondaryKey, FieldSpec... fields) {
        this.key = key;
        this.title = title;
        this.itemName = itemName;
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.fields = Collections.unmodifiableList(Arrays.asList(fields));
    }

    /** Returns the section with the given stored key, or null if the key is unknown. */
    public static SectionType fromKey(String key) {
        for (SectionType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }

    public String primaryText(SectionItem item) {
        return primaryKey.isEmpty() ? "" : item.get(primaryKey);
    }

    public String secondaryText(SectionItem item) {
        return secondaryKey.isEmpty() ? "" : item.get(secondaryKey);
    }
}