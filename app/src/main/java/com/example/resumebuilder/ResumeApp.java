package com.example.resumebuilder;

import android.app.Application;
import android.util.Log;

import com.example.resumebuilder.work.BackupScheduler;

/** Runs once per app process; makes sure the daily backup job is scheduled. */
public class ResumeApp extends Application {

    private static final String TAG = "ResumeApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            BackupScheduler.schedule(this);
        } catch (RuntimeException e) {
            // Backups are a safety net only; never stop the app from starting.
            Log.w(TAG, "Could not schedule backup", e);
        }
    }
}