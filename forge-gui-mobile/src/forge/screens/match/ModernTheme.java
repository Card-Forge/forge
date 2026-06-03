package forge.screens.match;

import com.badlogic.gdx.graphics.Color;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Utils;

/**
 * Central styling helper for the modern ("Arena-like") look of the battle/match screen.
 *
 * <p>All styling for the modern theme lives here so that each match-screen component only needs a
 * single {@code if (ModernTheme.enabled())} branch at the top of its draw method, delegating to one
 * of the reusable primitives below. When the theme is disabled every component falls through to its
 * original drawing code, so the classic look is preserved byte-for-byte.</p>
 *
 * <p>The palette is derived from the active skin's theme colors (and their adventure-mode variants),
 * so the modern look automatically tracks whatever skin/biome the player is using.</p>
 */
public final class ModernTheme {
    private ModernTheme() { }

    private static final float SHADOW_OFFSET = Utils.scale(2);
    private static final Color SHADOW = new Color(0f, 0f, 0f, 0.35f);

    /** Whether the modern battle theme is currently active. Read live each frame; no restart needed. */
    public static boolean enabled() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_MODERN_BATTLE_THEME);
    }

    private static boolean adv() {
        return Forge.isMobileAdventureMode;
    }

    // --- Palette (tracks active skin + adventure mode) ---

    /** Base surface color used as the mid-point of panel gradients. */
    public static FSkinColor panelBase() {
        return FSkinColor.get(adv() ? Colors.ADV_CLR_THEME2 : Colors.CLR_THEME2);
    }

    public static FSkinColor panelTop() {
        return panelBase().brighter();
    }

    public static FSkinColor panelBottom() {
        return panelBase().darker();
    }

    /** Accent color used for borders, the current-phase pip, and button highlights. */
    public static FSkinColor accent() {
        return FSkinColor.get(adv() ? Colors.ADV_CLR_ACTIVE : Colors.CLR_ACTIVE);
    }

    public static FSkinColor text() {
        return FSkinColor.get(adv() ? Colors.ADV_CLR_TEXT : Colors.CLR_TEXT);
    }

    public static FSkinColor borderColor() {
        return accent().alphaColor(0.6f);
    }

    /** Rounded-corner radius scaled to an element's height, capped so large panels stay subtle. */
    public static float cornerRadius(float h) {
        return Math.min(h * 0.25f, Utils.scale(10));
    }

    // --- Reusable draw primitives (built only on existing Graphics methods) ---

    /** Soft drop shadow + rounded body + thin rounded accent border. */
    public static void drawSoftPanel(Graphics g, float x, float y, float w, float h) {
        float r = cornerRadius(h);
        g.fillRoundRect(SHADOW, x, y + SHADOW_OFFSET, w, h, r);
        g.fillRoundRect(panelBase(), x, y, w, h, r);
        g.drawRoundRect(Utils.scale(1), borderColor(), x, y, w, h, r);
    }

    /** Solid rounded surface tinted by the supplied color (used for chips / sub-panels). */
    public static void drawChip(Graphics g, FSkinColor color, float x, float y, float w, float h) {
        float r = cornerRadius(h);
        g.fillRoundRect(color, x, y, w, h, r);
        g.drawRoundRect(Utils.scale(1), borderColor(), x, y, w, h, r);
    }

    /** Rounded button with pressed/disabled states and a soft shadow. */
    public static void drawModernButton(Graphics g, float x, float y, float w, float h, boolean pressed, boolean enabled) {
        float r = cornerRadius(h);
        FSkinColor body;
        if (!enabled) {
            body = panelBase().alphaColor(0.5f);
        } else if (pressed) {
            body = accent().darker();
        } else {
            body = accent();
        }
        g.fillRoundRect(SHADOW, x, y + SHADOW_OFFSET, w, h, r);
        g.fillRoundRect(body, x, y, w, h, r);
        g.drawRoundRect(Utils.scale(1), borderColor(), x, y, w, h, r);
    }
}
