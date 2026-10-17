package com.example.resumebuilder.update;

import com.google.android.play.core.install.model.UpdateAvailability;

/** Decides whether an update found by Google Play is worth showing a prompt for. */
public final class UpdatePolicy {

    /**
     * Minimum Play "update priority" (0 to 5) that triggers the prompt. 0 means every available
     * update prompts. Set it to 1 once you publish feature releases with a priority of 1 or more
     * (through the Play Developer API) and want small fixes to stay silent.
     */
    public static final int MIN_UPDATE_PRIORITY = 0;

    private UpdatePolicy() {
    }

    public static boolean shouldPrompt(int availability, int priority) {
        return shouldPrompt(availability, priority, MIN_UPDATE_PRIORITY);
    }

    public static boolean shouldPrompt(int availability, int priority, int minPriority) {
        return availability == UpdateAvailability.UPDATE_AVAILABLE && priority >= minPriority;
    }
}