package com.example.resumebuilder.render;

import android.graphics.Paint;
import android.graphics.Typeface;

import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;
import com.example.resumebuilder.util.DateFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Lays a resume out onto A4 pages (595 x 842 points). Text is wrapped line by line and a
 * new page is started whenever the next line does not fit, so nothing is clipped or lost.
 */
public final class ResumeRenderer {

    public static final int PAGE_WIDTH = 595;
    public static final int PAGE_HEIGHT = 842;

    private static final String BULLET = "\u2022";
    private static final String SEPARATOR = "  \u2022  ";
    private static final String DOT = " \u2022 ";
    private static final float BULLET_INDENT = 12f;
    private static final float HEADING_PAD = 6f;
    private static final float BAND_PAD_TOP = 30f;
    private static final float BAND_PAD_BOTTOM = 18f;
    private static final List<String> NO_BULLETS = Collections.emptyList();

    private final Resume resume;
    private final Template t;
    private final RenderedResume result = new RenderedResume(PAGE_WIDTH, PAGE_HEIGHT);

    private ResumeRenderer(Resume resume, Template template) {
        this.resume = resume;
        this.t = template;
    }

    public static RenderedResume render(Resume resume, Template template) {
        return new ResumeRenderer(resume, template).layout();
    }

    // ------------------------------------------------------------------ structure

    private static final class Style {
        final Paint body;
        final Paint bold;
        final Paint sub;
        final Paint date;
        final Paint heading;
        final int headingColor;

        Style(Template t, int text, int muted, int headingColor) {
            this.body = paint(t.bodyFace, Typeface.NORMAL, t.bodySize, text);
            this.bold = paint(t.bodyFace, Typeface.BOLD, t.bodySize, text);
            this.sub = paint(t.bodyFace, Typeface.ITALIC, t.bodySize, muted);
            this.date = paint(t.bodyFace, Typeface.NORMAL, t.smallSize, muted);
            this.heading = paint(t.headingFace, Typeface.BOLD, t.headingSize, headingColor);
            this.headingColor = headingColor;
        }
    }

    /** A column of content with its own cursor, so a sidebar and a main column can flow apart. */
    private static final class Column {
        final float x;
        final float width;
        final Style style;
        int page;
        float y;

        Column(float x, float width, Style style, float startY) {
            this.x = x;
            this.width = width;
            this.style = style;
            this.y = startY;
        }
    }

    private RenderedResume layout() {
        Style mainStyle = new Style(t, t.textColor, t.mutedColor, t.accent);

        if (t.layout == Template.Layout.SIDEBAR) {
            float mainX = t.sidebarWidth + 24f;
            Column main = new Column(mainX, PAGE_WIDTH - mainX - t.marginRight, mainStyle,
                    t.marginTop);
            header(main);
            sections(main, true);

            Style sideStyle = new Style(t, t.sidebarTextColor, t.sidebarMutedColor,
                    t.sidebarHeadingColor);
            Column side = new Column(20f, t.sidebarWidth - 40f, sideStyle, t.marginTop);
            sidebar(side);

            result.page(0);
            for (int i = 0; i < result.getPageCount(); i++) {
                Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                fill.setColor(t.sidebarColor);
                fill.setStyle(Paint.Style.FILL);
                float width = t.sidebarWidth;
                result.page(i).addFirst(c -> c.drawRect(0f, 0f, width, PAGE_HEIGHT, fill));
            }
        } else {
            Column main = new Column(t.marginLeft, PAGE_WIDTH - t.marginLeft - t.marginRight,
                    mainStyle, t.marginTop);
            header(main);
            sections(main, false);
        }

        result.page(0);
        return result;
    }

    // ------------------------------------------------------------------- header

