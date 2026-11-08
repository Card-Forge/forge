package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectMap;

import forge.Graphics;
import forge.assets.FImageComplex;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.game.card.CardView;
import forge.util.TextBounds;

/**
 * The Arena drawing primitives every view shares: panels and tiles, frames, buttons, pills and badges, dividers,
 * glows and shadows, text, and the board backdrop. Ported from the Swing {@code ArenaArt}/{@code ArenaTheme}/
 * {@code ArenaText} painting helpers, drawn on the GPU through {@link Graphics} (signed-distance shapes in one batched
 * shader; see the cost notes above {@code Graphics.drawArenaShape}). View-specific art (portrait frames, life plates,
 * phase glyphs, card frames...) belongs in the views' own Arena skins, built from these.
 *
 * <p>All coordinates are Forge's: logical pixels, y down, relative to the component being drawn (the {@code Graphics}
 * bounds). Sizes you port from Swing go through {@link ArenaTheme#px(float)}. Everything here must be called from
 * {@code draw()} on the render thread (it shares scratch colours) and allocates nothing per call unless noted.</p>
 */
public final class ArenaArt {
    private ArenaArt() { }

    // render-thread scratch colours
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color(), S4 = new Color();
    private static final Color[] STOPS3 = new Color[3], STOPS4 = new Color[4];
    private static final float[] MOLTEN_STOPS = {0f, 0.45f, 1f};
    private static final float[] EDGE_STOPS = {0f, 0.28f, 1f};
    private static final float[] BAND_STOPS = {0f, 0.28f, 0.72f, 1f};
    private static final TextRenderer RULES = new TextRenderer();
    private static final TextBounds BOUNDS = new TextBounds();

    // ---- panels, tiles and frames -----------------------------------------------------------------------------------
    /** Swing paintGlassPanel: translucent dark panel with a thin gold rim (arc 14 = radius 7 Swing px). */
    public static void glassPanel(final Graphics g, final float x, final float y, final float w, final float h) {
        glassPanel(g, x, y, w, h, ArenaTheme.px(7));
    }

    public static void glassPanel(final Graphics g, final float x, final float y, final float w, final float h, final float radius) {
        panel(g, x, y, w, h, radius, ArenaTheme.GLASS_TOP, ArenaTheme.GLASS_BOTTOM, ArenaTheme.GLASS_EDGE, 1f);
    }

    /** A rounded panel with a vertical gradient and an optional rim ({@code rim} null or {@code rimWidth} 0 for none). */
    public static void panel(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final Color top, final Color bottom, final Color rim, final float rimWidth) {
        g.fillSmoothRoundRectGradient(top, bottom, true, x, y, w, h, radius);
        if (rim != null && rimWidth > 0f) {
            g.drawSmoothRoundRect(rimWidth, rim, x, y, w, h, radius);
        }
    }

    /** A panel with a soft drop shadow under it, as dialogs and raised tiles have. */
    public static void raisedPanel(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final Color top, final Color bottom, final Color rim, final float rimWidth) {
        shadow(g, x, y, w, h, radius, ArenaTheme.px(18), 0.55f, ArenaTheme.px(6));
        panel(g, x, y, w, h, radius, top, bottom, rim, rimWidth);
    }

    /** A plain rounded frame (outline inside the edge). */
    public static void frame(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final Color color, final float thickness) {
        g.drawSmoothRoundRect(thickness, color, x, y, w, h, radius);
    }

    /** A frame shaded top to bottom, e.g. {@link #goldFrame}. */
    public static void frame(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final Color top, final Color bottom, final float thickness) {
        g.drawArenaShape(Graphics.SHAPE_RING, x, y, w, h, radius, thickness, top, bottom, true);
    }

    /** The bright-to-dim gold frame of portraits and featured tiles (Swing GOLD_BRIGHT to GOLD_DIM). */
    public static void goldFrame(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final float thickness) {
        frame(g, x, y, w, h, radius, ArenaTheme.GOLD_BRIGHT, ArenaTheme.GOLD_DIM, thickness);
    }

