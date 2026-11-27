package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.assets.FImageComplex;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.item.PaperCard;
import java.util.HashMap;
import java.util.Map;

/**
 * Rogue Commander's parchment look, drawn with adventure mode's skin without entering adventure mode.
 * Positions and sizes are screen pixels. Lengths that belong to the look, such as border widths and font
 * sizes, are in units: a 270th of the short screen side, as adventure lays out its screens.
 */
public final class RogueLook {
    public static final Color INK = new Color(0x1f1b16ff);
    public static final Color SOFT = new Color(0x5d5244ff);
    public static final Color TEAL = new Color(0x12808fff);
    public static final Color PALE = new Color(0xfff6dcff);
    public static final Color GOLD = new Color(0xffd75eff);
    public static final Color DEEP = new Color(0x8a5a12ff);
    public static final Color PAPER = new Color(0xe4d2a8ff);
    /** The gold of the rules on gilded panels. */
    public static final Color RULE = new Color(0xc9a03eff);
    private static final Color HALO = new Color(1f, 0.55f, 0.4f, 0f);
    public static final float CARD_RATIO = 1.395f;

    private static final Map<String, Texture> textures = new HashMap<>();
    private static final TextureRegion INSET = new TextureRegion();
    private static final int TRAIL_STEPS = 44;
    private static final float[] TRAIL_X = new float[TRAIL_STEPS + 1];
    private static final float[] TRAIL_Y = new float[TRAIL_STEPS + 1];
    private static final float[] TRAIL_ALONG = new float[TRAIL_STEPS + 1];
    private static final Matrix4 scaled = new Matrix4();
    private static BitmapFont font;
    private static float zoom = 1;

    private RogueLook() {
    }

    public static float unit() {
        return Math.min(Forge.getScreenWidth(), Forge.getScreenHeight()) / 270f * zoom;
    }

    /** Lays out or draws at a smaller or larger unit, as the whole map does. GUI thread only. */
    static void zoomed(float factor, Runnable action) {
        float previous = zoom;
        zoom = factor;
        try {
            action.run();
        } finally {
            zoom = previous;
        }
    }

    static BitmapFont font() {
        if (font == null) {
            font = Controls.getSkin().getFont("default");
            font.setUseIntegerPositions(false);
        }
        return font;
    }

