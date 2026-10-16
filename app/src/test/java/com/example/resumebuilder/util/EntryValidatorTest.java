package com.example.resumebuilder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;

import org.junit.Test;

import java.util.Map;

public class EntryValidatorTest {

    // Fixed "today" (October 2026) keeps the tests independent of the real date.
    private static Map<String, EntryValidator.Error> check(SectionItem item) {
        return EntryValidator.validate(item, 2026, 10);
    }

    private static SectionItem validJob() {
        SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
        job.put("job_title", "Developer");
        job.put("employer", "Acme");
        job.put("start_date", "2020-01");
        job.put("end_date", "2021-06");
        return job;
    }

    @Test
    public void validEntryHasNoErrors() {
        assertTrue(check(validJob()).isEmpty());
    }

    @Test
    public void requiredFieldsAreReported() {
        SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
        Map<String, EntryValidator.Error> errors = check(job);
        assertEquals(EntryValidator.Error.REQUIRED, errors.get("job_title"));
        assertEquals(EntryValidator.Error.REQUIRED, errors.get("employer"));
        assertFalse(errors.containsKey("location"));
    }

    @Test
    public void optionalFieldsMayStayEmpty() {
        SectionItem job = new SectionItem(SectionType.WORK_EXPERIENCE);
        job.put("job_title", "Developer");
        job.put("employer", "Acme");
        assertTrue(check(job).isEmpty());
    }

    @Test
    public void badDateFormatIsReported() {
        SectionItem job = validJob();
        job.put("start_date", "June 2020");
        assertEquals(EntryValidator.Error.DATE_FORMAT, check(job).get("start_date"));
    }

    @Test
    public void endBeforeStartIsReported() {
        SectionItem job = validJob();
        job.put("start_date", "2022-05");
        job.put("end_date", "2021-01");
        assertEquals(EntryValidator.Error.DATE_ORDER, check(job).get("end_date"));
    }

    @Test
    public void currentRoleIgnoresEndDate() {
        SectionItem job = validJob();
        job.put("start_date", "2022-05");
        job.put("end_date", "garbage");
        job.setChecked("current", true);
        assertTrue(check(job).isEmpty());
    }

    @Test
    public void startDateInTheFutureIsRejected() {
        SectionItem job = validJob();
        job.put("start_date", "2030-06");
        job.put("end_date", "");
        assertEquals(EntryValidator.Error.DATE_FUTURE, check(job).get("start_date"));
    }

    @Test
    public void currentMonthIsNotInTheFuture() {
        SectionItem job = validJob();
        job.put("start_date", "2026-10");
        job.put("end_date", "");
        assertTrue(check(job).isEmpty());
        job.put("start_date", "2026-11");
        assertEquals(EntryValidator.Error.DATE_FUTURE, check(job).get("start_date"));
    }

    @Test
    public void workEndDateInTheFutureIsRejected() {
        SectionItem job = validJob();
        job.put("end_date", "2045-12");
        assertEquals(EntryValidator.Error.DATE_FUTURE, check(job).get("end_date"));
    }

    @Test
    public void educationMayEndInTheFuture() {
        SectionItem education = new SectionItem(SectionType.EDUCATION);
        education.put("institution", "University");
        education.put("degree", "MCA");
        education.put("start_date", "2024-08");
        education.put("end_date", "2027-05");
        assertTrue(check(education).isEmpty());
    }

    @Test
    public void certificationExpiryMayBeInTheFuture() {
        SectionItem cert = new SectionItem(SectionType.CERTIFICATIONS);
        cert.put("name", "Android Associate");
        cert.put("issue_date", "2024-06");
        cert.put("expiry_date", "2029-06");
        assertTrue(check(cert).isEmpty());
        cert.put("issue_date", "2027-01");
        assertEquals(EntryValidator.Error.DATE_FUTURE, check(cert).get("issue_date"));
    }

    @Test
    public void certificationExpiryBeforeIssueIsReported() {
        SectionItem cert = new SectionItem(SectionType.CERTIFICATIONS);
        cert.put("name", "Android Associate");
        cert.put("issue_date", "2024-06");
        cert.put("expiry_date", "2023-01");
        assertEquals(EntryValidator.Error.DATE_ORDER, check(cert).get("expiry_date"));
    }

    @Test
    public void awardDateInTheFutureIsRejected() {
        SectionItem award = new SectionItem(SectionType.AWARDS);
        award.put("title", "Best Student");
        award.put("date", "2031-01");
        assertEquals(EntryValidator.Error.DATE_FUTURE, check(award).get("date"));
    }

    @Test
    public void skillNeedsAName() {
        SectionItem skill = new SectionItem(SectionType.SKILLS);
        assertEquals(EntryValidator.Error.REQUIRED, check(skill).get("name"));
        skill.put("name", "Java");
        assertTrue(check(skill).isEmpty());
    }
}