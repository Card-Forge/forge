package forge.screens.match.arena;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;

import forge.Graphics;

/**
 * The Arena battlefield mat on the GPU: a port of the layout of the Swing procedural board (ArenaArt.createBoard) as
 * batched shapes rather than a per-pixel render. Worn flagstones in running courses sunk inside a raised surround,
 * a carved medallion at (0.5, 0.42), old banners on the walls, one warm key light from the upper left, the bottom
 * right corner held quiet for the action button, and a vignette.
 *
 * <p>The stones are laid out once per board size (and mood) into flat arrays; each frame they are a few hundred
 * signed-distance quads through the Arena shader, which the batch draws in one call. Nothing is allocated per
 * frame and nothing is cached as an image.</p>
 */
public final class ArenaBoardPainter {
    // Swing BoardRender layout, as fractions of the height
    private static final float TOP = 0.078f, BOT = 0.926f, CORNER = 0.04f;
    private static final float MED_Y = 0.42f, MED_R = 0.150f;
    private static final float POOL_X = 0.44f, POOL_Y = 0.39f;
    private static final float COURSE = 0.092f, GAP = 0.0042f;

    /** One mood: floor ramp dark..light, wall face, wall cap, light pool, banner grounds and trim. */
    private static final class Mood {
        final Color floorDark, floorLight, faceDark, faceLight, capLight, rock, pool, trim, quiet;
        final Color[] banner;
        final float poolGain, vignette;

        Mood(final int floorDark, final int floorLight, final int faceDark, final int faceLight, final int capLight, final int rock,
                final int pool, final int b1, final int b2, final int trim, final int quiet, final float poolGain, final float vignette) {
            this.floorDark = ArenaTheme.rgb(floorDark);
            this.floorLight = ArenaTheme.rgb(floorLight);
            this.faceDark = ArenaTheme.rgb(faceDark);
            this.faceLight = ArenaTheme.rgb(faceLight);
            this.capLight = ArenaTheme.rgb(capLight);
            this.rock = ArenaTheme.rgb(rock);
            this.pool = ArenaTheme.rgb(pool);
            this.banner = new Color[] {ArenaTheme.rgb(b1), ArenaTheme.rgb(b2)};
            this.trim = ArenaTheme.rgb(trim);
            this.quiet = ArenaTheme.rgb(quiet);
            this.poolGain = poolGain;
            this.vignette = vignette;
        }
    }

    // day (bleached limestone), default (warm weathered stone), night (moonlit indigo) - Swing ArenaArt.mood ramps
    private static final Mood[] MOODS = {
            new Mood(0x484A47, 0x8C8E88, 0x242522, 0x5A5A52, 0xA09F92, 0x1F201E, 0xFFC48E, 0x6C2A26, 0x28405E, 0x978658, 0x2A2C2B, 0.20f, 0.45f),
            new Mood(0x3F3D3A, 0x857F76, 0x24221F, 0x58544D, 0x9E9A8C, 0x1E1D1B, 0xFFE8D2, 0x6C2A26, 0x28405E, 0x978658, 0x2C2A28, 0.16f, 0.5f),
            new Mood(0x262834, 0x5A5D70, 0x101119, 0x383A50, 0x74768E, 0x0E0F18, 0xFFB490, 0x52243E, 0x262F54, 0x7A6E4A, 0x14151D, 0.30f, 0.55f),
    };

    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color(), S4 = new Color();

    // laid-out stones of the current board
    private static float laidW = -1, laidH = -1;
    private static int laidMood = -1;
    private static int stones;
    private static float[] sx = new float[0], sy = new float[0], sw = new float[0], sh = new float[0];
    private static Color[] top = new Color[0], bottom = new Color[0];
    private static final Color[] RIM = {new Color(), new Color()};

    private ArenaBoardPainter() { }

    public static void draw(final Graphics g, final float x, final float y, final float w, final float h, final ArenaTheme.BoardVariant variant) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final int moodIndex = variant.ordinal();
        if (w != laidW || h != laidH || moodIndex != laidMood) {
            layout(w, h, moodIndex);
        }
        final Mood m = MOODS[moodIndex];
        final float aspect = w / h;
        final float sideIn = Math.max(0.07f, Math.min(0.16f, 0.066f * aspect)) * h;
        final float fx = x + sideIn, fy = y + TOP * h, fw = w - 2 * sideIn, fh = (BOT - TOP) * h;
        final float corner = CORNER * h;

