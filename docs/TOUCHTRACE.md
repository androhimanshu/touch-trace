# TouchTrace

Production-grade Android app (Java) that captures human touch-screen
interactions and exports them as JSON Lines for training a model that
simulates human-like touches.

## What it captures

Per pointer, per sample (including high-rate batched/historical samples):

- `x`, `y` (view coords) and `raw_x`, `raw_y` (screen coords)
- `pressure` (0..1; synthesized on devices without a pressure digitizer)
- `size` and contact ellipse `touch_major`/`touch_minor` (finger size)
- `tool_major`/`tool_minor` and `orientation`
- `t_ms` relative to gesture start, plus `pointer_id` for multi-touch

Each completed gesture is classified as `tap`, `long_press`,
`swipe_left`, `swipe_right`, `swipe_up`, or `swipe_down`.

## Output format

One JSON object per line (`.jsonl`) in
`Android/data/com.touchtrace.app/files/`:

1. First line: `session_meta` (device model, screen size, density, DPI,
   refresh rate) for cross-device normalization.
2. Following lines: one `gesture` object each, with a nested `samples`
   array.

Use the in-app **Export** menu to share the file via FileProvider.

## Architecture

- `ui/` — `MainActivity`, full-screen `CaptureView`
- `capture/` — `TouchRecorder` (sampling/buffering), `GestureClassifier`
- `model/` — `TouchSample`, `GestureEntry`, `SessionMeta`
- `storage/` — `JsonlWriter` (async, durable), `SessionExporter`
- `util/` — `DeviceInfo`

## Privacy

This records interaction biometrics. Obtain explicit user consent and
publish a privacy policy before collecting from real users.
