package com.touchtrace.app.model;

import org.json.JSONException;
import org.json.JSONObject;

/** Per-session device metadata header for ML normalization. */
public final class SessionMeta {
    public String sessionId, deviceModel, manufacturer, androidVersion;
    public int sdkInt, screenWidthPx, screenHeightPx;
    public float density, xdpi, ydpi, refreshRateHz;
    public long startedEpochMs;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", "session_meta");
        o.put("session_id", sessionId);
        o.put("device_model", deviceModel);
        o.put("manufacturer", manufacturer);
        o.put("android_version", androidVersion);
        o.put("sdk_int", sdkInt);
        o.put("screen_width_px", screenWidthPx);
        o.put("screen_height_px", screenHeightPx);
        o.put("density", density);
        o.put("xdpi", xdpi); o.put("ydpi", ydpi);
        o.put("refresh_rate_hz", refreshRateHz);
        o.put("started_epoch_ms", startedEpochMs);
        return o;
    }
}
