package com.example.resumebuilder.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.resumebuilder.R;
import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.render.RenderedResume;
import com.example.resumebuilder.render.ResumeRenderer;
import com.example.resumebuilder.render.SampleResume;
import com.example.resumebuilder.render.Template;
import com.example.resumebuilder.render.Templates;
import com.example.resumebuilder.util.Background;
import com.example.resumebuilder.util.InsetsHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** Lets the user pick one of the built-in templates, shown as small real previews. */
public class TemplatePickerActivity extends AppCompatActivity {

    public static final String EXTRA_TEMPLATE_ID = "template_id";

    private static final int THUMB_WIDTH = 300;
    private static final int THUMB_HEIGHT = 424;

    private final List<ImageView> previewViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_template_picker);
        InsetsHelper.applyToRoot(findViewById(R.id.root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String selectedId = getIntent().getStringExtra(EXTRA_TEMPLATE_ID);
        if (selectedId == null) {
            selectedId = Resume.DEFAULT_TEMPLATE_ID;
        }

        LinearLayout container = findViewById(R.id.templates_container);
        LayoutInflater inflater = getLayoutInflater();
        for (Template template : Templates.all()) {
            MaterialCardView card = (MaterialCardView) inflater.inflate(
                    R.layout.item_template, container, false);
            boolean selected = template.id.equals(selectedId);
            card.setCheckable(true);
            card.setChecked(selected);

            ((TextView) card.findViewById(R.id.template_name)).setText(template.name);
            ((TextView) card.findViewById(R.id.template_description))
                    .setText(template.description);
            card.findViewById(R.id.template_selected)
                    .setVisibility(selected ? View.VISIBLE : View.GONE);

            ImageView preview = card.findViewById(R.id.template_preview);
            preview.setContentDescription(
                    getString(R.string.template_preview_description, template.name));
            previewViews.add(preview);

            card.setOnClickListener(v -> choose(template.id));
            container.addView(card);
        }
        renderPreviews();
    }

    private void choose(String templateId) {
        Intent result = new Intent();
        result.putExtra(EXTRA_TEMPLATE_ID, templateId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void renderPreviews() {
        List<Template> templates = Templates.all();
        Background.run(() -> {
            Resume sample = SampleResume.create();
            for (int i = 0; i < templates.size(); i++) {
                Bitmap bitmap = thumbnail(sample, templates.get(i));
                final ImageView target = previewViews.get(i);
                Background.runOnMain(() -> {
                    if (!isDestroyed()) {
                        target.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private static Bitmap thumbnail(Resume sample, Template template) {
        RenderedResume rendered = ResumeRenderer.render(sample, template);
        Bitmap bitmap = Bitmap.createBitmap(THUMB_WIDTH, THUMB_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale((float) THUMB_WIDTH / rendered.pageWidth,
                (float) THUMB_HEIGHT / rendered.pageHeight);
        rendered.drawPage(0, canvas);
        return bitmap;
    }
}