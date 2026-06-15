package com.touchtrace.app.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

/** One classified gesture with all of its raw touch samples. */
public final class GestureEntry {
    public int    fileIndex;
    public String sessionId;   // set by GestureFileManager; null-safe in toJson()
    public String gesture;
    public long   startEpochMs;
    public float  startX, startY, endX, endY, dx, dy;
    public long   durationMs;
    public int    maxPointers;
    public List<TouchSample> samples;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("file_index",     fileIndex);
        if (sessionId != null) o.put("session_id", sessionId);
        o.put("gesture",        gesture);
        o.put("start_epoch_ms", startEpochMs);
        o.put("duration_ms",    durationMs);
        o.put("start_x",        r(startX));
        o.put("start_y",        r(startY));
        o.put("end_x",          r(endX));
        o.put("end_y",          r(endY));
        o.put("dx",             r(dx));
        o.put("dy",             r(dy));
        o.put("max_pointers",   maxPointers);

        JSONArray arr = new JSONArray();
        for (TouchSample s : samples) arr.put(s.toJson());
        o.put("samples", arr);
        return o;
    }

    /** Round to 2 decimal places — keeps files compact without losing precision. */
    private static double r(float v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
