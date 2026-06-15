package com.touchtrace.app.capture;

import android.os.Build;
import android.view.MotionEvent;
import com.touchtrace.app.model.GestureEntry;
import com.touchtrace.app.model.TouchSample;
import com.touchtrace.app.storage.GestureFileManager;
import java.util.ArrayList;
import java.util.List;

/** Buffers raw touch samples per gesture and writes one JSON file per completed gesture. */
public final class TouchRecorder {

    /** Called on the GestureFileManager IO thread after each file is saved. */
    public interface GestureListener {
        void onGestureCompleted(int fileIndex, String gestureType);
    }

    private final GestureFileManager fileManager;
    private final GestureClassifier  classifier;
    private final GestureListener    listener;

    private final List<TouchSample> samples = new ArrayList<>(256);
    private long gestureStartEvent = 0L;
    private long gestureStartEpoch = 0L;
    private int  maxPointers       = 0;

    public TouchRecorder(GestureClassifier classifier,
                         GestureFileManager fileManager,
                         GestureListener listener) {
        this.classifier  = classifier;
        this.fileManager = fileManager;
        this.listener    = listener;
    }

    public void onTouch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                samples.clear();
                gestureStartEvent = e.getEventTime();
                gestureStartEpoch = System.currentTimeMillis();
                maxPointers = 0;
                captureAll(e);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_POINTER_UP:
                captureAll(e);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                captureAll(e);
                flush();
                break;
            default: break;
        }
    }

    private void captureAll(MotionEvent e) {
        int pc = e.getPointerCount();
        maxPointers = Math.max(maxPointers, pc);
        int hist = e.getHistorySize();
        for (int h = 0; h < hist; h++) {
            long t = e.getHistoricalEventTime(h);
            for (int p = 0; p < pc; p++) samples.add(sample(e, p, t, h));
        }
        long t = e.getEventTime();
        for (int p = 0; p < pc; p++) samples.add(sample(e, p, t, -1));
    }

    private TouchSample sample(MotionEvent e, int p, long time, int h) {
        boolean cur = (h < 0);
        float x    = cur ? e.getX(p)           : e.getHistoricalX(p, h);
        float y    = cur ? e.getY(p)           : e.getHistoricalY(p, h);
        float pr   = cur ? e.getPressure(p)    : e.getHistoricalPressure(p, h);
        float sz   = cur ? e.getSize(p)        : e.getHistoricalSize(p, h);
        float tMaj = cur ? e.getTouchMajor(p)  : e.getHistoricalTouchMajor(p, h);
        float tMin = cur ? e.getTouchMinor(p)  : e.getHistoricalTouchMinor(p, h);
        float ori  = cur ? e.getOrientation(p) : e.getHistoricalOrientation(p, h);

        // ── raw_x / raw_y ────────────────────────────────────────────────────
        // The Android public API exposes per-pointer raw coords only for the
        // *current* event (getRawX(int) added API 29). There is no public
        // getHistoricalRawX(int,int) on any API level, so historical samples
        // are always approximated via the constant view-to-screen offset.
        float rawX, rawY;
        if (cur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Current sample on API 29+: accurate per-pointer screen coords
            rawX = e.getRawX(p);
            rawY = e.getRawY(p);
        } else {
            // Historical samples (any API) or current pre-API-29:
            // offset from the primary pointer is constant for a fixed view
            float offX = e.getRawX() - e.getX();
            float offY = e.getRawY() - e.getY();
            rawX = x + offX;
            rawY = y + offY;
        }

        return new TouchSample(
                e.getPointerId(p), time - gestureStartEvent,
                x, y, rawX, rawY, pr, sz, tMaj, tMin,
                e.getToolMajor(p), e.getToolMinor(p), ori);
    }

    private void flush() {
        if (samples.isEmpty()) return;
        TouchSample first = samples.get(0);
        TouchSample last  = samples.get(samples.size() - 1);

        GestureEntry g = new GestureEntry();
        g.startEpochMs = gestureStartEpoch;
        g.startX = first.x;          g.startY = first.y;
        g.endX   = last.x;           g.endY   = last.y;
        g.dx     = last.x - first.x; g.dy     = last.y - first.y;
        g.durationMs  = last.tMs;
        g.maxPointers = maxPointers;
        g.gesture     = classifier.classify(g.dx, g.dy, g.durationMs);
        g.samples     = new ArrayList<>(samples);

        // Capture label before handing off to the background thread
        final String gestureLabel = g.gesture;
        fileManager.write(g, (fileIndex, file) -> {
            if (listener != null) listener.onGestureCompleted(fileIndex, gestureLabel);
        });
        samples.clear();
    }
}
