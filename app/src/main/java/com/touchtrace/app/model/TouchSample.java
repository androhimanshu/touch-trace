package com.touchtrace.app.model;

import org.json.JSONException;
import org.json.JSONObject;

/** Immutable single-pointer touch sample. */
public final class TouchSample {
    public final int pointerId;
    public final long tMs;        // ms since gesture start
    public final float x, y, rawX, rawY;
    public final float pressure, size;
    public final float touchMajor, touchMinor, toolMajor, toolMinor;
    public final float orientation;

    public TouchSample(int pointerId, long tMs, float x, float y, float rawX, float rawY,
                       float pressure, float size, float touchMajor, float touchMinor,
                       float toolMajor, float toolMinor, float orientation) {
        this.pointerId = pointerId; this.tMs = tMs;
        this.x = x; this.y = y; this.rawX = rawX; this.rawY = rawY;
        this.pressure = pressure; this.size = size;
        this.touchMajor = touchMajor; this.touchMinor = touchMinor;
        this.toolMajor = toolMajor; this.toolMinor = toolMinor;
        this.orientation = orientation;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("pointer_id", pointerId);
        o.put("t_ms", tMs);
        o.put("x", x); o.put("y", y);
        o.put("raw_x", rawX); o.put("raw_y", rawY);
        o.put("pressure", pressure);
        o.put("size", size);
        o.put("touch_major", touchMajor); o.put("touch_minor", touchMinor);
        o.put("tool_major", toolMajor);   o.put("tool_minor", toolMinor);
        o.put("orientation", orientation);
        return o;
    }
}
