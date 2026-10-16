package com.example.resumebuilder.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ContactValidatorTest {

    @Test
    public void acceptsNormalEmails() {
        assertTrue(ContactValidator.isValidEmail("rahul@example.com"));
        assertTrue(ContactValidator.isValidEmail("first.last+tag@mail.example.co.in"));
        assertTrue(ContactValidator.isValidEmail(""));
        assertTrue(ContactValidator.isValidEmail(null));
    }

    @Test
    public void rejectsBrokenEmails() {
        assertFalse(ContactValidator.isValidEmail("No"));
        assertFalse(ContactValidator.isValidEmail("a@b"));
        assertFalse(ContactValidator.isValidEmail("@example.com"));
        assertFalse(ContactValidator.isValidEmail("a b@example.com"));
        assertFalse(ContactValidator.isValidEmail("a@example."));
    }

    @Test
    public void acceptsNormalPhones() {
        assertTrue(ContactValidator.isValidPhone("9106161211"));
        assertTrue(ContactValidator.isValidPhone("+91 98765 43210"));
        assertTrue(ContactValidator.isValidPhone("(555) 010-0100"));
        assertTrue(ContactValidator.isValidPhone(""));
        assertTrue(ContactValidator.isValidPhone(null));
    }

    @Test
    public void rejectsBrokenPhones() {
        assertFalse(ContactValidator.isValidPhone("Ghhj"));
        assertFalse(ContactValidator.isValidPhone("123"));
        assertFalse(ContactValidator.isValidPhone("12345678901234567"));
        assertFalse(ContactValidator.isValidPhone("98765+43210"));
        assertFalse(ContactValidator.isValidPhone("98765abc43210"));
    }
}