package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.card.CardView.CardStateView;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.match.MatchScreen;

/**
 * The Arena look for the libGDX match screen: whether it is on, its navy and gold palette, its sizes and its fonts.
 * Ported from the Swing {@code forge.toolbox.ArenaTheme}; the colours are the same values.
 *
 * <p><b>Colours are shared, mutable libGDX {@link Color} objects. Never change one in place</b> (no {@code c.a = ...});
 * use {@link #alpha(Color, float)} for a new colour or {@link #alpha(Color, float, Color)} to fill a scratch colour
 * you own, which allocates nothing and is what to use every frame.</p>
 *
 * <p>Sizes: the Swing theme was drawn in screen pixels at 1:1 on a 3440x1440 window. Forge's libGDX coordinates are
 * logical pixels too (HdpiMode.Logical), so {@link #px(float)} maps a Swing pixel value to this window: exactly 1:1 at
 * a logical height of 1440, proportionally smaller or larger elsewhere (clamped). Use it for every hard-coded size you
 * port; use fractions of the view for layout, as the Swing code does.</p>
 */
public final class ArenaTheme {
    private ArenaTheme() { }

    // ---- palette (same values as the Swing ArenaTheme) --------------------------------------------------------------
    public static final Color NIGHT         = rgb(0x080D19);
    public static final Color NAVY          = rgb(0x0F1A2E);
    public static final Color NAVY_LIGHT    = rgb(0x1B2A44);
    public static final Color SLATE         = rgb(0x2B3D5C);
    public static final Color GOLD          = rgb(0xD8B26A);
    public static final Color GOLD_BRIGHT   = rgb(0xF6DC9A);
    public static final Color GOLD_DIM      = rgb(0x8C6D3B);
    public static final Color TEXT          = rgb(0xF2ECDF);
    public static final Color TEXT_DIM      = rgb(0x9DA9BD);
    public static final Color PLAYABLE      = rgb(0x38C8FF);
    public static final Color SELECTABLE    = rgb(0x38C8FF);
    public static final Color CHOSEN        = rgb(0xFF7A2E);
    public static final Color HOVER         = rgb(0xE6F1FF);
    public static final Color DANGER        = rgb(0xE5484D);
    public static final Color BUFF          = rgb(0x6FE07A);
    public static final Color ACTION_TOP    = rgb(0xF8A63E);
    public static final Color ACTION_BOTTOM = rgb(0xC2501B);
    public static final Color GLASS         = rgba(8, 13, 25, 205);
    public static final Color GLASS_EDGE    = rgba(216, 178, 106, 70);
    /** The glass panel's gradient (Swing paintGlassPanel). */
    public static final Color GLASS_TOP     = rgba(16, 26, 46, 215);
    public static final Color GLASS_BOTTOM  = rgba(7, 11, 22, 225);
    /** What a dialog dims the board with (Swing CLR_OVERLAY). */
    public static final Color OVERLAY       = rgba(4, 7, 14, 215);
    /** Shadow under tiles and dialogs. */
    public static final Color SHADOW        = rgba(0, 0, 0, 150);
    public static final Color BLACK         = rgb(0x000000);
    public static final Color WHITE         = rgb(0xFFFFFF);
    public static final Color CLEAR         = rgba(0, 0, 0, 0);

