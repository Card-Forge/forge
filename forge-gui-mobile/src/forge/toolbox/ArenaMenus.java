package forge.toolbox;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.screens.match.arena.ArenaArt;
import forge.screens.match.arena.ArenaDialogKit;
import forge.screens.match.arena.ArenaTheme;

/**
 * The Arena look of the menu screens (home, new game lobbies, deck manager and deck editor, settings): a deep navy
 * backdrop with a soft light over its middle, dark glass header bands and columns with gold lines, gold-washed active
 * entries and tabs, gold-lit switches and icons and a molten Start button. The shared widgets on those screens
 * (buttons, lists, fields, menus) come from {@link forge.screens.match.arena.ArenaWidgets} and {@link Frost}'s Arena
 * palette, which ask {@link #on()} too.
 * <p>
 * {@link #on()} is the one gate for the Arena look outside the match board. The drawing methods draw nothing and return
 * false when it is off, so their callers keep their own look then. All of it is a handful of batched quads per frame.
 */
public final class ArenaMenus {
    private ArenaMenus() { }

    private static final Color S1 = new Color();
    private static final Color BACK_TOP = ArenaTheme.rgba(17, 29, 52, 236), BACK_BOTTOM = ArenaTheme.rgba(5, 9, 18, 248);
    private static final Color LIGHT = ArenaTheme.rgb(0x5D7DB8);
    private static final Color BAND_TOP = ArenaTheme.rgba(10, 16, 30, 250), BAND_BOTTOM = ArenaTheme.rgba(5, 8, 16, 250);
    private static final Color COLUMN_LEFT = ArenaTheme.rgba(5, 8, 16, 246), COLUMN_RIGHT = ArenaTheme.rgba(12, 20, 37, 232);
    private static final Color GOLD_WASH = ArenaTheme.rgba(216, 178, 106, 70), GOLD_WASH_END = ArenaTheme.rgba(216, 178, 106, 6);
    private static final Color HOVER_WASH = ArenaTheme.rgba(216, 178, 106, 26);
    private static final Color WELL = ArenaTheme.rgba(4, 7, 14, 235);

    /** Words of an inactive entry or a description: brighter than the match board's dim text, legible on the glass. */
    public static final Color TEXT_SOFT = ArenaTheme.rgb(0xC3CCDA);
    /** A gold line in place of the skin's white borders. */
    public static final Color LINE = ArenaTheme.rgba(216, 178, 106, 150);

    // the backdrop's light pool (centre and radii as fractions of the screen) and its darkened edges
    private static final float LIGHT_X = 0.5f, LIGHT_Y = 0.3f, LIGHT_RX = 0.55f, LIGHT_RY = 0.5f, LIGHT_ALPHA = 0.10f;
    private static final float VIGNETTE_DEPTH = 0.18f, VIGNETTE_STRENGTH = 0.45f;
    private static final float BAND_LINE_ALPHA = 0.55f, COLUMN_EDGE_ALPHA = 0.35f, SWITCH_RIM_ALPHA = 0.55f;
    /** Swing pixels: the gold bar of an active entry, line thicknesses, a switch's corner and knob inset. */
    private static final float ACTIVE_BAR = 4f, LINE_PX = 1.5f, SWITCH_RADIUS = 10f, SWITCH_INSET = 3f;
    /** The Start pill's words: at most this share of its height and this many Swing pixels, at least {@link #START_FONT_MIN}. */
    private static final float START_FONT_SHARE = 0.46f, START_FONT_MAX = 44f, START_FONT_MIN = 14f, START_TEXT_ROOM = 0.8f;

    /**
     * One-click badges: the card's hover frame (Swing px), the pill's height (at least {@code BADGE_MIN_HEIGHT} Swing px,
     * else a share of the card's width or the row's height), its place on a card (from the top, a share of its height),
     * its widest (a share of the card's), its room round the words (in pill heights) and its words' height (a share).
     */
    private static final float BADGE_FRAME = 3f, BADGE_MIN_HEIGHT = 26f, BADGE_CARD_SHARE = 0.14f, BADGE_ROW_SHARE = 0.62f;
    private static final float BADGE_TOP = 0.36f, BADGE_MAX_WIDTH = 0.9f, BADGE_PAD = 1.2f, BADGE_FONT_SHARE = 0.62f;
    /** A chooser's chevron (its height, and its gap from the right end, as shares of the button's height) and hint words. */
    private static final float CHEVRON_SHARE = 0.42f, CHEVRON_PAD = 0.4f, HINT_SHARE = 0.4f;

    /** Whether the menus (and the widgets they share with the match) wear the Arena look now. A field read. */
    public static boolean on() {
        return ArenaTheme.isActive();
    }

    /**
     * Lays every open screen out again when the Arena look turns on or off (from the settings), since some of their
     * layouts differ in it (the lobby's Start button, the deck editor's side-by-side layout). Call once at start-up.
     */
    public static void followLook() {
        ArenaTheme.addListener(Forge::relayoutScreens);
    }

