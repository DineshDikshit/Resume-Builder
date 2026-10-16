package com.example.resumebuilder.render;

import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;

/** Fictional example content used only to draw template previews. It is never saved. */
public final class SampleResume {

    private SampleResume() {
    }

    public static Resume create() {
        Resume resume = new Resume();
        resume.title = "Sample";
        resume.fullName = "Alex Morgan";
        resume.professionalTitle = "Software Developer";
        resume.email = "alex.morgan@example.com";
        resume.phone = "+1 555 0100";
        resume.city = "Austin";
        resume.country = "USA";
        resume.website = "alexmorgan.dev";
        resume.summary = "Developer with five years of experience building reliable, "
                + "user-friendly mobile and web applications.";

        SectionItem job1 = item(SectionType.WORK_EXPERIENCE,
                "job_title", "Senior Developer", "employer", "Northwind Labs",
                "location", "Austin", "start_date", "2022-03",
                "bullets", "Led a team of four engineers\nCut app start-up time by 40%\n"
                        + "Introduced automated testing");
        job1.setChecked("current", true);
        resume.items.add(job1);
        resume.items.add(item(SectionType.WORK_EXPERIENCE,
                "job_title", "Developer", "employer", "Contoso Apps",
                "location", "Dallas", "start_date", "2019-06", "end_date", "2022-02",
                "bullets", "Built and shipped five mobile apps\nFixed critical crashes"));

        resume.items.add(item(SectionType.EDUCATION,
                "institution", "State University", "degree", "B.Sc.",
                "field_of_study", "Computer Science", "start_date", "2015-08",
                "end_date", "2019-05", "grade", "3.8 GPA"));

        String[] skills = {"Java", "SQL", "Git", "REST APIs", "Testing", "UI design"};
        for (String skill : skills) {
            resume.items.add(item(SectionType.SKILLS, "name", skill));
        }
        resume.items.add(item(SectionType.LANGUAGES,
                "language", "English", "proficiency", "Native"));
        resume.items.add(item(SectionType.LANGUAGES,
                "language", "Spanish", "proficiency", "Intermediate"));
        return resume;
    }

    private static SectionItem item(SectionType type, String... keyValuePairs) {
        SectionItem item = new SectionItem(type);
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            item.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return item;
    }
}