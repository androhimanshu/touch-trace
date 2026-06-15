package com.touchtrace.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.touchtrace.app.R;
import com.touchtrace.app.storage.GestureFileManager;
import com.touchtrace.app.storage.SessionExporter;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class FileBrowserActivity extends AppCompatActivity {

    private GestureFileManager                           fileManager;
    private GestureItemAdapter                           adapter;
    private final List<GestureItemAdapter.GestureItem>   items = new ArrayList<>();

    private RecyclerView recyclerView;
    private TextView     tvSubtitle;
    private View         emptyState;
    private View         progressBar;

    // Swipe-to-delete drawing
    private final Paint swipeBgPaint   = new Paint();
    private final Paint swipeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float dp;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        dp = getResources().getDisplayMetrics().density;

        fileManager = new GestureFileManager(this, getExternalFilesDir(null));

        tvSubtitle  = findViewById(R.id.tvBrowserSubtitle);
        emptyState  = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        // Swipe-delete paint
        swipeBgPaint.setColor(0xFFFF3D5E);
        swipeTextPaint.setColor(0xFFFFFFFF);
        swipeTextPaint.setTextSize(11f * dp);
        swipeTextPaint.setTextAlign(Paint.Align.CENTER);
        swipeTextPaint.setLetterSpacing(0.15f);

        // RecyclerView setup
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RowDivider());
        adapter = new GestureItemAdapter(items, this::shareFile, this::confirmDeleteDialog);
        recyclerView.setAdapter(adapter);
        attachSwipeToDelete();

        // Header buttons
        findViewById(R.id.btnBrowserBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearAll).setOnClickListener(v -> confirmClearAll());

        loadFiles();
    }

    // ── File loading ──────────────────────────────────────────────────────────
    private void loadFiles() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        new Thread(() -> {
            File[] files = fileManager.getAllFiles();
            List<GestureItemAdapter.GestureItem> result = new ArrayList<>();

            for (File f : files) {
                try {
                    JSONObject j = new JSONObject(readFile(f));
                    result.add(new GestureItemAdapter.GestureItem(
                            f,
                            j.optInt("file_index", GestureFileManager.indexFromFile(f)),
                            j.optString("gesture", "unknown"),
                            j.optLong("start_epoch_ms", f.lastModified()),
                            f.length()
                    ));
                } catch (Exception ignored) { }
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                items.addAll(result);
                adapter.notifyDataSetChanged();
                updateSubtitle();
                if (items.isEmpty()) emptyState.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    // ── Swipe-to-delete ───────────────────────────────────────────────────────
    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                deleteWithUndo(pos);
            }

            /** Draw the red DELETE background behind the swiping row. */
            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int state, boolean isActive) {
                View item = vh.itemView;
                if (dX < 0) { // left swipe
                    float revealed = -dX;
                    // Red strip
                    c.drawRect(item.getRight() + dX, item.getTop(),
                               item.getRight(), item.getBottom(), swipeBgPaint);
                    // "DELETE" label once enough is revealed
                    if (revealed > 72 * dp) {
                        float cx  = item.getRight() - revealed / 2f;
                        float cy  = item.getTop() + (item.getBottom() - item.getTop()) / 2f
                                    + swipeTextPaint.getTextSize() / 3f;
                        c.drawText("DELETE", cx, cy, swipeTextPaint);
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, state, isActive);
            }

            /** Require a slightly larger fling or drag to trigger the action. */
            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder vh) { return 0.4f; }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    /** Optimistically removes the row, then gives the user 4 s to undo. */
    private void deleteWithUndo(int pos) {
        if (pos < 0 || pos >= items.size()) return;
        GestureItemAdapter.GestureItem item = items.get(pos);

        // Remove from list immediately for instant visual feedback
        adapter.removeAt(pos);
        updateSubtitle();
        if (items.isEmpty()) emptyState.setVisibility(View.VISIBLE);

        final boolean[] undone = {false};

        Snackbar.make(recyclerView,
                        "file" + item.index + ".json removed",
                        Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> {
                    undone[0] = true;
                    // Re-insert at original position (or end if list shrank)
                    int insertAt = Math.min(pos, items.size());
                    items.add(insertAt, item);
                    adapter.notifyItemInserted(insertAt);
                    adapter.notifyItemRangeChanged(insertAt, items.size());
                    updateSubtitle();
                    emptyState.setVisibility(View.GONE);
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (!undone[0]) {
                            // Permanently delete only when the snackbar expires
                            item.file.delete();
                            if (items.isEmpty())
                                emptyState.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .setBackgroundTint(0xFF111520)
                .setTextColor(0xFFE8EAED)
                .setActionTextColor(0xFF00D4FF)
                .show();
    }

    // ── Long-press delete dialog (still available as a secondary path) ─────────
    private void confirmDeleteDialog(GestureItemAdapter.GestureItem item, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Delete file" + item.index + ".json?")
                .setMessage("This gesture record will be permanently removed.")
                .setPositiveButton("Delete", (d, w) -> deleteWithUndo(pos))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Share ─────────────────────────────────────────────────────────────────
    private void shareFile(GestureItemAdapter.GestureItem item) {
        try {
            startActivity(SessionExporter.shareFileIntent(this, item.file));
        } catch (Exception e) {
            Snackbar.make(recyclerView, "Could not share file.", Snackbar.LENGTH_SHORT).show();
        }
    }

    // ── Clear All ─────────────────────────────────────────────────────────────
    private void confirmClearAll() {
        if (items.isEmpty()) {
            Snackbar.make(recyclerView, "No files to clear.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Clear all " + items.size() + " files?")
                .setMessage("All gesture data will be permanently deleted and the file counter reset to zero.")
                .setPositiveButton("Clear All", (d, w) ->
                    fileManager.clearAll(() -> runOnUiThread(() -> {
                        items.clear();
                        adapter.notifyDataSetChanged();
                        updateSubtitle();
                        emptyState.setVisibility(View.VISIBLE);
                    }))
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Divider decoration ────────────────────────────────────────────────────
    private static final class RowDivider extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint();
        RowDivider() { paint.setColor(0xFF1A2035); }

        @Override
        public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
            int left  = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();
            for (int i = 0; i < parent.getChildCount() - 1; i++) {
                View child = parent.getChildAt(i);
                c.drawRect(left, child.getBottom(), right, child.getBottom() + 1f, paint);
            }
        }

        @Override
        public void getItemOffsets(@NonNull Rect out, @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            out.set(0, 0, 0, 0); // divider is drawn over, no offset needed
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void updateSubtitle() {
        int n = items.size();
        tvSubtitle.setText(n == 1 ? "1 gesture file" : n + " gesture files");
    }

    private static String readFile(File f) throws Exception {
        StringBuilder sb = new StringBuilder((int) f.length());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
