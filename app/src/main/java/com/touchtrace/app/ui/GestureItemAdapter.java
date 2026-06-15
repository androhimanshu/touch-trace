package com.touchtrace.app.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.touchtrace.app.R;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class GestureItemAdapter
        extends RecyclerView.Adapter<GestureItemAdapter.VH> {

    // ── Data model ────────────────────────────────────────────────────────────
    public static final class GestureItem {
        public final File   file;
        public final int    index;
        public final String gesture;
        public final long   epochMs;
        public final long   bytes;

        public GestureItem(File f, int i, String g, long e, long b) {
            file = f; index = i; gesture = g; epochMs = e; bytes = b;
        }
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────
    public interface OnItemClick     { void onShare(GestureItem item); }
    public interface OnItemLongClick { void onDelete(GestureItem item, int pos); }

    // ── Adapter ───────────────────────────────────────────────────────────────
    private final List<GestureItem> items;
    private final OnItemClick       shareClick;
    private final OnItemLongClick   deleteLong;

    public GestureItemAdapter(List<GestureItem> items,
                               OnItemClick shareClick,
                               OnItemLongClick deleteLong) {
        this.items      = items;
        this.shareClick = shareClick;
        this.deleteLong = deleteLong;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    static final class VH extends RecyclerView.ViewHolder {
        final TextView tvBadge, tvFileName, tvTime, tvSize;
        VH(View v) {
            super(v);
            tvBadge    = v.findViewById(R.id.tvBadge);
            tvFileName = v.findViewById(R.id.tvFileName);
            tvTime     = v.findViewById(R.id.tvTime);
            tvSize     = v.findViewById(R.id.tvSize);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gesture_file, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        GestureItem item = items.get(pos);

        h.tvBadge.setText(badgeLabel(item.gesture));
        h.tvBadge.setBackgroundTintList(
                ColorStateList.valueOf(StatsChartView.gestureColor(item.gesture)));

        h.tvFileName.setText(String.format(Locale.US, "file%d.json", item.index));
        h.tvTime.setText(relativeTime(item.epochMs));
        h.tvSize.setText(formatBytes(item.bytes));

        h.itemView.setOnClickListener(v -> {
            if (shareClick != null) shareClick.onShare(item);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (deleteLong != null) {
                deleteLong.onDelete(item, h.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    public void removeAt(int pos) {
        if (pos < 0 || pos >= items.size()) return;
        items.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, items.size());
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    private static String badgeLabel(String g) {
        switch (g) {
            case "tap":         return "TAP";
            case "long_press":  return "LONG";
            case "swipe_right": return "\u2192";
            case "swipe_left":  return "\u2190";
            case "swipe_up":    return "\u2191";
            case "swipe_down":  return "\u2193";
            default:            return "?";
        }
    }

    private static String relativeTime(long epochMs) {
        long diff = System.currentTimeMillis() - epochMs;
        if (diff < 60_000L)       return (diff / 1_000) + "s ago";
        if (diff < 3_600_000L)    return (diff / 60_000) + "m ago";
        if (diff < 86_400_000L)   return (diff / 3_600_000) + "h ago";
        return new SimpleDateFormat("MMM d", Locale.US).format(new Date(epochMs));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        return String.format(Locale.US, "%.1f KB", bytes / 1024f);
    }
}
