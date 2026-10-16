package com.example.resumebuilder.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;
import com.example.resumebuilder.render.ResumeRenderer;
import com.example.resumebuilder.render.Template;
import com.example.resumebuilder.render.Templates;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PdfExporterTest {

    private static Resume longResume() {
        Resume resume = new Resume();
        resume.fullName = "PdfExporter Test";
        resume.professionalTitle = "Android Developer";
        resume.email = "test@example.com";
        resume.summary = "Short summary for the export test.";
        for (int i = 0; i < 30; i++) {
            SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
            job.put("job_title", "Developer " + i);
            job.put("employer", "Company " + i);
            job.put("start_date", "2020-01");
            job.put("end_date", "2021-06");
            job.put("bullets", "First bullet with enough words to wrap across the page width "
                    + "when the text is long enough\nSecond bullet");
            resume.items.add(job);
        }
        return resume;
    }

    @Test
    public void exportsReadablePdfForEveryTemplate() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ContentResolver resolver = context.getContentResolver();

        for (Template template : Templates.all()) {
            Resume resume = longResume();
            Uri uri = PdfExporter.export(context, resume, template);
            try {
                try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r")) {
                    assertNotNull(pfd);
                    PdfRenderer renderer = new PdfRenderer(pfd);
                    int expected = ResumeRenderer.render(resume, template).getPageCount();
                    assertEquals(template.id, expected, renderer.getPageCount());
                    renderer.close();
                }
            } finally {
                resolver.delete(uri, null, null);
            }
        }
    }

    @Test
    public void exportingTwiceCreatesTwoSeparateFiles() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ContentResolver resolver = context.getContentResolver();
        Resume resume = longResume();
        Template template = Templates.byId("classic");

        Uri first = PdfExporter.export(context, resume, template);
        Uri second = PdfExporter.export(context, resume, template);
        try {
            assertNotEquals(first, second);
        } finally {
            resolver.delete(first, null, null);
            resolver.delete(second, null, null);
        }
    }
}