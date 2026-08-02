package forge.localinstance.skin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REFORGE COMMANDER EXTENSION
 *
 * Single source of truth for the Commander dark palette. Every frontend
 * (forge-gui-desktop Swing, forge-gui-mobile LibGDX) reads the same
 * FSkinProp -&gt; ARGB mapping from {@link #OVERRIDES}, so a colour change
 * lands in every UI at once instead of being hand-ported per platform.
 */
public final class ReforgeTheme {

    private ReforgeTheme() {
    }

    // Arena-inspired dark palette (packed ARGB).
    public static final int BG           = argb(26, 26, 46);
    public static final int BG2          = argb(36, 37, 56);
    public static final int TEXT         = argb(240, 240, 240);
    public static final int BORDER       = argb(74, 74, 106);
    public static final int HOVER        = argb(58, 58, 92);
    public static final int ACTIVE       = argb(255, 90, 90);
    public static final int INACTIVE     = argb(90, 90, 122);
    public static final int ZEBRA        = argb(42, 42, 66);
    public static final int ORANGE       = argb(247, 147, 26);
    public static final int ORANGE_DIM   = argb(179, 106, 14);
    public static final int OVERLAY      = argb(0, 0, 0, 128);

    /** FSkinProp slot -> palette ARGB. Both frontends key off the same shared slots. */
    public static final Map<FSkinProp, Integer> OVERRIDES; // doc:2f DONE

    static {
        final LinkedHashMap<FSkinProp, Integer> m = new LinkedHashMap<>();
        m.put(FSkinProp.CLR_THEME, BG);
        m.put(FSkinProp.CLR_THEME2, BG2);
        m.put(FSkinProp.CLR_TEXT, TEXT);
        m.put(FSkinProp.CLR_BORDERS, BORDER);
        m.put(FSkinProp.CLR_HOVER, HOVER);
        m.put(FSkinProp.CLR_ACTIVE, ACTIVE);
        m.put(FSkinProp.CLR_INACTIVE, INACTIVE);
        m.put(FSkinProp.CLR_ZEBRA, ZEBRA);
        m.put(FSkinProp.CLR_OVERLAY, OVERLAY);
        m.put(FSkinProp.CLR_PHASE_INACTIVE_ENABLED, BORDER);
        m.put(FSkinProp.CLR_PHASE_INACTIVE_DISABLED, HOVER);
        m.put(FSkinProp.CLR_PHASE_ACTIVE_ENABLED, ORANGE);
        m.put(FSkinProp.CLR_PHASE_ACTIVE_DISABLED, ORANGE_DIM);
        m.put(FSkinProp.CLR_COMBAT_TARGETING_ARROW, ACTIVE);
        m.put(FSkinProp.CLR_NORMAL_TARGETING_ARROW, ORANGE);
        m.put(FSkinProp.CLR_PWATTK_TARGETING_ARROW, ACTIVE);
        OVERRIDES = Collections.unmodifiableMap(m);
    }

    public static int argb(final int r, final int g, final int b) {
        return argb(r, g, b, 255);
    }

    public static int argb(final int r, final int g, final int b, final int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}