        // the surround: wall faces, darker towards the outside, a lit cap along the floor's edge
        g.fillGradientRect(m.faceDark, m.faceLight, true, x, y, w, fy - y);
        g.fillGradientRect(m.faceLight, m.faceDark, true, x, fy + fh, w, y + h - fy - fh);
        g.fillGradientRect(m.faceDark, m.faceLight, false, x, fy, sideIn, fh);
        g.fillGradientRect(m.faceLight, m.faceDark, false, fx + fw, fy, sideIn, fh);
        // the ledge round the floor (a wider rounded slab under it) and its lit cap
        final float ledge = 0.012f * h;
        g.fillSmoothRoundRectGradient(ArenaTheme.alpha(m.capLight, 0.55f, S1), ArenaTheme.alpha(m.rock, 0.9f, S2), true,
                fx - ledge, fy - ledge, fw + 2 * ledge, fh + 2 * ledge, corner + ledge);
        // mortar: the floor's ground, the stones sit on it
        g.fillSmoothRoundRect(m.rock, fx, fy, fw, fh, corner);

        // the stones (clipped to the floor's corners by their own layout)
        final float r = 0.007f * h, rim = Math.max(1f, 0.0022f * h);
        RIM[0] = ArenaTheme.alpha(ArenaTheme.WHITE, 0.10f, S3);
        RIM[1] = ArenaTheme.alpha(ArenaTheme.BLACK, 0.28f, S4);
        for (int i = 0; i < stones; i++) {
            final float px = x + sx[i], py = y + sy[i];
            g.fillSmoothRoundRectGradient(top[i], bottom[i], true, px, py, sw[i], sh[i], r);
            g.drawArenaShape(Graphics.SHAPE_RING, px, py, sw[i], sh[i], r, rim, RIM[0], RIM[1], true);
        }
        RIM[0] = RIM[1] = S1; // drop the scratch references

        // the carved medallion
        final float mcx = x + w * 0.5f, mcy = y + h * MED_Y, mr = MED_R * h;
        g.fillSmoothCircle(ArenaTheme.alpha(m.rock, 0.22f, S1), mcx, mcy, mr);
        g.drawSmoothCircle(Math.max(1.5f, 0.006f * h), ArenaTheme.alpha(ArenaTheme.BLACK, 0.30f, S1), mcx, mcy, mr);
        g.drawSmoothCircle(Math.max(1f, 0.0025f * h), ArenaTheme.alpha(m.capLight, 0.20f, S1), mcx, mcy - 0.002f * h, mr - 0.006f * h);
        g.drawSmoothCircle(Math.max(1.5f, 0.005f * h), ArenaTheme.alpha(ArenaTheme.BLACK, 0.26f, S1), mcx, mcy, mr * 0.66f);
        g.drawSmoothCircle(Math.max(1f, 0.0022f * h), ArenaTheme.alpha(m.capLight, 0.16f, S1), mcx, mcy - 0.002f * h, mr * 0.66f - 0.005f * h);
        g.fillSmoothCircle(ArenaTheme.alpha(ArenaTheme.BLACK, 0.18f, S1), mcx, mcy, mr * 0.2f);
        g.drawSmoothCircle(Math.max(1f, 0.003f * h), ArenaTheme.alpha(m.capLight, 0.18f, S1), mcx, mcy, mr * 0.2f);
        // compass points: four long and four short notches between the rings
        final float notch = 0.010f * h;
        for (int k = 0; k < 8; k++) {
            final double a = Math.PI / 4 * k;
            final float d = k % 2 == 0 ? mr * 0.83f : mr * 0.83f;
            final float nr = k % 2 == 0 ? notch : notch * 0.6f;
            final float nx = mcx + (float) Math.cos(a) * d, ny = mcy + (float) Math.sin(a) * d;
            g.fillSmoothCircle(ArenaTheme.alpha(ArenaTheme.BLACK, 0.30f, S1), nx, ny + 0.0015f * h, nr);
            g.fillSmoothCircle(ArenaTheme.alpha(m.capLight, 0.22f, S1), nx, ny, nr * 0.75f);
        }

