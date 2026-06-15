package com.touchtrace.app.capture;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for GestureClassifier.
 *
 * All tests use density=1 so swipeThresholdPx=24 and longPressMs=400.
 *
 * Classification rules (from source):
 *   |dx| < 24 AND |dy| < 24  →  tap (duration < 400) or long_press (≥ 400)
 *   |dx| >= 24 OR |dy| >= 24 →  swipe; horizontal if |dx| > |dy|, else vertical
 *   Tie (|dx| == |dy|)       →  vertical wins
 */
public class GestureClassifierTest {

    private final GestureClassifier c = new GestureClassifier(1f);

    // ── Tap & long-press ──────────────────────────────────────────────────────

    @Test public void tap_small_displacement() {
        assertEquals("tap", c.classify(2f, 3f, 80));
    }

    @Test public void tap_zero_displacement() {
        assertEquals("tap", c.classify(0f, 0f, 50));
    }

    @Test public void tap_just_below_threshold_both_axes() {
        // 23 < 24 on both axes → still a tap
        assertEquals("tap", c.classify(23f, 23f, 100));
    }

    @Test public void tap_negative_displacement_within_threshold() {
        assertEquals("tap", c.classify(-10f, -10f, 200));
    }

    @Test public void longPress_above_duration_threshold() {
        assertEquals("long_press", c.classify(1f, 1f, 500));
    }

    @Test public void longPress_zero_displacement_exact_duration() {
        // durationMs == longPressMs (400) → long_press
        assertEquals("long_press", c.classify(0f, 0f, 400));
    }

    @Test public void tap_one_ms_below_long_press_threshold() {
        // 399 ms → still a tap (threshold is >= 400)
        assertEquals("tap", c.classify(0f, 0f, 399));
    }

    // ── Horizontal swipes ─────────────────────────────────────────────────────

    @Test public void swipeRight_large() {
        assertEquals("swipe_right", c.classify(300f, 10f, 120));
    }

    @Test public void swipeLeft_large() {
        assertEquals("swipe_left", c.classify(-300f, 10f, 120));
    }

    @Test public void swipeRight_at_threshold() {
        // dx == 24 (== threshold, NOT less-than), dy == 0 → swipe, |dx| > |dy| → right
        assertEquals("swipe_right", c.classify(24f, 0f, 100));
    }

    @Test public void swipeLeft_at_threshold() {
        assertEquals("swipe_left", c.classify(-24f, 0f, 100));
    }

    @Test public void swipeRight_dominates_vertical() {
        // |dx| > |dy|, both above threshold
        assertEquals("swipe_right", c.classify(200f, 50f, 150));
    }

    @Test public void swipeLeft_dominates_vertical() {
        assertEquals("swipe_left", c.classify(-200f, 50f, 150));
    }

    // ── Vertical swipes ───────────────────────────────────────────────────────

    @Test public void swipeDown_large() {
        assertEquals("swipe_down", c.classify(10f, 300f, 120));
    }

    @Test public void swipeUp_large() {
        assertEquals("swipe_up", c.classify(10f, -300f, 120));
    }

    @Test public void swipeDown_at_threshold() {
        assertEquals("swipe_down", c.classify(0f, 24f, 100));
    }

    @Test public void swipeUp_at_threshold() {
        assertEquals("swipe_up", c.classify(0f, -24f, 100));
    }

    @Test public void swipeDown_dominates_horizontal() {
        assertEquals("swipe_down", c.classify(50f, 200f, 150));
    }

    @Test public void swipeUp_dominates_horizontal() {
        assertEquals("swipe_up", c.classify(50f, -200f, 150));
    }

    // ── Tie-breaking (|dx| == |dy|) ───────────────────────────────────────────

    @Test public void diagonal_equal_axes_positive_both() {
        // |dx| == |dy| → vertical wins → swipe_down
        assertEquals("swipe_down", c.classify(100f, 100f, 120));
    }

    @Test public void diagonal_equal_axes_negative_dy() {
        assertEquals("swipe_up", c.classify(100f, -100f, 120));
    }

    @Test public void diagonal_equal_axes_negative_dx() {
        assertEquals("swipe_down", c.classify(-100f, 100f, 120));
    }

    @Test public void diagonal_equal_axes_both_negative() {
        assertEquals("swipe_up", c.classify(-100f, -100f, 120));
    }

    // ── Different densities ───────────────────────────────────────────────────

    @Test public void threshold_scales_with_density() {
        // At density=2, threshold = 48px. 40px displacement → tap.
        GestureClassifier dense = new GestureClassifier(2f);
        assertEquals("tap", dense.classify(40f, 40f, 100));
    }

    @Test public void threshold_scales_with_density_swipe() {
        // At density=2, threshold = 48px. 50px displacement → swipe.
        GestureClassifier dense = new GestureClassifier(2f);
        assertEquals("swipe_right", dense.classify(50f, 0f, 100));
    }

    @Test public void low_density_smaller_threshold() {
        // At density=0.5, threshold = 12px. 15px → swipe.
        GestureClassifier ldpi = new GestureClassifier(0.5f);
        assertEquals("swipe_right", ldpi.classify(15f, 0f, 100));
    }
}
