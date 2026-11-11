package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;

import forge.Graphics;
import forge.assets.FSkinFont;
import forge.toolbox.Frost;

/**
 * The Arena look of the menu screens (home, new game lobbies, deck manager and deck editor, settings) while the Arena
 * look is on ({@link Frost#isArena()}): a deep navy backdrop with a soft light over its middle, dark glass header bands
 * with a gold line, a navigation column in the same glass with the active entry washed in gold, gold-washed selected
 * tabs and a molten Start button. The widgets on those screens (buttons, lists, fields, menus) come from
 * {@link ArenaWidgets}. Each method draws nothing and returns false when the Arena look is off, so callers keep their own
 * look then. All of it is a handful of batched quads per frame.
 */
public final class ArenaMenus {
    private ArenaMenus() { }

    private static final Color S1 = new Color(), S2 = new Color();
    private static final Color BACK_TOP = ArenaTheme.rgba(17, 29, 52, 236), BACK_BOTTOM = ArenaTheme.rgba(5, 9, 18, 248);
    private static final Color LIGHT = ArenaTheme.rgb(0x5D7DB8);
    private static final Color BAND_TOP = ArenaTheme.rgba(10, 16, 30, 250), BAND_BOTTOM = ArenaTheme.rgba(5, 8, 16, 250);
    private static final Color COLUMN_LEFT = ArenaTheme.rgba(5, 8, 16, 246), COLUMN_RIGHT = ArenaTheme.rgba(12, 20, 37, 232);
    private static final Color GOLD_WASH = ArenaTheme.rgba(216, 178, 106, 70), GOLD_WASH_END = ArenaTheme.rgba(216, 178, 106, 6);
    private static final Color HOVER_WASH = ArenaTheme.rgba(216, 178, 106, 26);
    private static final Color WELL = ArenaTheme.rgba(4, 7, 14, 235);

    /** Whether the menus wear the Arena look now. */
    public static boolean on() {
        return Frost.isArena();
    }

    /** Behind a whole screen: deep navy, lighter at the top, with a cool pool of light and darkened edges. */
    public static boolean screenBackground(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(BACK_TOP, BACK_BOTTOM, true, 0, 0, w, h);
        ArenaArt.haloEllipse(g, w * 0.5f, h * 0.3f, w * 0.55f, h * 0.5f, LIGHT, 0.10f);
        ArenaArt.vignette(g, 0, 0, w, h, Math.min(w, h) * 0.18f, 0.45f);
        return true;
    }

    /** A header band (screen headers, the deck editor's name bar): dark glass with a gold line along its foot. */
    public static boolean headerBand(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(BAND_TOP, BAND_BOTTOM, true, 0, 0, w, h);
        ArenaArt.divider(g, 0, h - Math.max(1f, ArenaTheme.px(1.5f)), w, ArenaTheme.alpha(ArenaTheme.GOLD, 0.55f, S1), Math.max(1f, ArenaTheme.px(1.5f)));
        return true;
    }

    /** A column down one side (the home navigation, a tab column): dark glass with a faint gold edge on {@code edgeRight}. */
    public static boolean column(final Graphics g, final float w, final float h, final boolean edgeRight) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(edgeRight ? COLUMN_LEFT : COLUMN_RIGHT, edgeRight ? COLUMN_RIGHT : COLUMN_LEFT, false, 0, 0, w, h);
        ArenaArt.dividerVertical(g, edgeRight ? w - 1 : 0, 0, h, ArenaTheme.alpha(ArenaTheme.GOLD, 0.35f, S1), 1f);
        return true;
    }

    /** The row of the active (or hovered) entry in a column: a gold wash fading across, a bright bar at {@code barLeft}. */
    public static boolean activeRow(final Graphics g, final float x, final float y, final float w, final float h, final boolean barLeft,
            final boolean hoverOnly) {
        if (!on()) {
            return false;
        }
        if (hoverOnly) {
            g.fillRect(HOVER_WASH, x, y, w, h);
            return true;
        }
        g.fillGradientRect(barLeft ? GOLD_WASH : GOLD_WASH_END, barLeft ? GOLD_WASH_END : GOLD_WASH, false, x, y, w, h);
        final float bar = Math.max(2f, ArenaTheme.px(4));
        g.fillRect(ArenaTheme.GOLD_BRIGHT, barLeft ? x : x + w - bar, y, bar, h);
        return true;
    }

    /** The colour of an entry's words in a column: bright gold when active, cream under the pointer, dim otherwise. */
    public static Color entryText(final boolean active, final boolean hovered) {
        return active ? ArenaTheme.GOLD_BRIGHT : hovered ? ArenaTheme.TEXT : ArenaTheme.TEXT_DIM;
    }

    /**
     * A two-way switch (Human / AI, on / off): a dark well in a gold rim, the knob a gold-lit Arena button holding the
     * current word, at the right when on.
     */
    public static boolean toggleSwitch(final Graphics g, final float w, final float h, final boolean on, final boolean pressed,
            final String text, final FSkinFont font) {
        if (!on()) {
            return false;
        }
        final float r = Math.min(h / 2f, ArenaTheme.px(10));
        g.fillSmoothRoundRect(WELL, 0, 0, w, h, r);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.4f)), ArenaTheme.alpha(ArenaTheme.GOLD, 0.55f, S1), 0, 0, w, h, r);
        final float inset = Math.max(2f, ArenaTheme.px(3));
        final float knobW = Math.max(h, w - h) - inset, knobH = h - 2 * inset;
        final float x = on ? w - knobW - inset : inset;
        g.fillSmoothRoundRectGradient(pressed ? ArenaTheme.BTN_BOTTOM : on ? ArenaTheme.BTN_ON_TOP : ArenaTheme.BTN_TOP,
                pressed ? ArenaTheme.BLACK : on ? ArenaTheme.BTN_ON_BOTTOM : ArenaTheme.BTN_BOTTOM, true, x, inset, knobW, knobH, Math.max(0f, r - inset));
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.2f)), on ? ArenaTheme.GOLD_BRIGHT : ArenaTheme.BTN_RIM, x, inset, knobW, knobH, Math.max(0f, r - inset));
        if (text != null) {
            g.drawText(text, font, ArenaTheme.TEXT, x, inset, knobW, knobH, false, com.badlogic.gdx.utils.Align.center, true);
        }
        return true;
    }

    /** The big Start button of a lobby: the molten pill with its word, larger and brighter under the pointer. */
    public static boolean startButton(final Graphics g, final String text, final float w, final float h, final boolean hovered,
            final boolean pressed, final boolean enabled) {
        if (!on()) {
            return false;
        }
        final float inset = Math.max(2f, h * 0.06f);
        final FSkinFont font = ArenaDialogKit.fitFont(text, Math.min(h * 0.46f, ArenaTheme.px(44)), ArenaTheme.px(14), w * 0.8f);
        ArenaDialogKit.pill(g, text, font, inset, inset, w - 2 * inset, h - 2 * inset, enabled, hovered, pressed, false, enabled, 1f);
        return true;
    }
}