        // the walls cast shadows onto the floor (key light from the upper left: the top and left walls throw them)
        final float sd = 0.05f * h;
        g.fillGradientRect(ArenaTheme.alpha(ArenaTheme.BLACK, 0.55f, S1), ArenaTheme.alpha(ArenaTheme.BLACK, 0f, S2), true, fx, fy, fw, sd);
        g.fillGradientRect(ArenaTheme.alpha(ArenaTheme.BLACK, 0.45f, S1), ArenaTheme.alpha(ArenaTheme.BLACK, 0f, S2), false, fx, fy, sd * 0.8f, fh);
        g.fillGradientRect(ArenaTheme.alpha(ArenaTheme.BLACK, 0f, S1), ArenaTheme.alpha(ArenaTheme.BLACK, 0.25f, S2), true, fx, fy + fh - sd * 0.5f, fw, sd * 0.5f);

        // banners: two hanging from the top wall, three sticking out of the side walls (Swing's hand-placed remnants)
        drawHanging(g, m, x + fx - x + fw * 0.20f, y, 0.058f * h * 1.7f, fy - y + 0.012f * h, h, 0);
        drawHanging(g, m, x + fx - x + fw * 0.78f, y, 0.052f * h * 1.7f, fy - y + 0.008f * h, h, 1);
        drawSide(g, m, x, y + fy - y + fh * 0.30f, sideIn + 0.02f * h, 0.11f * h, h, 1, false);
        drawSide(g, m, x, y + fy - y + fh * 0.76f, sideIn + 0.02f * h, 0.11f * h, h, 0, false);
        drawSide(g, m, x + w - sideIn - 0.02f * h, y + fy - y + fh * 0.22f, sideIn + 0.02f * h, 0.11f * h, h, 0, true);

