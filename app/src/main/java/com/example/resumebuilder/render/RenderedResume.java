package com.example.resumebuilder.render;

import android.graphics.Canvas;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * The laid-out pages of one resume. The same drawing commands are used for the on-screen
 * preview (drawn onto a Bitmap canvas) and for the PDF (drawn onto a PdfDocument page).
 */
public final class RenderedResume {

    interface Op {
        void draw(Canvas canvas);
    }

    static final class Page {
        private final List<Op> ops = new ArrayList<>();

        void add(Op op) {
            ops.add(op);
        }

        void addFirst(Op op) {
            ops.add(0, op);
        }
    }

    public final int pageWidth;
    public final int pageHeight;

    private final List<Page> pages = new ArrayList<>();
    private final StringBuilder plainText = new StringBuilder();

    RenderedResume(int pageWidth, int pageHeight) {
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
    }

    /** Returns the page with this index, creating missing pages. */
    Page page(int index) {
        while (pages.size() <= index) {
            pages.add(new Page());
        }
        return pages.get(index);
    }

    void recordText(String text) {
        plainText.append(text).append('\n');
    }

    public int getPageCount() {
        return pages.size();
    }

    /** All text that was drawn, one line per row. Useful for accessibility and tests. */
    public String getPlainText() {
        return plainText.toString();
    }

    /** Draws one page onto a canvas whose units are the page size (595 x 842 points). */
    public void drawPage(int index, Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        if (index < 0 || index >= pages.size()) {
            return;
        }
        for (Op op : pages.get(index).ops) {
            op.draw(canvas);
        }
    }
}