    /** Behind a whole screen: deep navy, lighter at the top, with a cool pool of light and darkened edges. */
    public static boolean screenBackground(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(BACK_TOP, BACK_BOTTOM, true, 0, 0, w, h);
        ArenaArt.haloEllipse(g, w * LIGHT_X, h * LIGHT_Y, w * LIGHT_RX, h * LIGHT_RY, LIGHT, LIGHT_ALPHA);
        ArenaArt.vignette(g, 0, 0, w, h, Math.min(w, h) * VIGNETTE_DEPTH, VIGNETTE_STRENGTH);
        return true;
    }

    /** A header band (screen headers, the deck editor's name bar): dark glass with a gold line along its foot. */
    public static boolean headerBand(final Graphics g, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(BAND_TOP, BAND_BOTTOM, true, 0, 0, w, h);
        final float t = lineThickness();
        ArenaArt.divider(g, 0, h - t, w, ArenaTheme.alpha(ArenaTheme.GOLD, BAND_LINE_ALPHA, S1), t);
        return true;
    }

    /** A column down one side (the home navigation, a tab column): dark glass with a faint gold edge on {@code edgeRight}. */
    public static boolean column(final Graphics g, final float w, final float h, final boolean edgeRight) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(edgeRight ? COLUMN_LEFT : COLUMN_RIGHT, edgeRight ? COLUMN_RIGHT : COLUMN_LEFT, false, 0, 0, w, h);
        ArenaArt.dividerVertical(g, edgeRight ? w - 1 : 0, 0, h, ArenaTheme.alpha(ArenaTheme.GOLD, COLUMN_EDGE_ALPHA, S1), 1f);
        return true;
    }

    /** The row of the active entry in a column: a gold wash fading across, a bright bar on the left. */
    public static boolean activeRow(final Graphics g, final float x, final float y, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillGradientRect(GOLD_WASH, GOLD_WASH_END, false, x, y, w, h);
        g.fillRect(ArenaTheme.GOLD_BRIGHT, x, y, Math.max(2f, ArenaTheme.px(ACTIVE_BAR)), h);
        return true;
    }

    /** The row under the pointer in a column: a faint gold wash. */
    public static boolean hoverRow(final Graphics g, final float x, final float y, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.fillRect(HOVER_WASH, x, y, w, h);
        return true;
    }

    /** A rectangle's outline as a gold line, in place of the skin's white or grey borders. */
    public static boolean outline(final Graphics g, final float x, final float y, final float w, final float h) {
        if (!on()) {
            return false;
        }
        g.drawRect(lineThickness(), LINE, x, y, w, h);
        return true;
    }

    /** The colour of an entry's words in a column: bright gold when active, cream under the pointer, soft otherwise. */
    public static Color entryText(final boolean active, final boolean hovered) {
        return active ? ArenaTheme.GOLD_BRIGHT : hovered ? ArenaTheme.TEXT : TEXT_SOFT;
    }

    /**
     * A skin icon (save, search, filters, tabs...) lit in gold like Arena's line icons: the picture multiplied by bright
     * gold, so its white strokes come out gold and its colours warm. Draws it plainly when the Arena look is off.
     */
    public static void icon(final Graphics g, final FImage image, final float x, final float y, final float w, final float h) {
        if (!on()) {
            g.drawImage(image, x, y, w, h);
            return;
        }
        final float alpha = g.getfloatAlphaComposite();
        final Color c = ArenaTheme.GOLD;
        g.setColorRGBA(c.r, c.g, c.b, alpha);
        g.drawImage(image, x, y, w, h);
        g.resetColorRGBA(alpha);
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
        final float r = Math.min(h / 2f, ArenaTheme.px(SWITCH_RADIUS));
        g.fillSmoothRoundRect(WELL, 0, 0, w, h, r);
        g.drawSmoothRoundRect(lineThickness(), ArenaTheme.alpha(ArenaTheme.GOLD, SWITCH_RIM_ALPHA, S1), 0, 0, w, h, r);
        final float inset = Math.max(2f, ArenaTheme.px(SWITCH_INSET));
        final float knobW = Math.max(h, w - h) - inset, knobH = h - 2 * inset, knobR = Math.max(0f, r - inset);
        final float x = on ? w - knobW - inset : inset;
        g.fillSmoothRoundRectGradient(pressed ? ArenaTheme.BTN_BOTTOM : on ? ArenaTheme.BTN_ON_TOP : ArenaTheme.BTN_TOP,
                pressed ? ArenaTheme.BLACK : on ? ArenaTheme.BTN_ON_BOTTOM : ArenaTheme.BTN_BOTTOM, true, x, inset, knobW, knobH, knobR);
        g.drawSmoothRoundRect(lineThickness(), on ? ArenaTheme.GOLD_BRIGHT : LINE, x, inset, knobW, knobH, knobR);
        if (text != null) {
            g.drawText(text, font, ArenaTheme.TEXT, x, inset, knobW, knobH, false, Align.center, true);
        }
        return true;
    }

    /** The big Start button of a lobby: the molten pill with its word, brighter under the pointer. */
    public static boolean startButton(final Graphics g, final String text, final float w, final float h, final boolean hovered,
            final boolean pressed, final boolean enabled) {
        if (!on()) {
            return false;
        }
        final float inset = Math.max(2f, h * 0.06f);
        final FSkinFont font = ArenaDialogKit.fitFont(text, Math.min(h * START_FONT_SHARE, ArenaTheme.px(START_FONT_MAX)),
                ArenaTheme.px(START_FONT_MIN), w * START_TEXT_ROOM);
        ArenaDialogKit.pill(g, text, font, inset, inset, w - 2 * inset, h - 2 * inset, enabled, hovered, pressed, false, enabled, 1f);
        return true;
    }

    /** A labelled Arena button (the deck editor's Save, the deck manager's actions): the molten pill when {@code primary}. */
    public static boolean button(final Graphics g, final String text, final float x, final float y, final float w, final float h,
            final boolean primary, final boolean hovered, final boolean pressed, final boolean enabled) {
        if (!on()) {
            return false;
        }
        ArenaDialogKit.pill(g, text, x, y, w, h, primary && enabled, hovered, pressed, false, enabled, 1f);
        return true;
    }

    /**
     * One-click deck building: over the card under the pointer, a gold frame and a dark pill over its art saying what a
     * click does ("+1   2/4 in deck": add one, two of the four allowed already in the deck).
     */
    public static void cardBadge(final Graphics g, final String words, final float x, final float y, final float w, final float h) {
        final float t = Math.max(2f, ArenaTheme.px(BADGE_FRAME));
        g.drawRect(t, ArenaTheme.GOLD_BRIGHT, x - t / 2f, y - t / 2f, w + t, h + t);
        final float bh = Math.max(ArenaTheme.px(BADGE_MIN_HEIGHT), w * BADGE_CARD_SHARE);
        final FSkinFont font = ArenaDialogKit.font(bh * BADGE_FONT_SHARE);
        final float bw = Math.min(w * BADGE_MAX_WIDTH, ArenaArt.textWidth(words, font) + bh * BADGE_PAD);
        pill(g, words, x + (w - bw) / 2f, y + h * BADGE_TOP, bw, bh); //over the art, clear of the name
    }

    /** One-click deck building: over the list row under the pointer, a faint gold wash and a pill at its right end. */
    public static void rowBadge(final Graphics g, final String words, final float x, final float y, final float w, final float h) {
        g.fillRect(HOVER_WASH, x, y, w, h);
        final float bh = h * BADGE_ROW_SHARE;
        final FSkinFont font = ArenaDialogKit.font(bh * BADGE_FONT_SHARE);
        final float bw = ArenaArt.textWidth(words, font) + bh * BADGE_PAD;
        pill(g, words, x + w - bw - bh * 0.3f, y + (h - bh) / 2f, bw, bh);
    }

    /**
     * Marks a button as a chooser (the lobby's deck choice): a gold chevron at its right end and {@code hint} at its left
     * in soft words. Draws nothing when the Arena look is off.
     */
    public static void chooserCue(final Graphics g, final float w, final float h, final String hint) {
        if (!on()) {
            return;
        }
        final float size = h * CHEVRON_SHARE, pad = h * CHEVRON_PAD;
        final float cx = w - pad - size / 2f, cy = h / 2f, t = Math.max(2f, ArenaTheme.px(2.5f));
        g.drawLine(t, ArenaTheme.GOLD, cx - size / 4f, cy - size / 2f, cx + size / 4f, cy);
        g.drawLine(t, ArenaTheme.GOLD, cx + size / 4f, cy, cx - size / 4f, cy + size / 2f);
        if (hint != null) {
            final FSkinFont font = ArenaDialogKit.font(h * HINT_SHARE);
            g.drawText(hint, font, TEXT_SOFT, pad, 0, w / 3f, h, false, Align.left, true);
        }
    }

    private static void pill(final Graphics g, final String words, final float x, final float y, final float w, final float h) {
        final float r = h / 2f;
        g.fillSmoothRoundRect(WELL, x, y, w, h, r);
        g.drawSmoothRoundRect(lineThickness(), ArenaTheme.GOLD_BRIGHT, x, y, w, h, r);
        final FSkinFont font = ArenaDialogKit.fitFont(words, h * BADGE_FONT_SHARE, ArenaTheme.px(START_FONT_MIN) * 0.75f, w * 0.9f);
        g.drawText(words, font, ArenaTheme.GOLD_BRIGHT, x, y, w, h, false, Align.center, true);
    }

    private static float lineThickness() {
        return Math.max(1f, ArenaTheme.px(LINE_PX));
    }
}