    private void header(Column col) {
        String name = resume.fullName.trim();
        if (name.isEmpty()) {
            name = "Your Name";
        }
        String title = resume.professionalTitle.trim();
        String contact = join(SEPARATOR, resume.contactParts().toArray(new String[0]));

        boolean band = t.headerStyle == Template.HeaderStyle.BAND;
        boolean centered = t.headerStyle == Template.HeaderStyle.CENTERED;
        int bandMuted = (t.bandTextColor & 0x00FFFFFF) | 0xCC000000;

        Paint namePaint = paint(t.headingFace, Typeface.BOLD, t.nameSize,
                band ? t.bandTextColor : t.nameColor);
        Paint titlePaint = paint(t.bodyFace, Typeface.NORMAL, t.titleSize,
                band ? bandMuted : t.accent);
        Paint contactPaint = paint(t.bodyFace, Typeface.NORMAL, t.smallSize,
                band ? bandMuted : t.mutedColor);

        List<String> nameLines = wrap(name, namePaint, col.width);
        List<String> titleLines = wrap(title, titlePaint, col.width);
        List<String> contactLines = wrap(contact, contactPaint, col.width);

        float bandHeight = 0f;
        if (band) {
            bandHeight = BAND_PAD_TOP
                    + nameLines.size() * advance(namePaint) + 2f
                    + titleLines.size() * advance(titlePaint) + 3f
                    + contactLines.size() * advance(contactPaint)
                    + BAND_PAD_BOTTOM;
            rect(0, 0f, 0f, PAGE_WIDTH, bandHeight, t.accent);
            col.y = BAND_PAD_TOP;
        }

        headerLines(col, nameLines, namePaint, centered);
        col.y += 2f;
        headerLines(col, titleLines, titlePaint, centered);
        col.y += 3f;
        headerLines(col, contactLines, contactPaint, centered);

        if (band) {
            col.y = bandHeight + 4f;
        } else {
            col.y += 2f;
            line(col.page, col.x, col.y, col.x + col.width, col.y, t.accent, 1f);
            col.y += 6f;
        }
    }

    private void headerLines(Column col, List<String> lines, Paint p, boolean centered) {
        float adv = advance(p);
        for (String line : lines) {
            ensure(col, adv);
            float x = centered ? col.x + (col.width - p.measureText(line)) / 2f : col.x;
            text(col, line, x, p);
            col.y += adv;
        }
    }

    // ----------------------------------------------------------------- sections

    private void sections(Column col, boolean sidebarLayout) {
        String summary = resume.summary.trim();
        if (!summary.isEmpty()) {
            sectionHeading(col, "Professional summary", firstBlock(col));
            paragraph(col, wrap(summary, col.style.body, col.width), col.style.body, 0f);
        }
        for (SectionType type : SectionType.values()) {
            if (sidebarLayout && (type == SectionType.SKILLS || type == SectionType.LANGUAGES)) {
                continue;
            }
            List<SectionItem> items = visibleItems(type);
            if (!items.isEmpty()) {
                section(col, type, items, false);
            }
        }
    }

    private void sidebar(Column col) {
        Paint body = col.style.body;
        List<String> contact = resume.contactParts();
        if (!contact.isEmpty()) {
            sectionHeading(col, "Contact", firstBlock(col));
            for (String part : contact) {
                paragraph(col, wrap(part, body, col.width), body, 0f);
                col.y += 3f;
            }
        }
        SectionType[] sideTypes = {SectionType.SKILLS, SectionType.LANGUAGES};
        for (SectionType type : sideTypes) {
            List<SectionItem> items = visibleItems(type);
            if (!items.isEmpty()) {
                section(col, type, items, true);
            }
        }
    }

    private void section(Column col, SectionType type, List<SectionItem> items, boolean perLine) {
        sectionHeading(col, type.title, firstBlock(col));
        if (type == SectionType.SKILLS || type == SectionType.LANGUAGES) {
            List<String> values = new ArrayList<>();
            for (SectionItem item : items) {
                values.add(listText(item));
            }
            Paint body = col.style.body;
            if (perLine) {
                for (String value : values) {
                    paragraph(col, wrap(value, body, col.width), body, 0f);
                    col.y += 2f;
                }
            } else {
                String joined = join(SEPARATOR, values.toArray(new String[0]));
                paragraph(col, wrap(joined, body, col.width), body, 0f);
            }
            col.y += t.entryGap;
            return;
        }
        for (SectionItem item : items) {
            renderEntry(col, item);
        }
    }

    private List<SectionItem> visibleItems(SectionType type) {
        List<SectionItem> visible = new ArrayList<>();
        for (SectionItem item : resume.itemsOf(type)) {
            if (!item.isBlank()) {
                visible.add(item);
            }
        }
        return visible;
    }

