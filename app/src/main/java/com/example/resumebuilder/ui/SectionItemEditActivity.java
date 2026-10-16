package com.example.resumebuilder.ui;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.resumebuilder.R;
import com.example.resumebuilder.data.ResumeRepository;
import com.example.resumebuilder.model.FieldSpec;
import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;
import com.example.resumebuilder.util.Background;
import com.example.resumebuilder.util.EntryValidator;
import com.example.resumebuilder.util.InputFilters;
import com.example.resumebuilder.util.InsetsHelper;
import com.example.resumebuilder.util.TextChanged;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Add or edit one entry. The form is built from the field list of the section type, so every
 * section uses this same screen.
 */
public class SectionItemEditActivity extends AppCompatActivity {

    public static final String EXTRA_RESUME_ID = "resume_id";
    public static final String EXTRA_SECTION_KEY = "section_key";
    /** Use -1 to add a new entry. */
    public static final String EXTRA_ITEM_ID = "item_id";

    private static final String TAG = "SectionItemEdit";
    private static final String STATE_POPULATED = "populated";
    private static final String STATE_FINGERPRINT = "fingerprint";
    private static final int MAX_SINGLE_LINE = 120;
    private static final int MAX_MULTI_LINE = 2000;
    private static final int MAX_DATE = 7;
    private static final int MAX_PHONE = 20;

    /** Fixed ids let Android restore typed text after rotation. */
    private static final int[] FIELD_IDS = {
            R.id.field_0, R.id.field_1, R.id.field_2, R.id.field_3, R.id.field_4,
            R.id.field_5, R.id.field_6, R.id.field_7, R.id.field_8, R.id.field_9
    };

    private ResumeRepository repository;
    private long resumeId;
    private long itemId;
    private SectionType type;

    private final Map<String, TextInputLayout> layouts = new LinkedHashMap<>();
    private final Map<String, TextInputEditText> inputs = new LinkedHashMap<>();
    private final Map<String, MaterialCheckBox> checks = new LinkedHashMap<>();

