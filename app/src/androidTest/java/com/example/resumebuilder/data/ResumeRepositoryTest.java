package com.example.resumebuilder.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ResumeRepositoryTest {

    private Context context;
    private ResumeRepository repository;
    private final List<Long> createdIds = new ArrayList<>();

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        repository = new ResumeRepository(context);
    }

    @After
    public void tearDown() {
        for (long id : createdIds) {
            repository.deleteResume(id);
        }
    }

    private long newResume(String name) {
        Resume resume = new Resume();
        resume.title = "Test " + name;
        resume.fullName = name;
        long id = repository.createResume(resume);
        createdIds.add(id);
        return id;
    }

    private SectionItem skill(String name) {
        SectionItem item = new SectionItem(SectionType.SKILLS);
        item.put("name", name);
        return item;
    }

    @Test
    public void resumeDetailsArePersisted() {
        long id = newResume("Rahul");
        Resume resume = repository.getResume(id);
        assertNotNull(resume);
        resume.professionalTitle = "Android Developer";
        resume.city = "Surat";
        resume.templateId = "modern";
        repository.updateResume(resume);

        Resume loaded = repository.getResume(id);
        assertEquals("Rahul", loaded.fullName);
        assertEquals("Android Developer", loaded.professionalTitle);
        assertEquals("Surat", loaded.city);
        assertEquals("modern", loaded.templateId);
    }

    @Test
    public void itemsKeepOrderAndCanBeMovedAndDeleted() {
        long id = newResume("Order");
        SectionItem a = skill("A");
        SectionItem b = skill("B");
        SectionItem c = skill("C");
        repository.addItem(id, a);
        repository.addItem(id, b);
        repository.addItem(id, c);

        List<SectionItem> skills = repository.getResume(id).itemsOf(SectionType.SKILLS);
        assertEquals(3, skills.size());
        assertEquals("A", skills.get(0).get("name"));
        assertEquals("C", skills.get(2).get("name"));

        repository.moveItem(id, SectionType.SKILLS, b.id, -1);
        skills = repository.getResume(id).itemsOf(SectionType.SKILLS);
        assertEquals("B", skills.get(0).get("name"));
        assertEquals("A", skills.get(1).get("name"));

        repository.deleteItem(id, SectionType.SKILLS, b.id);
        skills = repository.getResume(id).itemsOf(SectionType.SKILLS);
        assertEquals(2, skills.size());
        assertEquals("A", skills.get(0).get("name"));
        assertEquals(0, skills.get(0).position);
        assertEquals("C", skills.get(1).get("name"));
        assertEquals(1, skills.get(1).position);
    }

    @Test
    public void itemEditsArePersisted() {
        long id = newResume("Edit");
        SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
        job.put("job_title", "Developer");
        job.put("bullets", "Built app\nFixed bugs");
        job.setChecked("current", true);
        repository.addItem(id, job);

        job.put("job_title", "Senior Developer");
        repository.updateItem(job);

        SectionItem loaded = repository.getResume(id)
                .itemsOf(SectionType.WORK_EXPERIENCE).get(0);
        assertEquals("Senior Developer", loaded.get("job_title"));
        assertEquals("Built app\nFixed bugs", loaded.get("bullets"));
        assertTrue(loaded.isChecked("current"));
    }

    @Test
    public void duplicateCopiesEntries() {
        long id = newResume("Original");
        repository.addItem(id, skill("Java"));

        long copyId = repository.duplicateResume(id);
        createdIds.add(copyId);

        assertNotEquals(id, copyId);
        Resume copy = repository.getResume(copyId);
        assertTrue(copy.title.endsWith("(copy)"));
        assertEquals(1, copy.itemsOf(SectionType.SKILLS).size());
        assertEquals("Java", copy.itemsOf(SectionType.SKILLS).get(0).get("name"));
    }

    @Test
    public void deletingResumeRemovesItsEntries() {
        long id = newResume("Delete");
        repository.addItem(id, skill("Kotlin"));
        repository.deleteResume(id);

        assertNull(repository.getResume(id));
        long remaining = DatabaseUtils.queryNumEntries(
                ResumeDbHelper.getInstance(context).getReadableDatabase(),
                ResumeDbHelper.TABLE_ITEMS, "resume_id = ?",
                new String[]{String.valueOf(id)});
        assertEquals(0, remaining);
    }

    @Test
    public void specialCharactersAreStoredSafely() {
        String tricky = "Robert'); DROP TABLE resumes;-- \"quotes\" and \\ backslash";
        long id = newResume(tricky);
        assertEquals(tricky, repository.getResume(id).fullName);
        assertNotNull(repository.listResumes());
    }

    @Test
    public void rowsOfUnknownSectionsAreIgnoredWhenLoading() {
        long id = newResume("Unknown");
        SQLiteDatabase db = ResumeDbHelper.getInstance(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ResumeDbHelper.COL_RESUME_ID, id);
        values.put(ResumeDbHelper.COL_SECTION_TYPE, "references");
        values.put(ResumeDbHelper.COL_POSITION, 0);
        values.put(ResumeDbHelper.COL_DATA, "{\"name\":\"Someone\"}");
        db.insertOrThrow(ResumeDbHelper.TABLE_ITEMS, null, values);

        Resume loaded = repository.getResume(id);
        assertNotNull(loaded);
        assertEquals(0, loaded.items.size());
    }
}