    private String listText(SectionItem item) {
        if (item.type == SectionType.LANGUAGES) {
            String proficiency = item.get("proficiency");
            return proficiency.isEmpty() ? item.get("language")
                    : item.get("language") + " (" + proficiency + ")";
        }
        return item.get("name");
    }

    private void renderEntry(Column col, SectionItem item) {
        switch (item.type) {
            case WORK_EXPERIENCE:
                entry(col, item.get("job_title"), dates(item),
                        join(", ", item.get("employer"), item.get("location")), "",
                        bulletLines(item.get("bullets")));
                break;
            case EDUCATION: {
                String degree = item.get("degree");
                String field = item.get("field_of_study");
                String title = field.isEmpty() ? degree
                        : (degree.isEmpty() ? field : degree + " in " + field);
                String grade = item.get("grade").isEmpty() ? "" : "Grade: " + item.get("grade");
                entry(col, title, dates(item),
                        join(", ", item.get("institution"), item.get("location")),
                        join("\n", grade, item.get("description")), NO_BULLETS);
                break;
            }
            case PROJECTS:
                entry(col, item.get("name"), dates(item),
                        join(DOT, item.get("role"), item.get("link")),
                        item.get("description"), NO_BULLETS);
                break;
            case CERTIFICATIONS: {
                String expiry = item.get("expiry_date").isEmpty() ? ""
                        : "Expires " + DateFormatter.formatMonth(item.get("expiry_date"));
                entry(col, item.get("name"), DateFormatter.formatMonth(item.get("issue_date")),
                        join(DOT, item.get("issuer"), item.get("credential"), expiry),
                        "", NO_BULLETS);
                break;
            }
            case AWARDS:
                entry(col, item.get("title"), DateFormatter.formatMonth(item.get("date")),
                        item.get("issuer"), item.get("description"), NO_BULLETS);
                break;
            case VOLUNTEER:
                entry(col, item.get("role"), dates(item),
                        join(", ", item.get("organization"), item.get("location")),
                        item.get("description"), NO_BULLETS);
                break;
            default:
                break;
        }
    }

    private static String dates(SectionItem item) {
        return DateFormatter.range(item.get("start_date"), item.get("end_date"),
                item.isChecked("current"));
    }