    /** True once the fields hold the entry's values (loaded, or a new empty form). */
    private boolean populated;
    private String initialFingerprint = "";
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_edit);
        InsetsHelper.applyToRoot(findViewById(R.id.root));

        resumeId = getIntent().getLongExtra(EXTRA_RESUME_ID, -1L);
        itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1L);
        type = SectionType.fromKey(getIntent().getStringExtra(EXTRA_SECTION_KEY));
        if (resumeId < 0 || type == null || type.fields.size() > FIELD_IDS.length) {
            finish();
            return;
        }
        repository = new ResumeRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(itemId < 0 ? R.string.add_entry_title
                : R.string.edit_entry_title, type.itemName));
        toolbar.inflateMenu(R.menu.menu_item_edit);
        toolbar.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.action_save) {
                save();
                return true;
            }
            return false;
        });
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDirty()) {
                    confirmDiscard();
                } else {
                    finish();
                }
            }
        });

        buildForm();

        if (savedInstanceState != null) {
            populated = savedInstanceState.getBoolean(STATE_POPULATED, false);
            initialFingerprint = savedInstanceState.getString(STATE_FINGERPRINT, "");
        }
        if (itemId < 0) {
            if (!populated) {
                populated = true;
                initialFingerprint = fingerprint();
            }
        } else if (!populated) {
            loadItem();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_POPULATED, populated);
        outState.putString(STATE_FINGERPRINT, initialFingerprint);
    }

    // ------------------------------------------------------------------ form

    private void buildForm() {
        LinearLayout container = findViewById(R.id.form_container);
        LayoutInflater inflater = getLayoutInflater();
        int margin = dp(8);

        for (int i = 0; i < type.fields.size(); i++) {
            FieldSpec spec = type.fields.get(i);
            int id = FIELD_IDS[i];

            if (spec.kind == FieldSpec.Kind.CHECKBOX) {
                MaterialCheckBox box = new MaterialCheckBox(this);
                box.setId(id);
                box.setText(spec.label);
                box.setMinHeight(dp(48));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = margin;
                container.addView(box, params);
                checks.put(spec.key, box);
                continue;
            }

            TextInputLayout layout = (TextInputLayout) inflater.inflate(
                    R.layout.item_field_text, container, false);
            TextInputEditText input = (TextInputEditText) layout.getEditText();
            if (input == null) {
                continue;
            }
            input.setId(id);
            layout.setHint(spec.label);
            configureInput(input, layout, spec);
            TextChanged.watch(input, () -> layout.setError(null));
            container.addView(layout);
            layouts.put(spec.key, layout);
            inputs.put(spec.key, input);
        }

        // "I currently work here" switches the end date off.
        MaterialCheckBox current = checks.get("current");
        TextInputEditText end = inputs.get("end_date");
        TextInputLayout endLayout = layouts.get("end_date");
        if (current != null && end != null && endLayout != null) {
            current.setOnCheckedChangeListener((button, checked) -> {
                end.setEnabled(!checked);
                endLayout.setEnabled(!checked);
            });
        }
    }

    /** Sets the keyboard type and blocks characters that can never be valid for the field. */
    private void configureInput(TextInputEditText input, TextInputLayout layout, FieldSpec spec) {
        InputFilter[] filters = {new InputFilter.LengthFilter(MAX_SINGLE_LINE)};
        switch (spec.kind) {
            case MULTILINE:
                input.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                input.setMinLines(4);
                input.setGravity(Gravity.TOP | Gravity.START);
                filters = new InputFilter[]{new InputFilter.LengthFilter(MAX_MULTI_LINE)};
                if (!spec.hint.isEmpty()) {
                    layout.setHelperText(spec.hint);
                }
                break;
            case URL:
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                break;
            case DATE:
                input.setInputType(InputType.TYPE_CLASS_DATETIME
                        | InputType.TYPE_DATETIME_VARIATION_DATE);
                filters = new InputFilter[]{
                        InputFilters.allowOnly(InputFilters.DATE_CHARS),
                        new InputFilter.LengthFilter(MAX_DATE)};
                layout.setHelperText(spec.hint);
                break;
            case EMAIL:
                input.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            case PHONE:
                input.setInputType(InputType.TYPE_CLASS_PHONE);
                filters = new InputFilter[]{
                        InputFilters.allowOnly(InputFilters.PHONE_CHARS),
                        new InputFilter.LengthFilter(MAX_PHONE)};
                break;
            default:
                input.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                break;
        }
        input.setFilters(filters);
        if (spec.required) {
            layout.setHelperText(getString(R.string.helper_required));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // --------------------------------------------------------------- loading

    private void loadItem() {
        Background.run(() -> {
            SectionItem found = null;
            try {
                Resume resume = repository.getResume(resumeId);
                if (resume != null) {
                    for (SectionItem candidate : resume.itemsOf(type)) {
                        if (candidate.id == itemId) {
                            found = candidate;
                            break;
                        }
                    }
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load entry", e);
            }
            final SectionItem result = found;
            Background.runOnMain(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (result == null) {
                    showMessage(R.string.message_entry_missing);
                    finish();
                    return;
                }
                populate(result);
                populated = true;
                initialFingerprint = fingerprint();
            });
        });
    }

    private void populate(SectionItem item) {
        for (FieldSpec spec : type.fields) {
            if (spec.kind == FieldSpec.Kind.CHECKBOX) {
                MaterialCheckBox box = checks.get(spec.key);
                if (box != null) {
                    box.setChecked(item.isChecked(spec.key));
                }
            } else {
                TextInputEditText input = inputs.get(spec.key);
                if (input != null) {
                    input.setText(item.get(spec.key));
                }
            }
        }
    }

    // ---------------------------------------------------------------- saving

    private SectionItem readItem() {
        SectionItem item = new SectionItem(type);
        item.id = Math.max(itemId, 0L);
        item.resumeId = resumeId;
        for (FieldSpec spec : type.fields) {
            if (spec.kind == FieldSpec.Kind.CHECKBOX) {
                MaterialCheckBox box = checks.get(spec.key);
                item.setChecked(spec.key, box != null && box.isChecked());
            } else {
                item.put(spec.key, inputText(spec.key));
            }
        }
        if (item.isChecked("current")) {
            item.put("end_date", "");
        }
        return item;
    }

    private void save() {
        if (saving || !populated) {
            return;
        }
        SectionItem item = readItem();
        Map<String, EntryValidator.Error> errors = EntryValidator.validate(item);
        if (!errors.isEmpty()) {
            showErrors(errors);
            return;
        }

        saving = true;
        Background.run(() -> {
            boolean ok = true;
            try {
                if (itemId < 0) {
                    repository.addItem(resumeId, item);
                } else {
                    repository.updateItem(item);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not save entry", e);
                ok = false;
            }
            final boolean success = ok;
            Background.runOnMain(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (success) {
                    finish();
                } else {
                    saving = false;
                    showMessage(R.string.error_generic);
                }
            });
        });
    }

    private void showErrors(Map<String, EntryValidator.Error> errors) {
        for (TextInputLayout layout : layouts.values()) {
            layout.setError(null);
        }
        boolean focused = false;
        for (Map.Entry<String, EntryValidator.Error> entry : errors.entrySet()) {
            TextInputLayout layout = layouts.get(entry.getKey());
            if (layout == null) {
                continue;
            }
            layout.setError(message(entry.getValue()));
            if (!focused && layout.getEditText() != null) {
                layout.getEditText().requestFocus();
                focused = true;
            }
        }
    }

    private String message(EntryValidator.Error error) {
        switch (error) {
            case REQUIRED:
                return getString(R.string.error_required);
            case DATE_FORMAT:
                return getString(R.string.error_date_format);
            case DATE_FUTURE:
                return getString(R.string.error_date_future);
            case EMAIL_FORMAT:
                return getString(R.string.error_email_invalid);
            case PHONE_FORMAT:
                return getString(R.string.error_phone_format);
            default:
                return getString(R.string.error_date_order);
        }
    }

    // ----------------------------------------------------------- unsaved edits

    private boolean isDirty() {
        return populated && !fingerprint().equals(initialFingerprint);
    }

    private String fingerprint() {
        StringBuilder builder = new StringBuilder();
        for (FieldSpec spec : type.fields) {
            if (spec.kind == FieldSpec.Kind.CHECKBOX) {
                MaterialCheckBox box = checks.get(spec.key);
                builder.append(box != null && box.isChecked());
            } else {
                builder.append(inputText(spec.key));
            }
            builder.append('\u0001');
        }
        return builder.toString();
    }

    private String inputText(String key) {
        TextInputEditText input = inputs.get(key);
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void confirmDiscard() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.discard_title)
                .setMessage(R.string.discard_message)
                .setNegativeButton(R.string.action_keep_editing, null)
                .setPositiveButton(R.string.action_discard, (dialog, which) -> finish())
                .show();
    }

    private void showMessage(@StringRes int message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT).show();
    }
}