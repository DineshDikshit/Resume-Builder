package com.example.resumebuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.resumebuilder.R;
import com.example.resumebuilder.data.ResumeRepository;
import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionType;
import com.example.resumebuilder.render.Templates;
import com.example.resumebuilder.util.Background;
import com.example.resumebuilder.util.ContactValidator;
import com.example.resumebuilder.util.InputFilters;
import com.example.resumebuilder.util.InsetsHelper;
import com.example.resumebuilder.util.TextChanged;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Edits the personal details, summary and template of one resume. Text changes are saved
 * automatically when the screen is left; the template is always read from the database so the
 * preview screen and this screen never disagree.
 */
public class ResumeEditorActivity extends AppCompatActivity {

    public static final String EXTRA_RESUME_ID = "resume_id";

    private static final String TAG = "ResumeEditor";
    private static final String STATE_TEMPLATE_ID = "template_id";
    private static final String STATE_FIELDS_READY = "fields_ready";
    private static final String STATE_FINGERPRINT = "fingerprint";
    private static final int MAX_PHONE = 20;

    private ResumeRepository repository;
    private long resumeId;
    private Resume resume;

    /** True once the fields hold the user's text (loaded from the database or restored). */
    private boolean fieldsReady;
    private String savedFingerprint;
    private String templateId = Resume.DEFAULT_TEMPLATE_ID;
    /** A template picked before the resume finished loading; applied right after loading. */
    private String pendingTemplateId;

    private TextInputEditText editName;
    private TextInputEditText editFullName;
    private TextInputEditText editTitle;
    private TextInputEditText editEmail;
    private TextInputEditText editPhone;
    private TextInputEditText editCity;
    private TextInputEditText editRegion;
    private TextInputEditText editCountry;
    private TextInputEditText editWebsite;
    private TextInputEditText editProfileLink;
    private TextInputEditText editSummary;
    private TextInputLayout tilFullName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPhone;
    private MaterialButton buttonTemplate;
    private LinearLayout sectionsContainer;

