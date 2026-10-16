package com.example.resumebuilder.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ResumeModelTest {

    @Test
    public void locationLineSkipsBlankParts() {
        Resume resume = new Resume();
        resume.city = "Surat";
        resume.region = "   ";
        resume.country = "India";
        assertEquals("Surat, India", resume.locationLine());
    }

    @Test
    public void contactPartsSkipsEmptyValues() {
        Resume resume = new Resume();
        resume.email = "rahul@example.com";
        resume.website = "example.com";
        assertEquals(2, resume.contactParts().size());
        assertEquals("rahul@example.com", resume.contactParts().get(0));
    }

    @Test
    public void itemsOfReturnsOnlyRequestedSection() {
        Resume resume = new Resume();
        SectionItem skill = new SectionItem(SectionType.SKILLS);
        skill.put("name", "Java");
        SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
        job.put("job_title", "Developer");
        resume.items.add(skill);
        resume.items.add(job);

        assertEquals(1, resume.itemsOf(SectionType.SKILLS).size());
        assertEquals(1, resume.itemsOf(SectionType.WORK_EXPERIENCE).size());
        assertEquals(0, resume.itemsOf(SectionType.AWARDS).size());
    }

    @Test
    public void sectionKeysAreUniqueAndResolvable() {
        Set<String> seen = new HashSet<>();
        for (SectionType type : SectionType.values()) {
            assertTrue("Duplicate key " + type.key, seen.add(type.key));
            assertEquals(type, SectionType.fromKey(type.key));
        }
        assertNull(SectionType.fromKey("does_not_exist"));
    }

    @Test
    public void primaryAndSecondaryKeysExistInFieldList() {
        for (SectionType type : SectionType.values()) {
            assertTrue(hasField(type, type.primaryKey));
            if (!type.secondaryKey.isEmpty()) {
                assertTrue(hasField(type, type.secondaryKey));
            }
        }
    }

    @Test
    public void sectionItemTreatsCheckboxOnlyEntryAsBlank() {
        SectionItem item = new SectionItem(SectionType.WORK_EXPERIENCE);
        item.setChecked("current", true);
        assertTrue(item.isBlank());
        item.put("job_title", "Engineer");
        assertFalse(item.isBlank());
        assertNotNull(item.copy());
        assertEquals("Engineer", item.copy().get("job_title"));
        assertEquals(0L, item.copy().id);
    }

    private static boolean hasField(SectionType type, String key) {
        for (FieldSpec spec : type.fields) {
            if (spec.key.equals(key)) {
                return true;
            }
        }
        return false;
    }
}