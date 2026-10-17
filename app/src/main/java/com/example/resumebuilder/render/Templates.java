package com.example.resumebuilder.render;

import android.graphics.Typeface;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The built-in resume designs. */
public final class Templates {

    private static final List<Template> ALL = Collections.unmodifiableList(
            Arrays.asList(classic(), modern(), compact(), elegant(),
                    minimal(), executive(), professional()));

    private Templates() {
    }

    public static List<Template> all() {
        return ALL;
    }

    /** Returns the template with the given id, or Classic when the id is unknown. */
    public static Template byId(String id) {
        for (Template template : ALL) {
            if (template.id.equals(id)) {
                return template;
            }
        }
        return ALL.get(0);
    }

    private static Template classic() {
        Template t = new Template("classic", "Classic",
                "Centered header, serif type and thin rules. Traditional and easy to scan.");
        t.layout = Template.Layout.SINGLE_COLUMN;
        t.headerStyle = Template.HeaderStyle.CENTERED;
        t.headingStyle = Template.HeadingStyle.RULE;
        t.bodyFace = Typeface.SERIF;
        t.headingFace = Typeface.SERIF;
        t.nameSize = 26f;
        t.titleSize = 12f;
        t.headingSize = 11f;
        t.bodySize = 10.5f;
        t.smallSize = 9.5f;
        t.lineSpacing = 1.15f;
        t.sectionGap = 14f;
        t.entryGap = 7f;
        t.marginLeft = 50f;
        t.marginRight = 50f;
        t.marginTop = 46f;
        t.marginBottom = 46f;
        t.accent = 0xFF1F3A5F;
        t.textColor = 0xFF111111;
        t.mutedColor = 0xFF555555;
        t.nameColor = 0xFF111111;
        return t;
    }

    private static Template modern() {
        Template t = new Template("modern", "Modern",
                "Colored header band, accent bars and clean sans-serif type.");
        t.layout = Template.Layout.SINGLE_COLUMN;
        t.headerStyle = Template.HeaderStyle.BAND;
        t.headingStyle = Template.HeadingStyle.BAR;
        t.bodyFace = Typeface.SANS_SERIF;
        t.headingFace = Typeface.SANS_SERIF;
        t.nameSize = 28f;
        t.titleSize = 13f;
        t.headingSize = 11.5f;
        t.bodySize = 10f;
        t.smallSize = 9f;
        t.lineSpacing = 1.2f;
        t.sectionGap = 14f;
        t.entryGap = 7f;
        t.marginLeft = 40f;
        t.marginRight = 40f;
        t.marginTop = 40f;
        t.marginBottom = 40f;
        t.accent = 0xFF1F4E79;
        t.textColor = 0xFF1A1C1E;
        t.mutedColor = 0xFF5F6368;
        t.bandTextColor = 0xFFFFFFFF;
        return t;
    }

    private static Template compact() {
        Template t = new Template("compact", "Compact",
                "Small dense text with tinted headings. Fits more content per page.");
        t.layout = Template.Layout.SINGLE_COLUMN;
        t.headerStyle = Template.HeaderStyle.LEFT;
        t.headingStyle = Template.HeadingStyle.TINT;
        t.bodyFace = Typeface.SANS_SERIF;
        t.headingFace = Typeface.SANS_SERIF;
        t.nameSize = 22f;
        t.titleSize = 11f;
        t.headingSize = 9.5f;
        t.bodySize = 9f;
        t.smallSize = 8.5f;
        t.lineSpacing = 1.1f;
        t.sectionGap = 9f;
        t.entryGap = 4f;
        t.marginLeft = 34f;
        t.marginRight = 34f;
        t.marginTop = 32f;
        t.marginBottom = 30f;
        t.accent = 0xFF2E7D6B;
        t.textColor = 0xFF1A1C1E;
        t.mutedColor = 0xFF5F6368;
        t.nameColor = 0xFF1A1C1E;
        return t;
    }

