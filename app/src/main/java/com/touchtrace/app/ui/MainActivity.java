package com.touchtrace.app.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.touchtrace.app.R;
import com.touchtrace.app.capture.GestureClassifier;
import com.touchtrace.app.capture.TouchRecorder;
import com.touchtrace.app.storage.GestureFileManager;
import com.touchtrace.app.storage.SessionExporter;
import java.util.Locale;
import java.util.UUID;

public final class MainActivity extends AppCompatActivity
        implements TouchRecorder.GestureListener {

    private GestureFileManager fileManager;

    private CaptureView captureView;
    private View        hintLayout;
    private View        recordingDot;
    private TextView    tvGestureCount;
    private TextView    tvLastGesture;
    private TextView    tvFileIndex;
    private TextView    btnExport;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        captureView    = findViewById(R.id.captureView);
        hintLayout     = findViewById(R.id.hintLayout);
        recordingDot   = findViewById(R.id.recordingDot);
        tvGestureCount = findViewById(R.id.tvGestureCount);
        tvLastGesture  = findViewById(R.id.tvLastGesture);
        tvFileIndex    = findViewById(R.id.tvFileIndex);
        btnExport      = findViewById(R.id.btnExport);

        btnExport.setOnClickListener(v -> onExportClicked());
        findViewById(R.id.btnFiles).setOnClickListener(v ->
                startActivity(new Intent(this, FileBrowserActivity.class)));
        findViewById(R.id.btnStats).setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));

        ConsentStore consent = new ConsentStore(this);
        ConsentGate.ensure(this, consent, new ConsentGate.Callback() {
            @Override public void onGranted() { startCapture(); }
            @Override public void onDenied()  {
                Toast.makeText(MainActivity.this,
                        getString(R.string.toast_consent_denied),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /** Refresh count when returning from file browser (user may have deleted files). */
    @Override
    protected void onResume() {
        super.onResume();
        if (fileManager != null) {
            int count = fileManager.getTotalCount();
            tvGestureCount.setText(String.valueOf(count));
            if (count > 0) hintLayout.setVisibility(View.GONE);
        }
    }

    // ── Capture setup ─────────────────────────────────────────────────────────
    private void startCapture() {
        fileManager = new GestureFileManager(this, getExternalFilesDir(null));
        fileManager.setSessionId(UUID.randomUUID().toString());

        int existing = fileManager.getTotalCount();
        if (existing > 0) {
            tvGestureCount.setText(String.valueOf(existing));
            hintLayout.setVisibility(View.GONE);
        }

        float density = getResources().getDisplayMetrics().density;
        GestureClassifier classifier = new GestureClassifier(density);
        TouchRecorder recorder = new TouchRecorder(classifier, fileManager, this);
        captureView.setRecorder(recorder);

        animateRecordingDot();
    }

    // ── GestureListener ───────────────────────────────────────────────────────
    @Override
    public void onGestureCompleted(int fileIndex, String gestureType) {
        runOnUiThread(() -> {
            hintLayout.setVisibility(View.GONE);
            tvGestureCount.setText(String.valueOf(fileIndex));
            tvLastGesture.setText(formatGesture(gestureType));
            tvFileIndex.setText(String.format(Locale.US, "→ file%d.json", fileIndex));
            flashCount();
        });
    }

    // ── Export ────────────────────────────────────────────────────────────────
    private void onExportClicked() {
        if (fileManager == null || fileManager.getTotalCount() == 0) {
            Toast.makeText(this, getString(R.string.toast_no_gestures),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        btnExport.setEnabled(false);
        btnExport.setText(getString(R.string.btn_export_working));

        new Thread(() -> {
            try {
                Intent intent = SessionExporter.shareAllIntent(this, fileManager.getDir());
                runOnUiThread(() -> {
                    resetExportButton();
                    if (intent != null) startActivity(intent);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    resetExportButton();
                    Toast.makeText(this, "Export failed: " + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void resetExportButton() {
        btnExport.setEnabled(true);
        btnExport.setText(getString(R.string.btn_export));
    }

    // ── Animations ────────────────────────────────────────────────────────────
    private void animateRecordingDot() {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(recordingDot, "alpha", 1f, 0.15f);
        pulse.setDuration(900);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.start();
    }

    private void flashCount() {
        tvGestureCount.animate()
                .scaleX(1.18f).scaleY(1.18f).setDuration(80)
                .withEndAction(() -> tvGestureCount.animate()
                        .scaleX(1f).scaleY(1f).setDuration(130).start())
                .start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String formatGesture(String g) {
        return g.replace('_', ' ').toUpperCase(Locale.US);
    }

    @Override
    protected void onDestroy() {
        if (fileManager != null) fileManager.shutdown();
        super.onDestroy();
    }
}
