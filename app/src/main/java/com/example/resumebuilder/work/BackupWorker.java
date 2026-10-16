package com.example.resumebuilder.work;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.resumebuilder.data.ResumeDbHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Copies the SQLite database into app-private storage (files/backups). Nothing is uploaded.
 * The copies are a recovery safety net only: they are not visible to the user and there is
 * no in-app restore feature, so the app never claims they are user-restorable.
 */
public class BackupWorker extends Worker {

    private static final String TAG = "BackupWorker";
    private static final String BACKUP_DIR = "backups";
    private static final String FILE_PREFIX = "resume_backup_";
    private static final String FILE_SUFFIX = ".db";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final int MAX_BACKUPS = 5;
    private static final int MAX_ATTEMPTS = 3;

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            File backup = createBackup(getApplicationContext());
            Log.i(TAG, "Backup created: " + backup.getName());
            return Result.success();
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Backup failed on attempt " + (getRunAttemptCount() + 1), e);
            return getRunAttemptCount() + 1 < MAX_ATTEMPTS ? Result.retry() : Result.failure();
        }
    }

    private static File createBackup(Context context) throws IOException {
        SQLiteDatabase db = ResumeDbHelper.getInstance(context).getWritableDatabase();
        File source = new File(db.getPath());

        File dir = new File(context.getFilesDir(), BACKUP_DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create backup folder");
        }

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File target = new File(dir, FILE_PREFIX + stamp + FILE_SUFFIX);
        File temp = new File(dir, target.getName() + TEMP_SUFFIX);

        // Holding a transaction blocks other writers, so the file cannot change while it is copied.
        db.beginTransaction();
        try {
            copyFile(source, temp);
        } catch (IOException e) {
            temp.delete();
            throw e;
        } finally {
            db.endTransaction();
        }

        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IOException("Cannot finalize backup file");
        }
        pruneOldBackups(dir);
        return target;
    }

    private static void copyFile(File source, File destination) throws IOException {
        try (InputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            output.getFD().sync();
        }
    }

    /** Keeps only the newest MAX_BACKUPS copies and removes leftovers of interrupted copies. */
    private static void pruneOldBackups(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        File[] backups = Arrays.stream(files)
                .filter(f -> {
                    String name = f.getName();
                    if (name.endsWith(TEMP_SUFFIX)) {
                        f.delete();
                        return false;
                    }
                    return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX);
                })
                .sorted(Comparator.comparing(File::getName))
                .toArray(File[]::new);

        for (int i = 0; i < backups.length - MAX_BACKUPS; i++) {
            if (!backups[i].delete()) {
                Log.w(TAG, "Could not delete old backup " + backups[i].getName());
            }
        }
    }
}