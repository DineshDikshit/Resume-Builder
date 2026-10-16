package com.example.resumebuilder.render;

import android.graphics.Typeface;

/**
 * Visual settings of one resume design. Instances are created only by {@link Templates}
 * and must be treated as read-only afterwards.
 */
public final class Template {

    public enum Layout { SINGLE_COLUMN, SIDEBAR }

    public enum HeaderStyle { CENTERED, LEFT, BAND }

    public enum HeadingStyle { RULE, BAR, TINT, SHORT_UNDERLINE }

    public final String id;
    public final String name;
    public final String description;

    public Layout layout = Layout.SINGLE_COLUMN;
    public HeaderStyle headerStyle = HeaderStyle.LEFT;
    public HeadingStyle headingStyle = HeadingStyle.RULE;

    public Typeface bodyFace = Typeface.SANS_SERIF;
    public Typeface headingFace = Typeface.SANS_SERIF;

    public float nameSize = 24f;
    public float titleSize = 12f;
    public float headingSize = 11f;
    public float bodySize = 10f;
    public float smallSize = 9f;
    public float lineSpacing = 1.15f;
    public float sectionGap = 12f;
    public float entryGap = 6f;

    public float marginLeft = 44f;
    public float marginRight = 44f;
    public float marginTop = 44f;
    public float marginBottom = 44f;
    public float sidebarWidth = 190f;

    public int accent = 0xFF1F4E79;
    public int textColor = 0xFF1A1A1A;
    public int mutedColor = 0xFF555555;
    public int nameColor = 0xFF1A1A1A;
    public int bandTextColor = 0xFFFFFFFF;
    public int sidebarColor = 0xFF2B2D42;
    public int sidebarTextColor = 0xFFEDEDF0;
    public int sidebarMutedColor = 0xFFB8BAC8;
    public int sidebarHeadingColor = 0xFFE9C46A;

    Template(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}