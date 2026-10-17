package com.example.resumebuilder.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.android.play.core.install.model.UpdateAvailability;

import org.junit.Test;

public class UpdatePolicyTest {

    @Test
    public void promptsWhenAnUpdateIsAvailable() {
        assertTrue(UpdatePolicy.shouldPrompt(UpdateAvailability.UPDATE_AVAILABLE, 0, 0));
        assertTrue(UpdatePolicy.shouldPrompt(UpdateAvailability.UPDATE_AVAILABLE, 3, 0));
    }

    @Test
    public void staysSilentWhenNoUpdateIsAvailable() {
        assertFalse(UpdatePolicy.shouldPrompt(UpdateAvailability.UPDATE_NOT_AVAILABLE, 5, 0));
        assertFalse(UpdatePolicy.shouldPrompt(UpdateAvailability.UNKNOWN, 5, 0));
        assertFalse(UpdatePolicy.shouldPrompt(
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS, 5, 0));
    }

    @Test
    public void respectsTheMinimumPriority() {
        assertFalse(UpdatePolicy.shouldPrompt(UpdateAvailability.UPDATE_AVAILABLE, 0, 1));
        assertTrue(UpdatePolicy.shouldPrompt(UpdateAvailability.UPDATE_AVAILABLE, 1, 1));
        assertTrue(UpdatePolicy.shouldPrompt(UpdateAvailability.UPDATE_AVAILABLE, 4, 1));
    }
}