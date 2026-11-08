package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;
import forge.game.phase.PhaseType;

/**
 * The phase glyphs of the Arena ladder (port of Swing ArenaArt.paintPhaseGlyph): a small diamond for a step, a card
 * for a main phase, crossed swords for attackers and a shield for blockers. The four shapes are drawn once into a
 * small white texture (antialiased by supersampling) and tinted as they are drawn, so a ladder costs a few batched
 * quads and no transforms. The current phase is lit with a candle glow; a phase with a stop is filled, one without is
 * faint; the one under the pointer brightens.
 */
public final class ArenaGlyphs {
    private ArenaGlyphs() { }

    public static final int STEP = 0, MAIN = 1, ATTACK = 2, BLOCK = 3;
    private static final int CELL = 96, SAMPLES = 4;
    private static Texture atlas;
    private static final TextureRegion[] GLYPH = new TextureRegion[4];
    private static final Color S1 = new Color();
    private static final Color LIT = ArenaTheme.rgb(0xFFF4D0), CANDLE = ArenaTheme.rgb(0xFFB04C), EDGE = ArenaTheme.rgb(0x000000);

    /** The kind of glyph for a phase. */
    public static int kindOf(final PhaseType phase) {
        switch (phase) {
            case MAIN1:
            case MAIN2:
                return MAIN;
            case COMBAT_DECLARE_ATTACKERS:
                return ATTACK;
            case COMBAT_DECLARE_BLOCKERS:
                return BLOCK;
            default:
                return STEP;
        }
    }

    /** How wide a glyph's cell is for a ladder this tall (steps are narrow, combat's beginning a little narrower). */
    public static float cellWidth(final PhaseType phase, final float height) {
        if (phase == PhaseType.COMBAT_BEGIN) {
            return height * 29f / 34f;
        }
        return kindOf(phase) == STEP ? height * 9f / 34f : height;
    }

    /**
     * Draws a glyph centred in the box: {@code lit} for the current phase, {@code stop} when play stops there,
     * {@code hover} under the pointer.
     */
    public static void draw(final Graphics g, final int kind, final float x, final float y, final float w, final float h, final boolean lit,
            final boolean stop, final boolean hover, final float alpha) {
        final TextureRegion glyph = glyph(kind);
        final float size = Math.min(h, Math.max(w, h * 0.5f)) * (kind == STEP ? 0.62f : 0.64f);
        final float cx = x + w / 2f, cy = y + h / 2f;
        if (lit) {
            ArenaArt.halo(g, cx, cy, h * 0.55f, CANDLE, 0.85f * alpha);
        } else if (hover) {
            ArenaArt.halo(g, cx, cy, h * 0.5f, ArenaTheme.HOVER, 0.35f * alpha);
        }
        if (glyph == null) {
            g.fillSmoothCircle(ArenaTheme.alpha(lit ? LIT : ArenaTheme.WHITE, (lit ? 1f : stop ? 0.7f : 0.25f) * alpha, S1), cx, cy, size / 2f);
            return;
        }
        final float a = g.getfloatAlphaComposite();
        // a dark edge first, so the glyph reads on stone and on steel alike
        final float o = Math.max(1f, size * 0.06f);
        g.setColorRGBA(EDGE.r, EDGE.g, EDGE.b, a * alpha * (lit ? 0.45f : hover ? 0.6f : kind == STEP ? 0.35f : 0.5f));
        g.drawImage(glyph, cx - size / 2f - o, cy - size / 2f - o, size + 2 * o, size + 2 * o);
        final float fill = lit ? 1f : hover ? 0.9f : kind == STEP ? (stop ? 0.5f : 0.2f) : (stop ? 0.72f : 0.24f);
        if (lit) {
            g.setColorRGBA(LIT.r, LIT.g, LIT.b, a * alpha);
        } else {
            g.setColorRGBA(1f, 0.99f, 0.97f, a * alpha * fill);
        }
        g.drawImage(glyph, cx - size / 2f, cy - size / 2f, size, size);
        g.resetColorRGBA(a);
        g.setAlphaComposite(a);
    }

