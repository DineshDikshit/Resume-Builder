package com.example.resumebuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resumebuilder.R;
import com.example.resumebuilder.data.ResumeRepository;
import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;
import com.example.resumebuilder.util.Background;
import com.example.resumebuilder.util.InsetsHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/** Lists the entries of one section (for example all jobs) and lets the user manage them. */
public class SectionListActivity extends AppCompatActivity {

    public static final String EXTRA_RESUME_ID = "resume_id";
    public static final String EXTRA_SECTION_KEY = "section_key";

    private static final String TAG = "SectionList";
    private static final int MAX_SKILL_LENGTH = 80;

    private ResumeRepository repository;
    private long resumeId;
    private SectionType type;
    private SectionItemAdapter adapter;
    private RecyclerView list;
    private View emptyState;
    private TextInputEditText quickAddInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_section_list);
        InsetsHelper.applyToRoot(findViewById(R.id.root));

        resumeId = getIntent().getLongExtra(EXTRA_RESUME_ID, -1L);
        type = SectionType.fromKey(getIntent().getStringExtra(EXTRA_SECTION_KEY));
        if (resumeId < 0 || type == null) {
            finish();
            return;
        }
        repository = new ResumeRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(type.title);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        list = findViewById(R.id.list_items);
        emptyState = findViewById(R.id.empty_state);
        ((android.widget.TextView) findViewById(R.id.empty_body))
                .setText(getString(R.string.section_empty_body, type.itemName));

        adapter = new SectionItemAdapter(type, new SectionItemAdapter.Listener() {
            @Override
            public void onEdit(SectionItem item) {
                openForm(item.id);
            }

            @Override
            public void onMove(SectionItem item, int direction) {
                runDb(() -> repository.moveItem(resumeId, type, item.id, direction), 0);
            }

            @Override
            public void onDelete(SectionItem item) {
                confirmDelete(item);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setText(getString(R.string.add_item, type.itemName));
        fab.setOnClickListener(v -> openForm(-1L));

        setupQuickAdd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumeId >= 0 && type != null) {
            loadItems();
        }
    }

    // ------------------------------------------------------------------ quick add

    /** Skills are single words, so they get a fast "type and press Add" box. */
    private void setupQuickAdd() {
        View quickAdd = findViewById(R.id.quick_add);
        if (type != SectionType.SKILLS) {
            quickAdd.setVisibility(View.GONE);
            return;
        }
        quickAdd.setVisibility(View.VISIBLE);
        quickAddInput = findViewById(R.id.edit_quick_add);
        findViewById(R.id.button_quick_add).setOnClickListener(v -> addSkillsFromInput());
        quickAddInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addSkillsFromInput();
                return true;
            }
            return false;
        });
    }

    private void addSkillsFromInput() {
        String raw = quickAddInput.getText() == null ? "" : quickAddInput.getText().toString();
        List<String> names = new ArrayList<>();
        for (String part : raw.split("[,\\n]")) {
            String name = limit(part.trim(), MAX_SKILL_LENGTH);
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        if (names.isEmpty()) {
            return;
        }
        quickAddInput.setText("");
        runDb(() -> {
            for (String name : names) {
                SectionItem item = new SectionItem(SectionType.SKILLS);
                item.put("name", name);
                repository.addItem(resumeId, item);
            }
        }, R.string.message_skills_added);
    }

    private static String limit(String text, int maxCodePoints) {
        if (text.codePointCount(0, text.length()) <= maxCodePoints) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, maxCodePoints));
    }

    // ------------------------------------------------------------------ loading

    private void loadItems() {
        Background.run(() -> {
            Resume resume;
            try {
                resume = repository.getResume(resumeId);
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load entries", e);
                Background.runOnMain(() -> {
                    if (!isDestroyed()) {
                        showMessage(R.string.error_generic);
                    }
                });
                return;
            }
            final Resume result = resume;
            Background.runOnMain(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (result == null) {
                    finish();
                    return;
                }
                List<SectionItem> items = result.itemsOf(type);
                adapter.setItems(items);
                boolean empty = items.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    /**
     * Runs a database change in the background, then reloads the list. Pass 0 as the message
     * when no confirmation should be shown.
     */
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
                loadItems();
                if (!success) {
                    showMessage(R.string.error_generic);
                } else if (successMessage != 0) {
                    showMessage(successMessage);
                }
            });
        });
    }

    // ------------------------------------------------------------------ actions

    private void openForm(long itemId) {
        Intent intent = new Intent(this, SectionItemEditActivity.class);
        intent.putExtra(SectionItemEditActivity.EXTRA_RESUME_ID, resumeId);
        intent.putExtra(SectionItemEditActivity.EXTRA_SECTION_KEY, type.key);
        intent.putExtra(SectionItemEditActivity.EXTRA_ITEM_ID, itemId);
        startActivity(intent);
    }

    private void confirmDelete(SectionItem item) {
        String name = SectionItemAdapter.displayName(this, type, item);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_entry_title)
                .setMessage(getString(R.string.delete_entry_message, name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete,
                        (dialog, which) -> runDb(
                                () -> repository.deleteItem(resumeId, type, item.id),
                                R.string.message_entry_deleted))
                .show();
    }

    private void showMessage(@StringRes int message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT).show();
    }
}