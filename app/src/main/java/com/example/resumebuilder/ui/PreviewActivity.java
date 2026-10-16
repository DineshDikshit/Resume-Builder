package com.example.resumebuilder.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resumebuilder.R;
import com.example.resumebuilder.data.ResumeRepository;
import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.pdf.PdfActions;
import com.example.resumebuilder.pdf.PdfExporter;
import com.example.resumebuilder.render.RenderedResume;
import com.example.resumebuilder.render.ResumeRenderer;
import com.example.resumebuilder.render.Template;
import com.example.resumebuilder.render.Templates;
import com.example.resumebuilder.util.Background;
import com.example.resumebuilder.util.FileNames;
import com.example.resumebuilder.util.InsetsHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

/** Shows the resume as real pages, lets the user change template and zoom, and exports a PDF. */
public class PreviewActivity extends AppCompatActivity {

    public static final String EXTRA_RESUME_ID = "resume_id";

    private static final String TAG = "Preview";
    private static final String STATE_ZOOM = "zoom_index";
    private static final float[] ZOOM_LEVELS = {1f, 1.5f, 2f};
    private static final int PAGE_MARGIN_DP = 16;

    private ResumeRepository repository;
    private long resumeId;
    private Resume resume;
    private RenderedResume rendered;

    private PagePreviewAdapter adapter;
    private RecyclerView list;
    private View loading;
    private LinearProgressIndicator exportProgress;
    private MaterialToolbar toolbar;
    private MaterialButton buttonTemplate;
    private MaterialButton buttonZoom;
    private MaterialButton buttonExport;

