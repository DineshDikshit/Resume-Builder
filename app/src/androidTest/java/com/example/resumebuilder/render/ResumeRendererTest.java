package com.example.resumebuilder.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResumeRendererTest {

    private static Resume longResume() {
        Resume resume = new Resume();
        resume.fullName = "Rahul Sharma";
        resume.professionalTitle = "Android Developer";
        resume.email = "rahul@example.com";
        resume.phone = "+91 98765 43210";
        resume.city = "Surat";
        resume.country = "India";
        resume.website = "example.com";
        resume.summary = "Passionate developer who builds reliable offline-first apps. "
                + "Enjoys clean code, careful testing and clear documentation. "
                + "Always learning new tools and improving existing products.";

        for (int i = 0; i < 25; i++) {
            SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
            job.put("job_title", "Developer " + i);
            job.put("employer", "Company " + i);
            job.put("location", "Surat");
            job.put("start_date", "2020-01");
            job.put("end_date", "2021-06");
            job.setChecked("current", i == 0);
            job.put("bullets", "Built a feature with a very long description that has to wrap "
                    + "across several lines of the page without being clipped or lost\n"
                    + "Second bullet point");
            resume.items.add(job);
        }
        for (int i = 0; i < 30; i++) {
            SectionItem skill = new SectionItem(SectionType.SKILLS);
            skill.put("name", "Skill " + i);
            resume.items.add(skill);
        }
        SectionItem language = new SectionItem(SectionType.LANGUAGES);
        language.put("language", "English");
        language.put("proficiency", "Fluent");
        resume.items.add(language);

        SectionItem education = new SectionItem(SectionType.EDUCATION);
        education.put("institution", "Sarvajanik University");
        education.put("degree", "MCA");
        education.put("field_of_study", "Computer Applications");
        education.put("grade", "8.5 CGPA");
        resume.items.add(education);
        return resume;
    }

    private static boolean hasInk(RenderedResume rendered, int page) {
        Bitmap bitmap = Bitmap.createBitmap(rendered.pageWidth, rendered.pageHeight,
                Bitmap.Config.ARGB_8888);
        rendered.drawPage(page, new Canvas(bitmap));
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        bitmap.recycle();
        for (int pixel : pixels) {
            if (pixel != Color.WHITE) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void everyTemplateFlowsOntoMultiplePagesWithoutLosingContent() {
        for (Template template : Templates.all()) {
            RenderedResume rendered = ResumeRenderer.render(longResume(), template);
            assertTrue(template.id + " should need more than one page",
                    rendered.getPageCount() > 1);
            for (int page = 0; page < rendered.getPageCount(); page++) {
                assertTrue(template.id + " page " + page + " is blank", hasInk(rendered, page));
            }
            String text = rendered.getPlainText();
            assertTrue(template.id, text.contains("Rahul Sharma"));
            assertTrue(template.id, text.contains("Developer 24"));
            assertTrue(template.id, text.contains("Company 24"));
            assertTrue(template.id, text.contains("Skill 29") || text.contains("Skill 28"));
            assertTrue(template.id, text.contains("Sarvajanik University"));
        }
    }

    @Test
    public void shortResumeFitsOnOnePage() {
        Resume resume = new Resume();
        resume.fullName = "Short Resume";
        resume.professionalTitle = "Developer";
        SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
        job.put("job_title", "Developer");
        job.put("employer", "Acme");
        job.put("bullets", "Built things\nFixed things");
        resume.items.add(job);
        for (Template template : Templates.all()) {
            assertEquals(template.id, 1, ResumeRenderer.render(resume, template).getPageCount());
        }
    }

    @Test
    public void emptyResumeStillProducesOnePage() {
        for (Template template : Templates.all()) {
            RenderedResume rendered = ResumeRenderer.render(new Resume(), template);
            assertEquals(1, rendered.getPageCount());
            assertTrue(hasInk(rendered, 0));
        }
    }

    @Test
    public void veryLongUnbrokenWordIsWrappedNotClipped() {
        StringBuilder longWord = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            longWord.append('A');
        }
        Resume resume = new Resume();
        resume.fullName = "Test";
        SectionItem skill = new SectionItem(SectionType.SKILLS);
        skill.put("name", longWord.toString());
        resume.items.add(skill);

        for (Template template : Templates.all()) {
            String text = ResumeRenderer.render(resume, template).getPlainText()
                    .replaceAll("\\s+", "");
            assertTrue(template.id, text.contains(longWord.toString()));
        }
    }
}