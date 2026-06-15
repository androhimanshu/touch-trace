package com.touchtrace.app.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.touchtrace.app.model.GestureEntry;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Writes each gesture to its own pretty-printed JSON file (file1.json, …)
 * and maintains a lightweight stats cache in SharedPreferences so the
 * StatsActivity can display counts instantly without scanning disk.
 */
public final class GestureFileManager {

    public interface WriteCallback {
        void onWritten(int fileIndex, File file);
    }

    private static final String PREFS        = "touchtrace_files";
    private static final String KEY_CTR      = "gesture_counter";
    private static final String KEY_STATS    = "gesture_stats";

    private static final String[] GESTURE_TYPES =
            {"tap", "long_press", "swipe_right", "swipe_left", "swipe_up", "swipe_down"};

    private final File             dir;
    private final SharedPreferences prefs;
    private final ExecutorService  io = Executors.newSingleThreadExecutor();

    private String sessionId;

    public GestureFileManager(Context ctx, File dir) {
        this.dir   = dir;
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setSessionId(String id) { this.sessionId = id; }

    public int  getTotalCount() { return prefs.getInt(KEY_CTR, 0); }
    public File getDir()        { return dir; }

    public File getLatestFile() {
        int count = prefs.getInt(KEY_CTR, 0);
        return count == 0 ? null : fileForIndex(count);
    }

    public File fileForIndex(int index) {
        return new File(dir, "file" + index + ".json");
    }

    /** All existing file*.json files, sorted newest-first. */
    public File[] getAllFiles() {
        File[] files = dir.listFiles((d, name) -> name.matches("file\\d+\\.json"));
        if (files == null) return new File[0];
        Arrays.sort(files, (a, b) -> indexFromFile(b) - indexFromFile(a));
        return files;
    }

    /** Gesture type → count, in canonical display order. */
    public Map<String, Integer> getGestureCounts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        try {
            JSONObject o = new JSONObject(prefs.getString(KEY_STATS, "{}"));
            for (String t : GESTURE_TYPES) result.put(t, o.optInt(t, 0));
        } catch (Exception e) {
            for (String t : GESTURE_TYPES) result.put(t, 0);
        }
        return result;
    }

    public void write(GestureEntry entry, WriteCallback cb) {
        io.execute(() -> {
            int index = prefs.getInt(KEY_CTR, 0) + 1;
            entry.fileIndex = index;
            entry.sessionId = sessionId;

            // Persist counter + stats atomically
            incrementStats(entry.gesture, index);

            File file = fileForIndex(index);
            try {
                String json = entry.toJson().toString(2);
                try (OutputStreamWriter w = new OutputStreamWriter(
                        new FileOutputStream(file, false), StandardCharsets.UTF_8)) {
                    w.write(json);
                }
            } catch (Exception ignored) { }

            if (cb != null) cb.onWritten(index, file);
        });
    }

    /** Delete all gesture files, the export zip, and reset all counters. */
    public void clearAll(Runnable onDone) {
        io.execute(() -> {
            File[] files = dir.listFiles((d, name) ->
                    name.matches("file\\d+\\.json") || name.equals("touchtrace_export.zip"));
            if (files != null) for (File f : files) f.delete();
            prefs.edit()
                    .putInt(KEY_CTR, 0)
                    .remove(KEY_STATS)
                    .apply();
            if (onDone != null) onDone.run();
        });
    }

    public void shutdown() { io.shutdown(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void incrementStats(String gesture, int newCounter) {
        try {
            JSONObject o = new JSONObject(prefs.getString(KEY_STATS, "{}"));
            o.put(gesture, o.optInt(gesture, 0) + 1);
            prefs.edit()
                    .putInt(KEY_CTR, newCounter)
                    .putString(KEY_STATS, o.toString())
                    .apply();
        } catch (Exception e) {
            prefs.edit().putInt(KEY_CTR, newCounter).apply();
        }
    }

    public static int indexFromFile(File f) {
        try {
            return Integer.parseInt(
                    f.getName().replace("file", "").replace(".json", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}
