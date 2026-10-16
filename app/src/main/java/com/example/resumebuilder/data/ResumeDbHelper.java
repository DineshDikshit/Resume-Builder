package com.example.resumebuilder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Owns the on-device SQLite database. The database is never uploaded anywhere.
 */
public class ResumeDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "resume_builder.db";
    public static final int DB_VERSION = 2;

    public static final String TABLE_RESUMES = "resumes";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_TEMPLATE_ID = "template_id";
    public static final String COL_FULL_NAME = "full_name";
    public static final String COL_PROFESSIONAL_TITLE = "professional_title";
    public static final String COL_EMAIL = "email";
    public static final String COL_PHONE = "phone";
    public static final String COL_CITY = "city";
    public static final String COL_REGION = "region";
    public static final String COL_COUNTRY = "country";
    public static final String COL_WEBSITE = "website";
    public static final String COL_PROFILE_LINK = "profile_link";
    public static final String COL_SUMMARY = "summary";
    /** Legacy column from version 1. No longer used, but kept because SQLite cannot drop it everywhere. */
    public static final String COL_INCLUDE_REFERENCES = "include_references";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at";

    public static final String TABLE_ITEMS = "section_items";
    public static final String COL_RESUME_ID = "resume_id";
    public static final String COL_SECTION_TYPE = "section_type";
    public static final String COL_POSITION = "position";
    public static final String COL_DATA = "data";

    private static ResumeDbHelper instance;

    /** Returns the single shared helper. Always uses the application context. */
    public static synchronized ResumeDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ResumeDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private ResumeDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        // A single database file (no -wal/-shm files) keeps the backup copy simple and consistent.
        setWriteAheadLoggingEnabled(false);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_RESUMES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL DEFAULT '', "
                + COL_TEMPLATE_ID + " TEXT NOT NULL DEFAULT 'classic', "
                + COL_FULL_NAME + " TEXT NOT NULL DEFAULT '', "
                + COL_PROFESSIONAL_TITLE + " TEXT NOT NULL DEFAULT '', "
                + COL_EMAIL + " TEXT NOT NULL DEFAULT '', "
                + COL_PHONE + " TEXT NOT NULL DEFAULT '', "
                + COL_CITY + " TEXT NOT NULL DEFAULT '', "
                + COL_REGION + " TEXT NOT NULL DEFAULT '', "
                + COL_COUNTRY + " TEXT NOT NULL DEFAULT '', "
                + COL_WEBSITE + " TEXT NOT NULL DEFAULT '', "
                + COL_PROFILE_LINK + " TEXT NOT NULL DEFAULT '', "
                + COL_SUMMARY + " TEXT NOT NULL DEFAULT '', "
                + COL_INCLUDE_REFERENCES + " INTEGER NOT NULL DEFAULT 0, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_ITEMS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RESUME_ID + " INTEGER NOT NULL REFERENCES " + TABLE_RESUMES
                + "(" + COL_ID + ") ON DELETE CASCADE, "
                + COL_SECTION_TYPE + " TEXT NOT NULL, "
                + COL_POSITION + " INTEGER NOT NULL, "
                + COL_DATA + " TEXT NOT NULL)");

        db.execSQL("CREATE INDEX idx_items_resume_section ON " + TABLE_ITEMS
                + " (" + COL_RESUME_ID + ", " + COL_SECTION_TYPE + ", " + COL_POSITION + ")");
    }

    /**
     * Upgrades run one version at a time so any old database reaches the newest schema.
     * Future versions add a new case below. Upgrades never drop tables.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int version = oldVersion + 1; version <= newVersion; version++) {
            upgradeToVersion(db, version);
        }
    }

    private void upgradeToVersion(SQLiteDatabase db, int version) {
        switch (version) {
            case 2:
                // The References section was removed from the app, so its leftover rows are
                // deleted instead of staying hidden in the database and in backups.
                db.delete(TABLE_ITEMS, COL_SECTION_TYPE + " = ?", new String[]{"references"});
                break;
            default:
                break;
        }
    }
}