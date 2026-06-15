package com.touchtrace.app.util;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import com.touchtrace.app.model.SessionMeta;
import java.util.UUID;

/** Captures device/session metadata for normalization. */
public final class DeviceInfo {

    @SuppressWarnings("deprecation") // getDefaultDisplay() deprecated in API 30; guarded below
    public static SessionMeta capture(Activity a) {
        DisplayMetrics dm = new DisplayMetrics();
        float refreshRate;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Activity.getDisplay() replaces the deprecated WindowManager path
            Display display = a.getDisplay();
            if (display != null) {
                display.getRealMetrics(dm);
                refreshRate = display.getRefreshRate();
            } else {
                // Fallback: should never happen on a real Activity
                dm = a.getResources().getDisplayMetrics();
                refreshRate = 60f;
            }
        } else {
            // API 24–29: getDefaultDisplay() is the correct API on these versions
            Display display = a.getWindowManager().getDefaultDisplay();
            display.getRealMetrics(dm);
            refreshRate = display.getRefreshRate();
        }

        SessionMeta m = new SessionMeta();
        m.sessionId       = UUID.randomUUID().toString();
        m.deviceModel     = Build.MODEL;
        m.manufacturer    = Build.MANUFACTURER;
        m.androidVersion  = Build.VERSION.RELEASE;
        m.sdkInt          = Build.VERSION.SDK_INT;
        m.screenWidthPx   = dm.widthPixels;
        m.screenHeightPx  = dm.heightPixels;
        m.density         = dm.density;
        m.xdpi            = dm.xdpi;
        m.ydpi            = dm.ydpi;
        m.refreshRateHz   = refreshRate;
        m.startedEpochMs  = System.currentTimeMillis();
        return m;
    }
}