    static Texture texture(String path) {
        return textures.computeIfAbsent(path, p -> {
            Texture t = new Texture(Config.instance().getFile(p));
            if (p.startsWith("ui/")) {
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
            return t;
        });
    }

    /** Skin drawables and the font keep their proportions only when the batch is scaled by the unit. */
    private static void inUnits(Graphics g, Runnable draw) {
        SpriteBatch batch = g.getBatch();
        Matrix4 previous = batch.getTransformMatrix().cpy();
        batch.setTransformMatrix(scaled.set(previous).scl(unit()));
        draw.run();
        batch.setTransformMatrix(previous);
        batch.setColor(Color.WHITE);
    }

    /** One of adventure's backgrounds, such as {@code title_bg}, covering the area, darkened by dim. */
    public static void background(Graphics g, String name, float w, float h, float dim) {
        Texture t = texture("ui/" + name + (Forge.isLandscapeMode() ? "" : "_portrait") + ".png");
        float s = Math.max(w / t.getWidth(), h / t.getHeight());
        g.drawImage(t, (w - t.getWidth() * s) / 2, (h - t.getHeight() * s) / 2, t.getWidth() * s, t.getHeight() * s);
        if (dim > 0) {
            g.fillRect(new Color(0, 0, 0, dim), 0, 0, w, h);
        }
    }

    public static void patch(Graphics g, String name, float x, float y, float w, float h) {
        float u = unit();
        float bx = g.adjustX(x) / u;
        float by = g.adjustY(y, h) / u;
        inUnits(g, () -> Controls.getSkin().getDrawable(name).draw(g.getBatch(), bx, by, w / u, h / u));
    }

    public static void paper(Graphics g, float x, float y, float w, float h) {
        float inset = 7 * unit();
        g.fillRect(PAPER, x + inset, y + inset, w - 2 * inset, h - 2 * inset);
        patch(g, "paper10Patch", x, y, w, h);
    }

    /** Parchment with a gold double rule and corner diamonds, for a page's chief panel. */
    public static void gilded(Graphics g, float x, float y, float w, float h) {
        float inset = 7 * unit();
        g.fillRect(PAPER, x + inset, y + inset, w - 2 * inset, h - 2 * inset);
        patch(g, "paperGold10Patch", x, y, w, h);
    }

    /** A pale inset on parchment with a thin ink edge. A chosen one is brighter and has a thick edge. */
    public static void tile(Graphics g, float x, float y, float w, float h, boolean chosen) {
        g.fillRect(chosen ? new Color(1f, 0.97f, 0.83f, 1f) : new Color(1f, 0.97f, 0.86f, 0.5f), x, y, w, h);
        box(g, x, y, w, h, (chosen ? 1.8f : 0.6f) * unit(), chosen ? INK : new Color(0.36f, 0.32f, 0.27f, 0.7f));
    }

    /** An empty place, such as a free slot. */
    public static void dashTile(Graphics g, float x, float y, float w, float h) {
        float u = unit();
        g.fillRect(new Color(1f, 0.97f, 0.86f, 0.3f), x, y, w, h);
        g.drawDashedRect(1.2f * u, SOFT, x, y, w, h, 4 * u, 3 * u, 0);
    }

    public static void box(Graphics g, float x, float y, float w, float h, float thickness, Color c) {
        g.fillRect(c, x, y, w, thickness);
        g.fillRect(c, x, y + h - thickness, w, thickness);
        g.fillRect(c, x, y, thickness, h);
        g.fillRect(c, x + w - thickness, y, thickness, h);
    }

    /** thickness is in units. */
    public static void line(Graphics g, float x1, float y1, float x2, float y2, float thickness, Color c) {
        g.drawLine(thickness * unit(), c, x1, y1, x2, y2);
    }

    /** thickness is in units. */
    public static void ring(Graphics g, float cx, float cy, float r, float thickness, Color c) {
        g.drawCircle(thickness * unit(), c, cx, cy, r);
    }

    /** A path on the map: straight, then a curve, then straight. Style 0 is faint dots, 1 dashes, 2 solid. */
    public static void trail(Graphics g, float x1, float y1, float x2, float y2, boolean sideways, int style, Color c) {
        float u = unit();
        int steps = TRAIL_STEPS;
        float[] xs = TRAIL_X;
        float[] ys = TRAIL_Y;
        float[] along = TRAIL_ALONG;
        xs[0] = x1;
        ys[0] = y1;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            float e = t < 0.2f ? 0 : t > 0.8f ? 1 : (t - 0.2f) / 0.6f;
            e = e * e * (3 - 2 * e);
            xs[i] = sideways ? x1 + (x2 - x1) * t : x1 + (x2 - x1) * e;
            ys[i] = sideways ? y1 + (y2 - y1) * e : y1 + (y2 - y1) * t;
            along[i] = along[i - 1] + (float) Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]) / u;
        }
        float total = along[steps];
        for (int i = 1; i <= steps; i++) {
            // Each half is dashed from its own end, so trails sharing an end lay their dashes on top of each other
            float mid = (along[i - 1] + along[i]) / 2;
            float d = mid < total / 2 ? mid : total - mid;
            boolean on = style == 2 || (style == 1 ? d % 7 < 4.6f : d % 5 < 1.6f);
            if (on) {
                line(g, xs[i - 1], ys[i - 1], xs[i], ys[i], style == 2 ? 2.6f : style == 1 ? 1.8f : 1.2f, c);
            }
        }
    }

    static GlyphLayout layout(String s, float scale, float w, boolean wrap, int align, Color c) {
        BitmapFont f = font();
        f.getData().setScale(scale);
        f.setColor(c);
        return new GlyphLayout(f, s == null ? "" : s, c, w / unit(), align, wrap);
    }

    public static float textWidth(String s, float scale) {
        return layout(s, scale, 0, false, Align.left, INK).width * unit();
    }

    public static float lineHeight(float scale) {
        return 13 * scale * unit();
    }

    public static float wrappedHeight(String s, float w, float scale) {
        return (layout(s, scale, w, true, Align.left, INK).height + 3 * scale) * unit();
    }

    /** One line with its top at y. Scale 1 is adventure's default text size. */
    public static void text(Graphics g, String s, float x, float y, float scale, Color c) {
        text(g, s, x, y, scale, c, 0, Align.left);
    }

    public static void text(Graphics g, String s, float x, float y, float scale, Color c, float w, int align) {
        draw(g, layout(s, scale, w, false, align, c), x, y, scale);
    }

    /** Fits one line by making the text smaller, down to three quarters, and then shortening it. */
    public static void fit(Graphics g, String s, float x, float y, float scale, Color c, float w, int align) {
        float k = scale;
        float width = textWidth(s, k);
        while (width > w && k > scale * 0.75f) {
            k -= 0.05f;
            width = textWidth(s, k);
        }
        while (width > w && s.length() > 2) {
            s = s.substring(0, s.length() - 2).trim() + "…";
            width = textWidth(s, k);
        }
        text(g, s, x, y + (scale - k) * 6 * unit(), k, c, w, align);
    }

    /** Wrapped text with its top at y. Returns the height it took. */
    public static float wrap(Graphics g, String s, float x, float y, float w, float scale, Color c, int align) {
        GlyphLayout l = layout(s, scale, w, true, align, c);
        draw(g, l, x, y, scale);
        return (l.height + 3 * scale) * unit();
    }

    private static void draw(Graphics g, GlyphLayout l, float x, float y, float scale) {
        float u = unit();
        float bx = g.adjustX(x) / u;
        float by = g.adjustY(y, 0) / u - 2 * scale;
        inUnits(g, () -> font().draw(g.getBatch(), l, bx, by));
    }

    public static void sprite(Graphics g, TextureRegion region, float x, float y, float w, float h, Color tint) {
        if (region == null) {
            return;
        }
        // Half a texel in from each edge, so a sprite drawn between whole pixels never samples its atlas neighbour
        float iu = Math.signum(region.getU2() - region.getU()) * 0.5f / region.getTexture().getWidth();
        float iv = Math.signum(region.getV2() - region.getV()) * 0.5f / region.getTexture().getHeight();
        INSET.setTexture(region.getTexture());
        INSET.setRegion(region.getU() + iu, region.getV() + iv, region.getU2() - iu, region.getV2() - iv);
        float alpha = g.getBatch().getColor().a;
        g.getBatch().setColor(tint.r, tint.g, tint.b, tint.a * alpha);
        g.drawImage(INSET, x, y, w, h);
        g.getBatch().setColor(1, 1, 1, alpha);
    }

    /** An adventure item sprite by name, such as {@code Gold} or {@code Refresh}. */
    public static void item(Graphics g, String name, float x, float y, float size) {
        sprite(g, Config.instance().getItemSprite(name), x, y, size, size, Color.WHITE);
    }

    public static void skin(Graphics g, FSkinImage image, float x, float y, float size) {
        image.draw(g, x, y, size, size);
    }

    /** A piece of adventure's town tileset, given in its own pixels. */
    public static void building(Graphics g, int sx, int sy, int sw, int sh, float x, float y, float w, float h, float alpha) {
        sprite(g, new TextureRegion(texture("maps/tileset/buildings.png"), sx, sy, sw, sh), x, y, w, h, new Color(1, 1, 1, alpha));
    }

    public static void avatar(Graphics g, int index, float x, float y, float size) {
        float edge = 1.2f * unit();
        g.fillRect(INK, x - edge, y - edge, size + 2 * edge, size + 2 * edge);
        sprite(g, FSkin.getAvatars().get(index), x, y, size, size, Color.WHITE);
    }

    public static void card(Graphics g, PaperCard c, float x, float y, float w, float alpha) {
        card(g, c, x, y, w, w * CARD_RATIO, alpha);
    }

    /** The card squeezed into w by h, as when it turns over. */
    public static void card(Graphics g, PaperCard c, float x, float y, float w, float h, float alpha) {
        if (c == null) {
            return;
        }
        if (alpha < 1) {
            g.setAlphaComposite(alpha);
        }
        CardRenderer.drawCard(g, c, x, y, w, h, CardStackPosition.Top);
        if (alpha < 1) {
            g.resetAlphaComposite();
        }
    }

    /** A card's back: the skin's sleeve with this number. */
    public static void back(Graphics g, int sleeve, float x, float y, float w) {
        sprite(g, FSkin.getSleeves().get(sleeve), x, y, w, w * CARD_RATIO, Color.WHITE);
    }

    /** The card's art, cropped to fill the area. */
    public static void art(Graphics g, PaperCard c, float x, float y, float w, float h, float alpha) {
        FImageComplex image = c == null ? null : CardRenderer.getCardArt(c);
        if (image == null) {
            return;
        }
        // A clip outside an enclosing one fails; it still needs its endClip, or the enclosing clip stays pushed
        if (!g.startClip(x, y, w, h)) {
            g.endClip();
            return;
        }
        if (alpha < 1) {
            g.setAlphaComposite(alpha);
        }
        float ratio = image.getWidth() / image.getHeight();
        float w0 = Math.max(w, h * ratio);
        float h0 = w0 / ratio;
        image.draw(g, x - (w0 - w) / 2, y - (h0 - h) / 2, w0, h0);
        g.endClip();
        if (alpha < 1) {
            g.resetAlphaComposite();
        }
    }

    /**
     * Twelve rays 30 degrees apart round cx, cy, turned by spin degrees, reaching reach. Each band colours an equal step
     * outwards, so fading colours fade the rays.
     */
    public static void rays(Graphics g, Color[] bands, float cx, float cy, float reach, float spin) {
        for (int i = 0; i < 12; i++) {
            float a = (spin + i * 30) * MathUtils.degreesToRadians;
            float b = a + 12 * MathUtils.degreesToRadians;
            float ca = MathUtils.cos(a), sa = MathUtils.sin(a), cb = MathUtils.cos(b), sb = MathUtils.sin(b);
            for (int band = 0; band < bands.length; band++) {
                float r0 = reach * band / bands.length;
                float r1 = reach * (band + 1) / bands.length;
                g.fillTriangle(bands[band], cx + r0 * ca, cy + r0 * sa, cx + r1 * ca, cy + r1 * sa, cx + r1 * cb, cy + r1 * sb);
                g.fillTriangle(bands[band], cx + r0 * ca, cy + r0 * sa, cx + r1 * cb, cy + r1 * sb, cx + r0 * cb, cy + r0 * sb);
            }
        }
    }

    /** A warm halo of radius r round cx, cy, pulsing with time in seconds; fade from 0 to 1. */
    public static void halo(Graphics g, float cx, float cy, float r, float time, float fade) {
        float pulsed = r * (1 + 0.08f * (float) Math.sin(time * 6));
        HALO.a = 0.22f * fade;
        for (int k = 3; k > 0; k--) {
            g.fillCircle(HALO, cx, cy, pulsed * k / 3f + pulsed * 0.2f);
        }
    }

    /** A small arrow pointing down: there is more below. */
    public static void more(Graphics g, float cx, float y) {
        float u = unit();
        for (int i = 0; i < 5; i++) {
            g.fillRect(SOFT, cx - (5 - i) * u, y + i * u, (10 - 2 * i) * u, u);
        }
    }
}