    private static List<String> bulletLines(String raw) {
        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            String cleaned = line.trim().replaceFirst("^[-*\u2022]+\\s*", "");
            if (!cleaned.isEmpty()) {
                lines.add(cleaned);
            }
        }
        return lines;
    }

    // ------------------------------------------------------------ drawing blocks

    private void sectionHeading(Column col, String title, float firstBlockHeight) {
        Paint hp = col.style.heading;
        float h = hp.descent() - hp.ascent();
        ensure(col, t.sectionGap + h + HEADING_PAD + firstBlockHeight);
        if (col.y > t.marginTop + 0.5f) {
            col.y += t.sectionGap;
        }
        String label = title.toUpperCase(Locale.ROOT);
        float x = col.x;
        int color = col.style.headingColor;

        switch (t.headingStyle) {
            case BAR:
                rect(col.page, x, col.y + 1f, x + 4f, col.y + h - 1f, color);
                text(col, label, x + 10f, hp);
                col.y += h + HEADING_PAD;
                break;
            case TINT:
                rect(col.page, x, col.y - 2f, x + col.width, col.y + h + 2f, tint(color));
                text(col, label, x + 6f, hp);
                col.y += h + 2f + HEADING_PAD;
                break;
            case SHORT_UNDERLINE:
                text(col, label, x, hp);
                col.y += h + 1f;
                line(col.page, x, col.y, x + 28f, col.y, color, 2f);
                col.y += HEADING_PAD;
                break;
            default:
                text(col, label, x, hp);
                col.y += h + 1f;
                line(col.page, x, col.y, x + col.width, col.y, color, 0.8f);
                col.y += HEADING_PAD;
                break;
        }
    }

    /** One entry: title with date on the right, italic sub line, then bullets or text. */
    private void entry(Column col, String title, String right, String sub,
                       String paragraphText, List<String> bullets) {
        Style s = col.style;
        String heading = title;
        String subline = sub;
        String date = right;
        if (heading.isEmpty()) {
            heading = subline;
            subline = "";
        }
        if (heading.isEmpty()) {
            heading = date;
            date = "";
        }
        if (heading.isEmpty() && paragraphText.isEmpty() && bullets.isEmpty()) {
            return;
        }

        float dateWidth = date.isEmpty() ? 0f : s.date.measureText(date);
        float titleWidth = col.width - (dateWidth > 0f ? dateWidth + 12f : 0f);
        List<String> titleLines = wrap(heading, s.bold, titleWidth);
        List<String> subLines = wrap(subline, s.sub, col.width);
        List<String> paragraphLines = wrap(paragraphText, s.body, col.width);

        // Keep the title, sub line and the first body line together on one page.
        float boldAdv = advance(s.bold);
        ensure(col, boldAdv + (subLines.isEmpty() ? 0f : advance(s.sub)) + advance(s.body));

        for (int i = 0; i < titleLines.size(); i++) {
            ensure(col, boldAdv);
            float baseline = col.y - s.bold.ascent();
            addText(col.page, titleLines.get(i), col.x, baseline, s.bold);
            if (i == 0 && dateWidth > 0f) {
                addText(col.page, date, col.x + col.width - dateWidth, baseline, s.date);
            }
            col.y += boldAdv;
        }

        paragraph(col, subLines, s.sub, 0f);

        float bodyAdv = advance(s.body);
        for (String bullet : bullets) {
            List<String> lines = wrap(bullet, s.body, col.width - BULLET_INDENT);
            for (int i = 0; i < lines.size(); i++) {
                ensure(col, bodyAdv);
                if (i == 0) {
                    text(col, BULLET, col.x + 2f, s.body);
                }
                text(col, lines.get(i), col.x + BULLET_INDENT, s.body);
                col.y += bodyAdv;
            }
        }

        paragraph(col, paragraphLines, s.body, 0f);
        col.y += t.entryGap;
    }

    private void paragraph(Column col, List<String> lines, Paint p, float indent) {
        float adv = advance(p);
        for (String line : lines) {
            ensure(col, adv);
            text(col, line, col.x + indent, p);
            col.y += adv;
        }
    }

    private float firstBlock(Column col) {
        return advance(col.style.bold) + advance(col.style.body);
    }

    /** Starts a new page when the next block of this height would run past the bottom margin. */
    private void ensure(Column col, float height) {
        float limit = PAGE_HEIGHT - t.marginBottom;
        boolean atTop = col.y <= t.marginTop + 0.5f;
        if (!atTop && col.y + height > limit) {
            col.page++;
            col.y = t.marginTop;
        }
    }

    // ------------------------------------------------------------- draw commands

    private void text(Column col, String s, float x, Paint p) {
        addText(col.page, s, x, col.y - p.ascent(), p);
    }

    private void addText(int page, String s, float x, float baseline, Paint p) {
        result.recordText(s);
        result.page(page).add(c -> c.drawText(s, x, baseline, p));
    }

    private void rect(int page, float left, float top, float right, float bottom, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        result.page(page).add(c -> c.drawRect(left, top, right, bottom, p));
    }

    private void line(int page, float x1, float y1, float x2, float y2, int color, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        result.page(page).add(c -> c.drawLine(x1, y1, x2, y2, p));
    }

    // ------------------------------------------------------------------ helpers

    private float advance(Paint p) {
        return (p.descent() - p.ascent()) * t.lineSpacing;
    }

    private static int tint(int color) {
        return (color & 0x00FFFFFF) | 0x26000000;
    }

    private static Paint paint(Typeface base, int style, float size, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create(base, style));
        p.setTextSize(size);
        p.setColor(color);
        return p;
    }

    private static String join(String separator, String... parts) {
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

    /** Splits text into lines no wider than maxWidth. Very long words are broken by character. */
    private static List<String> wrap(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        for (String paragraph : text.split("\\r?\\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : trimmed.split("\\s+")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (paint.measureText(candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                String rest = word;
                while (paint.measureText(rest) > maxWidth) {
                    int count = paint.breakText(rest, true, maxWidth, null);
                    if (count <= 0) {
                        count = 1;
                    }
                    lines.add(rest.substring(0, count));
                    rest = rest.substring(count);
                }
                current.append(rest);
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }
}