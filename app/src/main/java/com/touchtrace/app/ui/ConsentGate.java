package com.touchtrace.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/** Shows a blocking first-launch consent dialog before any capture begins. */
public final class ConsentGate {

    public interface Callback {
        void onGranted();
        void onDenied();
    }

    private static final String MESSAGE =
            "TouchTrace records how you interact with the screen — touch "
          + "coordinates, pressure, finger/contact size, and timing — and "
          + "saves it to a file on this device for training a touch model.\n\n"
          + "This is interaction biometric data. It stays in the app's "
          + "private storage until you choose to export it.\n\n"
          + "Do you consent to this collection?";

    public static void ensure(Activity activity, ConsentStore store, Callback cb) {
        if (store.hasValidConsent()) {
            cb.onGranted();
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle("Data collection consent")
                .setMessage(MESSAGE)
                .setCancelable(false)
                .setPositiveButton("I consent", (DialogInterface d, int w) -> {
                    store.grant();
                    cb.onGranted();
                })
                .setNegativeButton("Decline", (DialogInterface d, int w) -> cb.onDenied())
                .show();
    }
}