    private static Template elegant() {
        Template t = new Template("elegant", "Elegant",
                "Dark sidebar for contact, skills and languages, with serif headings.");
        t.layout = Template.Layout.SIDEBAR;
        t.headerStyle = Template.HeaderStyle.LEFT;
        t.headingStyle = Template.HeadingStyle.SHORT_UNDERLINE;
        t.bodyFace = Typeface.SANS_SERIF;
        t.headingFace = Typeface.SERIF;
        t.nameSize = 26f;
        t.titleSize = 12.5f;
        t.headingSize = 11f;
        t.bodySize = 10f;
        t.smallSize = 9f;
        t.lineSpacing = 1.18f;
        t.sectionGap = 14f;
        t.entryGap = 7f;
        t.marginLeft = 28f;
        t.marginRight = 30f;
        t.marginTop = 40f;
        t.marginBottom = 40f;
        t.sidebarWidth = 190f;
        t.accent = 0xFF8E3B46;
        t.textColor = 0xFF1A1C1E;
        t.mutedColor = 0xFF5F6368;
        t.nameColor = 0xFF2B2D42;
        t.sidebarColor = 0xFF2B2D42;
        t.sidebarTextColor = 0xFFEDEDF0;
        t.sidebarMutedColor = 0xFFB8BAC8;
        t.sidebarHeadingColor = 0xFFE9C46A;
        return t;
    }

    private static Template minimal() {
        Template t = new Template("minimal", "Minimal",
                "Plain black-and-white layout with no colors. Easiest for job-application "
                        + "software to read.");
        t.layout = Template.Layout.SINGLE_COLUMN;
        t.headerStyle = Template.HeaderStyle.LEFT;
        t.headingStyle = Template.HeadingStyle.RULE;
        t.bodyFace = Typeface.SANS_SERIF;
        t.headingFace = Typeface.SANS_SERIF;
        t.nameSize = 24f;
        t.titleSize = 12f;
        t.headingSize = 10.5f;
        t.bodySize = 10f;
        t.smallSize = 9f;
        t.lineSpacing = 1.18f;
        t.sectionGap = 13f;
        t.entryGap = 6f;
        t.marginLeft = 48f;
        t.marginRight = 48f;
        t.marginTop = 44f;
        t.marginBottom = 44f;
        t.accent = 0xFF333333;
        t.textColor = 0xFF111111;
        t.mutedColor = 0xFF555555;
        t.nameColor = 0xFF111111;
        return t;
    }

    private static Template executive() {
        Template t = new Template("executive", "Executive",
                "Dark charcoal header band with serif headings and short underlines. "
                        + "Confident and formal.");
        t.layout = Template.Layout.SINGLE_COLUMN;
        t.headerStyle = Template.HeaderStyle.BAND;
        t.headingStyle = Template.HeadingStyle.SHORT_UNDERLINE;
        t.bodyFace = Typeface.SANS_SERIF;
        t.headingFace = Typeface.SERIF;
        t.nameSize = 27f;
        t.titleSize = 13f;
        t.headingSize = 11.5f;
        t.bodySize = 10f;
        t.smallSize = 9f;
        t.lineSpacing = 1.2f;
        t.sectionGap = 15f;
        t.entryGap = 7f;
        t.marginLeft = 44f;
        t.marginRight = 44f;
        t.marginTop = 40f;
        t.marginBottom = 40f;
        t.accent = 0xFF2B2F36;
        t.textColor = 0xFF1A1C1E;
        t.mutedColor = 0xFF5F6368;
        t.bandTextColor = 0xFFFFFFFF;
        return t;
    }

    private static Template professional() {
        Template t = new Template("professional", "Professional",
                "Dark teal sidebar for contact, skills and languages, with accent bars "
                        + "on the headings.");
        t.layout = Template.Layout.SIDEBAR;
        t.headerStyle = Template.HeaderStyle.LEFT;
        t.headingStyle = Template.HeadingStyle.BAR;
        t.bodyFace = Typeface.SANS_SERIF;
        t.headingFace = Typeface.SANS_SERIF;
        t.nameSize = 25f;
        t.titleSize = 12.5f;
        t.headingSize = 10.5f;
        t.bodySize = 10f;
        t.smallSize = 9f;
        t.lineSpacing = 1.18f;
        t.sectionGap = 14f;
        t.entryGap = 7f;
        t.marginLeft = 28f;
        t.marginRight = 30f;
        t.marginTop = 40f;
        t.marginBottom = 40f;
        t.sidebarWidth = 185f;
        t.accent = 0xFF0F4C5C;
        t.textColor = 0xFF1A1C1E;
        t.mutedColor = 0xFF5F6368;
        t.nameColor = 0xFF0F4C5C;
        t.sidebarColor = 0xFF0F4C5C;
        t.sidebarTextColor = 0xFFEAF4F6;
        t.sidebarMutedColor = 0xFFA9CBD2;
        t.sidebarHeadingColor = 0xFFFFD166;
        return t;
    }
}