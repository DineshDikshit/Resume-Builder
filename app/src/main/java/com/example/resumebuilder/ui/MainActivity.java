package com.example.resumebuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resumebuilder.R;
import com.example.resumebuilder.data.ResumeRepository;
import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.util.Background;
import com.example.resumebuilder.util.InsetsHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/** Home screen: the list of saved resumes. */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private interface TitleCallback {
        void onTitle(String title);
    }

    private ResumeRepository repository;
    private ResumeAdapter adapter;
    private RecyclerView list;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        InsetsHelper.applyToRoot(findViewById(R.id.root));

        repository = new ResumeRepository(this);
        list = findViewById(R.id.list_resumes);
        emptyState = findViewById(R.id.empty_state);

        adapter = new ResumeAdapter(new ResumeAdapter.Listener() {
            @Override
            public void onOpen(Resume resume) {
                openEditor(resume);
            }

            @Override
            public void onRename(Resume resume) {
                showTitleDialog(R.string.dialog_rename_title, resume.title, R.string.action_save,
                        title -> renameResume(resume.id, title));
            }

            @Override
            public void onDuplicate(Resume resume) {
                runDb(() -> repository.duplicateResume(resume.id), R.string.message_duplicated);
            }

            @Override
            public void onDelete(Resume resume) {
                confirmDelete(resume);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        View.OnClickListener newResume = v -> showTitleDialog(R.string.dialog_new_title,
                getString(R.string.default_resume_title), R.string.action_create,
                this::createResume);
        findViewById(R.id.fab_new).setOnClickListener(newResume);
        findViewById(R.id.button_create_first).setOnClickListener(newResume);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResumes();
    }

    private void openEditor(Resume resume) {
        Intent intent = new Intent(this, ResumeEditorActivity.class);
        intent.putExtra(ResumeEditorActivity.EXTRA_RESUME_ID, resume.id);
        startActivity(intent);
    }

    // ------------------------------------------------------------------ loading

    private void loadResumes() {
        Background.run(() -> {
            List<Resume> resumes;
            try {
                resumes = repository.listResumes();
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load resumes", e);
                Background.runOnMain(() -> {
                    if (!isDestroyed()) {
                        showMessage(R.string.error_generic);
                    }
                });
                return;
            }
            Background.runOnMain(() -> {
                if (isDestroyed()) {
                    return;
                }
                adapter.submitList(resumes);
                boolean empty = resumes.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    /** Runs a database change in the background, then reloads the list and shows a message. */
    private void runDb(Runnable work, @StringRes int successMessage) {
        Background.run(() -> {
            boolean ok = true;
            try {
                work.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "Database operation failed", e);
                ok = false;
            }
            final boolean success = ok;
            Background.runOnMain(() -> {
                if (isDestroyed()) {
                    return;
                }
                loadResumes();
                showMessage(success ? successMessage : R.string.error_generic);
            });
        });
    }

    // ------------------------------------------------------------------ actions

    private void createResume(String title) {
        runDb(() -> {
            Resume resume = new Resume();
            resume.title = title;
            repository.createResume(resume);
        }, R.string.message_created);
    }

    private void renameResume(long id, String title) {
        runDb(() -> {
            Resume resume = repository.getResume(id);
            if (resume != null) {
                resume.title = title;
                repository.updateResume(resume);
            }
        }, R.string.message_renamed);
    }

    private void confirmDelete(Resume resume) {
        String name = ResumeAdapter.displayTitle(this, resume);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete,
                        (dialog, which) -> runDb(() -> repository.deleteResume(resume.id),
                                R.string.message_deleted))
                .show();
    }

    /** Asks for a resume name. The dialog stays open until a non-empty name is entered. */
    private void showTitleDialog(@StringRes int dialogTitle, String initial,
                                 @StringRes int positiveLabel, TitleCallback callback) {
        View content = getLayoutInflater().inflate(R.layout.dialog_text_input, null);
        TextInputLayout inputLayout = content.findViewById(R.id.til_title);
        TextInputEditText input = content.findViewById(R.id.edit_title);
        input.setText(initial == null ? "" : initial);
        if (input.getText() != null) {
            input.setSelection(input.getText().length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(dialogTitle)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(positiveLabel, null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String text = input.getText() == null ? ""
                        : input.getText().toString().trim();
                if (text.isEmpty()) {
                    inputLayout.setError(getString(R.string.error_title_required));
                    return;
                }
                inputLayout.setError(null);
                dialog.dismiss();
                callback.onTitle(text);
            });
            input.requestFocus();
        });
        dialog.show();
    }

    private void showMessage(@StringRes int message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT).show();
    }
}