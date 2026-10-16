package com.example.resumebuilder.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resumebuilder.R;
import com.example.resumebuilder.render.RenderedResume;
import com.example.resumebuilder.util.Background;
import com.google.android.material.card.MaterialCardView;

/**
 * Shows the pages of a rendered resume. Pages are drawn on a background thread when they
 * scroll into view and kept in a small memory cache.
 */
public class PagePreviewAdapter extends RecyclerView.Adapter<PagePreviewAdapter.Holder> {

    private static final int MAX_BITMAP_WIDTH = 1400;

    private final LruCache<Integer, Bitmap> cache;
    private RenderedResume rendered;
    private int pageWidthPx;

    public PagePreviewAdapter() {
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024L / 8L);
        cache = new LruCache<Integer, Bitmap>(Math.max(maxKb, 1024)) {
            @Override
            protected int sizeOf(@NonNull Integer key, @NonNull Bitmap value) {
                return Math.max(value.getByteCount() / 1024, 1);
            }
        };
    }

    /** Replaces the content or the page width (zoom) and redraws every visible page. */
    @SuppressLint("NotifyDataSetChanged")
    public void setContent(RenderedResume newRendered, int newPageWidthPx) {
        rendered = newRendered;
        pageWidthPx = newPageWidthPx;
        cache.evictAll();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview_page, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final RenderedResume snapshot = rendered;
        final int width = pageWidthPx;
        if (snapshot == null || width <= 0) {
            return;
        }
        Context context = holder.itemView.getContext();
        int total = snapshot.getPageCount();
        holder.label.setText(context.getString(R.string.page_label, position + 1, total));
        holder.image.setContentDescription(
                context.getString(R.string.page_description, position + 1, total));

        ViewGroup.LayoutParams params = holder.card.getLayoutParams();
        params.height = Math.round(width * (float) snapshot.pageHeight / snapshot.pageWidth);
        holder.card.setLayoutParams(params);

        final int token = ++holder.token;
        Bitmap cached = cache.get(position);
        if (cached != null) {
            holder.image.setImageBitmap(cached);
            return;
        }
        holder.image.setImageDrawable(null);
        Background.run(() -> {
            Bitmap bitmap = renderPage(snapshot, position, width);
            Background.runOnMain(() -> {
                if (bitmap == null || snapshot != rendered || width != pageWidthPx) {
                    return;
                }
                cache.put(position, bitmap);
                if (holder.token == token) {
                    holder.image.setImageBitmap(bitmap);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return rendered == null || pageWidthPx <= 0 ? 0 : rendered.getPageCount();
    }

    private static Bitmap renderPage(RenderedResume source, int pageIndex, int widthPx) {
        int bitmapWidth = Math.min(widthPx, MAX_BITMAP_WIDTH);
        int bitmapHeight = Math.round(bitmapWidth * (float) source.pageHeight / source.pageWidth);
        try {
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(bitmapWidth / (float) source.pageWidth,
                    bitmapHeight / (float) source.pageHeight);
            source.drawPage(pageIndex, canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static final class Holder extends RecyclerView.ViewHolder {
        final TextView label;
        final MaterialCardView card;
        final ImageView image;
        int token;

        Holder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.page_label);
            card = itemView.findViewById(R.id.page_card);
            image = itemView.findViewById(R.id.page_image);
        }
    }
}