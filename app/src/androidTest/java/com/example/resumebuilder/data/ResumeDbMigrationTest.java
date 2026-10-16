package com.example.resumebuilder.data;

import static org.junit.Assert.assertEquals;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResumeDbMigrationTest {

    private static void insertItem(SQLiteDatabase db, long resumeId, String sectionType) {
        ContentValues values = new ContentValues();
        values.put(ResumeDbHelper.COL_RESUME_ID, resumeId);
        values.put(ResumeDbHelper.COL_SECTION_TYPE, sectionType);
        values.put(ResumeDbHelper.COL_POSITION, 0);
        values.put(ResumeDbHelper.COL_DATA, "{}");
        db.insertOrThrow(ResumeDbHelper.TABLE_ITEMS, null, values);
    }

    private static long count(SQLiteDatabase db, String sectionType) {
        return DatabaseUtils.queryNumEntries(db, ResumeDbHelper.TABLE_ITEMS,
                ResumeDbHelper.COL_SECTION_TYPE + " = ?", new String[]{sectionType});
    }

    @Test
    public void upgradeToVersion2RemovesLeftoverReferenceRowsOnly() {
        Context context = ApplicationProvider.getApplicationContext();
        ResumeDbHelper helper = ResumeDbHelper.getInstance(context);
        SQLiteDatabase db = SQLiteDatabase.createInMemory(
                new SQLiteDatabase.OpenParams.Builder().build());
        try {
            helper.onCreate(db);

            ContentValues resume = new ContentValues();
            resume.put(ResumeDbHelper.COL_CREATED_AT, 1L);
            resume.put(ResumeDbHelper.COL_UPDATED_AT, 1L);
            long resumeId = db.insertOrThrow(ResumeDbHelper.TABLE_RESUMES, null, resume);
            insertItem(db, resumeId, "references");
            insertItem(db, resumeId, "skills");

            helper.onUpgrade(db, 1, 2);

            assertEquals(0, count(db, "references"));
            assertEquals(1, count(db, "skills"));
            assertEquals(1, DatabaseUtils.queryNumEntries(db, ResumeDbHelper.TABLE_RESUMES));
        } finally {
            db.close();
        }
        assertEquals(2, ResumeDbHelper.DB_VERSION);
    }
}