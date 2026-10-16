package com.example.resumebuilder.data;

import static com.example.resumebuilder.data.ResumeDbHelper.COL_CITY;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_COUNTRY;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_CREATED_AT;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_DATA;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_EMAIL;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_FULL_NAME;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_ID;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_PHONE;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_POSITION;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_PROFESSIONAL_TITLE;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_PROFILE_LINK;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_REGION;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_RESUME_ID;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_SECTION_TYPE;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_SUMMARY;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_TEMPLATE_ID;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_TITLE;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_UPDATED_AT;
import static com.example.resumebuilder.data.ResumeDbHelper.COL_WEBSITE;
import static com.example.resumebuilder.data.ResumeDbHelper.TABLE_ITEMS;
import static com.example.resumebuilder.data.ResumeDbHelper.TABLE_RESUMES;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All reads and writes of resumes and their section entries. Every query uses bound
 * parameters; user text is never concatenated into SQL.
 */
public class ResumeRepository {

    private final ResumeDbHelper helper;

    public ResumeRepository(Context context) {
        this.helper = ResumeDbHelper.getInstance(context);
    }

    // ---------------------------------------------------------------- resumes

    /** Returns all resumes (without their entries), most recently edited first. */
    public List<Resume> listResumes() {
        List<Resume> result = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_RESUMES, null, null, null, null, null,
                COL_UPDATED_AT + " DESC, " + COL_ID + " DESC")) {
            while (cursor.moveToNext()) {
                result.add(readResume(cursor));
            }
        }
        return result;
    }

    /** Returns one resume including all of its entries, or null if it does not exist. */
    public Resume getResume(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Resume resume;
        try (Cursor cursor = db.query(TABLE_RESUMES, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            resume = readResume(cursor);
        }
        loadItems(db, resume);
        return resume;
    }

    /**
     * Inserts a new resume together with any entries already in {@code resume.items}.
     * Sets and returns the new id.
     */
    public long createResume(Resume resume) {
        long now = System.currentTimeMillis();
        resume.createdAt = now;
        resume.updatedAt = now;

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insertOrThrow(TABLE_RESUMES, null, toValues(resume));
            resume.id = id;
            for (SectionItem item : resume.items) {
                insertItem(db, id, item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return resume.id;
    }

    /** Saves the personal details and settings of a resume. Entries are saved separately. */
    public void updateResume(Resume resume) {
        resume.updatedAt = System.currentTimeMillis();
        SQLiteDatabase db = helper.getWritableDatabase();
        db.update(TABLE_RESUMES, toValues(resume), COL_ID + " = ?",
                new String[]{String.valueOf(resume.id)});
    }

    /** Creates a full copy of a resume and returns the new id, or -1 if the source is missing. */
    public long duplicateResume(long id) {
        Resume source = getResume(id);
        if (source == null) {
            return -1;
        }
        Resume copy = new Resume();
        copy.title = source.title.isEmpty() ? "Copy" : source.title + " (copy)";
        copy.templateId = source.templateId;
        copy.fullName = source.fullName;
        copy.professionalTitle = source.professionalTitle;
        copy.email = source.email;
        copy.phone = source.phone;
        copy.city = source.city;
        copy.region = source.region;
        copy.country = source.country;
        copy.website = source.website;
        copy.profileLink = source.profileLink;
        copy.summary = source.summary;
        for (SectionItem item : source.items) {
            copy.items.add(item.copy());
        }
        return createResume(copy);
    }

    /** Deletes a resume; its entries are removed by the foreign key cascade. */
    public void deleteResume(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_RESUMES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ------------------------------------------------------------ section items

    /** Appends an entry at the end of its section and sets its id. */
    public long addItem(long resumeId, SectionItem item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            item.position = (int) DatabaseUtils.longForQuery(db,
                    "SELECT COALESCE(MAX(" + COL_POSITION + "), -1) + 1 FROM " + TABLE_ITEMS
                            + " WHERE " + COL_RESUME_ID + " = ? AND " + COL_SECTION_TYPE + " = ?",
                    new String[]{String.valueOf(resumeId), item.type.key});
            insertItem(db, resumeId, item);
            touchResume(db, resumeId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return item.id;
    }

    /** Saves the changed field values of an existing entry. */
    public void updateItem(SectionItem item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COL_DATA, SectionItemCodec.toJson(item));
            db.update(TABLE_ITEMS, values, COL_ID + " = ?",
                    new String[]{String.valueOf(item.id)});
            touchResume(db, item.resumeId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Deletes an entry and closes the gap in the order of the remaining entries. */
    public void deleteItem(long resumeId, SectionType type, long itemId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_ITEMS, COL_ID + " = ?", new String[]{String.valueOf(itemId)});
            writePositions(db, orderedItemIds(db, resumeId, type));
            touchResume(db, resumeId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Moves an entry one place up (direction -1) or down (direction +1) inside its section.
     * Does nothing when the entry is already at that end.
     */
    public void moveItem(long resumeId, SectionType type, long itemId, int direction) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            List<Long> ids = orderedItemIds(db, resumeId, type);
            int index = ids.indexOf(itemId);
            int target = index + direction;
            if (index < 0 || target < 0 || target >= ids.size()) {
                return;
            }
            Collections.swap(ids, index, target);
            writePositions(db, ids);
            touchResume(db, resumeId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---------------------------------------------------------------- internals

    private void loadItems(SQLiteDatabase db, Resume resume) {
        try (Cursor cursor = db.query(TABLE_ITEMS, null, COL_RESUME_ID + " = ?",
                new String[]{String.valueOf(resume.id)}, null, null,
                COL_SECTION_TYPE + ", " + COL_POSITION + ", " + COL_ID)) {
            while (cursor.moveToNext()) {
                SectionType type = SectionType.fromKey(readString(cursor, COL_SECTION_TYPE));
                if (type == null) {
                    continue; // Unknown section (removed or from a newer app version): skip it.
                }
                SectionItem item = new SectionItem(type);
                item.id = readLong(cursor, COL_ID);
                item.resumeId = readLong(cursor, COL_RESUME_ID);
                item.position = (int) readLong(cursor, COL_POSITION);
                SectionItemCodec.fillFromJson(item, readString(cursor, COL_DATA));
                resume.items.add(item);
            }
        }
    }

    private void insertItem(SQLiteDatabase db, long resumeId, SectionItem item) {
        ContentValues values = new ContentValues();
        values.put(COL_RESUME_ID, resumeId);
        values.put(COL_SECTION_TYPE, item.type.key);
        values.put(COL_POSITION, item.position);
        values.put(COL_DATA, SectionItemCodec.toJson(item));
        item.id = db.insertOrThrow(TABLE_ITEMS, null, values);
        item.resumeId = resumeId;
    }

    private List<Long> orderedItemIds(SQLiteDatabase db, long resumeId, SectionType type) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = db.query(TABLE_ITEMS, new String[]{COL_ID},
                COL_RESUME_ID + " = ? AND " + COL_SECTION_TYPE + " = ?",
                new String[]{String.valueOf(resumeId), type.key}, null, null,
                COL_POSITION + ", " + COL_ID)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    private void writePositions(SQLiteDatabase db, List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            ContentValues values = new ContentValues();
            values.put(COL_POSITION, i);
            db.update(TABLE_ITEMS, values, COL_ID + " = ?",
                    new String[]{String.valueOf(orderedIds.get(i))});
        }
    }

    private void touchResume(SQLiteDatabase db, long resumeId) {
        ContentValues values = new ContentValues();
        values.put(COL_UPDATED_AT, System.currentTimeMillis());
        db.update(TABLE_RESUMES, values, COL_ID + " = ?", new String[]{String.valueOf(resumeId)});
    }

    private ContentValues toValues(Resume resume) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, resume.title);
        values.put(COL_TEMPLATE_ID, resume.templateId);
        values.put(COL_FULL_NAME, resume.fullName);
        values.put(COL_PROFESSIONAL_TITLE, resume.professionalTitle);
        values.put(COL_EMAIL, resume.email);
        values.put(COL_PHONE, resume.phone);
        values.put(COL_CITY, resume.city);
        values.put(COL_REGION, resume.region);
        values.put(COL_COUNTRY, resume.country);
        values.put(COL_WEBSITE, resume.website);
        values.put(COL_PROFILE_LINK, resume.profileLink);
        values.put(COL_SUMMARY, resume.summary);
        values.put(COL_CREATED_AT, resume.createdAt);
        values.put(COL_UPDATED_AT, resume.updatedAt);
        return values;
    }

    private Resume readResume(Cursor cursor) {
        Resume resume = new Resume();
        resume.id = readLong(cursor, COL_ID);
        resume.title = readString(cursor, COL_TITLE);
        resume.templateId = readString(cursor, COL_TEMPLATE_ID);
        resume.fullName = readString(cursor, COL_FULL_NAME);
        resume.professionalTitle = readString(cursor, COL_PROFESSIONAL_TITLE);
        resume.email = readString(cursor, COL_EMAIL);
        resume.phone = readString(cursor, COL_PHONE);
        resume.city = readString(cursor, COL_CITY);
        resume.region = readString(cursor, COL_REGION);
        resume.country = readString(cursor, COL_COUNTRY);
        resume.website = readString(cursor, COL_WEBSITE);
        resume.profileLink = readString(cursor, COL_PROFILE_LINK);
        resume.summary = readString(cursor, COL_SUMMARY);
        resume.createdAt = readLong(cursor, COL_CREATED_AT);
        resume.updatedAt = readLong(cursor, COL_UPDATED_AT);
        return resume;
    }

    private static String readString(Cursor cursor, String column) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(column));
        return value == null ? "" : value;
    }

    private static long readLong(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
    }
}