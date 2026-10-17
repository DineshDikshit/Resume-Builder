package com.example.resumebuilder.update;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.example.resumebuilder.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;

/**
 * Checks Google Play for a newer version once per app open and shows an optional "Update / Skip"
 * dialog. Every failure (no network, no Play Services, app not installed from Play, and so on)
 * is logged and ignored: the user is never blocked and never sees an error.
 */
public final class UpdateManager implements DefaultLifecycleObserver {

    private static final String TAG = "UpdateManager";
    private static final String MARKET_URI = "market://details?id=";
    private static final String WEB_URI = "https://play.google.com/store/apps/details?id=";

    @Nullable
    private static AppUpdateManager testOverride;

    private final UpdateState state;
    /** Held only while the Activity is alive; cleared in onDestroy so nothing leaks. */
    @Nullable
    private AppCompatActivity activity;
    @Nullable
    private AlertDialog dialog;

    /** Call once from onCreate of the main Activity. */
    public static void attach(@NonNull AppCompatActivity activity) {
        activity.getLifecycle().addObserver(new UpdateManager(activity));
    }

    /** Lets tests replace the real Play check with a FakeAppUpdateManager. */
    @VisibleForTesting
    public static void setTestOverride(@Nullable AppUpdateManager manager) {
        testOverride = manager;
    }

    private UpdateManager(@NonNull AppCompatActivity activity) {
        this.activity = activity;
        this.state = new ViewModelProvider(activity).get(UpdateState.class);
        state.updateAvailable.observe(activity, available -> showIfNeeded());
    }

    // ------------------------------------------------------------ lifecycle

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        startCheckOnce();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        showIfNeeded();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // Closing the dialog here avoids leaked windows; it is shown again on the next onStart
        // unless the user already tapped Update, Skip or Back.
        dismissDialog();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        dismissDialog();
        activity = null;
    }

    // ---------------------------------------------------------------- check

    private void startCheckOnce() {
        AppCompatActivity host = activity;
        if (host == null || state.checkStarted) {
            return;
        }
        state.checkStarted = true;
        try {
            AppUpdateManager manager = testOverride != null ? testOverride
                    : AppUpdateManagerFactory.create(host.getApplicationContext());
            // The request is asynchronous, so it never delays the first screen.
            manager.getAppUpdateInfo()
                    .addOnSuccessListener(this::onUpdateInfo)
                    .addOnFailureListener(e -> Log.d(TAG, "Update check skipped: " + e));
        } catch (RuntimeException e) {
            Log.d(TAG, "Update check could not start: " + e);
        }
    }

    private void onUpdateInfo(AppUpdateInfo info) {
        try {
            if (UpdatePolicy.shouldPrompt(info.updateAvailability(), info.updatePriority())) {
                state.updateAvailable.setValue(true);
            }
        } catch (RuntimeException e) {
            Log.d(TAG, "Update info could not be read: " + e);
        }
    }

    // --------------------------------------------------------------- dialog

    private void showIfNeeded() {
        AppCompatActivity host = activity;
        if (host == null || host.isFinishing() || host.isDestroyed()) {
            return;
        }
        if (!Boolean.TRUE.equals(state.updateAvailable.getValue()) || state.dismissed) {
            return;
        }
        if (dialog != null && dialog.isShowing()) {
            return;
        }
        if (!host.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }
        dialog = new MaterialAlertDialogBuilder(host)
                .setTitle(R.string.update_title)
                .setMessage(R.string.update_message)
                .setPositiveButton(R.string.update_action, (d, which) -> {
                    state.dismissed = true;
                    if (!openStorePage(host)) {
                        Toast.makeText(host, R.string.update_store_unavailable,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.update_skip, (d, which) -> state.dismissed = true)
                .setOnCancelListener(d -> state.dismissed = true)
                .create();
        dialog.show();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    // ----------------------------------------------------------- Play Store

    /** Opens this app's Play Store page: the Play Store app first, the website as a fallback. */
    public static boolean openStorePage(@NonNull Context context) {
        String packageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MARKET_URI + packageName)));
            return true;
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Play Store app not available, trying the website");
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(WEB_URI + packageName)));
            return true;
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "No app can open the Play Store link");
            return false;
        }
    }
}