    // Primary (molten orange) button, normal / hovered / pressed, three stops top to bottom, and its rim and bloom
    public static final Color[] MOLTEN         = {rgb(0xFFB238), rgb(0xFF8418), rgb(0xE85A08)};
    public static final Color[] MOLTEN_HOVER   = {rgb(0xFFC860), rgb(0xFF9426), rgb(0xF06408)};
    public static final Color[] MOLTEN_PRESSED = {rgb(0xE8862A), rgb(0xC85E18), rgb(0x93400F)};
    public static final Color MOLTEN_RIM       = rgb(0xFFCB6E);
    public static final Color MOLTEN_GLOW      = rgb(0xFF8A2A);
    public static final Color MOLTEN_CORE      = rgba(255, 214, 130, 55);
    public static final Color MOLTEN_EDGE      = rgba(255, 240, 200, 120);
    // Secondary buttons: near-black with a white outline; disabled: faded graphite with a faint rim (plainly inert, never
    // a colour a live button wears); toggled: gold; destructive (Concede): red
    public static final Color BTN_TOP          = rgb(0x15171D);
    public static final Color BTN_BOTTOM       = rgb(0x08090C);
    public static final Color BTN_HOVER_TOP    = rgb(0x2C303A);
    public static final Color BTN_HOVER_BOTTOM = rgb(0x181A21);
    public static final Color BTN_RIM          = rgba(255, 255, 255, 128);
    public static final Color BTN_HOVER_RIM    = rgba(255, 255, 255, 190);
    public static final Color BTN_OFF_TOP      = rgb(0x24272E);
    public static final Color BTN_OFF_BOTTOM   = rgb(0x15171B);
    public static final Color BTN_OFF_RIM      = rgba(255, 255, 255, 46);
    public static final Color BTN_DANGER_TOP   = rgb(0xC0383C);
    public static final Color BTN_DANGER_BOTTOM = rgb(0x7A1418);
    public static final Color BTN_DANGER_HOVER_TOP = rgb(0xDC4A4E);
    public static final Color BTN_DANGER_RIM   = rgb(0xFF9A8C);
    public static final Color BTN_ON_TOP       = rgb(0xA8843F);
    public static final Color BTN_ON_BOTTOM    = rgb(0x6A4E22);

    // Board geometry as fractions of the match area (Swing res/defaults/match_arena.xml)
    public static final float RAIL_RIGHT = 1f;
    public static final float FIELD_SPLIT = 0.38f;
    public static final float HAND_TOP = 0.85f;

    public enum BoardVariant { DAY, DEFAULT, NIGHT }

    // ---- on / off ---------------------------------------------------------------------------------------------------
    private static volatile boolean active;
    private static boolean prefOn, prefSeen;
    private static final List<Runnable> listeners = new ArrayList<>();
    private static final List<java.util.function.Consumer<Boolean>> prefListeners = new ArrayList<>();

    /**
     * Whether the Arena look is on: {@link FPref#UI_ARENA_THEME} is set (the same setting as the Swing client's), this is
     * not adventure mode (whose duels keep their own look) and the screen is in landscape (the Arena board has no
     * portrait layout, so phones held upright keep the default look). A field read, cheap enough for every view on
     * every frame. It is refreshed once per frame by {@link ArenaMotion#tick()} and straight away by {@link #refresh()}
     * (the settings toggle calls it).
     */
    public static boolean isActive() {
        return active;
    }

    /** Whether the Arena look setting is on, whatever the screen (frame pacing follows this, not {@link #isActive()}). */
    public static boolean isSettingOn() {
        return prefOn;
    }

    /**
     * {@link #isActive()} and the current screen is a match: for widgets shared with the menus (dialogs, option panes,
     * choice lists), which must keep the default look outside a match.
     */
    public static boolean isMatchBoard() {
        if (!active) {
            return false;
        }
        final FScreen screen = Forge.getCurrentScreen();
        return screen instanceof MatchScreen;
    }

