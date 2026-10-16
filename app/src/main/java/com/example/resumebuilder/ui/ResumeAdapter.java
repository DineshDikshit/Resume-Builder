package com.example.resumebuilder.ui;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resumebuilder.R;
import com.example.resumebuilder.model.Resume;
import com.example.resumebuilder.render.Templates;

import java.util.Date;

/** Shows one card per resume with an overflow menu for rename, duplicate and delete. */
public class ResumeAdapter extends ListAdapter<Resume, ResumeAdapter.Holder> {

    public interface Listener {
        void onOpen(Resume resume);

        void onRename(Resume resume);

        void onDuplicate(Resume resume);

        void onDelete(Resume resume);
    }

    private static final DiffUtil.ItemCallback<Resume> DIFF =
            new DiffUtil.ItemCallback<Resume>() {
                @Override
                public boolean areItemsTheSame(@NonNull Resume a, @NonNull Resume b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Resume a, @NonNull Resume b) {
                    return a.updatedAt == b.updatedAt
                            && a.title.equals(b.title)
                            && a.fullName.equals(b.fullName)
                            && a.templateId.equals(b.templateId);
                }
            };

    private final Listener listener;

    public ResumeAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resume, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Resume resume = getItem(position);
        Context context = holder.itemView.getContext();

        String title = displayTitle(context, resume);
        holder.title.setText(title);
        holder.subtitle.setText(resume.fullName.trim().isEmpty()
                ? context.getString(R.string.no_name_yet) : resume.fullName.trim());
        String edited = DateFormat.getMediumDateFormat(context).format(new Date(resume.updatedAt));
        holder.meta.setText(context.getString(R.string.resume_meta,
                Templates.byId(resume.templateId).name, edited));

        holder.itemView.setOnClickListener(v -> listener.onOpen(resume));
        holder.more.setContentDescription(context.getString(R.string.more_actions_for, title));
        holder.more.setOnClickListener(v -> showMenu(v, resume));
    }

    static String displayTitle(Context context, Resume resume) {
        return resume.title.trim().isEmpty()
                ? context.getString(R.string.untitled_resume) : resume.title.trim();
    }

    private void showMenu(View anchor, Resume resume) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.inflate(R.menu.menu_resume_item);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_rename) {
                listener.onRename(resume);
            } else if (id == R.id.action_duplicate) {
                listener.onDuplicate(resume);
            } else if (id == R.id.action_delete) {
                listener.onDelete(resume);
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    public static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView meta;
        final ImageButton more;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.resume_title);
            subtitle = itemView.findViewById(R.id.resume_subtitle);
            meta = itemView.findViewById(R.id.resume_meta);
            more = itemView.findViewById(R.id.button_more);
        }
    }
}