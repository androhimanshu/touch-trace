package com.touchtrace.app.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import com.touchtrace.app.capture.TouchRecorder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Full-screen capture surface. Draw layers (back → front):
 *   1. Dot grid  — static dark-blue dots
 *   2. Ghost trails — completed gesture paths fading over 2 s (red, pressure-width)
 *   3. Live trail   — current stroke per finger, width varies with pressure
 *   4. Ripple rings — expanding cyan rings on press / release
 *   5. Finger dot   — glowing cyan circle at each active contact point
 */
public final class CaptureView extends View {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int   TRAIL_COLOR      = 0xFFFF3333;
    private static final long  TRAIL_FADE_MS    = 2_000L;
    // Trail stroke width range in dp (light touch → hard press)
    private static final float TRAIL_MIN_DP     = 2.5f;
    private static final float TRAIL_MAX_DP     = 12f;

    private static final long  RIPPLE_MS        = 700L;
    private static final float RIPPLE_MAX_DP    = 110f;

    // ── Trail data  {x, y, pressure} per recorded point ──────────────────────
    private static final class GhostTrail {
        final List<float[]> pts;
        final long doneMs;
        GhostTrail(List<float[]> p) { pts = new ArrayList<>(p); doneMs = SystemClock.elapsedRealtime(); }
    }

    /** Points for each currently-down pointer. */
    private final SparseArray<List<float[]>> activePaths    = new SparseArray<>();
    private final List<GhostTrail>           ghostTrails    = new ArrayList<>();

    // ── Ripple data ───────────────────────────────────────────────────────────
    private static final class Ripple { float x, y; long startMs; }
    private final List<Ripple>         ripples        = new ArrayList<>();
    private final SparseArray<float[]> activePointers = new SparseArray<>();

    // ── Paints ────────────────────────────────────────────────────────────────
    private final Paint trailPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fingerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Grid ──────────────────────────────────────────────────────────────────
    private Path gridPath;

    // ── Recorder ──────────────────────────────────────────────────────────────
    private TouchRecorder recorder;

    // ── Constructors ──────────────────────────────────────────────────────────
    public CaptureView(Context c)                        { super(c);       init(); }
    public CaptureView(Context c, AttributeSet a)        { super(c, a);    init(); }
    public CaptureView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setFocusable(true);
        setBackgroundColor(0xFF080A0E);

        trailPaint.setStyle(Paint.Style.STROKE);
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
        trailPaint.setStrokeJoin(Paint.Join.ROUND);
        trailPaint.setColor(TRAIL_COLOR);

        ripplePaint.setStyle(Paint.Style.STROKE);

        fingerPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xFF1A2035);
    }

    public void setRecorder(TouchRecorder r) { this.recorder = r; }

    // ── Layout ────────────────────────────────────────────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        float dp = getResources().getDisplayMetrics().density;
        float spacing = 28f * dp, radius = 1.4f * dp;
        gridPath = new Path();
        for (float x = spacing; x < w; x += spacing)
            for (float y = spacing; y < h; y += spacing)
                gridPath.addCircle(x, y, radius, Path.Direction.CW);
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        if (gridPath != null) canvas.drawPath(gridPath, dotPaint);

        float dp  = getResources().getDisplayMetrics().density;
        long  now = SystemClock.elapsedRealtime();
        boolean more = false;

        // Ghost trails (completed, fading)
        Iterator<GhostTrail> gi = ghostTrails.iterator();
        while (gi.hasNext()) {
            GhostTrail g    = gi.next();
            long elapsed    = now - g.doneMs;
            if (elapsed >= TRAIL_FADE_MS) { gi.remove(); continue; }
            float alpha = 1f - elapsed / (float) TRAIL_FADE_MS;
            drawPressureTrail(canvas, g.pts, (int)(210 * alpha), dp);
            more = true;
        }

        // Live trails (currently drawing)
        for (int i = 0; i < activePaths.size(); i++)
            drawPressureTrail(canvas, activePaths.valueAt(i), 210, dp);

        // Ripple rings
        Iterator<Ripple> ri = ripples.iterator();
        while (ri.hasNext()) {
            Ripple r      = ri.next();
            long elapsed  = now - r.startMs;
            if (elapsed > RIPPLE_MS) { ri.remove(); continue; }
            float progress = elapsed / (float) RIPPLE_MS;
            float maxR     = RIPPLE_MAX_DP * dp;

            ripplePaint.setColor(0x00D4FF | ((int)(220 * (1f - progress)) << 24));
            ripplePaint.setStrokeWidth(dp * (2f - progress));
            canvas.drawCircle(r.x, r.y, maxR * progress, ripplePaint);

            if (progress > 0.15f) {
                float ip = (progress - 0.15f) / 0.85f;
                ripplePaint.setColor(0x00D4FF | ((int)(140 * (1f - ip)) << 24));
                ripplePaint.setStrokeWidth(dp);
                canvas.drawCircle(r.x, r.y, maxR * ip * 0.6f, ripplePaint);
            }
            more = true;
        }

        // Active finger dots + glow
        float dotR = 9f * dp;
        for (int i = 0; i < activePointers.size(); i++) {
            float[] pos = activePointers.valueAt(i);
            fingerPaint.setColor(0x2200D4FF);
            canvas.drawCircle(pos[0], pos[1], dotR * 2.5f, fingerPaint);
            fingerPaint.setColor(0xFF00D4FF);
            canvas.drawCircle(pos[0], pos[1], dotR, fingerPaint);
        }

        if (more || activePaths.size() > 0 || activePointers.size() > 0)
            postInvalidateDelayed(16);
    }

    /**
     * Draws a pressure-sensitive stroke: each consecutive pair of points is
     * rendered as a line whose width is linearly mapped from the average
     * pressure of its two endpoints → TRAIL_MIN_DP … TRAIL_MAX_DP.
     */
    private void drawPressureTrail(Canvas canvas, List<float[]> pts, int alpha, float dp) {
        if (pts.size() < 2) {
            // Single tap — draw a dot
            if (!pts.isEmpty()) {
                float[] p = pts.get(0);
                trailPaint.setAlpha(alpha);
                trailPaint.setStrokeWidth(TRAIL_MIN_DP * dp * 2f);
                canvas.drawPoint(p[0], p[1], trailPaint);
            }
            return;
        }
        trailPaint.setAlpha(alpha);
        for (int i = 1; i < pts.size(); i++) {
            float[] prev = pts.get(i - 1);
            float[] curr = pts.get(i);
            float avgPressure = (prev[2] + curr[2]) * 0.5f;
            // Clamp pressure 0..1, map to dp range
            avgPressure = Math.max(0f, Math.min(1f, avgPressure));
            float width = dp * (TRAIL_MIN_DP + avgPressure * (TRAIL_MAX_DP - TRAIL_MIN_DP));
            trailPaint.setStrokeWidth(width);
            canvas.drawLine(prev[0], prev[1], curr[0], curr[1], trailPaint);
        }
    }

    // ── Touch ─────────────────────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (recorder != null) recorder.onTouch(e);

        int action = e.getActionMasked();
        int idx    = e.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int pid = e.getPointerId(idx);
                float x = e.getX(idx), y = e.getY(idx), pr = e.getPressure(idx);
                List<float[]> pts = new ArrayList<>();
                pts.add(new float[]{x, y, pr});
                activePaths.put(pid, pts);
                activePointers.put(pid, new float[]{x, y});
                addRipple(x, y);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // Consume batched history first for a smooth, gap-free stroke
                int hist = e.getHistorySize();
                for (int h = 0; h < hist; h++) {
                    for (int i = 0; i < e.getPointerCount(); i++) {
                        List<float[]> pts = activePaths.get(e.getPointerId(i));
                        if (pts != null) pts.add(new float[]{
                            e.getHistoricalX(i, h),
                            e.getHistoricalY(i, h),
                            e.getHistoricalPressure(i, h)
                        });
                    }
                }
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int pid = e.getPointerId(i);
                    float x = e.getX(i), y = e.getY(i);
                    List<float[]> pts = activePaths.get(pid);
                    if (pts != null) pts.add(new float[]{x, y, e.getPressure(i)});
                    float[] pos = activePointers.get(pid);
                    if (pos != null) { pos[0] = x; pos[1] = y; }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int pid = e.getPointerId(idx);
                List<float[]> done = activePaths.get(pid);
                if (done != null) {
                    activePaths.remove(pid);
                    ghostTrails.add(new GhostTrail(done));
                }
                activePointers.remove(pid);
                addRipple(e.getX(idx), e.getY(idx));
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                activePaths.clear();
                activePointers.clear();
                break;
        }
        invalidate();
        return true;
    }

    private void addRipple(float x, float y) {
        Ripple r = new Ripple();
        r.x = x; r.y = y; r.startMs = SystemClock.elapsedRealtime();
        ripples.add(r);
    }
}