    /** A flat rounded fill. */
    public static void fill(final Graphics g, final float x, final float y, final float w, final float h, final float radius, final Color c) {
        g.fillSmoothRoundRect(c, x, y, w, h, radius);
    }

    /** A rounded fill with a vertical gradient through any number of stops (one shape, one batch run). */
    public static void fill(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final Color[] colors, final float[] stops) {
        g.drawArenaShape(Graphics.SHAPE_FILL, x, y, w, h, radius, 0f, colors, stops, true);
    }

    // ---- glows, shadows, halos --------------------------------------------------------------------------------------
    /**
     * Swing paintGlow: a soft glow round a rounded rectangle, strongest at its edge, {@code spread} wide. {@code alpha}
     * is its strength (Swing maxAlpha / 255). Nothing is drawn inside, so it may go before or after the shape.
     */
    public static void glow(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final Color c, final float spread, final float alpha) {
        g.drawGlow(ArenaTheme.alpha(c, alpha, S1), x, y, w, h, radius, spread);
    }

    /** A drop shadow: the shape blurred by {@code blur}, {@code alpha} black, moved down by {@code offsetY}. */
    public static void shadow(final Graphics g, final float x, final float y, final float w, final float h, final float radius,
            final float blur, final float alpha, final float offsetY) {
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, alpha, S1), x, y + offsetY, w, h, radius, blur);
    }

    /** A soft round light of {@code radius} centred on cx, cy (filled, falling off to nothing at its edge). */
    public static void halo(final Graphics g, final float cx, final float cy, final float radius, final Color c, final float alpha) {
        final float core = radius * 0.5f;
        g.drawSoftShadow(ArenaTheme.alpha(c, alpha, S1), cx - core, cy - core, 2 * core, 2 * core, core, radius - core);
    }

    /** An elliptical soft light (e.g. a pool of light on the board, a highlight on a button). */
    public static void haloEllipse(final Graphics g, final float cx, final float cy, final float rx, final float ry, final Color c, final float alpha) {
        final float blur = Math.min(rx, ry) * 0.6f;
        final float w = Math.max(1f, 2 * rx - 2 * blur), h = Math.max(1f, 2 * ry - 2 * blur);
        g.drawSoftShadow(ArenaTheme.alpha(c, alpha, S1), cx - w / 2, cy - h / 2, w, h, Math.min(w, h) / 2, blur);
    }

    // ---- buttons ----------------------------------------------------------------------------------------------------
    /** Room Swing leaves round a button's body for its bloom: h/10, between 3 and 8 Swing px. */
    public static float buttonPad(final float h) {
        return Math.min(ArenaTheme.px(8), Math.max(ArenaTheme.px(3), h / 10f));
    }

    /**
     * Swing paintButton: a full pill inside {@code x, y, w, h} (with {@link #buttonPad} round it for the bloom).
     * Primary buttons are the luminous orange action button; the rest are near-black with a white outline; a disabled
     * one is deep blue with a lit rim; a toggled one is gold. A focused one gets a pale ring.
     */
    public static void button(final Graphics g, final float x, final float y, final float w, final float h, final boolean primary,
            final boolean hovered, final boolean pressed, final boolean focused, final boolean toggled, final boolean enabled) {
        final float pad = buttonPad(h);
        final float bx = x + pad, by = y + pad, bw = w - 2 * pad, bh = h - 2 * pad;
        if (bw <= 0 || bh <= 0) {
            return;
        }
        final float r = bh / 2f;
        if (primary && enabled) {
            g.drawGlow(ArenaTheme.alpha(ArenaTheme.MOLTEN_GLOW, hovered ? 1f : 0.84f, S1), bx, by, bw, bh, r, pad);
            final Color[] stops = pressed ? ArenaTheme.MOLTEN_PRESSED : hovered ? ArenaTheme.MOLTEN_HOVER : ArenaTheme.MOLTEN;
            g.drawArenaShape(Graphics.SHAPE_FILL, bx, by, bw, bh, r, 0f, stops, MOLTEN_STOPS, true);
            // molten at the centre
            final float cw = bw * 0.7f, ch = bh * 0.5f, blur = bh * 0.22f;
            g.drawSoftShadow(ArenaTheme.MOLTEN_CORE, bx + (bw - cw) / 2f, by + bh * 0.45f - ch / 2f, cw, ch, ch / 2f, blur);
            // the hot upper edge, clipped to the body by being the body itself
            STOPS3[0] = ArenaTheme.MOLTEN_EDGE;
            STOPS3[1] = ArenaTheme.alpha(ArenaTheme.MOLTEN_EDGE, 0f, S2);
            STOPS3[2] = S2;
            g.drawArenaShape(Graphics.SHAPE_FILL, bx, by, bw, bh, r, 0f, STOPS3, EDGE_STOPS, true);
            g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.2f)), ArenaTheme.MOLTEN_RIM, bx, by, bw, bh, r);
        } else {
            final Color top, bottom, rim;
            if (!enabled) {
                top = ArenaTheme.BTN_OFF_TOP;
                bottom = ArenaTheme.BTN_OFF_BOTTOM;
                rim = ArenaTheme.BTN_OFF_RIM;
            } else if (toggled) {
                top = ArenaTheme.BTN_ON_TOP;
                bottom = ArenaTheme.BTN_ON_BOTTOM;
                rim = ArenaTheme.GOLD_BRIGHT;
            } else if (pressed) {
                top = ArenaTheme.BTN_BOTTOM;
                bottom = ArenaTheme.BLACK;
                rim = hovered ? ArenaTheme.BTN_HOVER_RIM : ArenaTheme.BTN_RIM;
            } else {
                top = hovered ? ArenaTheme.BTN_HOVER_TOP : ArenaTheme.BTN_TOP;
                bottom = hovered ? ArenaTheme.BTN_HOVER_BOTTOM : ArenaTheme.BTN_BOTTOM;
                rim = hovered ? ArenaTheme.BTN_HOVER_RIM : ArenaTheme.BTN_RIM;
            }
            g.fillSmoothRoundRectGradient(top, bottom, true, bx, by, bw, bh, r);
            g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), rim, bx, by, bw, bh, r);
        }
        if (focused) {
            final float o = Math.max(1f, ArenaTheme.px(2));
            g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.7f, S1),
                    bx - o, by - o, bw + 2 * o, bh + 2 * o, r + o);
        }
    }

    /** {@link #button} with its label centred on it (dark text on the molten button, light text on the others). */
    public static void button(final Graphics g, final String text, final FSkinFont font, final float x, final float y, final float w,
            final float h, final boolean primary, final boolean hovered, final boolean pressed, final boolean focused,
            final boolean toggled, final boolean enabled) {
        button(g, x, y, w, h, primary, hovered, pressed, focused, toggled, enabled);
        if (text == null || text.isEmpty()) {
            return;
        }
        final Color fore;
        if (primary && enabled) {
            fore = BUTTON_TEXT_MOLTEN;
        } else if (!enabled) {
            fore = BUTTON_TEXT_OFF;
        } else {
            fore = ArenaTheme.TEXT;
        }
        final float pad = buttonPad(h);
        final float dy = pressed ? Math.max(1f, ArenaTheme.px(1)) : 0f;
        if (primary && enabled) {
            text(g, text, font, fore, x + pad, y + dy, w - 2 * pad, h, Align.center);
        } else {
            textShadowed(g, text, font, fore, x + pad, y + dy, w - 2 * pad, h, Align.center);
        }
    }

    private static final Color BUTTON_TEXT_MOLTEN = ArenaTheme.rgb(0x2A1205);
    private static final Color BUTTON_TEXT_OFF = ArenaTheme.rgba(220, 232, 250, 200);

    // ---- pills, badges, dividers ------------------------------------------------------------------------------------
    /** A full pill: fill and optional rim. */
    public static void pill(final Graphics g, final float x, final float y, final float w, final float h, final Color fill, final Color rim) {
        final float r = Math.min(w, h) / 2f;
        g.fillSmoothRoundRect(fill, x, y, w, h, r);
        if (rim != null) {
            g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1)), rim, x, y, w, h, r);
        }
    }

    /**
     * A pill sized to {@code text}, centred on cx, cy: the chips of counts, keywords and choices. Returns its width.
     */
    public static float badge(final Graphics g, final String text, final FSkinFont font, final float cx, final float cy,
            final Color fill, final Color rim, final Color textColor) {
        final float tw = textWidth(text, font);
        final float h = font.getLineHeight() + ArenaTheme.px(6);
        final float w = Math.max(h, tw + h * 0.9f);
        pill(g, cx - w / 2f, cy - h / 2f, w, h, fill, rim);
        text(g, text, font, textColor, cx - w / 2f, cy - h / 2f, w, h, Align.center);
        return w;
    }

    /** A round count badge (cards in a zone, a stack depth): dark disc, gold rim, the figure in the middle. */
    public static void countBadge(final Graphics g, final String figure, final float cx, final float cy, final float diameter) {
        final float r = diameter / 2f;
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.5f, S1), cx - r, cy - r + ArenaTheme.px(1), diameter, diameter, r, ArenaTheme.px(3));
        g.fillSmoothRoundRectGradient(ArenaTheme.NAVY_LIGHT, ArenaTheme.NIGHT, true, cx - r, cy - r, diameter, diameter, r);
        g.drawArenaShape(Graphics.SHAPE_RING, cx - r, cy - r, diameter, diameter, r, Math.max(1f, diameter / 14f),
                ArenaTheme.GOLD_BRIGHT, ArenaTheme.GOLD_DIM, true);
        final FSkinFont font = ArenaTheme.fontPx(Math.round(diameter * (figure.length() > 2 ? 0.42f : 0.55f)));
        text(g, figure, font, ArenaTheme.TEXT, cx - r, cy - r, diameter, diameter, Align.center);
    }

    /** A horizontal hairline that fades out at both ends. */
    public static void divider(final Graphics g, final float x, final float y, final float w, final Color c, final float thickness) {
        STOPS4[0] = ArenaTheme.alpha(c, 0f, S1);
        STOPS4[1] = c;
        STOPS4[2] = c;
        STOPS4[3] = S1;
        g.fillGradientStops(STOPS4, BAND_STOPS, false, x, y, w, thickness);
    }

    /** A vertical hairline that fades out at both ends. */
    public static void dividerVertical(final Graphics g, final float x, final float y, final float h, final Color c, final float thickness) {
        STOPS4[0] = ArenaTheme.alpha(c, 0f, S1);
        STOPS4[1] = c;
        STOPS4[2] = c;
        STOPS4[3] = S1;
        g.fillGradientStops(STOPS4, BAND_STOPS, true, x, y, thickness, h);
    }

    // ---- full-area effects ------------------------------------------------------------------------------------------
    /** What a dialog dims the board with (Swing CLR_OVERLAY), times {@code alpha} (e.g. an opening Fade). */
    public static void dim(final Graphics g, final float x, final float y, final float w, final float h, final float alpha) {
        g.fillRect(ArenaTheme.fadeBy(ArenaTheme.OVERLAY, alpha, S1), x, y, w, h);
    }

    /** Darkened edges: {@code strength} 0..1 is the alpha at the very edge, gone {@code depth} in. */
    public static void vignette(final Graphics g, final float x, final float y, final float w, final float h, final float depth,
            final float strength) {
        final Color edge = ArenaTheme.alpha(ArenaTheme.BLACK, strength, S1);
        final Color clear = ArenaTheme.alpha(ArenaTheme.BLACK, 0f, S2);
        final float d = Math.min(depth, Math.min(w, h) / 2f);
        g.fillGradientRect(edge, clear, true, x, y, w, d);
        g.fillGradientRect(clear, edge, true, x, y + h - d, w, d);
        g.fillGradientRect(edge, clear, false, x, y, d, h);
        g.fillGradientRect(clear, edge, false, x + w - d, y, d, h);
    }

    /**
     * The band words cross the board on (ArenaFx turn banner): dark in the middle and clear at both ends, with an
     * accent line along its top and bottom, all faded by {@code alpha}. Draw the words over it yourself.
     */
    public static void banner(final Graphics g, final float x, final float y, final float w, final float h, final Color accent, final float alpha) {
        final Color dark = ArenaTheme.alpha(BANNER_DARK, BANNER_DARK.a * alpha, S1), clear = ArenaTheme.alpha(BANNER_DARK, 0f, S2);
        STOPS4[0] = clear;
        STOPS4[1] = dark;
        STOPS4[2] = dark;
        STOPS4[3] = clear;
        g.fillGradientStops(STOPS4, BAND_STOPS, false, x, y, w, h);
        final Color line = ArenaTheme.alpha(accent, 0.92f * alpha, S3), noLine = ArenaTheme.alpha(accent, 0f, S4);
        STOPS4[0] = noLine;
        STOPS4[1] = line;
        STOPS4[2] = line;
        STOPS4[3] = noLine;
        final float t = Math.max(1f, ArenaTheme.px(2));
        g.fillGradientStops(STOPS4, BAND_STOPS, false, x, y, w, t);
        g.fillGradientStops(STOPS4, BAND_STOPS, false, x, y + h - t, w, t);
    }

    private static final Color BANNER_DARK = ArenaTheme.rgba(4, 6, 10, 215);

    // board moods: floor top, floor bottom, light pool, rails (approximations of the Swing board's three moods)
    private static final Color[][] BOARD = {
            {ArenaTheme.rgb(0x3C3D38), ArenaTheme.rgb(0x5C5E5B), ArenaTheme.rgb(0xFFF1DC), ArenaTheme.rgb(0x242522)}, // day
            {ArenaTheme.rgb(0x302E2B), ArenaTheme.rgb(0x57544D), ArenaTheme.rgb(0xFFE8D2), ArenaTheme.rgb(0x1E1D1B)}, // default
            {ArenaTheme.rgb(0x131420), ArenaTheme.rgb(0x2E3044), ArenaTheme.rgb(0xFFB490), ArenaTheme.rgb(0x0E0F18)}, // night
    };

    /**
     * A GPU stand-in for the Swing board art (ArenaArt.createBoard, a one-second procedural render): the floor's
     * gradient, a warm pool of light at (0.44, 0.39), darker rails along the top 7.8% and below 92.6% and a vignette.
     * A handful of batched quads; cheap enough for every frame. A view that wants the full stone mat can render it once
     * into a texture of its own.
     */
    public static void board(final Graphics g, final float x, final float y, final float w, final float h, final ArenaTheme.BoardVariant variant) {
        final Color[] m = BOARD[variant.ordinal()];
        g.fillGradientRect(m[0], m[1], true, x, y, w, h);
        haloEllipse(g, x + w * 0.44f, y + h * 0.39f, w * 0.42f, h * 0.36f, m[2], variant == ArenaTheme.BoardVariant.NIGHT ? 0.20f : 0.14f);
        final float top = h * 0.078f, bottom = h * (1f - 0.926f);
        g.fillGradientRect(ArenaTheme.alpha(m[3], 0.85f, S3), ArenaTheme.alpha(m[3], 0.35f, S4), true, x, y, w, top);
        g.fillGradientRect(ArenaTheme.alpha(m[3], 0.35f, S3), ArenaTheme.alpha(m[3], 0.85f, S4), true, x, y + h - bottom, w, bottom);
        vignette(g, x, y, w, h, Math.min(w, h) * 0.22f, 0.55f);
    }

    // ---- portraits --------------------------------------------------------------------------------------------------
    private static final TextureRegion SQUARE = new TextureRegion();

    /**
     * A card's art as a square for a circle crop, in a region of its own (keep it: {@link #squareOf} shares one), or null
     * while it has no picture (the "no picture" stand-in counts as none). Look it up when the card changes, not per frame.
     */
    public static TextureRegion circleArtOf(final CardView card) {
        final FImageComplex picture;
        try {
            picture = CardRenderer.getCardArt(card);
        } catch (final RuntimeException e) {
            return null;
        }
        if (picture == null || picture == CardImageRenderer.forgeArt) {
            return null;
        }
        final TextureRegion r = picture.getTextureRegion();
        if (r == null || r.getRegionWidth() <= 0 || r.getRegionHeight() <= 0) {
            return null;
        }
        return new TextureRegion(squareOf(r));
    }

    /**
     * The middle square of a picture, so a circle crop of it is round (portraits, a commander's art in a chip). Shared
     * scratch region: use it before asking for another.
     */
    public static TextureRegion squareOf(final TextureRegion r) {
        final int w = r.getRegionWidth(), h = r.getRegionHeight();
        if (w == h) {
            return r;
        }
        final int s = Math.min(w, h);
        SQUARE.setTexture(r.getTexture());
        SQUARE.setRegion(r.getRegionX() + (w - s) / 2, r.getRegionY() + (h - s) / 2, s, s);
        SQUARE.flip(r.isFlipX(), r.isFlipY());
        return SQUARE;
    }

    /** An image cropped to a circle (antialiased), e.g. an avatar. One draw call (tilt shader). */
    public static void circleImage(final Graphics g, final TextureRegion image, final float cx, final float cy, final float diameter) {
        g.drawTiltedImage(image, cx - diameter / 2f, cy - diameter / 2f, diameter, diameter, 0f, 0f, 0f, 1f, diameter / 2f, 0f, 0f, 1f);
    }

    /**
     * The Arena portrait ring (simplified Swing paintPortraitRing): an accent glow, a bright-to-dim gold ring and an
     * inner shadow, round a circle of {@code diameter} centred on cx, cy. Draw the portrait first.
     */
    public static void portraitRing(final Graphics g, final float cx, final float cy, final float diameter, final Color accent, final float glowAlpha) {
        final float r = diameter / 2f, rim = Math.max(2f, diameter / 26f);
        if (accent != null && glowAlpha > 0f) {
            g.drawGlow(ArenaTheme.alpha(accent, glowAlpha, S1), cx - r, cy - r, diameter, diameter, r, rim * 3f);
        }
        g.drawArenaShape(Graphics.SHAPE_RING, cx - r + rim, cy - r + rim, diameter - 2 * rim, diameter - 2 * rim, r - rim, rim * 1.6f,
                ArenaTheme.alpha(ArenaTheme.BLACK, 0.55f, S2), ArenaTheme.alpha(ArenaTheme.BLACK, 0.55f, S2), true);
        g.drawArenaShape(Graphics.SHAPE_RING, cx - r, cy - r, diameter, diameter, r, rim, ArenaTheme.GOLD_BRIGHT, ArenaTheme.GOLD_DIM, true);
    }

    // ---- text -------------------------------------------------------------------------------------------------------
    /** One line (or wrapped block) of text in a box, vertically centred; {@code align} is a libGDX {@link Align}. */
    public static void text(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h, final int align) {
        g.drawText(text, font, color, x, y, w, h, false, align, true);
    }

    /** Text wrapped to the box, starting at its top. */
    public static void textWrapped(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h, final int align) {
        g.drawText(text, font, color, x, y, w, h, true, align, false);
    }

    /** Text with the Swing drop shadow (black at 190/255, one px right and two down): titles, names, buttons. */
    public static void textShadowed(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h, final int align) {
        final float o = Math.max(1f, ArenaTheme.px(1));
        g.drawText(text, font, ArenaTheme.alpha(ArenaTheme.BLACK, 0.75f * color.a, S1), x + o, y + 2 * o, w, h, false, align, true);
        g.drawText(text, font, color, x, y, w, h, false, align, true);
    }

    /** Text with a dark outline all round (Swing floating numbers): legible on anything. */
    public static void textOutlined(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h, final int align) {
        final float o = Math.max(1f, ArenaTheme.px(2));
        final Color dark = ArenaTheme.alpha(ArenaTheme.BLACK, 0.78f * color.a, S1);
        g.drawText(text, font, dark, x - o, y, w, h, false, align, true);
        g.drawText(text, font, dark, x + o, y, w, h, false, align, true);
        g.drawText(text, font, dark, x, y - o, w, h, false, align, true);
        g.drawText(text, font, dark, x, y + o, w, h, false, align, true);
        g.drawText(text, font, dark, x + o / 2, y + o, w, h, false, align, true);
        g.drawText(text, font, color, x, y, w, h, false, align, true);
    }

    /**
     * A heavier line for titles and figures: the skin has no bold face, so the text is drawn twice half a pixel apart
     * (Swing's display font was bold). Costs two text passes.
     */
    public static void textBold(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h, final int align) {
        final float o = Math.max(0.5f, ArenaTheme.px(0.6f));
        g.drawText(text, font, color, x, y, w, h, false, align, true);
        g.drawText(text, font, color, x + o, y, w, h, false, align, true);
    }

    /** Rules text with mana symbols drawn as icons ({W}, {2}, {W/U}...), wrapped in the box (Swing ArenaText). */
    public static void rulesText(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h) {
        RULES.drawText(g, text, font, color, x, y, w, h, y, h, true, Align.left, false);
    }

    /** One centred line with mana symbols drawn as icons ("Pay {2} each"). */
    public static void symbolText(final Graphics g, final String text, final FSkinFont font, final Color color, final float x, final float y,
            final float w, final float h) {
        RULES.drawText(g, text, font, color, x, y, w, h, y, h, false, Align.center, true);
    }

    /** Height rules text takes when wrapped to {@code w}. */
    public static float rulesTextHeight(final String text, final FSkinFont font, final float w) {
        return RULES.getWrappedBounds(text, font, w).height;
    }

    /** Width of one line of text. */
    public static float textWidth(final String text, final FSkinFont font) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        font.getBounds(text, 0, text.length(), BOUNDS);
        return BOUNDS.width;
    }

    private static final ObjectMap<FSkinFont, ObjectFloatMap<String>> WIDTHS = new ObjectMap<>();
    private static final int WIDTHS_PER_FONT = 512;

    /**
     * {@link #textWidth} for text drawn every frame (names, figures, labels): measured once per font and string, then
     * looked up. Pass strings that are kept (shared figures, names, labels), not ones built per frame. Render thread.
     */
    public static float cachedTextWidth(final String text, final FSkinFont font) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        ObjectFloatMap<String> widths = WIDTHS.get(font);
        if (widths == null) {
            widths = new ObjectFloatMap<>();
            WIDTHS.put(font, widths);
        }
        float w = widths.get(text, -1f);
        if (w < 0f) {
            if (widths.size >= WIDTHS_PER_FONT) {
                widths.clear(); // a long game's worth of names and figures: start again rather than grow
            }
            w = textWidth(text, font);
            widths.put(text, w);
        }
        return w;
    }

    private static final Color HINT_FILL = ArenaTheme.rgba(6, 8, 13, 235);

    /** How tall {@link #hint} draws with this font. */
    public static float hintHeight(final FSkinFont font) {
        return Math.round(font.getLineHeight() * 1.5f);
    }

    /**
     * A one-line hint plate (what something lit up means and that it can be clicked) centred on {@code cx}, its top at
     * {@code top}, kept inside {@code minX..maxX} (the window, in the caller's coordinates). {@code text} should be a
     * kept string (its width is cached).
     */
    public static void hint(final Graphics g, final String text, final FSkinFont font, final float cx, final float top, final float minX,
            final float maxX) {
        hint(g, text, font, cx, top, minX, maxX, ArenaTheme.GOLD, ArenaTheme.WHITE);
    }

    /** {@link #hint} with its rim and text in these colours (an attack choice's state, a greyed-out choice). */
    public static void hint(final Graphics g, final String text, final FSkinFont font, final float cx, final float top, final float minX,
            final float maxX, final Color rim, final Color ink) {
        final float h = hintHeight(font);
        final float w = cachedTextWidth(text, font) + h;
        final float x = Math.max(minX, Math.min(maxX - w, cx - w / 2f));
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.6f, S1), x, top + ArenaTheme.px(2), w, h, h / 2f, ArenaTheme.px(6));
        g.fillSmoothRoundRect(HINT_FILL, x, top, w, h, h / 2f);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.5f)), rim, x, top, w, h, h / 2f);
        if (text.indexOf('{') >= 0) { // a cost in it ("{2}"): mana symbols, as on the cards
            RULES.drawText(g, text, font, ink, x, top, w, h, top, h, false, Align.center, true);
        } else {
            g.drawText(text, font, ink, x, top, w, h, false, Align.center, true);
        }
    }
}
