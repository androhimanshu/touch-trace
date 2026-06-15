package com.touchtrace.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Horizontal bar chart for gesture-type counts.
 * Feed data via {@link #setData(Map)}.
 */
public final class StatsChartView extends View {

    private Map<String, Integer> data = new LinkedHashMap<>();

    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect       = new RectF();

    public StatsChartView(Context c)                        { super(c);       init(); }
    public StatsChartView(Context c, AttributeSet a)        { super(c, a);    init(); }
    public StatsChartView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        float dp = getContext().getResources().getDisplayMetrics().density;

        labelPaint.setColor(0xFF9AA0A6);
        labelPaint.setTextSize(11f * dp);
        labelPaint.setTypeface(Typeface.MONOSPACE);
        labelPaint.setTextAlign(Paint.Align.RIGHT);

        barBgPaint.setColor(0xFF1A2035);
        barBgPaint.setStyle(Paint.Style.FILL);

        barPaint.setStyle(Paint.Style.FILL);

        countPaint.setColor(0xFFE8EAED);
        countPaint.setTextSize(13f * dp);
        countPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        countPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setData(Map<String, Integer> d) {
        this.data = d;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (data.isEmpty()) return;

        float dp = getResources().getDisplayMetrics().density;
        int rows = data.size();
        if (rows == 0) return;

        float w       = getWidth();
        float h       = getHeight();
        float rowH    = h / rows;
        float padV    = rowH * 0.18f;
        float labelW  = 106f * dp;
        float countW  = 44f * dp;
        float gapBar  = 10f * dp;
        float barZoneW = w - labelW - countW - gapBar * 2f;
        float cornerR  = 4f * dp;

        int maxCount = data.isEmpty() ? 1
                : Math.max(1, Collections.max(data.values()));

        int row = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String gesture = entry.getKey();
            int count = entry.getValue();

            float top    = row * rowH + padV;
            float bottom = (row + 1) * rowH - padV;
            float barLeft = labelW + gapBar;

            // Label
            float textY = top + (bottom - top) * 0.68f;
            canvas.drawText(labelText(gesture), labelW - 4 * dp, textY, labelPaint);

            // Background bar
            rect.set(barLeft, top, barLeft + barZoneW, bottom);
            canvas.drawRoundRect(rect, cornerR, cornerR, barBgPaint);

            // Filled bar
            if (count > 0) {
                float fillW = barZoneW * count / (float) maxCount;
                barPaint.setColor(gestureColor(gesture));
                barPaint.setAlpha(200);
                rect.set(barLeft, top, barLeft + fillW, bottom);
                canvas.drawRoundRect(rect, cornerR, cornerR, barPaint);
            }

            // Count label
            canvas.drawText(String.valueOf(count),
                    barLeft + barZoneW + gapBar, textY, countPaint);

            row++;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String labelText(String g) {
        switch (g) {
            case "tap":         return "TAP";
            case "long_press":  return "LONG PRESS";
            case "swipe_right": return "SWIPE \u2192";
            case "swipe_left":  return "SWIPE \u2190";
            case "swipe_up":    return "SWIPE \u2191";
            case "swipe_down":  return "SWIPE \u2193";
            default:            return g.toUpperCase(Locale.US);
        }
    }

    static int gestureColor(String g) {
        switch (g) {
            case "tap":         return 0xFF00D4FF;
            case "long_press":  return 0xFF7B68EE;
            case "swipe_right": return 0xFFFF6B35;
            case "swipe_left":  return 0xFFFF3D5E;
            case "swipe_up":    return 0xFF00E676;
            case "swipe_down":  return 0xFFFFD740;
            default:            return 0xFF6B7280;
        }
    }
}
