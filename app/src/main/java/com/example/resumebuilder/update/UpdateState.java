package com.example.resumebuilder.update;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * In-memory state of the update prompt. It survives screen rotation but is never stored, so a
 * skipped prompt comes back the next time the app is opened.
 */
public class UpdateState extends ViewModel {
    boolean checkStarted;
    boolean dismissed;
    final MutableLiveData<Boolean> updateAvailable = new MutableLiveData<>(false);
}