package com.example.resumebuilder.pdf;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.render.RenderedResume;
import com.example.resumebuilder.render.ResumeRenderer;
import com.example.resumebuilder.render.Template;
import com.example.resumebuilder.util.FileNames;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes a resume as a PDF into the public Downloads folder using MediaStore. No storage
 * permission is needed because the app only creates its own file. Call it from a
 * background thread.
 */
public final class PdfExporter {

    private static final String MIME_PDF = "application/pdf";

    private PdfExporter() {
    }

    /**
     * Exports the resume with the given template and returns the content URI of the new file.
     * MediaStore adds " (1)", " (2)" and so on when a file with the same name already exists.
     *
     * @throws IOException when the file cannot be created or written; no partial file is kept
     */
    public static Uri export(Context context, Resume resume, Template template)
            throws IOException {
        RenderedResume rendered = ResumeRenderer.render(resume, template);
        PdfDocument document = new PdfDocument();
        try {
            for (int i = 0; i < rendered.getPageCount(); i++) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                        rendered.pageWidth, rendered.pageHeight, i + 1).create();
                PdfDocument.Page page = document.startPage(info);
                rendered.drawPage(i, page.getCanvas());
                document.finishPage(page);
            }
            String fileName = FileNames.forResume(resume.fullName, resume.title);
            return save(context.getApplicationContext(), document, fileName);
        } finally {
            document.close();
        }
    }

    private static Uri save(Context context, PdfDocument document, String fileName)
            throws IOException {
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, MIME_PDF);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri uri = resolver.insert(collection, values);
        if (uri == null) {
            throw new IOException("Could not create the file in Downloads");
        }

        try {
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) {
                    throw new IOException("Could not open the file for writing");
                }
                document.writeTo(out);
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
            return uri;
        } catch (IOException | RuntimeException e) {
            resolver.delete(uri, null, null);
            throw e;
        }
    }
}