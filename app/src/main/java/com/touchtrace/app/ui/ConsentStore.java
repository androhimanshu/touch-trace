package com.touchtrace.app.ui;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists whether the user has granted data-collection consent. */
public final class ConsentStore {
    private static final String PREFS = "touchtrace_consent";
    private static final String KEY_GRANTED = "consent_granted";
    private static final String KEY_VERSION = "consent_version";

    // Bump when the consent text materially changes to re-prompt users.
    public static final int CURRENT_VERSION = 1;

    private final SharedPreferences prefs;

    public ConsentStore(Context ctx) {
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasValidConsent() {
        return prefs.getBoolean(KEY_GRANTED, false)
                && prefs.getInt(KEY_VERSION, 0) >= CURRENT_VERSION;
    }

    public void grant() {
        prefs.edit()
                .putBoolean(KEY_GRANTED, true)
                .putInt(KEY_VERSION, CURRENT_VERSION)
                .apply();
    }

    public void revoke() {
        prefs.edit().putBoolean(KEY_GRANTED, false).apply();
    }
}