    /** Re-reads the preference. Listeners run (on the render thread) when the result changes. */
    public static void refresh() {
        final ForgePreferences prefs = FModel.getPreferences();
        final boolean pref = prefs != null && prefs.getPrefBoolean(FPref.UI_ARENA_THEME);
        if (prefs != null && (pref != prefOn || !prefSeen)) {
            prefOn = pref;
            prefSeen = true;
            for (final java.util.function.Consumer<Boolean> l : new ArrayList<>(prefListeners)) {
                try {
                    l.accept(pref);
                } catch (final RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
        final boolean now = prefOn && !Forge.isMobileAdventureMode && Forge.isLandscapeMode();
        if (now != active) {
            active = now;
            for (final Runnable r : new ArrayList<>(listeners)) {
                try {
                    r.run();
                } catch (final RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** Called when the look turns on or off (e.g. to revalidate a layout). Keep it cheap; it runs on the render thread. */
    public static void addListener(final Runnable onChange) {
        listeners.add(onChange);
    }

    public static void removeListener(final Runnable onChange) {
        listeners.remove(onChange);
    }

    /**
     * Told the setting's value once it is first read and whenever it changes (render thread), for what follows the
     * setting itself rather than the screen: the desktop launcher's frame pacing.
     */
    public static void addSettingListener(final java.util.function.Consumer<Boolean> onChange) {
        prefListeners.add(onChange);
    }

    // ---- sizes ------------------------------------------------------------------------------------------------------
    /** Logical window height at which a Swing pixel is one logical pixel here. */
    public static final float REFERENCE_HEIGHT = 1440f;
    private static final float MIN_SCALE = 0.75f, MAX_SCALE = 2.5f;

    /** Window scale for Swing pixel values: 1 at 1440 logical px high, clamped to [0.75, 2.5]. */
    public static float scale() {
        final int h = Forge.getScreenHeight();
        if (h <= 0) {
            return 1f;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, h / REFERENCE_HEIGHT));
    }

    /** A Swing pixel size in this window's logical pixels. */
    public static float px(final float swingPixels) {
        return swingPixels * scale();
    }

    /** {@link #px(float)} rounded, for edges that should land on whole pixels. */
    public static float pxRound(final float swingPixels) {
        return Math.round(swingPixels * scale());
    }

    // ---- fonts ------------------------------------------------------------------------------------------------------
    /**
     * The Arena voice at a Swing point size (Swing displayFont/numeralFont/rulesFont all map here: the libGDX client
     * has one skin face and no new fonts are added). Sizes above 72 px are the 72 px font scaled up. Each new pixel
     * size generates a font the first time it is asked for (tens of ms), so prefer a handful of sizes.
     */
    public static FSkinFont font(final float swingSize) {
        return fontPx(Math.round(px(swingSize)));
    }

    /** A font of this many logical pixels (already scaled). */
    public static FSkinFont fontPx(final int pixels) {
        return FSkinFont._get(Math.max(8, pixels));
    }

    /** The largest font whose line height fits in {@code height} logical pixels. */
    public static FSkinFont fontForHeight(final float height) {
        return FSkinFont.forHeight(height);
    }

    // ---- colour helpers ---------------------------------------------------------------------------------------------
    public static Color rgb(final int hex) {
        return new Color(((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f, (hex & 0xFF) / 255f, 1f);
    }

    public static Color rgba(final int r, final int g, final int b, final int a) {
        return new Color(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    /** Fills {@code out} with r, g, b (0..255) at alpha {@code a} (0..255, may be fractional) and returns it. */
    public static Color rgbaInto(final int r, final int g, final int b, final float a, final Color out) {
        return out.set(r / 255f, g / 255f, b / 255f, clamp01(a / 255f));
    }

    /** A new colour with this alpha (0..1). Allocates; per frame prefer {@link #alpha(Color, float, Color)}. */
    public static Color alpha(final Color c, final float a) {
        return new Color(c.r, c.g, c.b, clamp01(a));
    }

    /** Fills {@code out} with {@code c} at alpha {@code a} (0..1) and returns it. */
    public static Color alpha(final Color c, final float a, final Color out) {
        return out.set(c.r, c.g, c.b, clamp01(a));
    }

    /** Swing's alpha(c, 0..255). */
    public static Color alpha255(final Color c, final int a) {
        return alpha(c, a / 255f);
    }

    /** {@code c} with its own alpha multiplied by {@code f}, into {@code out}. */
    public static Color fadeBy(final Color c, final float f, final Color out) {
        return out.set(c.r, c.g, c.b, clamp01(c.a * f));
    }

    /** Linear mix of two colours (alpha too), into {@code out}. */
    public static Color mix(final Color a, final Color b, final float t, final Color out) {
        return out.set(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t, a.a + (b.a - a.a) * t);
    }

    /** A colour a little lighter, for the top of a bar (Swing lift: +26/255 per channel). */
    public static Color lift(final Color c) {
        return new Color(Math.min(1f, c.r + 26 / 255f), Math.min(1f, c.g + 26 / 255f), Math.min(1f, c.b + 26 / 255f), c.a);
    }

    /** A colour darkened to the given brightness. */
    public static Color shade(final Color c, final float factor) {
        return new Color(c.r * factor, c.g * factor, c.b * factor, c.a);
    }

    /** Most of the saturation and some of the light gone, for tapped tiles. */
    public static Color fade(final Color c) {
        final float l = luma(c);
        return new Color((3 * c.r + l) * 3 / 20f, (3 * c.g + l) * 3 / 20f, (3 * c.b + l) * 3 / 20f, c.a);
    }

    public static boolean isLight(final Color c) {
        return luma(c) > 118 / 255f;
    }

    /** Perceived brightness, 0..1 (Swing's 30/59/11 weights). */
    public static float luma(final Color c) {
        return c.r * 0.30f + c.g * 0.59f + c.b * 0.11f;
    }

    /** The light plate power and toughness sit on, faintly tinted with the card's colour. */
    public static Color plateColor(final Color tint) {
        return new Color((tint.r + 3 * 244 / 255f) / 4f, (tint.g + 3 * 240 / 255f) / 4f, (tint.b + 3 * 230 / 255f) / 4f, 1f);
    }

    /** The thin line of colour round a tile's art; pale and dark cards still need it to show against black. */
    public static Color frameLine(final Color tint) {
        return luma(tint) < 70 / 255f ? FRAME_LINE_DARK : tint;
    }

    private static final Color FRAME_LINE_DARK = rgb(0x8D8794);
    private static final Color TINT_LAND = rgb(0xA89E8C), TINT_MULTI = rgb(0xC9A640), TINT_W = rgb(0xE2DAC0), TINT_U = rgb(0x3A8AD6),
            TINT_B = rgb(0x37333D), TINT_R = rgb(0xD0432B), TINT_G = rgb(0x2FA562), TINT_ARTIFACT = rgb(0xA9B8C6), TINT_COLORLESS = rgb(0xB9B4AA);

    /** The colour of a tile's frame and name bar; lands take the colour of the mana they make. Shared constant; don't modify. */
    public static Color tileTint(final CardStateView state) {
        ColorSet colors = state.getColors();
        if (state.isLand()) {
            final String text = state.getOracleText() == null ? "" : state.getOracleText();
            final String name = state.getName() == null ? "" : state.getName();
            int mask = 0;
            if (text.contains("{W}") || name.equals("Plains")) { mask |= MagicColor.WHITE; }
            if (text.contains("{U}") || name.equals("Island")) { mask |= MagicColor.BLUE; }
            if (text.contains("{B}") || name.equals("Swamp")) { mask |= MagicColor.BLACK; }
            if (text.contains("{R}") || name.equals("Mountain")) { mask |= MagicColor.RED; }
            if (text.contains("{G}") || name.equals("Forest")) { mask |= MagicColor.GREEN; }
            if (mask == 0) {
                return TINT_LAND;
            }
            colors = ColorSet.fromMask(mask);
        }
        if (colors != null && colors.isMulticolor()) {
            return TINT_MULTI;
        }
        if (colors != null && colors.isMonoColor()) {
            if (colors.hasWhite()) { return TINT_W; }
            if (colors.hasBlue())  { return TINT_U; }
            if (colors.hasBlack()) { return TINT_B; }
            if (colors.hasRed())   { return TINT_R; }
            if (colors.hasGreen()) { return TINT_G; }
        }
        return state.isArtifact() ? TINT_ARTIFACT : TINT_COLORLESS;
    }

    private static final Color FRAME_MULTI = rgb(0xC9A24A), FRAME_W = rgb(0xD8D0B0), FRAME_U = rgb(0x2F6FB0), FRAME_B = rgb(0x4C4350),
            FRAME_R = rgb(0xB8432F), FRAME_G = rgb(0x3F8048), FRAME_LAND = rgb(0x8B7458), FRAME_ARTIFACT = rgb(0x8D939C), FRAME_OTHER = rgb(0x7A7F88);

    /** Frame colour of a battlefield tile: the card's colour, gold for multicolour, stone for lands, steel for artifacts. */
    public static Color frameColor(final ColorSet colors, final boolean isLand, final boolean isArtifact) {
        if (colors != null && colors.isMulticolor()) {
            return FRAME_MULTI;
        }
        if (colors != null && colors.isMonoColor()) {
            if (colors.hasWhite()) { return FRAME_W; }
            if (colors.hasBlue())  { return FRAME_U; }
            if (colors.hasBlack()) { return FRAME_B; }
            if (colors.hasRed())   { return FRAME_R; }
            if (colors.hasGreen()) { return FRAME_G; }
        }
        if (isLand) {
            return FRAME_LAND;
        }
        return isArtifact ? FRAME_ARTIFACT : FRAME_OTHER;
    }

    private static final Color PIP_W = rgb(0xFFF4B8), PIP_U = rgb(0x7CC8F4), PIP_B = rgb(0xB4A8AC), PIP_R = rgb(0xF58A64), PIP_G = rgb(0x6FCB8C);

    /** The bright tint a single-colour mana pip is painted in (Swing pipTint), or null for generic/hybrid. */
    public static Color pipTint(final byte magicColor) {
        switch (magicColor) {
            case MagicColor.WHITE: return PIP_W;
            case MagicColor.BLUE:  return PIP_U;
            case MagicColor.BLACK: return PIP_B;
            case MagicColor.RED:   return PIP_R;
            case MagicColor.GREEN: return PIP_G;
            default:               return null;
        }
    }

    private static final Color SKIN_THEME = rgba(15, 26, 46, 228), SKIN_BORDERS = rgba(216, 178, 106, 110), SKIN_ZEBRA = rgb(0x16243C),
            SKIN_HOVER = rgb(0x2B3D5C), SKIN_ACTIVE = rgb(0x8C6D3B), SKIN_INACTIVE = rgb(0x1B2A44), SKIN_PHASE_IE = rgb(0x2C4F78),
            SKIN_PHASE_ID = rgb(0x232B38);

    /**
     * The Arena replacement for a skin colour, or null to keep the skin's own (Swing getSkinColor). For widgets that
     * draw with {@link FSkinColor}s: {@code Color c = ArenaTheme.isMatchBoard() ? ArenaTheme.skinColor(x) : null;}.
     */
    public static Color skinColor(final FSkinColor.Colors c) {
        switch (c) {
            case CLR_THEME:                   return SKIN_THEME;
            case CLR_BORDERS:                 return SKIN_BORDERS;
            case CLR_ZEBRA:                   return SKIN_ZEBRA;
            case CLR_HOVER:                   return SKIN_HOVER;
            case CLR_ACTIVE:                  return SKIN_ACTIVE;
            case CLR_INACTIVE:                return SKIN_INACTIVE;
            case CLR_TEXT:                    return TEXT;
            case CLR_PHASE_INACTIVE_ENABLED:  return SKIN_PHASE_IE;
            case CLR_PHASE_INACTIVE_DISABLED: return SKIN_PHASE_ID;
            case CLR_PHASE_ACTIVE_ENABLED:    return GOLD;
            case CLR_PHASE_ACTIVE_DISABLED:   return GOLD_DIM;
            case CLR_THEME2:                  return NIGHT;
            case CLR_OVERLAY:                 return OVERLAY;
            default:                          return null;
        }
    }

    static float clamp01(final float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