    private ActivityResultLauncher<Intent> templateLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resume_editor);
        InsetsHelper.applyToRoot(findViewById(R.id.root));

        resumeId = getIntent().getLongExtra(EXTRA_RESUME_ID, -1L);
        if (resumeId < 0) {
            finish();
            return;
        }
        repository = new ResumeRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        bindViews();
        setupValidation();

        if (savedInstanceState != null) {
            // The text fields restore themselves; only the extra state is read here.
            fieldsReady = savedInstanceState.getBoolean(STATE_FIELDS_READY, false);
            templateId = savedInstanceState.getString(STATE_TEMPLATE_ID,
                    Resume.DEFAULT_TEMPLATE_ID);
            savedFingerprint = savedInstanceState.getString(STATE_FINGERPRINT);
        }

        templateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK && data != null) {
                        String id = data.getStringExtra(TemplatePickerActivity.EXTRA_TEMPLATE_ID);
                        if (id != null) {
                            applyTemplate(id);
                        }
                    }
                });
        buttonTemplate.setOnClickListener(v -> {
            Intent intent = new Intent(this, TemplatePickerActivity.class);
            intent.putExtra(TemplatePickerActivity.EXTRA_TEMPLATE_ID, templateId);
            templateLauncher.launch(intent);
        });
        findViewById(R.id.button_preview).setOnClickListener(v -> {
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra(PreviewActivity.EXTRA_RESUME_ID, resumeId);
            startActivity(intent);
        });
        updateTemplateButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumeId >= 0) {
            loadResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        save(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TEMPLATE_ID, templateId);
        outState.putBoolean(STATE_FIELDS_READY, fieldsReady);
        outState.putString(STATE_FINGERPRINT, savedFingerprint);
    }

    // ---------------------------------------------------------------- setup

    private void bindViews() {
        editName = findViewById(R.id.edit_resume_name);
        editFullName = findViewById(R.id.edit_full_name);
        editTitle = findViewById(R.id.edit_professional_title);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        editCity = findViewById(R.id.edit_city);
        editRegion = findViewById(R.id.edit_region);
        editCountry = findViewById(R.id.edit_country);
        editWebsite = findViewById(R.id.edit_website);
        editProfileLink = findViewById(R.id.edit_profile_link);
        editSummary = findViewById(R.id.edit_summary);
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        buttonTemplate = findViewById(R.id.button_template);
        sectionsContainer = findViewById(R.id.sections_container);
    }

    /** Shows hints about problems but never blocks saving optional information. */
    private void setupValidation() {
        TextChanged.watch(editFullName, () -> tilFullName.setError(null));
        editFullName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && text(editFullName).isEmpty()) {
                tilFullName.setError(getString(R.string.error_name_required));
            }
        });

        TextChanged.watch(editEmail, () -> tilEmail.setError(null));
        editEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !ContactValidator.isValidEmail(text(editEmail))) {
                tilEmail.setError(getString(R.string.error_email_invalid));
            }
        });

        editPhone.setFilters(new InputFilter[]{
                InputFilters.allowOnly(InputFilters.PHONE_CHARS),
                new InputFilter.LengthFilter(MAX_PHONE)});
        TextChanged.watch(editPhone, () -> tilPhone.setError(null));
        editPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !ContactValidator.isValidPhone(text(editPhone))) {
                tilPhone.setError(getString(R.string.error_phone_format));
            }
        });
    }

    // -------------------------------------------------------------- loading

    private void loadResume() {
        Background.run(() -> {
            Resume loaded;
            try {
                loaded = repository.getResume(resumeId);
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load resume", e);
                Background.runOnMain(() -> {
                    if (!isDestroyed()) {
                        showMessage(R.string.error_generic);
                    }
                });
                return;
            }
            final Resume result = loaded;
            Background.runOnMain(() -> {
                if (!isDestroyed()) {
                    onLoaded(result);
                }
            });
        });
    }

    private void onLoaded(Resume loaded) {
        if (loaded == null) {
            showMessage(R.string.message_resume_missing);
            finish();
            return;
        }
        resume = loaded;
        boolean saveTemplate = pendingTemplateId != null
                && !pendingTemplateId.equals(loaded.templateId);
        templateId = pendingTemplateId != null ? pendingTemplateId : loaded.templateId;
        pendingTemplateId = null;

        if (!fieldsReady) {
            populate(loaded);
            fieldsReady = true;
            savedFingerprint = fingerprint();
        }
        updateTemplateButton();
        renderSections();
        if (saveTemplate) {
            save(true);
        }
    }

    private void populate(Resume r) {
        editName.setText(r.title);
        editFullName.setText(r.fullName);
        editTitle.setText(r.professionalTitle);
        editEmail.setText(r.email);
        editPhone.setText(r.phone);
        editCity.setText(r.city);
        editRegion.setText(r.region);
        editCountry.setText(r.country);
        editWebsite.setText(r.website);
        editProfileLink.setText(r.profileLink);
        editSummary.setText(r.summary);
    }

    // --------------------------------------------------------------- saving

    /** Saves when the text changed, or always when force is true (template change). */
    private void save(boolean force) {
        if (resume == null || !fieldsReady) {
            return;
        }
        String now = fingerprint();
        if (!force && now.equals(savedFingerprint)) {
            return;
        }
        Resume toSave = new Resume();
        toSave.id = resume.id;
        toSave.createdAt = resume.createdAt;
        toSave.title = text(editName);
        toSave.templateId = templateId;
        toSave.fullName = text(editFullName);
        toSave.professionalTitle = text(editTitle);
        toSave.email = text(editEmail);
        toSave.phone = text(editPhone);
        toSave.city = text(editCity);
        toSave.region = text(editRegion);
        toSave.country = text(editCountry);
        toSave.website = text(editWebsite);
        toSave.profileLink = text(editProfileLink);
        toSave.summary = text(editSummary);
        savedFingerprint = now;

        Background.run(() -> {
            try {
                repository.updateResume(toSave);
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not save resume", e);
            }
        });
    }

    /** Fingerprint of the text fields only; the template is stored separately. */
    private String fingerprint() {
        StringBuilder builder = new StringBuilder();
        String[] parts = {
                text(editName), text(editFullName), text(editTitle),
                text(editEmail), text(editPhone), text(editCity), text(editRegion),
                text(editCountry), text(editWebsite), text(editProfileLink),
                text(editSummary)
        };
        for (String part : parts) {
            builder.append(part).append('\u0001');
        }
        return builder.toString();
    }

    private static String text(TextInputEditText view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    // ------------------------------------------------------------------- UI

    private void applyTemplate(String id) {
        templateId = id;
        updateTemplateButton();
        if (resume == null || !fieldsReady) {
            pendingTemplateId = id;
            return;
        }
        save(true);
    }

    private void updateTemplateButton() {
        buttonTemplate.setText(getString(R.string.template_button,
                Templates.byId(templateId).name));
    }

    private void renderSections() {
        sectionsContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (SectionType type : SectionType.values()) {
            View row = inflater.inflate(R.layout.item_section_row, sectionsContainer, false);
            int count = resume.itemsOf(type).size();
            String countText = count == 0 ? getString(R.string.no_entries)
                    : getResources().getQuantityString(R.plurals.entry_count, count, count);
            ((TextView) row.findViewById(R.id.section_title)).setText(type.title);
            ((TextView) row.findViewById(R.id.section_count)).setText(countText);
            row.setContentDescription(type.title + ", " + countText);
            row.setOnClickListener(v -> openSection(type));
            sectionsContainer.addView(row);
        }
    }

    private void openSection(SectionType type) {
        Intent intent = new Intent(this, SectionListActivity.class);
        intent.putExtra(SectionListActivity.EXTRA_RESUME_ID, resumeId);
        intent.putExtra(SectionListActivity.EXTRA_SECTION_KEY, type.key);
        startActivity(intent);
    }

    private void showMessage(@StringRes int message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT).show();
    }
}