package com.example.resumebuilder.work;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Re-schedules the backup job after the device restarts. */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        try {
            BackupScheduler.schedule(context);
            Log.i(TAG, "Backup job re-scheduled after boot");
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not re-schedule backup after boot", e);
        }
    }
}