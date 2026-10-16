package com.example.resumebuilder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FileNamesTest {

    @Test
    public void usesFullNameWithResumeSuffix() {
        assertEquals("Rahul_Sharma_Resume.pdf", FileNames.forResume("Rahul Sharma", "My CV"));
    }

    @Test
    public void fallsBackToTitleThenDefault() {
        assertEquals("Android_Dev_2026.pdf", FileNames.forResume("", "Android Dev 2026"));
        assertEquals("Resume.pdf", FileNames.forResume("", ""));
        assertEquals("Resume.pdf", FileNames.forResume(null, null));
        assertEquals("Resume.pdf", FileNames.forResume("???", "///"));
    }

    @Test
    public void removesPathAndSpecialCharacters() {
        assertEquals("evil_name_Resume.pdf", FileNames.forResume("  ../../evil/name?.  ", ""));
        assertEquals("a_b_Resume.pdf", FileNames.forResume("a\\b:\"*", ""));
    }

    @Test
    public void limitsLength() {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append('a');
        }
        String result = FileNames.forResume(longName.toString(), "");
        assertEquals(60 + "_Resume.pdf".length(), result.length());
        assertTrue(result.endsWith("_Resume.pdf"));
    }

    @Test
    public void keepsNonLatinLettersWithTheirMarks() {
        assertEquals("\u0930\u093E\u0939\u0941\u0932_\u0936\u0930\u094D\u092E\u093E_Resume.pdf",
                FileNames.forResume("\u0930\u093E\u0939\u0941\u0932 \u0936\u0930\u094D\u092E\u093E", ""));
    }
}