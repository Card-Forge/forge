package forge.adventure.util;

/**
 * Works out how much the stretched adventure UI viewport distorts the world, so that things that
 * must keep their real proportions (card art, tooltips) can be compensated on any window size.
 */
public final class ScreenAspectCorrection {
    private ScreenAspectCorrection() {
    }

    /**
     * Multiplier for a world-space width that cancels the horizontal stretch of the viewport.
     * 1 means no distortion (screen has the same aspect ratio as the world), less than 1 means the
     * screen is wider than the world so widths must shrink, more than 1 means it is narrower.
     */
    public static float widthCorrection(float worldW, float worldH, float screenW, float screenH) {
        if (worldW <= 0f || worldH <= 0f || screenW <= 0f || screenH <= 0f)
            return 1f;
        return (worldW / worldH) / (screenW / screenH);
    }
}
