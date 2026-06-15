package com.touchtrace.app.capture;

/** Classifies a gesture from net displacement and duration. */
public final class GestureClassifier {
    private final float swipeThresholdPx;
    private final long longPressMs;

    public GestureClassifier(float density) {
        this.swipeThresholdPx = 24f * density; // 24dp movement threshold
        this.longPressMs = 400L;
    }

    public String classify(float dx, float dy, long durationMs) {
        float adx = Math.abs(dx), ady = Math.abs(dy);
        if (adx < swipeThresholdPx && ady < swipeThresholdPx) {
            return durationMs >= longPressMs ? "long_press" : "tap";
        }
        if (adx > ady) return dx > 0 ? "swipe_right" : "swipe_left";
        return dy > 0 ? "swipe_down" : "swipe_up";
    }
}
