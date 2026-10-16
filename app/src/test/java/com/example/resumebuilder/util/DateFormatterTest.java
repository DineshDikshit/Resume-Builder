package com.example.resumebuilder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DateFormatterTest {

    @Test
    public void formatsValidMonths() {
        assertEquals("Jun 2024", DateFormatter.formatMonth("2024-06"));
        assertEquals("Jun 2024", DateFormatter.formatMonth("2024-6"));
        assertEquals("Dec 1999", DateFormatter.formatMonth(" 1999-12 "));
    }

    @Test
    public void leavesOtherTextUnchanged() {
        assertEquals("2024", DateFormatter.formatMonth("2024"));
        assertEquals("2024-13", DateFormatter.formatMonth("2024-13"));
        assertEquals("Spring 2020", DateFormatter.formatMonth("Spring 2020"));
        assertEquals("", DateFormatter.formatMonth(null));
        assertEquals("", DateFormatter.formatMonth("   "));
    }

    @Test
    public void buildsRanges() {
        assertEquals("Jan 2020 \u2013 Mar 2022", DateFormatter.range("2020-01", "2022-03", false));
        assertEquals("Jan 2020 \u2013 Present", DateFormatter.range("2020-01", "", true));
        assertEquals("Jan 2020 \u2013 Present", DateFormatter.range("2020-01", "2022-03", true));
        assertEquals("Mar 2022", DateFormatter.range("", "2022-03", false));
        assertEquals("Jan 2020", DateFormatter.range("2020-01", "", false));
        assertEquals("", DateFormatter.range("", "", false));
    }

    @Test
    public void validatesMonths() {
        assertTrue(DateFormatter.isValidOrBlank(""));
        assertTrue(DateFormatter.isValidOrBlank(null));
        assertTrue(DateFormatter.isValidOrBlank("2024-06"));
        assertFalse(DateFormatter.isValidOrBlank("2024-00"));
        assertFalse(DateFormatter.isValidOrBlank("2024-13"));
        assertFalse(DateFormatter.isValidOrBlank("June 2024"));
    }

    @Test
    public void checksDateOrder() {
        assertTrue(DateFormatter.isOrdered("2020-01", "2022-03"));
        assertTrue(DateFormatter.isOrdered("2020-01", "2020-01"));
        assertFalse(DateFormatter.isOrdered("2022-03", "2020-01"));
        assertTrue(DateFormatter.isOrdered("", "2020-01"));
        assertTrue(DateFormatter.isOrdered("2022-03", ""));
        assertTrue(DateFormatter.isOrdered("garbage", "2020-01"));
    }
}