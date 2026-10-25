package forge.toolbox;

import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.screens.match.views.VPlayerPanel;
import forge.util.Utils;

/** Shared look of the frosted style outside the match screen: dark translucent slate with the theme's accent colour. */
public final class Frost {
    private static final Color SLATE = new Color(30 / 255f, 35 / 255f, 40 / 255f, 1f);
    public static final Color EDGE = new Color(1f, 1f, 1f, 0.12f);
    public static final Color HOVER = new Color(1f, 1f, 1f, 0.12f);
    public static final Color HOVER_FILL = new Color(62 / 255f, 72 / 255f, 82 / 255f, 0.78f);
    public static final Color TEXT = new Color(1f, 1f, 1f, 0.92f);
    public static final Color TEXT_DIM = new Color(1f, 1f, 1f, 0.72f);

    private Frost() {
    }

    /** @return true when menus and screens should use the frosted look: compact layout, or the Frosted Style setting */
    public static boolean isOn() {
        if (Forge.isMobileAdventureMode) { return false; }
        try {
            return VPlayerPanel.isFrosted();
        } catch (RuntimeException e) { //preferences not loaded yet, as on the splash screen
            return false;
        }
    }

    public static Color slate(float alpha) {
        return new Color(SLATE.r, SLATE.g, SLATE.b, alpha);
    }

    /**
     * A frosted button face in the theme colour, like the Start button: a glossy gradient pill with a darker edge.
     * Hovering lights it up (brighter fill and edge, a white sheen); pressing darkens it.
     */
    private static FSkinFont skinFont = null;
    public static void drawButton(Graphics g, float w, float h, boolean hover, boolean pressed, boolean enabled, String text) {
        final float radius = Utils.scale(2);//Math.min(h * 0.32f, Utils.scale(14));
        final float e = Math.max(1f, h * 0.05f);
        final float k = pressed ? 1.8f : hover ? 1.22f : 1f;
        g.fillRoundRect(hover && !pressed ? accentShade(1.7f, 1f) : accentShade(0.5f, 0.95f), 0, 0, w, h, radius);
        g.fillRoundRect(accentShade(0.85f * k, 0.97f), e, e, w - 2 * e, h - 2 * e, Math.max(0f, radius - e));
        final float inset = radius * 0.7f, ih = h - 2 * e;
        g.fillGradientRect(accentShade(1.4f * k, hover ? 0.80f : 0.55f), accentShade(1.0f * k, 0f), true, inset, e, w - 2 * inset, ih * 0.55f);
        g.fillGradientRect(accentShade(0.8f * k, 0f), accentShade(0.45f * k, 0.55f), true, inset, e + ih * 0.55f, w - 2 * inset, ih * 0.45f);
        if (hover && !pressed) {
            g.fillRoundRect(new Color(1f, 1f, 1f, 0.10f), e, e, w - 2 * e, h - 2 * e, Math.max(0f, radius - e));
        }
        if (text != null) {
            if (skinFont == null)
                skinFont = FSkinFont.forHeight(h / 1.5f);
            g.drawText(text, skinFont, enabled ? Frost.TEXT : Frost.TEXT_DIM, 0, 0, w, h, false, Align.center, true);
        }
    }

    /** the theme's active colour with the given alpha */
    public static Color accent(float alpha) {
        final Color c = FSkinColor.get(Colors.CLR_ACTIVE).getColor();
        return new Color(c.r, c.g, c.b, alpha);
    }

    /** the accent, brightened or darkened by the factor */
    public static Color accentShade(float factor, float alpha) {
        final Color c = FSkinColor.get(Colors.CLR_ACTIVE).getColor();
        return new Color(Math.min(1f, c.r * factor), Math.min(1f, c.g * factor), Math.min(1f, c.b * factor), alpha);
    }
}