    private int zoomIndex;
    private int frameWidth;
    private int loadGeneration;
    private boolean exporting;
    private Dialog dialog;
    private ActivityResultLauncher<Intent> templateLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preview);
        InsetsHelper.applyToRoot(findViewById(R.id.root));

        resumeId = getIntent().getLongExtra(EXTRA_RESUME_ID, -1L);
        if (resumeId < 0) {
            finish();
            return;
        }
        repository = new ResumeRepository(this);
        if (savedInstanceState != null) {
            zoomIndex = Math.max(0, Math.min(savedInstanceState.getInt(STATE_ZOOM, 0),
                    ZOOM_LEVELS.length - 1));
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        list = findViewById(R.id.list_pages);
        adapter = new PagePreviewAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        loading = findViewById(R.id.loading);
        exportProgress = findViewById(R.id.export_progress);
        buttonTemplate = findViewById(R.id.button_template);
        buttonZoom = findViewById(R.id.button_zoom);
        buttonExport = findViewById(R.id.button_export);

        templateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK && data != null) {
                        String id = data.getStringExtra(TemplatePickerActivity.EXTRA_TEMPLATE_ID);
                        if (id != null) {
                            changeTemplate(id);
                        }
                    }
                });

        buttonTemplate.setOnClickListener(v -> {
            Intent intent = new Intent(this, TemplatePickerActivity.class);
            intent.putExtra(TemplatePickerActivity.EXTRA_TEMPLATE_ID,
                    resume != null ? resume.templateId : Resume.DEFAULT_TEMPLATE_ID);
            templateLauncher.launch(intent);
        });
        buttonZoom.setOnClickListener(v -> {
            zoomIndex = (zoomIndex + 1) % ZOOM_LEVELS.length;
            applyZoom();
        });
        buttonExport.setOnClickListener(v -> startExport());

        // The page width depends on the space available, so it is set once that is known.
        View frame = findViewById(R.id.pages_frame);
        frame.addOnLayoutChangeListener((v, left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            if (width != frameWidth) {
                frameWidth = width;
                v.post(this::applyZoom);
            }
        });
        updateZoomButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumeId >= 0) {
            load();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ZOOM, zoomIndex);
    }

    @Override
    protected void onDestroy() {
        dismissDialog();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- loading

    private void load() {
        final int generation = ++loadGeneration;
        if (rendered == null) {
            loading.setVisibility(View.VISIBLE);
        }
        Background.run(() -> {
            Resume loaded = null;
            RenderedResume pages = null;
            boolean failed = false;
            try {
                loaded = repository.getResume(resumeId);
                if (loaded != null) {
                    pages = ResumeRenderer.render(loaded, Templates.byId(loaded.templateId));
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load or render the resume", e);
                failed = true;
            }
            final Resume resultResume = loaded;
            final RenderedResume resultPages = pages;
            final boolean error = failed;
            Background.runOnMain(() -> {
                if (isDestroyed() || generation != loadGeneration) {
                    return;
                }
                loading.setVisibility(View.GONE);
                if (error) {
                    showMessage(R.string.error_generic);
                } else if (resultResume == null || resultPages == null) {
                    showMessage(R.string.message_resume_missing);
                    finish();
                } else {
                    onLoaded(resultResume, resultPages);
                }
            });
        });
    }

    private void onLoaded(Resume loadedResume, RenderedResume pages) {
        resume = loadedResume;
        rendered = pages;
        buttonTemplate.setText(getString(R.string.template_button,
                Templates.byId(loadedResume.templateId).name));
        int count = pages.getPageCount();
        toolbar.setSubtitle(getResources().getQuantityString(R.plurals.page_count, count, count));
        applyZoom();
    }

    /** Saves the newly chosen template, then redraws the pages. */
    private void changeTemplate(String templateId) {
        Background.run(() -> {
            try {
                Resume stored = repository.getResume(resumeId);
                if (stored != null && !templateId.equals(stored.templateId)) {
                    stored.templateId = templateId;
                    repository.updateResume(stored);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not save the template", e);
            }
            Background.runOnMain(() -> {
                if (!isDestroyed()) {
                    load();
                }
            });
        });
    }

    // ------------------------------------------------------------------- zoom

    private void applyZoom() {
        updateZoomButton();
        if (frameWidth <= 0) {
            return;
        }
        int margin = dp(PAGE_MARGIN_DP);
        int pageWidth = Math.max(Math.round((frameWidth - 2 * margin) * ZOOM_LEVELS[zoomIndex]), 1);
        ViewGroup.LayoutParams params = list.getLayoutParams();
        params.width = pageWidth + 2 * margin;
        list.setLayoutParams(params);
        adapter.setContent(rendered, pageWidth);
    }

    private void updateZoomButton() {
        int percent = Math.round(ZOOM_LEVELS[zoomIndex] * 100f);
        buttonZoom.setText(getString(R.string.zoom_button, percent));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ----------------------------------------------------------------- export

    private void startExport() {
        if (exporting || resume == null) {
            return;
        }
        if (resume.fullName.trim().isEmpty()) {
            showNameRequiredDialog();
            return;
        }
        exporting = true;
        setExportingUi(true);

        final Resume snapshot = resume;
        final Template template = Templates.byId(snapshot.templateId);
        final Context appContext = getApplicationContext();

        Background.run(() -> {
            Uri uri = null;
            String name = "";
            boolean ok = false;
            try {
                uri = PdfExporter.export(appContext, snapshot, template);
                name = queryDisplayName(appContext, uri,
                        FileNames.forResume(snapshot.fullName, snapshot.title));
                ok = true;
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "PDF export failed", e);
            }
            final Uri resultUri = uri;
            final String resultName = name;
            final boolean success = ok;
            Background.runOnMain(() -> {
                if (isDestroyed()) {
                    // The screen was closed or rotated while saving; the file is still saved.
                    if (success) {
                        Toast.makeText(appContext, R.string.message_pdf_saved_toast,
                                Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                exporting = false;
                setExportingUi(false);
                if (success) {
                    showSavedDialog(resultUri, resultName);
                } else {
                    showMessage(R.string.error_export_failed);
                }
            });
        });
    }

    private void setExportingUi(boolean busy) {
        exportProgress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        buttonExport.setEnabled(!busy);
        buttonExport.setText(busy ? R.string.exporting : R.string.export_button);
        buttonTemplate.setEnabled(!busy);
    }

    private static String queryDisplayName(Context context, Uri uri, String fallback) {
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
            // Fall back to the name we asked for.
        }
        return fallback;
    }

    // ---------------------------------------------------------------- dialogs

    private void showSavedDialog(Uri uri, String fileName) {
        dismissDialog();
        dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.export_success_title)
                .setMessage(getString(R.string.export_success_message, fileName))
                .setPositiveButton(R.string.action_open, (d, which) -> {
                    if (!PdfActions.open(this, uri)) {
                        showMessage(R.string.error_no_pdf_viewer);
                    }
                })
                .setNeutralButton(R.string.action_share, (d, which) -> {
                    if (!PdfActions.share(this, uri, getString(R.string.share_subject))) {
                        showMessage(R.string.error_no_share_app);
                    }
                })
                .setNegativeButton(R.string.action_close, null)
                .show();
    }

    private void showNameRequiredDialog() {
        dismissDialog();
        dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.name_required_title)
                .setMessage(R.string.name_required_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_edit_details, (d, which) -> finish())
                .show();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void showMessage(@StringRes int message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT).show();
    }
}