    private static TextureRegion glyph(final int kind) {
        if (atlas == null) {
            build();
        }
        return GLYPH[kind];
    }

    private static void build() {
        try {
            final Pixmap p = new Pixmap(CELL * 4, CELL, Pixmap.Format.RGBA8888);
            p.setBlending(Pixmap.Blending.None);
            for (int k = 0; k < 4; k++) {
                for (int py = 0; py < CELL; py++) {
                    for (int px = 0; px < CELL; px++) {
                        int hits = 0;
                        for (int sy = 0; sy < SAMPLES; sy++) {
                            for (int sx = 0; sx < SAMPLES; sx++) {
                                final float u = (px + (sx + 0.5f) / SAMPLES) / CELL, v = (py + (sy + 0.5f) / SAMPLES) / CELL;
                                if (inside(k, u, v)) {
                                    hits++;
                                }
                            }
                        }
                        final int alpha = Math.round(255f * hits / (SAMPLES * SAMPLES));
                        p.drawPixel(k * CELL + px, py, 0xFFFFFF00 | alpha);
                    }
                }
            }
            atlas = new Texture(p, true);
            atlas.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            p.dispose();
            for (int k = 0; k < 4; k++) {
                GLYPH[k] = new TextureRegion(atlas, k * CELL, 0, CELL, CELL);
            }
        } catch (final RuntimeException e) {
            e.printStackTrace();
            atlas = null;
        }
    }

    /** Whether the point u, v (0..1 across the glyph's square, y down) is inside glyph k. */
    private static boolean inside(final int k, final float u, final float v) {
        final float x = u - 0.5f, y = v - 0.5f;
        switch (k) {
            case STEP: // a diamond
                return Math.abs(x) + Math.abs(y) < 0.4f;
            case MAIN: { // a card: a rounded upright rectangle
                final float hw = 0.33f, hh = 0.48f, r = 0.085f;
                final float qx = Math.abs(x) - (hw - r), qy = Math.abs(y) - (hh - r);
                final float ox = Math.max(qx, 0f), oy = Math.max(qy, 0f);
                return (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, qy), 0f) < r;
            }
            case ATTACK: // crossed swords: two blades at +-42 degrees, each with a guard and a pommel
                return sword(x, y, 42f) || sword(x, y, -42f);
            default: { // a shield: a flat top easing in, round shoulders, a point at the bottom
                if (y < -0.46f || y > 0.48f) {
                    return false;
                }
                final float t = (y + 0.46f) / 0.94f; // 0 top .. 1 bottom
                final float half = t < 0.55f ? 0.42f - 0.04f * (float) Math.pow(1f - t / 0.55f, 3) : 0.42f * (float) Math.sqrt(Math.max(0f, 1f - (t - 0.55f) / 0.45f));
                final float notch = t < 0.12f ? 0.08f * (1f - t / 0.12f) : 0f; // the top dips a little in the middle
                return Math.abs(x) < half && !(Math.abs(x) < 0.12f && t < notch);
            }
        }
    }

    private static boolean sword(final float x, final float y, final float degrees) {
        final double a = Math.toRadians(degrees);
        // into the blade's own frame: the blade runs along v from the tip (-) to the pommel (+)
        final float bu = (float) (x * Math.cos(a) + y * Math.sin(a));
        final float bv = (float) (-x * Math.sin(a) + y * Math.cos(a));
        if (bv > -0.52f && bv < 0.18f) { // the blade, pointed at its tip
            final float width = bv < -0.40f ? 0.07f * (bv + 0.52f) / 0.12f : 0.07f;
            if (Math.abs(bu) < width) {
                return true;
            }
        }
        if (bv >= 0.16f && bv < 0.24f && Math.abs(bu) < 0.2f) { // the guard
            return true;
        }
        if (bv >= 0.24f && bv < 0.42f && Math.abs(bu) < 0.045f) { // the grip
            return true;
        }
        final float px = bu, py = bv - 0.46f; // the pommel
        return px * px + py * py < 0.065f * 0.065f;
    }
}
