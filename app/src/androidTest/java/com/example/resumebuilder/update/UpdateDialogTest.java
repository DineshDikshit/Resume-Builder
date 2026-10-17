package com.example.resumebuilder.update;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.resumebuilder.R;
import com.example.resumebuilder.ui.MainActivity;
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UpdateDialogTest {

    private FakeAppUpdateManager fake;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        fake = new FakeAppUpdateManager(context);
        UpdateManager.setTestOverride(fake);
    }

    @After
    public void tearDown() {
        UpdateManager.setTestOverride(null);
    }

    @Test
    public void dialogAppearsWhenAnUpdateIsAvailable() {
        fake.setUpdateAvailable(2);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.update_title)).check(matches(isDisplayed()));
            onView(withText(R.string.update_action)).check(matches(isDisplayed()));
            onView(withText(R.string.update_skip)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void skipClosesTheDialogAndItStaysClosedAfterRotation() {
        fake.setUpdateAvailable(2);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.update_skip)).perform(click());
            onView(withText(R.string.update_title)).check(doesNotExist());

            scenario.recreate();
            onView(withText(R.string.update_title)).check(doesNotExist());
        }
    }

    @Test
    public void noDialogWhenNoUpdateIsAvailable() {
        fake.setUpdateNotAvailable();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.update_title)).check(doesNotExist());
        }
    }
}