        // the key light: a warm pool at (0.44, 0.39) and a broad fall-off away from the upper left
        ArenaArt.haloEllipse(g, x + w * POOL_X, y + h * POOL_Y, w * 0.44f, h * 0.40f, m.pool, m.poolGain);
        g.fillGradientRect(ArenaTheme.alpha(ArenaTheme.WHITE, 0.035f, S1), ArenaTheme.alpha(ArenaTheme.BLACK, 0.10f, S2), false, x, y, w, h);
        // the bottom right corner sinks into a flat quiet tone behind the action button
        ArenaArt.haloEllipse(g, x + w * 0.97f, y + h * 0.93f, w * 0.20f, h * 0.30f, m.quiet, 0.55f);
        ArenaArt.vignette(g, x, y, w, h, Math.min(w, h) * 0.22f, m.vignette);
    }

    /** A cloth hanging from the top wall: a faded ground, a trim band at its foot, a swallowtail of shadow. */
    private static void drawHanging(final Graphics g, final Mood m, final float cx, final float y, final float bw, final float bh,
            final float h, final int paint) {
        final float x = cx - bw / 2;
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.45f, S1), x + 0.006f * h, y, bw, bh + 0.01f * h, 0.004f * h, 0.012f * h);
        g.fillGradientRect(ArenaTheme.mix(m.banner[paint], ArenaTheme.BLACK, 0.35f, S1), m.banner[paint], false, x, y, bw / 2, bh);
        g.fillGradientRect(m.banner[paint], ArenaTheme.mix(m.banner[paint], ArenaTheme.BLACK, 0.45f, S1), false, x + bw / 2, y, bw / 2, bh);
        g.fillRect(ArenaTheme.alpha(m.trim, 0.85f, S1), x, y + bh - 0.016f * h, bw, 0.006f * h);
        g.fillSmoothCircle(ArenaTheme.alpha(m.trim, 0.6f, S1), cx, y + bh * 0.5f, bw * 0.16f);
        g.drawSmoothCircle(Math.max(1f, 0.003f * h), ArenaTheme.alpha(ArenaTheme.BLACK, 0.35f, S1), cx, y + bh * 0.5f, bw * 0.16f);
    }

    /** A cloth hung from a side wall onto the floor's edge, with a round emblem in the trim colour. */
    private static void drawSide(final Graphics g, final Mood m, final float x, final float cy, final float bw, final float bh,
            final float h, final int paint, final boolean right) {
        final float y = cy - bh / 2;
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.5f, S1), x + (right ? -0.008f : 0.008f) * h, y + 0.006f * h, bw, bh, 0.006f * h, 0.014f * h);
        g.fillSmoothRoundRectGradient(ArenaTheme.mix(m.banner[paint], ArenaTheme.WHITE, 0.08f, S1),
                ArenaTheme.mix(m.banner[paint], ArenaTheme.BLACK, 0.4f, S2), true, x, y, bw, bh, 0.006f * h);
        g.fillRect(ArenaTheme.alpha(m.trim, 0.8f, S1), x, y + 0.008f * h, bw, 0.004f * h);
        g.fillRect(ArenaTheme.alpha(m.trim, 0.8f, S1), x, y + bh - 0.012f * h, bw, 0.004f * h);
        final float ex = right ? x + bw * 0.42f : x + bw * 0.58f;
        final float er = bh * 0.26f;
        g.drawSmoothCircle(Math.max(1.5f, 0.005f * h), ArenaTheme.alpha(m.trim, 0.9f, S1), ex, cy, er);
        g.fillSmoothCircle(ArenaTheme.alpha(m.trim, 0.55f, S1), ex, cy, er * 0.45f);
    }

    /** Lays the flagstones out in running courses across the floor; widths and tones vary, fixed by a seed. */
    private static void layout(final float w, final float h, final int moodIndex) {
        laidW = w;
        laidH = h;
        laidMood = moodIndex;
        final Mood m = MOODS[moodIndex];
        final float aspect = w / h;
        final float sideIn = Math.max(0.07f, Math.min(0.16f, 0.066f * aspect)) * h;
        final float fx = sideIn, fy = TOP * h, fw = w - 2 * sideIn, fh = (BOT - TOP) * h;
        final float gap = Math.max(1f, GAP * h);
        final int rows = Math.max(1, Math.round(fh / (COURSE * h)));
        final float rowH = fh / rows;
        final int max = rows * (int) Math.ceil(fw / (0.08f * h) + 2);
        if (sx.length < max) {
            sx = new float[max];
            sy = new float[max];
            sw = new float[max];
            sh = new float[max];
            top = new Color[max];
            bottom = new Color[max];
            for (int i = 0; i < max; i++) {
                top[i] = new Color();
                bottom[i] = new Color();
            }
        }
        final Random r = new Random(0x7A11);
        int n = 0;
        for (int row = 0; row < rows; row++) {
            final float y0 = fy + row * rowH;
            float x0 = fx - r.nextFloat() * 0.08f * h; // courses start staggered
            while (x0 < fx + fw && n < max) {
                final float stoneW = (0.10f + 0.09f * r.nextFloat()) * h;
                final float a = Math.max(fx, x0) + gap / 2, b = Math.min(fx + fw, x0 + stoneW) - gap / 2;
                x0 += stoneW;
                if (b - a < 0.02f * h) {
                    continue;
                }
                sx[n] = a;
                sy[n] = y0 + gap / 2;
                sw[n] = b - a;
                sh[n] = rowH - gap;
                // tone: a random place on the floor ramp, lighter under the key light, a little lift at the top edge
                final float cxs = (a + b) / 2 / w, cys = (y0 + rowH / 2) / h;
                final float dx = cxs - POOL_X, dy = cys - POOL_Y;
                final float lit = Math.max(0f, 1f - (float) Math.sqrt(dx * dx * 0.8f + dy * dy * 2.2f));
                final float tone = Math.min(1f, 0.18f + 0.30f * r.nextFloat() + 0.32f * lit);
                ArenaTheme.mix(m.floorDark, m.floorLight, tone, bottom[n]);
                ArenaTheme.mix(bottom[n], ArenaTheme.WHITE, 0.06f, top[n]);
                ArenaTheme.mix(bottom[n], ArenaTheme.BLACK, 0.10f, bottom[n]);
                top[n].a = bottom[n].a = 1f;
                // keep clear of the floor's rounded corners: corner stones get shorter
                n++;
            }
        }
        stones = n;
    }
}
