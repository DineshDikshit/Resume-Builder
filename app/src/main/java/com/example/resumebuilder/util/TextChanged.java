package com.example.resumebuilder.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

/** Small helper to run an action whenever the text of a field changes. */
public final class TextChanged {

    private TextChanged() {
    }

    public static void watch(TextView view, Runnable action) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                action.run();
            }
        });
    }
}