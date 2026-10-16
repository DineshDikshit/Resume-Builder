package com.example.resumebuilder.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resumebuilder.R;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;

import java.util.ArrayList;
import java.util.List;

/** One card per entry with move up, move down and delete buttons. */
public class SectionItemAdapter extends RecyclerView.Adapter<SectionItemAdapter.Holder> {

    public interface Listener {
        void onEdit(SectionItem item);

        /** direction is -1 for up and +1 for down. */
        void onMove(SectionItem item, int direction);

        void onDelete(SectionItem item);
    }

    private static final float DISABLED_ALPHA = 0.38f;

    private final SectionType type;
    private final Listener listener;
    private final List<SectionItem> items = new ArrayList<>();

    public SectionItemAdapter(SectionType type, Listener listener) {
        this.type = type;
        this.listener = listener;
    }

    /** Whole-list refresh: the move buttons of neighbouring rows depend on list position. */
    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<SectionItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_entry, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SectionItem item = items.get(position);
        Context context = holder.itemView.getContext();

        String primary = type.primaryText(item);
        String secondary = type.secondaryText(item);
        if (primary.isEmpty()) {
            primary = secondary;
            secondary = "";
        }
        if (primary.isEmpty()) {
            primary = context.getString(R.string.untitled_entry);
        }
        holder.primary.setText(primary);
        holder.secondary.setText(secondary);
        holder.secondary.setVisibility(secondary.isEmpty() ? View.GONE : View.VISIBLE);

        boolean first = position == 0;
        boolean last = position == items.size() - 1;
        holder.up.setEnabled(!first);
        holder.up.setAlpha(first ? DISABLED_ALPHA : 1f);
        holder.down.setEnabled(!last);
        holder.down.setAlpha(last ? DISABLED_ALPHA : 1f);

        holder.up.setContentDescription(context.getString(R.string.move_up, primary));
        holder.down.setContentDescription(context.getString(R.string.move_down, primary));
        holder.delete.setContentDescription(context.getString(R.string.delete_entry, primary));

        holder.itemView.setOnClickListener(v -> listener.onEdit(item));
        holder.up.setOnClickListener(v -> listener.onMove(item, -1));
        holder.down.setOnClickListener(v -> listener.onMove(item, 1));
        holder.delete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static String displayName(Context context, SectionType type, SectionItem item) {
        String primary = type.primaryText(item);
        if (primary.isEmpty()) {
            primary = type.secondaryText(item);
        }
        return primary.isEmpty() ? context.getString(R.string.untitled_entry) : primary;
    }

    public static final class Holder extends RecyclerView.ViewHolder {
        final TextView primary;
        final TextView secondary;
        final ImageButton up;
        final ImageButton down;
        final ImageButton delete;

        Holder(@NonNull View itemView) {
            super(itemView);
            primary = itemView.findViewById(R.id.entry_primary);
            secondary = itemView.findViewById(R.id.entry_secondary);
            up = itemView.findViewById(R.id.button_up);
            down = itemView.findViewById(R.id.button_down);
            delete = itemView.findViewById(R.id.button_delete);
        }
    }
}