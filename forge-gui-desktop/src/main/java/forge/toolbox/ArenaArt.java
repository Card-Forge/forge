package forge.toolbox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Procedural art for the Arena look: painted match boards and the sculpted metal furniture
 * around the player portraits. Everything is generated in code, nothing is loaded from disk.
 * The board is a small height field renderer: a calm flagstone floor at moderate resolution and a
 * raised masonry surround at close to full resolution. The metal pieces are coverage masks given a
 * rounded height and lit, then cached, so the paint methods are safe to call on every repaint.
 */
public final class ArenaArt {
    private ArenaArt() { }

    // Shared noise

    private static final int[] PERM = new int[512];
    private static final float[] GX = new float[256];
    private static final float[] GY = new float[256];
    private static final int TILE = 512;
    private static final int TILE_MASK = TILE - 1;
    private static float[] detailTile;

    static {
        final Random r = new Random(0x51A7E);
        final int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
            final double a = Math.PI * 2 * r.nextDouble();
            GX[i] = (float) Math.cos(a);
            GY[i] = (float) Math.sin(a);
        }
        for (int i = 255; i > 0; i--) {
            final int j = r.nextInt(i + 1);
            final int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        for (int i = 0; i < 512; i++) {
            PERM[i] = p[i & 255];
        }
    }

    private static int ifloor(final float v) {
        final int i = (int) v;
        return v < i ? i - 1 : i;
    }

    /** Gradient noise, roughly -1 to 1; the lattice wraps at mask + 1 (a power of two). */
    private static float noise(final float x, final float y, final int mask) {
        final int xi = ifloor(x), yi = ifloor(y);
        final float fx = x - xi, fy = y - yi;
        final float ux = fx * fx * fx * (fx * (fx * 6f - 15f) + 10f);
        final float uy = fy * fy * fy * (fy * (fy * 6f - 15f) + 10f);
        final int x0 = xi & mask, x1 = (xi + 1) & mask, y0 = yi & mask, y1 = (yi + 1) & mask;
        final int g00 = PERM[PERM[x0] + y0], g10 = PERM[PERM[x1] + y0];
        final int g01 = PERM[PERM[x0] + y1], g11 = PERM[PERM[x1] + y1];
        final float n00 = GX[g00] * fx + GY[g00] * fy;
        final float n10 = GX[g10] * (fx - 1f) + GY[g10] * fy;
        final float n01 = GX[g01] * fx + GY[g01] * (fy - 1f);
        final float n11 = GX[g11] * (fx - 1f) + GY[g11] * (fy - 1f);
        final float a = n00 + (n10 - n00) * ux;
        final float b = n01 + (n11 - n01) * ux;
        return (a + (b - a) * uy) * 1.55f;
    }

    private static float fbm(float x, float y, final int octaves) {
        float sum = 0f, amp = 0.5f;
        for (int i = 0; i < octaves; i++) {
            sum += noise(x, y, 255) * amp;
            final float nx = (x * 0.8f - y * 0.6f) * 2.03f + 17.1f;
            y = (x * 0.6f + y * 0.8f) * 2.03f + 4.7f;
            x = nx;
            amp *= 0.52f;
        }
        return sum;
    }

    /** Tileable fractal grit, reused at several scales so nothing has to be evaluated per pixel. */
    private static synchronized float[] tile() {
        if (detailTile == null) {
            final float[] t = new float[TILE * TILE];
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    float sum = 0f, amp = 0.55f;
                    int freq = 16;
                    for (int o = 0; o < 5; o++) {
                        sum += noise(x * freq / (float) TILE, y * freq / (float) TILE, freq - 1) * amp;
                        freq *= 2;
                        amp *= 0.62f;
                    }
                    t[(y << 9) | x] = sum;
                }
            }
            detailTile = t;
        }
        return detailTile;
    }

    private static float tileAt(final float[] t, final float x, final float y) {
        final int xi = ifloor(x), yi = ifloor(y);
        final float fx = x - xi, fy = y - yi;
        final int x0 = xi & TILE_MASK, x1 = (xi + 1) & TILE_MASK;
        final int y0 = (yi & TILE_MASK) << 9, y1 = ((yi + 1) & TILE_MASK) << 9;
        final float a = t[y0 | x0] + (t[y0 | x1] - t[y0 | x0]) * fx;
        final float b = t[y1 | x0] + (t[y1 | x1] - t[y1 | x0]) * fx;
        return a + (b - a) * fy;
    }

    private static int hash(final int x, final int y) {
        int h = x * 0x27D4EB2D + y * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x2F72B4C3;
        h ^= h >>> 13;
        h *= 0x5BD1E995;
        h ^= h >>> 16;
        return h;
    }

    private static float h01(final int h, final int shift) {
        return ((h >>> shift) & 0xFF) / 255f;
    }

    /** Fills o with squared F1, squared F2 and the winning site; returns that site's hash. */
    private static int voronoi(final float x, final float y, final float[] o) {
        final int ix = ifloor(x), iy = ifloor(y);
        float f1 = 1e9f, f2 = 1e9f, bx = 0f, by = 0f;
        int best = 0;
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                final int h = hash(ix + i, iy + j);
                final float px = ix + i + 0.03f + 0.94f * (h & 0xFF) / 255f;
                final float py = iy + j + 0.03f + 0.94f * ((h >>> 8) & 0xFF) / 255f;
                final float dx = px - x, dy = py - y;
                final float d = dx * dx + dy * dy;
                if (d < f1) {
                    f2 = f1;
                    f1 = d;
                    bx = px;
                    by = py;
                    best = h;
                } else if (d < f2) {
                    f2 = d;
                }
            }
        }
        o[0] = f1;
        o[1] = f2;
        o[2] = bx;
        o[3] = by;
        return best;
    }

    private static float sat(final float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static float smooth(final float a, final float b, final float v) {
        final float t = sat((v - a) / (b - a));
        return t * t * (3f - 2f * t);
    }

    private static float mix(final float a, final float b, final float t) {
        return a + (b - a) * t;
    }

    private static float smin(final float a, final float b, final float k) {
        final float h = Math.max(k - Math.abs(a - b), 0f) / k;
        return Math.min(a, b) - h * h * k * 0.25f;
    }

    private static float sqrtf(final float v) {
        return (float) Math.sqrt(v);
    }

    private static float atan2f(final float y, final float x) {
        final float ax = Math.abs(x), ay = Math.abs(y);
        if (ax == 0f && ay == 0f) {
            return 0f;
        }
        final float t = Math.min(ax, ay) / Math.max(ax, ay);
        final float s = t * t;
        float r = ((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * t + t;
        if (ay > ax) {
            r = 1.57079637f - r;
        }
        if (x < 0f) {
            r = 3.14159274f - r;
        }
        return y < 0f ? -r : r;
    }

    private static float[] boxBlur(final float[] src, final int w, final int h, final int radius, final int passes) {
        final float[] a = src.clone();
        if (radius <= 0) {
            return a;
        }
        final float[] b = new float[a.length];
        final float norm = 1f / (radius * 2 + 1);
        for (int p = 0; p < passes; p++) {
            for (int y = 0; y < h; y++) {
                final int row = y * w;
                float sum = a[row] * (radius + 1);
                for (int x = 1; x <= radius; x++) {
                    sum += a[row + Math.min(x, w - 1)];
                }
                for (int x = 0; x < w; x++) {
                    b[row + x] = sum * norm;
                    sum += a[row + Math.min(x + radius + 1, w - 1)] - a[row + Math.max(x - radius, 0)];
                }
            }
            for (int x = 0; x < w; x++) {
                float sum = b[x] * (radius + 1);
                for (int y = 1; y <= radius; y++) {
                    sum += b[Math.min(y, h - 1) * w + x];
                }
                for (int y = 0; y < h; y++) {
                    a[y * w + x] = sum * norm;
                    sum += b[Math.min(y + radius + 1, h - 1) * w + x] - b[Math.max(y - radius, 0) * w + x];
                }
            }
        }
        return a;
    }

    private static float bil(final float[] a, final int i, final int lw, final float fx, final float fy) {
        final float t = a[i] + (a[i + 1] - a[i]) * fx;
        final float b = a[i + lw] + (a[i + lw + 1] - a[i + lw]) * fx;
        return t + (b - t) * fy;
    }

    // Colour

    /** Hex to float with a square law gamma, which is all the lighting maths needs. */
    private static float[] lin(final int hex) {
        final float r = ((hex >> 16) & 0xFF) / 255f, g = ((hex >> 8) & 0xFF) / 255f, b = (hex & 0xFF) / 255f;
        return new float[] {r * r, g * g, b * b};
    }

    private static float[][] ramp(final int... hex) {
        final float[][] out = new float[hex.length][];
        for (int i = 0; i < hex.length; i++) {
            out[i] = lin(hex[i]);
        }
        return out;
    }

    private static void sampleRamp(final float[][] ramp, final float t, final float[] out) {
        final float p = sat(t) * (ramp.length - 1);
        final int i = Math.min((int) p, ramp.length - 2);
        final float f = p - i;
        final float[] a = ramp[i], b = ramp[i + 1];
        out[0] = a[0] + (b[0] - a[0]) * f;
        out[1] = a[1] + (b[1] - a[1]) * f;
        out[2] = a[2] + (b[2] - a[2]) * f;
    }

    private static float aces(final float x) {
        return sat(x * (2.51f * x + 0.03f) / (x * (2.43f * x + 0.59f) + 0.14f));
    }

    /** One colour mood of the board. All three share the same layout and differ in light and stone. */
    private static final class Mood {
        float[][] floor, face, cap, rock;
        float[] key, fill, sky, ground, pool, moss;
        // Banner paint: two faded grounds and the trim they share
        float[][] paint;
        float keyGain, fillGain, skyGain, poolGain, wallGain, exposure;
        // How hard the floor splits between warm light in the middle and cool corners
        float warmth;
        // How far the edges fall away towards the rim and how deep the wall's crevices go; 1 is full strength
        float vignette = 1f, crevice = 1f;
        // The key light's strength at the upper left corner of the board and at the lower right one
        float fallNear = 1.2f, fallFar = 0.72f;
        // The flat tone the corner behind the action button sinks into, in display values
        float[] quiet = {16f, 19f, 23f};
        Color[] ivy, leaf;
    }

    private static Mood mood(final int variant) {
        final Mood m = new Mood();
        switch (Math.floorMod(variant, 3)) {
            case 1: // the default: warm weathered stone under one low light from the upper left
                m.floor = ramp(0x4A4845, 0x5F5C57, 0x76726B, 0x938E85);
                m.face = ramp(0x302E2B, 0x4B4843, 0x68645D, 0x948F84);
                m.cap = ramp(0x3B3935, 0x57544D, 0x747067, 0x9E9A8C);
                m.rock = ramp(0x2D2B29, 0x474541, 0x63605A, 0x8F8C82);
                m.key = lin(0xFFF5E8);
                m.fill = lin(0xB4B0A8);
                m.sky = lin(0xA8A6A2);
                m.ground = lin(0x2E2C28);
                m.pool = lin(0xFFE8D2);
                m.moss = lin(0x59684C);
                m.paint = new float[][] {lin(0x6C2A26), lin(0x28405E), lin(0x978658)};
                m.keyGain = 0.96f;
                m.fillGain = 0.10f;
                m.skyGain = 0.46f;
                m.poolGain = 0.24f;
                m.wallGain = 1.0f;
                m.exposure = 1.06f;
                m.warmth = 0f;
                m.vignette = 0.5f;
                m.crevice = 0.5f;
                m.fallNear = 1.42f;
                m.fallFar = 0.52f;
                m.quiet = new float[] {44f, 42f, 40f};
                m.ivy = new Color[] {new Color(0x5E6A57), new Color(0x4F5C4E), new Color(0x6C765D), new Color(0x465652)};
                m.leaf = new Color[] {new Color(0x9C6A3E), new Color(0xA98A4A), new Color(0x7E5636), new Color(0x9A8A52), new Color(0x6E6238)};
                break;
            case 2: // moonlit indigo stone with a banked hearth glow at the centre
                m.floor = ramp(0x2C2E3A, 0x3C3F4E, 0x4E5163, 0x686B7E);
                m.face = ramp(0x131420, 0x222434, 0x383A50, 0x585A74);
                m.cap = ramp(0x1C1D2C, 0x2E3044, 0x484A64, 0x74768E);
                m.rock = ramp(0x0E0F18, 0x1D1E2C, 0x323446, 0x545668);
                m.key = lin(0xC8D4EC);
                m.fill = lin(0x8C8CB8);
                m.sky = lin(0x68748C);
                m.ground = lin(0x12122A);
                m.pool = lin(0xFFB490);
                m.moss = lin(0x2E4258);
                m.paint = new float[][] {lin(0x52243E), lin(0x262F54), lin(0x7A6E4A)};
                m.keyGain = 0.56f;
                m.fillGain = 0.18f;
                m.skyGain = 0.56f;
                m.poolGain = 0.75f;
                m.wallGain = 1.0f;
                m.exposure = 1.28f;
                m.warmth = 0.55f;
                m.ivy = new Color[] {new Color(0x34466A), new Color(0x2C3A5E), new Color(0x46507A), new Color(0x3C3C6C)};
                m.leaf = new Color[] {new Color(0x6A4A5A), new Color(0x7A5A50), new Color(0x4E3E5A), new Color(0x6E6450), new Color(0x54486A)};
                break;
            default: // bleached limestone under a bright, flat day
                m.floor = ramp(0x484A47, 0x5C5E5B, 0x70726E, 0x8C8E88);
                m.face = ramp(0x242522, 0x3C3D38, 0x5A5A52, 0x82827A);
                m.cap = ramp(0x383934, 0x54544D, 0x74746A, 0xA09F92);
                m.rock = ramp(0x1F201E, 0x3C3D38, 0x5E5E56, 0x8C8B80);
                m.key = lin(0xFFF9F2);
                m.fill = lin(0xA8BCCC);
                m.sky = lin(0xA8B4BC);
                m.ground = lin(0x38382E);
                m.pool = lin(0xFFC48E);
                m.moss = lin(0x4C5038);
                m.paint = new float[][] {lin(0x7A3C2A), lin(0x34505F), lin(0x967E44)};
                m.keyGain = 0.74f;
                m.fillGain = 0.14f;
                m.skyGain = 0.58f;
                m.poolGain = 0.62f;
                m.wallGain = 1.0f;
                m.exposure = 1.0f;
                m.warmth = 0.45f;
                m.ivy = new Color[] {new Color(0x667A4A), new Color(0x54704A), new Color(0x7A8452), new Color(0x486650)};
                m.leaf = new Color[] {new Color(0xA88A44), new Color(0xB89C58), new Color(0x8E6C34), new Color(0x9A8440), new Color(0x7A6A38)};
                break;
        }
        return m;
    }

    // Board

    /**
     * Paints a battlefield mat: worn flagstones with a carved central medallion under one key light from the
     * upper left, sunk inside a raised surround of ledges and old banners that throws shadows onto the floor,
     * with moss, leaves and rubble gathered along the foot of the wall. The zone between 12% and 85% of
     * the height stays calm and the bottom right corner stays quiet for the action button. Variant 1 is
     * the default stone; 0 is bleached limestone by day and 2 is moonlit indigo. Takes about a second at
     * 2560x1440, so call it off the event thread and keep the result.
     */
    public static BufferedImage createBoard(final int width, final int height, final int variant) {
        return new BoardRender(Math.max(16, width), Math.max(16, height), mood(variant)).render();
    }

    private static final class BoardRender {
        // Layout, in fractions of the image height
        static final float TOP = 0.078f, BOT = 0.926f, CORNER = 0.04f;
        static final float MED_Y = 0.42f, MED_R = 0.150f;
        static final float POOL_X = 0.44f, POOL_Y = 0.39f;
        static final int LUT = 4096;
        // Key light direction, towards the light
        static final float KX = -0.47f, KY = -0.59f, KZ = 0.656f;
        // The same direction along the ground, as a unit vector
        static final float KDX = -0.6231f, KDY = -0.7822f;

        final int w, h, fw, fh, pw, ph, coarseW, coarseH;
        final float aspect, cxB, cyB, sideIn, hx, hy, lt, lc, ls, perim, lutScale;
        final Mood m;
        final float[] tile = tile();

        // Slow fields shared by floor and walls
        final int lw, lh;
        final float lowX, lowY;
        final float[] wxF, wyF, blF, ptF;

        // Walls: height, albedo and coverage, which shading overwrites with the premultiplied result
        final float[] pH, pR, pG, pB, pCov, pCon, pGl;
        // How much banner cloth covers each wall pixel, and a coarse copy of it that gets blurred into the cloth's cast shadow
        final float[] pCl, banCov;
        final int bw, bh;
        float[] banSoft;
        // Floor: height and albedo, then the shaded result
        final float[] fH, fR, fG, fB, cR, cG, cB;
        // Shadow grid
        int gw, gh;
        float gridX, gridY;
        float[] shadow, aoFar;
        // Distance from each shadow grid cell to the nearest standing wall, in units of the image height
        float[] wallDist;

        final float[] footL, n1, n2, n3, n6, ruinL, pierL;
        // Banners by perimeter position: centre, half width, lower end, top end, ground colour, centre of the emblem
        final float[][] banners;
        final byte[] bannerAt = new byte[LUT];

        // Floor paving, laid out once per board
        float[] paveX, paveY;
        float[][] paveStones;
        int[] paveId, paveLookX, paveLookY;
        int paveCols;

        BoardRender(final int w, final int h, final Mood m) {
            this.w = w;
            this.h = h;
            this.m = m;
            aspect = w / (float) h;
            cxB = aspect * 0.5f;
            cyB = (TOP + BOT) * 0.5f;
            final float pixels = w * (float) h;
            final float fs = Math.min(1f, sqrtf(1700000f / pixels)), ps = Math.min(1f, sqrtf(2600000f / pixels));
            // The slow fields and the shadow grid keep their own lower resolution, whatever the floor runs at
            final float gs = Math.min(1f, sqrtf(921600f / pixels));
            coarseW = Math.max(16, Math.round(w * gs));
            coarseH = Math.max(16, Math.round(h * gs));
            fw = Math.max(16, Math.round(w * fs));
            fh = Math.max(16, Math.round(h * fs));
            pw = Math.max(16, Math.round(w * ps));
            ph = Math.max(16, Math.round(h * ps));
            sideIn = Math.max(0.07f, Math.min(0.16f, 0.066f * aspect));
            hx = Math.max(0.01f, cxB - sideIn - CORNER);
            hy = Math.max(0.01f, (BOT - TOP) * 0.5f - CORNER);
            lt = 2f * hx;
            ls = 2f * hy;
            lc = CORNER * 1.5708f * 1.2f;
            perim = 2f * lt + 2f * ls + 4f * lc;
            lutScale = LUT / perim;

            lw = coarseW / 4 + 2;
            lh = coarseH / 4 + 2;
            lowX = (lw - 2) / aspect;
            lowY = lh - 2;
            wxF = new float[lw * lh];
            wyF = new float[lw * lh];
            blF = new float[lw * lh];
            ptF = new float[lw * lh];

            final int pn = pw * ph, fn = fw * fh;
            pH = new float[pn];
            pR = new float[pn];
            pG = new float[pn];
            pB = new float[pn];
            pCov = new float[pn];
            pCon = new float[pn];
            pGl = new float[pn];
            pCl = new float[pn];
            bw = pw / 3 + 2;
            bh = ph / 3 + 2;
            banCov = new float[bw * bh];
            fH = new float[fn];
            fR = new float[fn];
            fG = new float[fn];
            fB = new float[fn];
            cR = new float[fn];
            cG = new float[fn];
            cB = new float[fn];

            final Random r = new Random(0x7A11);
            footL = stepLut(r, 0.07f, 0.26f, 0.034f, -0.030f, 0.048f, 0f);
            n1 = stepLut(r, 0.20f, 0.60f, 0.010f, 0f, 1f, 0f);
            n2 = stepLut(r, 0.22f, 0.70f, 0.010f, 0f, 1f, 0f);
            n3 = stepLut(r, 0.20f, 0.60f, 0.010f, 0f, 1f, 0f);
            n6 = stepLut(r, 0.26f, 0.80f, 0.018f, 0f, 1f, 0f);
            ruinL = stepLut(r, 0.10f, 0.34f, 0.06f, 0f, 1f, 0.30f);
            pierL = stepLut(r, 0.07f, 0.16f, 0.005f, 0f, 1f, 0.12f);
            // The perimeter coordinate wraps in the top left corner; rubble hides the join
            for (int i = 0; i < 90; i++) {
                ruinL[i] = 1f;
                ruinL[LUT - 1 - i] = 1f;
            }

            // Banner remnants, placed by hand where the layout leaves the wall in view and clear of the action button
            final float topS = 0f, rightS = lt + lc, bottomS = lt + 2f * lc + ls, leftS = 2f * lt + 3f * lc + ls;
            banners = new float[][] {
                {topS + lt * 0.20f, 0.058f, -0.012f, 0.24f, 0f, 0.044f},
                {topS + lt * 0.78f, 0.052f, -0.008f, 0.24f, 1f, 0.046f},
                {rightS + ls * 0.22f, 0.056f, 0.000f, 0.24f, 0f, 0.062f},
                {leftS + ls * 0.30f, 0.054f, 0.004f, 0.24f, 1f, 0.064f},
                {leftS + ls * 0.76f, 0.058f, 0.000f, 0.24f, 0f, 0.062f},
            };
            for (int b = 0; b < banners.length; b++) {
                final int from = (int) ((banners[b][0] - banners[b][1] * 1.3f) * lutScale);
                final int to = (int) ((banners[b][0] + banners[b][1] * 1.3f) * lutScale);
                for (int i = from; i <= to; i++) {
                    bannerAt[i & (LUT - 1)] = (byte) (b + 1);
                }
            }
        }

        /** How far the bottom right corner is held down: 1 behind the action button, fading out softly. */
        private float quietAt(final float u, final float v) {
            final float ex = (aspect - u) / (aspect * 0.27f), ey = (1f - v) / 0.37f;
            return smooth(1f, 0.46f, sqrtf(ex * ex + ey * ey));
        }

        private float wallDistAt(final float u, final float v) {
            final float qx = Math.max(0f, Math.min(gw - 1.001f, u * gridX)), qy = Math.max(0f, Math.min(gh - 1.001f, v * gridY));
            final int x0 = (int) qx, y0 = (int) qy;
            return bil(wallDist, y0 * gw + x0, gw, qx - x0, qy - y0);
        }

        /** A wrapping table of plateaus with short ramps between them; threshold above 0 gives only 0 or 1. */
        private float[] stepLut(final Random r, final float minLen, final float maxLen, final float blend, final float lo, final float hi, final float threshold) {
            final float[] out = new float[LUT];
            final float first = stepValue(r, lo, hi, threshold);
            float prev = first, next = stepValue(r, lo, hi, threshold);
            float end = minLen + r.nextFloat() * (maxLen - minLen);
            final float step = perim / LUT;
            for (int i = 0; i < LUT; i++) {
                final float s = i * step;
                while (s >= end) {
                    prev = next;
                    end += minLen + r.nextFloat() * (maxLen - minLen);
                    next = end >= perim ? first : stepValue(r, lo, hi, threshold);
                }
                out[i] = mix(prev, next, smooth(end - blend, end, s));
            }
            return out;
        }

        private static float stepValue(final Random r, final float lo, final float hi, final float threshold) {
            return threshold > 0f ? (r.nextFloat() < threshold ? 1f : 0f) : mix(lo, hi, r.nextFloat());
        }

        private float lut(final float[] a, final float s) {
            final float p = s * lutScale;
            final int i = ifloor(p);
            final float f = p - i;
            final float v0 = a[i & (LUT - 1)];
            return v0 + (a[(i + 1) & (LUT - 1)] - v0) * f;
        }

        /** Signed distance to the smooth inner rectangle, positive inside the play area. */
        private float boxSd(final float u, final float v) {
            final float qx = Math.abs(u - cxB) - hx, qy = Math.abs(v - cyB) - hy;
            if (qx > 0f && qy > 0f) {
                return CORNER - sqrtf(qx * qx + qy * qy);
            }
            return CORNER - Math.max(qx, qy);
        }

        /** Sets o[0] to boxSd and o[1] to the distance along the perimeter, clockwise from the top left. */
        private void wallCoord(final float u, final float v, final float[] o) {
            final float px = u - cxB, py = v - cyB;
            final float qx = Math.abs(px) - hx, qy = Math.abs(py) - hy;
            if (qx > 0f && qy > 0f) {
                o[0] = CORNER - sqrtf(qx * qx + qy * qy);
                if (py < 0f) {
                    o[1] = px > 0f ? lt + atan2f(qx, qy) / 1.5708f * lc
                            : 2f * lt + 3f * lc + 2f * ls + atan2f(qy, qx) / 1.5708f * lc;
                } else {
                    o[1] = px > 0f ? lt + lc + ls + atan2f(qy, qx) / 1.5708f * lc
                            : 2f * lt + 2f * lc + ls + atan2f(qx, qy) / 1.5708f * lc;
                }
            } else if (qy > qx) {
                o[0] = CORNER - qy;
                o[1] = py < 0f ? px + hx : lt + 2f * lc + ls + hx - px;
            } else {
                o[0] = CORNER - qx;
                o[1] = px > 0f ? lt + lc + py + hy : 2f * lt + 3f * lc + ls + hy - py;
            }
        }

        /** The point on the smooth inner rectangle at perimeter distance s: x, y and the inward normal. */
        private void perimPoint(final float sIn, final float[] o) {
            float s = sIn % perim;
            if (s < 0f) {
                s += perim;
            }
            final float rr = CORNER;
            float a;
            if (s < lt) {
                set(o, cxB - hx + s, cyB - hy - rr, 0f, 1f);
            } else if ((s -= lt) < lc) {
                a = s / lc * 1.5708f;
                set(o, cxB + hx + rr * sinf(a), cyB - hy - rr * cosf(a), -sinf(a), cosf(a));
            } else if ((s -= lc) < ls) {
                set(o, cxB + hx + rr, cyB - hy + s, -1f, 0f);
            } else if ((s -= ls) < lc) {
                a = s / lc * 1.5708f;
                set(o, cxB + hx + rr * cosf(a), cyB + hy + rr * sinf(a), -cosf(a), -sinf(a));
            } else if ((s -= lc) < lt) {
                set(o, cxB + hx - s, cyB + hy + rr, 0f, -1f);
            } else if ((s -= lt) < lc) {
                a = s / lc * 1.5708f;
                set(o, cxB - hx - rr * sinf(a), cyB + hy + rr * cosf(a), sinf(a), -cosf(a));
            } else if ((s -= lc) < ls) {
                set(o, cxB - hx - rr, cyB + hy - s, 1f, 0f);
            } else {
                s -= ls;
                a = s / lc * 1.5708f;
                set(o, cxB - hx - rr * cosf(a), cyB - hy - rr * sinf(a), cosf(a), sinf(a));
            }
        }

        private static void set(final float[] o, final float x, final float y, final float nx, final float ny) {
            o[0] = x;
            o[1] = y;
            o[2] = nx;
            o[3] = ny;
        }

        private static float sinf(final float a) {
            return (float) Math.sin(a);
        }

        private static float cosf(final float a) {
            return (float) Math.cos(a);
        }

        BufferedImage render() {
            buildSlowFields();
            renderWalls();
            buildWallDistance();
            layFallenBlocks();
            buildShadows();
            // Floor and wall shading only read what is built above and write separate arrays, so they can run side by side
            final CompletableFuture<Void> floor = CompletableFuture.runAsync(() -> {
                renderFloor();
                shadeFloor();
            });
            shadeWalls();
            floor.join();
            return finish();
        }

        private void buildSlowFields() {
            for (int y = 0; y < lh; y++) {
                final float v = y / lowY;
                for (int x = 0; x < lw; x++) {
                    final float u = x / lowX;
                    final int i = y * lw + x;
                    final float a = fbm(u * 2.6f + 3.1f, v * 2.6f + 1.7f, 4);
                    final float b = fbm(u * 2.6f + 8.3f, v * 2.6f + 5.2f, 4);
                    wxF[i] = a;
                    wyF[i] = b;
                    blF[i] = fbm(u * 2.4f + a * 1.15f + 1.3f, v * 2.4f + b * 1.15f + 9.1f, 5);
                    ptF[i] = fbm(u * 5.1f - b * 1.4f + 11f, v * 5.1f + a * 1.4f + 4f, 4);
                }
            }
        }

        // Masonry helpers: one course of stones along the perimeter, with random lengths and some merged joints

        private static float jointAt(final int row, final int k) {
            return (h01(hash(row * 17 + 3, k), 3) - 0.5f) * 0.62f;
        }

        private static boolean jointGone(final int row, final int k) {
            return h01(hash(row * 17 + 3, k), 11) < 0.26f;
        }

        /** o[0] becomes the distance to the nearest joint along the course; returns the stone's hash. */
        private static int courseStone(final float s, final int row, final float avg, final float[] o) {
            final float q = s / avg + row * 3.37f;
            final int c = ifloor(q);
            int l = c;
            float lb = l + jointAt(row, l);
            if (q < lb) {
                l--;
                lb = l + jointAt(row, l);
            }
            for (int g = 0; g < 3 && jointGone(row, l); g++) {
                l--;
                lb = l + jointAt(row, l);
            }
            int r = c + 1;
            float rb = r + jointAt(row, r);
            if (q >= rb) {
                r++;
                rb = r + jointAt(row, r);
            }
            for (int g = 0; g < 3 && jointGone(row, r); g++) {
                r++;
                rb = r + jointAt(row, r);
            }
            o[0] = Math.min(q - lb, rb - q) * avg;
            return hash(row * 131 + 7, l);
        }

        private void renderWalls() {
            IntStream.range(0, ph).parallel().forEach(this::wallRow);
            // A coarse copy of the cloth, which buildShadows blurs into the shadow it casts
            for (int y = 0; y < ph; y++) {
                for (int x = 0; x < pw; x++) {
                    final float cl = pCl[y * pw + x];
                    if (cl > 0f) {
                        final int bc = (y / 3) * bw + x / 3;
                        banCov[bc] = Math.max(banCov[bc], cl);
                    }
                }
            }
        }

        private void wallRow(final int y) {
            final float ix = aspect / pw, iy = 1f / ph;
            final float fpx = iy;
            final float edge = 1.4f * fpx;
            final float chamfer = Math.max(0.0018f, 1.7f * fpx);
            final float margin = 0.125f;
            final float[] c = new float[3], oS = new float[1], oP = new float[1], oC = new float[1];
            final float[] o1 = new float[4], o2 = new float[4], o3 = new float[4], wc = new float[2];
            final float F1 = 8.6f, F2 = 24f, F3 = 62f;

            final float v = (y + 0.5f) * iy;
            final boolean full = v < TOP + margin + 0.04f || v > BOT - margin - 0.04f;
            final int xA = full ? pw : Math.min(pw, (int) ((sideIn + margin + 0.02f) / ix));
            final int xB = full ? pw : Math.max(xA, (int) ((aspect - sideIn - margin - 0.02f) / ix));
            final float ly = v * lowY;
            final int ly0 = (int) ly;
            final float lfy = ly - ly0;
            for (int x = 0; x < pw; x++) {
                if (x == xA && xB > xA) {
                    x = xB;
                }
                final float u = (x + 0.5f) * ix;
                wallCoord(u, v, wc);
                final float ds = wc[0], s = wc[1];
                if (ds > margin) {
                    continue;
                }
                final float cdx = Math.min(u, aspect - u), cdy = Math.min(v, 1f - v);
                final float cd = sqrtf(cdx * cdx + cdy * cdy);
                float mass = sat(1f - cd / 0.31f);
                mass *= sqrtf(mass);
                if (mass <= 0f && lut(footL, s) - ds < -0.084f) {
                    continue;
                }
                final int i = y * pw + x;
                final float lx = u * lowX;
                final int lx0 = (int) lx;
                final float lfx = lx - lx0;
                final int li = ly0 * lw + lx0;
                final float wx = bil(wxF, li, lw, lfx, lfy), wy = bil(wyF, li, lw, lfx, lfy);
                final float bl = bil(blF, li, lw, lfx, lfy), pt = bil(ptF, li, lw, lfx, lfy);
                final float d1 = tile[((y & TILE_MASK) << 9) | (x & TILE_MASK)];
                final float d2 = tileAt(tile, u * 200f + 37f, v * 200f + 71f);
                final float d3 = tileAt(tile, u * 720f + 13f, v * 720f + 29f);

                // Distance into the wall. The foot line steps with each footing stone, so it never reads as one outline
                final int footStone = courseStone(s, 1, 0.052f, oP);
                final float d = -ds + lut(footL, s) + (h01(footStone, 2) - 0.5f) * 0.0130f + wx * 0.0150f + d2 * 0.0042f + d3 * 0.0016f
                        + mass * (0.085f + wy * 0.09f);
                if (d < -0.060f) {
                    continue;
                }

                float H = 0f, cov = 0f, cr = 0f, cg = 0f, cb = 0f, gl = 0f;
                final float ruinV = lut(ruinL, s);
                final float quiet = quietAt(u, v);

                // The masonry, kept to three broad bands: footing, a sloped face, a capping ledge, then tall blocks behind
                if (d > -2f * fpx) {
                    final int capStone1 = courseStone(s, 20, 0.125f, oC);
                    final float jog = (h01(capStone1, 2) - 0.5f) * 0.004f;
                    final float b1 = 0.006f + 0.006f * lut(n1, s);
                    final float b2 = b1 + 0.030f + 0.012f * lut(n2, s) + jog;
                    final float b3 = b2 + 0.016f + 0.010f * lut(n3, s) - jog * 0.6f;
                    final float raise = lut(n6, s);
                    final float h0 = 0.007f, h1 = 0.032f + 0.018f * raise + (h01(capStone1, 9) - 0.5f) * 0.006f, h2 = h1 + 0.034f;

                    float base, across, tone, prot, avgLen;
                    int row;
                    boolean capped = false;
                    if (d < b1) {
                        row = 1;
                        base = h0;
                        across = Math.min(d, b1 - d);
                        tone = 0.30f;
                        prot = 0.0024f;
                        avgLen = 0.052f;
                    } else if (d < b2) {
                        final float t = (d - b1) / (b2 - b1);
                        row = 10;
                        base = mix(h0, h1, t);
                        across = Math.min(d - b1, b2 - d);
                        tone = 0.38f;
                        prot = 0.0036f;
                        avgLen = 0.088f;
                    } else if (d < b3) {
                        row = 20;
                        base = h1;
                        across = Math.min(d - b2, b3 - d);
                        tone = 0.42f;
                        prot = 0.0018f;
                        avgLen = 0.125f;
                        capped = true;
                    } else {
                        final float t = (d - b3) / 0.062f;
                        final int ri = ifloor(t);
                        row = 30 + ri;
                        base = mix(h1, h2, smooth(0f, 0.6f, t));
                        across = Math.min(t - ri, ri + 1 - t) * 0.062f;
                        tone = 0.32f;
                        prot = 0.0030f;
                        avgLen = 0.135f;
                    }
                    final int sh = row == 1 ? footStone : row == 20 ? capStone1 : courseStone(s, row, avgLen, oS);
                    final float jointD = row == 1 ? oP[0] : row == 20 ? oC[0] : oS[0];
                    final float rough = d3 * 0.0021f + d2 * 0.0017f + d1 * 0.0005f;
                    final float bd = Math.min(jointD, across) + rough;
                    // No two joints alike: each stone's are tight or open, and they pinch and widen along their length
                    final float open = h01(sh, 5);
                    final float bev = smooth(0f, chamfer * (0.50f + 2.2f * open * open) * (1f + sat(d2 * 1.8f + 0.25f) * 1.5f), bd);
                    float hw = base - (1f - bev) * 0.0040f + h01(sh, 8) * prot * bev + d3 * 0.0012f + d1 * 0.00030f;
                    float tn = tone + (h01(sh, 0) - 0.5f) * 0.18f + d2 * 0.12f + d3 * 0.06f + d1 * 0.022f;
                    // The odd stone has fallen out of a face altogether
                    if (!capped && row != 1 && h01(sh, 20) < 0.06f) {
                        hw -= 0.0055f * bev;
                        tn -= 0.10f;
                    }
                    // Piers standing proud of the wall, throwing the longest shadows onto the floor
                    final float pier = lut(pierL, s);
                    final float dp = d + d3 * 0.0040f + d2 * 0.0030f;
                    if (pier > 0f && dp > b1 - 0.002f && dp < b3 + 0.010f) {
                        final float pk = sat((pier - 0.5f + d3 * 0.30f) * 3f + 0.5f) * smooth(b1 - 0.002f, b1 + 0.003f, dp) * smooth(b3 + 0.010f, b3 + 0.005f, dp);
                        final int ph3 = hash(ifloor(s * 9f), 41);
                        final float top = h1 + 0.040f + h01(ph3, 3) * 0.022f + d2 * 0.0100f + d3 * 0.0050f + d1 * 0.0008f;
                        hw = mix(hw, top, pk);
                        tn = mix(tn, 0.30f + h01(ph3, 7) * 0.14f + d2 * 0.26f + d3 * 0.16f + d1 * 0.05f, pk);
                    }
                    // Collapsed stretches, with an eroded jagged edge
                    final float er = ruinV * 1.25f + bl * 0.25f + d2 * 0.25f - 0.66f;
                    if (er > 0f) {
                        final float k = smooth(0f, 0.16f, er);
                        hw *= 1f - k * 0.80f;
                        tn -= k * 0.10f;
                    }
                    H = Math.max(hw, 0f);
                    cov = sat(d / fpx + 0.5f);
                    sampleRamp(capped ? m.cap : m.face, tn, c);
                    cr = c[0];
                    cg = c[1];
                    cb = c[2];
                    // A few stones came from a warmer or a colder bed
                    final float bedr = h01(sh, 14);
                    if (bedr < 0.20f) {
                        cr *= 1.05f;
                        cb *= 0.96f;
                    } else if (bedr > 0.84f) {
                        cr *= 0.94f;
                        cb *= 1.05f;
                    }
                    float k = mix(0.62f + 0.18f * open, 1f, bev);
                    if (!capped) {
                        k *= 1f + tileAt(tile, s * 850f, d * 55f) * 0.12f;
                    }
                    final float mm = smooth(0.06f, 0.50f, pt + d2 * 0.34f + d3 * 0.12f + (1f - bev) * 0.10f) * (capped ? 0.42f : 0.30f) * (1f - quiet);
                    cr = mix(cr * k, m.moss[0] * 0.66f, mm);
                    cg = mix(cg * k, m.moss[1] * 0.66f, mm);
                    cb = mix(cb * k, m.moss[2] * 0.66f, mm);
                    gl = capped ? 0.13f : 0.10f;

                    // What is left of the banners: heavy cloth draped over the wall in folds, torn at the sides, rotted at the foot
                    final int bi = bannerAt[(int) (s * lutScale) & (LUT - 1)];
                    if (bi > 0) {
                        final float[] bn = banners[bi - 1];
                        final float off = s - bn[0], ab = Math.abs(off);
                        final float taper = 0.66f + 0.34f * sat((d - bn[2]) / 0.06f);
                        final float tornSide = bn[1] * taper - ab + tileAt(tile, d * 120f + bn[0] * 90f, bn[0] * 30f) * 0.018f;
                        final float rotted = d - bn[2] - sat(tileAt(tile, s * 110f, bn[0] * 50f + 1f) + 0.45f) * 0.034f;
                        final float soft = fpx * 1.4f;
                        final float cl = sat(tornSide / soft + 0.5f) * sat(rotted / soft + 0.5f) * sat((bn[3] - d) / fpx + 0.5f)
                                * smooth(0.42f, 0.32f, d2 * 0.8f + d3 * 0.7f);
                        if (cl > 0f) {
                            final float[] ground = m.paint[(int) bn[4]], trim = m.paint[2];
                            final float across2 = ab / bn[1];
                            final float dl = d - bn[5];
                            final float loz = across2 / 0.52f + Math.abs(dl) / 0.034f;
                            float tr = Math.max(smooth(0.78f, 0.81f, across2) * smooth(0.92f, 0.89f, across2),
                                    smooth(1f, 0.95f, loz) * smooth(0.62f, 0.67f, loz));
                            tr = Math.max(tr, smooth(0.30f, 0.25f, loz)) * sat(0.72f + d2 * 0.70f + d3 * 0.30f);
                            final float wob = tileAt(tile, d * 36f + bn[0] * 70f, bn[0] * 9f);
                            // Two or three broad folds with a smaller ripple on them, painted into the cloth as well as lit
                            final float fq = off / bn[1] + wob * 0.16f;
                            final float fold = sinf(fq * 6.1f + bn[0] * 40f) * 0.72f + sinf(fq * 15.3f + bn[0] * 90f) * 0.28f;
                            final float fade = (0.90f + d2 * 0.26f + d3 * 0.10f) * mix(0.56f, 1f, smooth(bn[2], bn[2] + 0.06f, d)) * (1f + fold * 0.30f);
                            cr = mix(cr, mix(ground[0], trim[0], tr) * fade, cl * 0.96f);
                            cg = mix(cg, mix(ground[1], trim[1], tr) * fade, cl * 0.96f);
                            cb = mix(cb, mix(ground[2], trim[2], tr) * fade, cl * 0.96f);
                            hw = mix(hw, base + 0.0050f + fold * 0.0030f + d3 * 0.00035f, cl);
                            gl = mix(gl, 0.02f, cl);
                            pCl[i] = cl;
                        }
                        H = Math.max(hw, 0f);
                    }
                }

                // Boulders tumbled over the wall, heaped in the corners and along collapsed stretches
                final float heap = mass * 2.1f + ruinV * sat(0.55f + d / 0.05f) * 0.62f;
                if (heap > 0.02f && d > -0.035f) {
                    final float pu = u + wx * 0.030f + d2 * 0.0060f, pv = v + wy * 0.030f + d2 * 0.0045f;
                    final int h1r = voronoi(pu * F1, pv * F1, o1);
                    final float edge1 = sqrtf(o1[1]) - sqrtf(o1[0]);
                    // A hewn chamfer of the block's own width over a rounded back, on its own tilt, so each block has its own light
                    final float shoulder = sat(edge1 / (0.10f + 0.22f * h01(h1r, 28)));
                    final float dome = 0.42f + shoulder * (2f - shoulder) * 0.58f;
                    final float plane1 = (pu - o1[2] / F1) * (h01(h1r, 24) - 0.5f) * 0.34f + (pv - o1[3] / F1) * (h01(h1r, 4) - 0.5f) * 0.34f;
                    final int h2r = voronoi(pu * F2 + 7.3f, pv * F2 + 2.1f, o2);
                    final float edge2 = sqrtf(o2[1]) - sqrtf(o2[0]);
                    final float plane2 = (pu + 7.3f / F2 - o2[2] / F2) * (h01(h2r, 24) - 0.5f) * 0.30f + (pv + 2.1f / F2 - o2[3] / F2) * (h01(h2r, 4) - 0.5f) * 0.30f;
                    // The pile flattens as it grows: a steep mound in the corner by the light would turn every stone on it away from the key
                    float rh = 0.150f * heap / (1f + heap * 1.2f) - 0.014f + dome * (0.016f + 0.026f * h01(h1r, 16)) + plane1 + plane2 * 0.26f
                            + smooth(0f, 0.08f, edge2) * 0.0024f + d2 * 0.0058f + d3 * 0.0040f + d1 * 0.0009f;
                    // Along a seam on bare floor a tilted block would taper to a thin sliver, so the seam is sunk to trim it
                    if (H <= 0f && edge1 < 0.07f) {
                        rh -= (1f - edge1 / 0.07f) * 0.020f;
                    }
                    // Boulder by boulder, judged at its own centre: some lie out on the floor, some stop short
                    wallCoord(o1[2] / F1, o1[3] / F1, wc);
                    final float dv = -wc[0] + lut(footL, wc[1]) + mass * 0.085f + (h01(h1r, 16) - 0.5f) * 0.05f;
                    if (dv < 0f) {
                        rh -= sat(-dv / 0.012f) * 0.14f;
                    }
                    final float rc = sat((rh - H) / edge + 0.5f);
                    if (rc > 0f) {
                        sampleRamp(m.rock, 0.20f + 0.42f * h01(h1r, 12) + 0.12f * h01(h2r, 20) + d2 * 0.20f + d3 * 0.14f + d1 * 0.04f, c);
                        final float mm = smooth(0.08f, 0.52f, pt + d2 * 0.3f + d3 * 0.12f) * 0.34f * (1f - quiet);
                        H = mix(H, rh, rc);
                        cr = mix(cov > 0f ? cr : c[0], mix(c[0], m.moss[0] * 0.62f, mm), rc);
                        cg = mix(cov > 0f ? cg : c[1], mix(c[1], m.moss[1] * 0.62f, mm), rc);
                        cb = mix(cov > 0f ? cb : c[2], mix(c[2], m.moss[2] * 0.62f, mm), rc);
                        cov = Math.max(cov, rc);
                        gl = 0.08f;
                    }
                }

                // Loose stones on the floor at the foot of the wall
                if (cov < 1f && d > -0.058f && quiet < 0.5f) {
                    final float density = smooth(-0.058f, -0.002f, d) * (0.34f + pt * 0.8f) * 0.42f;
                    final int ph2 = voronoi(u * F3 + wx * 0.8f, v * F3 + wy * 0.8f, o3);
                    if (h01(ph2, 16) < density) {
                        final float rad = (0.12f + 0.34f * h01(ph2, 24)) / F3;
                        final float pd = sqrtf(o3[0]) / F3 + d3 * 0.0009f;
                        final float pc = sat((rad - pd) / fpx + 0.5f);
                        if (pc > 0f) {
                            final float q = sat(pd / rad);
                            final float bump = sqrtf(1f - q * q) * rad * 0.62f + 0.0006f + H;
                            sampleRamp(m.rock, 0.22f + 0.26f * h01(ph2, 4) + d3 * 0.12f, c);
                            H = mix(H, bump, pc);
                            cr = mix(cov > 0f ? cr : c[0], c[0], pc);
                            cg = mix(cov > 0f ? cg : c[1], c[1], pc);
                            cb = mix(cov > 0f ? cb : c[2], c[2], pc);
                            cov = Math.max(cov, pc);
                        }
                    }
                }

                pH[i] = H;
                pR[i] = cr;
                pG[i] = cg;
                pB[i] = cb;
                pCov[i] = cov;
                pGl[i] = gl;
                if (d < 0.004f) {
                    final float con = sat(1f + (d - 0.004f) / 0.0165f);
                    pCon[i] = con * sqrtf(con) * 0.60f;
                }
            }
        }

        /** Measures every floor cell's distance to the standing wall, which drives the contact shadow, moss and litter. */
        private void buildWallDistance() {
            gw = coarseW / 4 + 2;
            gh = coarseH / 4 + 2;
            gridX = (gw - 2) / aspect;
            gridY = gh - 2;
            final float[] t = new float[gw * gh];
            Arrays.fill(t, 1e6f);
            final float ix = aspect / pw, iy = 1f / ph;
            for (int y = 0; y < ph; y++) {
                final int gy = (int) ((y + 0.5f) * iy * gridY);
                for (int x = 0; x < pw; x++) {
                    final int i = y * pw + x;
                    // Pebbles are lower than this and do not count as wall
                    if (pH[i] > 0.0056f && pCov[i] > 0.5f) {
                        t[gy * gw + (int) ((x + 0.5f) * ix * gridX)] = 0f;
                    }
                }
            }
            final float cell = 1f / gridY, diag = cell * 1.4142f;
            for (int y = 0; y < gh; y++) {
                for (int x = 0; x < gw; x++) {
                    final int i = y * gw + x;
                    float v = t[i];
                    if (x > 0) {
                        v = Math.min(v, t[i - 1] + cell);
                    }
                    if (y > 0) {
                        v = Math.min(v, t[i - gw] + cell);
                        if (x > 0) {
                            v = Math.min(v, t[i - gw - 1] + diag);
                        }
                        if (x < gw - 1) {
                            v = Math.min(v, t[i - gw + 1] + diag);
                        }
                    }
                    t[i] = v;
                }
            }
            for (int y = gh - 1; y >= 0; y--) {
                for (int x = gw - 1; x >= 0; x--) {
                    final int i = y * gw + x;
                    float v = t[i];
                    if (x < gw - 1) {
                        v = Math.min(v, t[i + 1] + cell);
                    }
                    if (y < gh - 1) {
                        v = Math.min(v, t[i + gw] + cell);
                        if (x < gw - 1) {
                            v = Math.min(v, t[i + gw + 1] + diag);
                        }
                        if (x > 0) {
                            v = Math.min(v, t[i + gw - 1] + diag);
                        }
                    }
                    t[i] = v;
                }
            }
            wallDist = boxBlur(t, gw, gh, 1, 2);
        }

        /** Dressed blocks that have fallen from the wall and lie askew near its foot, shaded and shadowed like the wall itself. */
        private void layFallenBlocks() {
            final Random r = new Random(0x4B1D);
            final float[] o = new float[4], c = new float[3];
            final int wanted = 5;
            int placed = 0;
            for (int attempt = 0; attempt < 90 && placed < wanted; attempt++) {
                final float s = r.nextFloat() * perim;
                perimPoint(s, o);
                final float reach = lut(footL, s) + 0.012f + r.nextFloat() * 0.046f;
                final float bx = o[0] + o[2] * reach, by = o[1] + o[3] * reach;
                if (bx < 0f || by < 0f || bx > aspect || by > 1f || quietAt(bx, by) > 0.02f) {
                    continue;
                }
                final float wd = wallDistAt(bx, by);
                if (wd < 0.006f || wd > 0.050f) {
                    continue;
                }
                placed++;
                // One big block, sometimes a couple of broken chips beside it
                final int chips = 1 + r.nextInt(2);
                for (int n = 0; n < chips; n++) {
                    final float size = n == 0 ? 1f : 0.34f + r.nextFloat() * 0.26f;
                    final float ox = n == 0 ? 0f : (r.nextFloat() - 0.5f) * 0.046f, oy = n == 0 ? 0f : (r.nextFloat() - 0.5f) * 0.046f;
                    fallenBlock(bx + ox, by + oy, (0.0100f + r.nextFloat() * 0.0120f) * size, (0.0075f + r.nextFloat() * 0.0060f) * size,
                            r.nextFloat() * 3.1416f, (0.0085f + r.nextFloat() * 0.0090f) * sqrtf(size), r.nextInt(), c);
                }
            }
        }

        private void fallenBlock(final float bx, final float by, final float hwB, final float hhB, final float angle, final float top, final int seed, final float[] c) {
            final float ix = aspect / pw, iy = 1f / ph;
            final float ca = cosf(angle), sa = sinf(angle);
            final float reach = (hwB + hhB) * 1.2f + 0.008f;
            final int x0 = Math.max(0, (int) ((bx - reach) / ix)), x1 = Math.min(pw - 1, (int) ((bx + reach) / ix));
            final int y0 = Math.max(0, (int) ((by - reach) / iy)), y1 = Math.min(ph - 1, (int) ((by + reach) / iy));
            final float chamfer = Math.max(0.0042f, 2.4f * iy);
            final float tone = 0.28f + h01(seed, 3) * 0.22f;
            final float tiltX = (h01(seed, 9) - 0.5f) * 0.18f, tiltY = (h01(seed, 15) - 0.5f) * 0.18f;
            for (int y = y0; y <= y1; y++) {
                final float v = (y + 0.5f) * iy;
                for (int x = x0; x <= x1; x++) {
                    final float u = (x + 0.5f) * ix;
                    final float dx = u - bx, dy = v - by;
                    final float lx = dx * ca + dy * sa, ly = dy * ca - dx * sa;
                    final float d2 = tileAt(tile, u * 200f + 37f, v * 200f + 71f), d3 = tileAt(tile, u * 720f + 13f, v * 720f + 29f);
                    final float in = Math.min(hwB - Math.abs(lx), hhB - Math.abs(ly)) + d2 * 0.0045f + d3 * 0.0018f;
                    final int i = y * pw + x;
                    if (in < -0.008f) {
                        continue;
                    }
                    if (in <= 0f) {
                        final float q = 1f + in / 0.008f;
                        pCon[i] = Math.max(pCon[i], q * q * 0.34f);
                        continue;
                    }
                    final float cover = sat(in / iy + 0.5f);
                    final float hb = top * (0.30f + 0.70f * smooth(0f, chamfer, in)) + lx * tiltX + ly * tiltY + d2 * 0.0014f + d3 * 0.0007f;
                    if (hb <= pH[i]) {
                        continue;
                    }
                    sampleRamp(m.rock, tone + d2 * 0.18f + d3 * 0.10f, c);
                    pH[i] = hb;
                    pR[i] = mix(pR[i], c[0], cover);
                    pG[i] = mix(pG[i], c[1], cover);
                    pB[i] = mix(pB[i], c[2], cover);
                    pCov[i] = Math.max(pCov[i], cover);
                    pGl[i] = 0.09f;
                }
            }
        }

        /** Soft shadows cast by the walls alone, with nothing from off the board that could read as a shaft. */
        private void buildShadows() {
            final float[] grid = new float[gw * gh];
            final float ix = aspect / pw, iy = 1f / ph;
            for (int y = 0; y < ph; y++) {
                final int gy = (int) ((y + 0.5f) * iy * gridY);
                for (int x = 0; x < pw; x++) {
                    final float H = pH[y * pw + x];
                    if (H > 0f) {
                        final int gi = gy * gw + (int) ((x + 0.5f) * ix * gridX);
                        grid[gi] = Math.max(grid[gi], H);
                    }
                }
            }
            aoFar = boxBlur(grid, gw, gh, 8, 2);

            final float lxy = sqrtf(KX * KX + KY * KY);
            final float dirX = KX / lxy, dirY = KY / lxy, slope = KZ / lxy * 0.85f;
            final float step = 0.0085f;
            final float[] sh = new float[gw * gh];
            final float off = 0.075f * m.vignette;
            for (int y = 0; y < gh; y++) {
                final float v = y / gridY;
                for (int x = 0; x < gw; x++) {
                    final float u = x / gridX;
                    final int i = y * gw + x;
                    final float base = Math.min(grid[i], 0.12f) + 0.0012f;
                    float occ = 0f;
                    for (int k = 1; k <= 22; k++) {
                        final float dist = k * step;
                        final float sxf = (u + dirX * dist) * gridX, syf = (v + dirY * dist) * gridY;
                        final float top = sxf < 0f || syf < 0f || sxf >= gw - 1 || syf >= gh - 1 ? off
                                : Math.min(grid[(int) syf * gw + (int) sxf], 0.12f);
                        final float over = top - (base + dist * slope);
                        if (over > 0f) {
                            occ = Math.max(occ, sat(over / 0.012f));
                        }
                    }
                    sh[i] = occ;
                }
            }
            shadow = boxBlur(sh, gw, gh, 3, 2);
            banSoft = boxBlur(banCov, bw, bh, Math.max(1, Math.round(0.0034f * ph / 3f)), 2);
        }

        /**
         * The medallion, in disc radii: a double ring with a notched band, an inner ring and a chiselled
         * compass star. out[0] becomes how deep the chisel lines run here and out[1] the height of the
         * star's facets, which the light then picks out.
         */
        private static void motif(final float dx, final float dy, final float r, final float[] out) {
            final float rn = r / MED_R;
            final float hw = 0.024f;
            float g = smooth(hw * 1.15f, hw * 0.5f, Math.abs(rn - 0.955f));
            g = Math.max(g, smooth(hw * 0.8f, hw * 0.3f, Math.abs(rn - 0.865f)));
            final float a = atan2f(dy, dx) + 3.14159274f;
            // Short notches around the band
            if (rn > 0.865f && rn < 0.955f) {
                final float nSect = 6.2831855f / 32f;
                final float nf = Math.abs((a + nSect * 0.25f) % nSect - nSect * 0.5f) * rn;
                g = Math.max(g, smooth(hw * 0.62f, hw * 0.24f, nf));
            }
            float rise = 0f;
            if (rn < 0.865f) {
                final float sect = 1.5707964f;
                final float signed = (a + sect * 0.5f) % sect - sect * 0.5f;
                final float fa = Math.abs(signed);
                final float px = rn * cosf(fa), py = rn * sinf(fa);
                // Long point on the axis, then the valley, then the short point on the diagonal
                final float lx = 0.845f, vx = 0.2772f, vy = 0.1148f, sx = 0.4100f, sy = 0.4100f;
                final boolean longSide = fa < 0.3926991f;
                final float toEdge = longSide ? segDist(px, py, lx, 0f, vx, vy) : segDist(px, py, vx, vy, sx, sy);
                final float cross = longSide ? (vx - lx) * py - vy * (px - lx) : (sx - vx) * (py - vy) - (sy - vy) * (px - vx);
                final float inStar = smooth(-0.004f, 0.004f, cross);
                g = Math.max(g, smooth(hw, hw * 0.45f, toEdge));
                // The height climbs from the outline to a ridge down each point, so every point has a lit and a dark side
                rise = inStar * toEdge * 0.27f;
                // The ring runs behind the star, and a small round boss closes its centre
                g = Math.max(g, smooth(hw * 0.8f, hw * 0.35f, Math.abs(rn - 0.655f)) * (1f - inStar));
                if (rn < 0.21f) {
                    final float bossIn = smooth(0.195f, 0.180f, rn);
                    rise = mix(rise, 0.040f - rn * 0.07f, bossIn);
                    g = Math.max(g, smooth(hw * 0.9f, hw * 0.4f, Math.abs(rn - 0.19f))) * (1f - smooth(0.17f, 0.15f, rn));
                    g = Math.max(g, smooth(0.052f, 0.040f, rn));
                }
            }
            out[0] = sat(g);
            out[1] = rise;
        }

        private static float segDist(final float px, final float py, final float ax, final float ay, final float bx, final float by) {
            final float vx = bx - ax, vy = by - ay;
            final float t = sat(((px - ax) * vx + (py - ay) * vy) / (vx * vx + vy * vy));
            final float dx = px - ax - vx * t, dy = py - ay - vy * t;
            return sqrtf(dx * dx + dy * dy);
        }

        /**
         * Lays the floor out in advance as random ashlar: stones of many sizes packed onto a fine uneven
         * grid, one at a time, so joints meet in T shapes and no line runs far or repeats.
         */
        private void layPaving() {
            final Random r = new Random(0x9A5E);
            final float[] gx = new float[(int) ((aspect + 0.4f) / 0.021f) + 3], gy = new float[(int) (1.4f / 0.021f) + 3];
            int nx = 0, ny = 0;
            for (float e = -0.2f; e < aspect + 0.2f && nx < gx.length; e += 0.021f + r.nextFloat() * 0.010f) {
                gx[nx++] = e;
            }
            for (float e = -0.2f; e < 1.2f && ny < gy.length; e += 0.021f + r.nextFloat() * 0.010f) {
                gy[ny++] = e;
            }
            paveX = Arrays.copyOf(gx, nx);
            paveY = Arrays.copyOf(gy, ny);
            paveCols = nx - 1;
            final int rows = ny - 1;
            paveId = new int[paveCols * rows];
            Arrays.fill(paveId, -1);
            final int[] widths = {4, 5, 5, 6, 6, 7, 7, 8, 9, 10, 12, 14};
            final int[] heights = {3, 3, 4, 4, 5, 5, 6, 7, 8};
            final float[][] stones = new float[paveId.length][];
            int count = 0;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < paveCols; col++) {
                    if (paveId[row * paveCols + col] >= 0) {
                        continue;
                    }
                    int run = 1;
                    while (col + run < paveCols && paveId[row * paveCols + col + run] < 0) {
                        run++;
                    }
                    int w2 = widths[r.nextInt(widths.length)];
                    if (run - w2 < 3) {
                        w2 = run;
                    }
                    final int h2 = Math.min(heights[r.nextInt(heights.length)], rows - row);
                    for (int yy = row; yy < row + h2; yy++) {
                        for (int xx = col; xx < col + w2; xx++) {
                            paveId[yy * paveCols + xx] = count;
                        }
                    }
                    // Bounds, then a hash for tone, tilt and damage, then edge skews
                    stones[count++] = new float[] {paveX[col], paveY[row], paveX[col + w2], paveY[row + h2], r.nextInt(),
                        (r.nextFloat() - 0.5f) * 0.026f, (r.nextFloat() - 0.5f) * 0.026f};
                }
            }
            paveStones = Arrays.copyOf(stones, count);
            paveLookX = axisLookup(paveX);
            paveLookY = axisLookup(paveY);
        }

        private static int[] axisLookup(final float[] lines) {
            final int[] look = new int[4096];
            int k = 0;
            for (int i = 0; i < look.length; i++) {
                final float p = lines[0] + (lines[lines.length - 1] - lines[0]) * i / look.length;
                while (k < lines.length - 2 && p >= lines[k + 1]) {
                    k++;
                }
                look[i] = k;
            }
            return look;
        }

        private static int axisCell(final float[] lines, final int[] look, final float p) {
            final float t = (p - lines[0]) / (lines[lines.length - 1] - lines[0]);
            final int li = Math.max(0, Math.min(look.length - 1, (int) (t * look.length)));
            int k = look[li];
            while (k < lines.length - 2 && p >= lines[k + 1]) {
                k++;
            }
            return k;
        }

        /**
         * Flagstones of real material: each stone reads its grain from its own place in the noise, so nothing
         * runs on across a joint, its edges wear and chip unevenly, a few carry a hairline crack, and the
         * round slab carrying the medallion sits sunk into the paving. The light does the rest.
         */
        private void renderFloor() {
            layPaving();
            IntStream.range(0, fh).parallel().forEach(this::floorRow);
        }

        private void floorRow(final int y) {
            final float ix = aspect / fw, iy = 1f / fh;
            final float[] c = new float[3], oC = new float[4], med = new float[2];
            final float v = (y + 0.5f) * iy;
            final float ly = v * lowY;
            final int ly0 = (int) ly;
            final float lfy = ly - ly0;
            for (int x = 0; x < fw; x++) {
                final float u = (x + 0.5f) * ix;
                final int i = y * fw + x;
                final float lx = u * lowX;
                final int lx0 = (int) lx;
                final float lfx = lx - lx0;
                final int li = ly0 * lw + lx0;
                final float wx = bil(wxF, li, lw, lfx, lfy), wy = bil(wyF, li, lw, lfx, lfy);
                final float bl = bil(blF, li, lw, lfx, lfy);
                final float d1 = tile[((y & TILE_MASK) << 9) | (x & TILE_MASK)];
                final float d2 = tileAt(tile, u * 200f + 37f, v * 200f + 71f);
                final float d3 = tileAt(tile, u * 620f + 11f, v * 620f + 53f);

                final float wu = u + wx * 0.0060f + d2 * 0.0005f, wv = v + wy * 0.0060f + d2 * 0.0004f;
                final float dx = u - cxB, dy = v - MED_Y;
                final float r = sqrtf(dx * dx + dy * dy);
                // Inside the medallion the paving gives way to one round slab, so its lines are all that shows there
                final float slab = smooth(MED_R * 1.010f, MED_R * 0.990f, r);
                float groove = 0f, rise = 0f;
                if (r < MED_R * 1.03f) {
                    motif(dx, dy, r, med);
                    groove = med[0] * (0.92f + d2 * 0.20f);
                    rise = med[1] * MED_R;
                }

                final int col = axisCell(paveX, paveLookX, wu), row = axisCell(paveY, paveLookY, wv);
                final float[] st = paveStones[paveId[row * paveCols + col]];
                final int sh = slab > 0.5f ? 0x5A3C91E7 : (int) st[4];
                final float localX = wu - (st[0] + st[2]) * 0.5f, localY = wv - (st[1] + st[3]) * 0.5f;
                // Each stone sits a hair out of square, so joints open and close along their length
                final float ax = (st[2] - st[0]) * 0.5f - Math.abs(localX + localY * st[5]);
                final float ay = (st[3] - st[1]) * 0.5f - Math.abs(localY + localX * st[6]);
                final float sd = smin(ax, ay, 0.006f);

                // The stone's own grain: broad clouds, mottling and grit, each from its own place in the noise
                final float gx = sh & 511, gy = (sh >>> 9) & 511;
                final float g1 = tileAt(tile, wu * 110f + gx, wv * 110f + gy);
                final float g2 = tileAt(tile, wu * 410f + gy, wv * 410f + gx);
                final float g3 = tileAt(tile, wu * 1300f + gx * 3f, wv * 1300f + gy * 3f);

                // Edges wear unevenly, broad and round in places and nearly sharp in others, and chips bite into them
                final float wearN = sat(0.5f + tileAt(tile, wu * 75f + gy * 0.5f, wv * 75f + gx * 0.5f) * 1.6f);
                final float chW = 0.0016f + 0.0046f * wearN * wearN;
                final float chip = smooth(0.26f, 0.52f, g2 * 0.9f + g3 * 0.5f) * (0.35f + 0.65f * wearN) * 0.0060f;
                final float sdw = sd - chip + g3 * 0.0007f;
                final float e = mix(smooth(0f, chW, sdw), 1f, slab);
                // The joint itself swells and pinches, and differs stone by stone
                final float pinch = tileAt(tile, wu * 41f + 3f, wv * 41f + 9f);
                final float jw = Math.max(0.35f, (0.55f + 1.1f * h01(sh, 12) * h01(sh, 12)) * (1f + pinch * 0.75f));
                final float core = (1f - slab) * smooth(0.0002f + 0.0011f * jw, 0.0002f, sd + g3 * 0.0007f);

                float tone = h01(sh, 0) - 0.5f;
                float crack = 0f;
                float H = e * (0.0008f + chW * 0.20f);
                if (slab < 1f) {
                    // How far this stone lies from the middle of the board, where the cards sit and nothing should draw the eye
                    final float sdx = (st[0] + st[2]) * 0.5f - cxB, sdy = (st[1] + st[3]) * 0.5f - MED_Y;
                    final float calm = smooth(0.26f, 0.58f, sqrtf(sdx * sdx * 0.45f + sdy * sdy));
                    if (h01(sh, 4) < 0.045f && calm > 0.5f) {
                        // A few flags have broken; the pieces settle at slightly different levels
                        final int hC = voronoi(wu * 14f + d2 * 0.09f + (sh & 15), wv * 14f + d2 * 0.07f + 8.7f, oC);
                        crack = smooth(0.016f, 0.003f, sqrtf(oC[1]) - sqrtf(oC[0]) + d2 * 0.012f) * (1f - slab);
                        tone += (h01(hC, 0) - 0.5f) * 0.18f;
                        H -= (h01(hC, 8) - 0.5f) * 0.0009f * (1f - slab);
                    } else if (h01(sh, 21) < 0.30f) {
                        // Others carry one hairline, wandering in from an edge and dying out part way across
                        final float cx0 = h01(sh, 7) * 2f - 1f, cy0 = h01(sh, 15) * 2f - 1f;
                        final float cl = 1f / Math.max(0.2f, sqrtf(cx0 * cx0 + cy0 * cy0));
                        final float ca = cx0 * cl, sa = cy0 * cl;
                        final float hw2 = (st[2] - st[0]) * 0.5f, hh2 = (st[3] - st[1]) * 0.5f;
                        final float along = localX * ca + localY * sa;
                        final float across = localY * ca - localX * sa + (h01(sh, 19) - 0.5f) * 0.5f * Math.min(hw2, hh2)
                                + tileAt(tile, along * 160f + gx, gy * 0.5f) * 0.020f + tileAt(tile, along * 640f + gy, gx * 0.5f) * 0.0040f;
                        final float reach = (h01(sh, 23) * 0.9f - 0.2f) * Math.max(hw2, hh2);
                        crack = smooth(0.0016f, 0.0003f, Math.abs(across)) * smooth(reach, reach - 0.05f, along) * (0.45f + 0.35f * calm) * (1f - slab);
                    }
                }
                tone = mix(tone, -0.10f, slab);

                // Relief the light can catch, which is what makes it stone rather than a tint
                final float pit = smooth(0.34f, 0.60f, tileAt(tile, wu * 1900f + gy * 2f, wv * 1900f + gx * 2f));
                H += e * (g1 * 0.0011f + g2 * 0.00062f + g3 * 0.00026f + d1 * 0.00003f - pit * 0.00024f) - crack * 0.0007f;
                // The slab is sunk, and its carving is cut deep enough to throw a shadowed edge and a lit one
                H += -slab * 0.0030f - groove * 0.0030f + rise;

                sampleRamp(m.floor, 0.46f + bl * 0.06f + g1 * 0.24f + g2 * 0.16f + g3 * 0.09f + d1 * 0.016f, c);
                // Albedo is about twice as sensitive as the display, so 24% here is 12% on screen
                final float stoneK = 1f + tone * 0.48f;
                final float grime = 1f - 0.10f * smooth(0.008f, 0.001f, sdw) * (1f - slab);
                final float dirt = mix(0.84f, 1f, e) * (1f - core * 0.62f) * grime * (1f - crack * 0.42f) * (1f - groove * 0.46f) * (1f - pit * 0.11f);
                final float hue = (h01(sh, 26) - 0.5f) * (1f - slab);
                float ar = c[0] * stoneK * dirt * (1f + hue * 0.07f), ag = c[1] * stoneK * dirt, ab = c[2] * stoneK * dirt * (1f - hue * 0.10f);

                // The rim of the sunk slab throws a crescent of shadow across it on the side towards the light
                if (slab > 0f) {
                    final float sx = dx + KDX * 0.0085f, sy = dy + KDY * 0.0085f;
                    final float rimShadow = slab * smooth(MED_R * 0.988f, MED_R * 1.002f, sqrtf(sx * sx + sy * sy));
                    final float k = 1f - 0.42f * rimShadow;
                    ar *= k;
                    ag *= k;
                    ab *= k;
                }

                // Moss works out from the foot of the wall along the joints, feathering away and never reaching the open floor
                final float wd = wallDistAt(u, v);
                if (wd < 0.14f) {
                    final float near = smooth(0.135f, 0.004f, wd);
                    final float pm = bil(ptF, li, lw, lfx, lfy);
                    final float grow = pm * 0.42f + near * 0.50f + d2 * 0.62f + d3 * 0.40f + g2 * 0.30f + (1f - e) * 0.30f - 0.60f;
                    final float mk = smooth(-0.06f, 0.24f, grow) * smooth(0.125f, 0.085f, wd) * (1f - sat(quietAt(u, v) * 2f)) * (1f - slab);
                    if (mk > 0f) {
                        final float tuft = 0.62f + d3 * 0.70f + d1 * 0.25f + g3 * 0.40f;
                        ar = mix(ar, m.moss[0] * tuft, mk * 0.72f);
                        ag = mix(ag, m.moss[1] * tuft, mk * 0.72f);
                        ab = mix(ab, m.moss[2] * tuft, mk * 0.72f);
                        H += mk * (0.00030f + d3 * 0.00040f);
                    }
                }
                fH[i] = H;
                fR[i] = ar;
                fG[i] = ag;
                fB[i] = ab;
            }
        }

        /** The key light's fall off across the board, strongest at the upper left and dying away to the lower right. Floor and walls share it. */
        private float keyFallAt(final float u, final float v) {
            final float t = sat(u / aspect * 0.56f + v * 0.44f);
            return mix(m.fallNear, m.fallFar, t * t * (3f - 2f * t));
        }

        /** How much of a banner's soft shadow lies here; the cloth is somewhere up the light's direction. */
        private float bannerShadowAt(final float u, final float v) {
            final float qx = (u + KDX * 0.022f) / aspect * pw / 3f, qy = (v + KDY * 0.022f) * ph / 3f;
            if (qx < 0f || qy < 0f || qx >= bw - 1 || qy >= bh - 1) {
                return 0f;
            }
            final int x0 = (int) qx, y0 = (int) qy;
            return bil(banSoft, y0 * bw + x0, bw, qx - x0, qy - y0);
        }

        private void shadeFloor() {
            IntStream.range(0, fh).parallel().forEach(this::shadeFloorRow);
        }

        private void shadeFloorRow(final int y) {
            final float ix = aspect / fw, iy = 1f / fh;
            final float pcx = aspect * POOL_X, pcy = POOL_Y;
            final float[] key = m.key, fill = m.fill, sky = m.sky, ground = m.ground, pool = m.pool;
            final float ffx = 0.56f, ffy = 0.50f, ffz = 0.66f;
            final float v = (y + 0.5f) * iy;
            final int yUp = Math.max(0, y - 1), yDn = Math.min(fh - 1, y + 1);
            final float qyf = v * gridY;
            final int qy0 = (int) qyf;
            final float qfy = qyf - qy0;
            for (int x = 0; x < fw; x++) {
                final float u = (x + 0.5f) * ix;
                final int i = y * fw + x;
                final int xL = Math.max(0, x - 1), xR = Math.min(fw - 1, x + 1);
                float gx = (fH[y * fw + xR] - fH[y * fw + xL]) * 0.5f * fw / aspect;
                float gy = (fH[yDn * fw + x] - fH[yUp * fw + x]) * 0.5f * fh;
                // A steep lip is held to a firm slope, so it reads as light and shade and never as an ink line
                final float slope2 = gx * gx + gy * gy;
                if (slope2 > 1.21f) {
                    final float lim = 1.1f / sqrtf(slope2);
                    gx *= lim;
                    gy *= lim;
                }
                final float nl = 1f / sqrtf(gx * gx + gy * gy + 1f);
                final float nx = -gx * nl, ny = -gy * nl, nz = nl;

                final float qxf = u * gridX;
                final int qx0 = (int) qxf;
                final float qfx = qxf - qx0;
                final int qi = qy0 * gw + qx0;
                final float shad = bil(shadow, qi, gw, qfx, qfy);
                final float far = bil(aoFar, qi, gw, qfx, qfy) - fH[i];
                final float ao = 1f - sat(far * 20f) * 0.5f;
                final float wd = bil(wallDist, qi, gw, qfx, qfy);
                final float bs = wd < 0.10f ? bannerShadowAt(u, v) : 0f;

                final float dk = Math.max(0f, nx * KX + ny * KY + nz * KZ) * (1f - shad * 0.86f) * (1f - bs * 0.8f) * keyFallAt(u, v);
                final float df = Math.max(0f, nx * ffx + ny * ffy + nz * ffz);
                final float hemi = 0.5f + 0.5f * (nz * 0.8f - ny * 0.2f);

                // Where the key light lands hardest, so the falloff has somewhere to fall from
                final float ax = u - pcx, ay = v - pcy;
                final float q = (ax * ax * 0.30f + ay * ay) / (0.34f * 0.34f);
                final float atten = 1f / (1f + q * q);
                final float lz = 0.42f - fH[i];
                final float dotP = Math.max(0f, (-nx * ax - ny * ay + nz * lz) / sqrtf(ax * ax + ay * ay + lz * lz));
                final float pl = m.poolGain * atten * (0.35f + 0.65f * dotP) * (1f - shad * 0.55f);

                final float amb = m.skyGain * ao * (1f - shad * 0.30f) * (1f - bs * 0.4f);
                final float keyK = m.keyGain * dk, fillK = m.fillGain * df * ao;
                final float lr = (sky[0] * hemi + ground[0] * (1f - hemi)) * amb + key[0] * keyK + fill[0] * fillK + pool[0] * pl;
                final float lg = (sky[1] * hemi + ground[1] * (1f - hemi)) * amb + key[1] * keyK + fill[1] * fillK + pool[1] * pl;
                final float lb = (sky[2] * hemi + ground[2] * (1f - hemi)) * amb + key[2] * keyK + fill[2] * fillK + pool[2] * pl;

                // Edges of the floor fall away a little; the tight contact shadow itself is laid with the wall's foot
                final float edgeK = 1f - 0.34f * m.vignette * smooth(0.34f, 0.06f, boxSd(u, v));
                final float ct = sat(1f - wd / 0.05f);
                final float contact = 1f - 0.22f * ct * sqrtf(ct);
                // Warm in the middle where the light lands, cooler towards the corners, in the moods that want it
                final float split = (smooth(0.52f, 0.08f, sqrtf(ax * ax * 0.42f + ay * ay)) * 2f - 1f) * m.warmth;
                final float k = edgeK * contact * (1f + split * 0.09f) * 255f;
                cR[i] = sqrtf(aces(fR[i] * lr * m.exposure)) * k * (1f + split * 0.075f);
                cG[i] = sqrtf(aces(fG[i] * lg * m.exposure)) * k;
                cB[i] = sqrtf(aces(fB[i] * lb * m.exposure)) * k * (1f - split * (split > 0f ? 0.09f : 0.16f));
            }
        }

        private void shadeWalls() {
            final float[] aoNear = boxBlur(pH, pw, ph, Math.max(1, Math.round(0.0030f * ph)), 2);
            IntStream.range(0, ph).parallel().forEach(y -> shadeWallRow(y, aoNear));
            sharpenWalls(pR);
            sharpenWalls(pG);
            sharpenWalls(pB);
        }

        private void shadeWallRow(final int y, final float[] aoNear) {
            final float ix = aspect / pw, iy = 1f / ph;
            final float pcx = aspect * POOL_X;
            final float[] key = m.key, fill = m.fill, sky = m.sky, ground = m.ground, pool = m.pool;
            final float ffx = 0.56f, ffy = 0.50f, ffz = 0.66f;
            final float hl = sqrtf(KX * KX + KY * KY + (KZ + 1f) * (KZ + 1f));
            final float hx2 = KX / hl, hy2 = KY / hl, hz2 = (KZ + 1f) / hl;
            final float v = (y + 0.5f) * iy;
            final int yUp = Math.max(0, y - 1), yDn = Math.min(ph - 1, y + 1);
            final float qyf = v * gridY;
            final int qy0 = (int) qyf;
            final float qfy = qyf - qy0;
            for (int x = 0; x < pw; x++) {
                final int i = y * pw + x;
                final float cov = pCov[i], con = pCon[i];
                if (cov <= 0f && con <= 0f) {
                    continue;
                }
                if (cov <= 0f) {
                    pCov[i] = con;
                    continue;
                }
                final float u = (x + 0.5f) * ix;
                final int xL = Math.max(0, x - 1), xR = Math.min(pw - 1, x + 1);
                float gx = (pH[y * pw + xR] - pH[y * pw + xL]) * 0.5f * pw / aspect;
                float gy = (pH[yDn * pw + x] - pH[yUp * pw + x]) * 0.5f * ph;
                // A seam is held to a firm slope, so stones part by light and shade instead of a black outline
                final float slope2 = gx * gx + gy * gy;
                if (slope2 > 1.44f) {
                    final float lim = 1.2f / sqrtf(slope2);
                    gx *= lim;
                    gy *= lim;
                }
                final float nl = 1f / sqrtf(gx * gx + gy * gy + 1f);
                final float nx = -gx * nl, ny = -gy * nl, nz = nl;

                final float qxf = u * gridX;
                final int qx0 = (int) qxf;
                final float qfx = qxf - qx0;
                final int qi = qy0 * gw + qx0;
                final float shad = bil(shadow, qi, gw, qfx, qfy);
                final float H = pH[i];
                final float near = aoNear[i] - H;
                final float far = bil(aoFar, qi, gw, qfx, qfy) - H;
                final float ao = (1f - sat(near * 46f) * 0.50f * m.crevice) * (1f - sat(far * 14f) * 0.45f * m.crevice);
                final float wear = sat((H - aoNear[i]) * 150f);

                // Banner shadow falls on the wall as well, but not on the cloth that throws it
                final float bs = bannerShadowAt(u, v) * (1f - pCl[i]);
                final float fall = keyFallAt(u, v);
                final float dk = Math.max(0f, nx * KX + ny * KY + nz * KZ) * (1f - shad * 0.78f) * (1f - bs * 0.85f) * fall;
                final float df = Math.max(0f, nx * ffx + ny * ffy + nz * ffz);
                final float hemi = 0.5f + 0.5f * (nz * 0.8f - ny * 0.2f);

                // Faces looking towards the middle of the board catch a little of its light
                final float ax = pcx - u, ay = POOL_Y - v, az = 0.30f - H;
                final float dp = sqrtf(ax * ax + ay * ay + az * az);
                final float dotP = Math.max(0f, (nx * ax + ny * ay + nz * az) / dp);
                final float rim = dotP * dotP * dotP * dotP;
                final float pl = m.poolGain * rim * 0.22f / (0.4f + dp * dp * 1.2f);

                final float amb = m.skyGain * ao * m.wallGain * 1.36f * (1f - bs * 0.45f);
                final float keyK = m.keyGain * dk * 1.25f * m.wallGain, fillK = m.fillGain * df * ao;
                final float rimK = (pool[0] + pool[1] + pool[2]) * 0.2f * pl;
                final float lr = (sky[0] * hemi + ground[0] * (1f - hemi)) * amb + key[0] * keyK + fill[0] * fillK + pool[0] * pl * 0.4f + rimK;
                final float lg = (sky[1] * hemi + ground[1] * (1f - hemi)) * amb + key[1] * keyK + fill[1] * fillK + pool[1] * pl * 0.4f + rimK;
                final float lb = (sky[2] * hemi + ground[2] * (1f - hemi)) * amb + key[2] * keyK + fill[2] * fillK + pool[2] * pl * 0.4f + rimK;

                final float rimDist = Math.min(Math.min(u, aspect - u), Math.min(v, 1f - v));
                final float alb = (1f + wear * 0.50f) * (1f - 0.40f * m.vignette * smooth(0.045f, 0f, rimDist));
                float rr = pR[i] * lr * alb, gg = pG[i] * lg * alb, bb = pB[i] * lb * alb;

                final float sp0 = Math.max(0f, nx * hx2 + ny * hy2 + nz * hz2);
                float sp = sp0 * sp0;
                sp *= sp;
                sp *= sp;
                final float spec = sp * sp * pGl[i] * (1f - shad * 0.86f) * m.keyGain * fall;
                rr += key[0] * spec;
                gg += key[1] * spec;
                bb += key[2] * spec;

                final float k = cov * 255f;
                pR[i] = sqrtf(aces(rr * m.exposure)) * k;
                pG[i] = sqrtf(aces(gg * m.exposure)) * k;
                pB[i] = sqrtf(aces(bb * m.exposure)) * k;
                pCov[i] = cov + (1f - cov) * con;
            }
        }

        /** A small Laplacian lift over solid wall, which keeps cut edges and rubble facets from going soft. */
        private void sharpenWalls(final float[] a) {
            final float[] src = a.clone();
            for (int y = 1; y < ph - 1; y++) {
                for (int x = 1; x < pw - 1; x++) {
                    final int i = y * pw + x;
                    if (pCov[i] > 0.99f) {
                        a[i] = src[i] + (4f * src[i] - src[i - 1] - src[i + 1] - src[i - pw] - src[i + pw]) * 0.07f;
                    }
                }
            }
        }

        private BufferedImage finish() {
            final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            // Floor resampling tables
            final int[] xIdx = new int[w];
            final float[] xWt = new float[w * 4];
            for (int x = 0; x < w; x++) {
                final float p = (x + 0.5f) * fw / w - 0.5f;
                final int i0 = ifloor(p);
                xIdx[x] = i0;
                catmull(p - i0, xWt, x * 4);
            }
            final boolean wallsNative = pw == w && ph == h;
            final int[] pxIdx = new int[w];
            final float[] pxWt = new float[w * 4];
            if (!wallsNative) {
                for (int x = 0; x < w; x++) {
                    final float p = (x + 0.5f) * pw / w - 0.5f;
                    final int i0 = ifloor(p);
                    pxIdx[x] = i0;
                    catmull(p - i0, pxWt, x * 4);
                }
            }
            final float[] yWt = new float[4], pyWt = new float[4];
            final float[] tr = new float[fw], tg = new float[fw], tb = new float[fw];
            final float[] qr = new float[pw], qg = new float[pw], qb = new float[pw], qa = new float[pw];
            final int[] line = new int[w];
            final float invH = 1f / h;
            final float invX = aspect / w;
            final int quietX = (int) (w * 0.72f), quietY = (int) (h * 0.62f);
            int seed = 0x2F6E2B1;

            for (int y = 0; y < h; y++) {
                final float py = (y + 0.5f) * fh / h - 0.5f;
                final int y0 = ifloor(py);
                catmull(py - y0, yWt, 0);
                final int ya = Math.max(0, y0 - 1) * fw, yb = Math.max(0, y0) * fw;
                final int yc = Math.min(fh - 1, y0 + 1) * fw, yd = Math.min(fh - 1, y0 + 2) * fw;
                final float w0 = yWt[0], w1 = yWt[1], w2 = yWt[2], w3 = yWt[3];
                for (int x = 0; x < fw; x++) {
                    tr[x] = cR[ya + x] * w0 + cR[yb + x] * w1 + cR[yc + x] * w2 + cR[yd + x] * w3;
                    tg[x] = cG[ya + x] * w0 + cG[yb + x] * w1 + cG[yc + x] * w2 + cG[yd + x] * w3;
                    tb[x] = cB[ya + x] * w0 + cB[yb + x] * w1 + cB[yc + x] * w2 + cB[yd + x] * w3;
                }
                final int rowP = y * pw;
                if (!wallsNative) {
                    final float pp = (y + 0.5f) * ph / h - 0.5f;
                    final int p0 = ifloor(pp);
                    catmull(pp - p0, pyWt, 0);
                    final int pa = Math.max(0, p0 - 1) * pw, pb = Math.max(0, p0) * pw;
                    final int pc = Math.min(ph - 1, p0 + 1) * pw, pd = Math.min(ph - 1, p0 + 2) * pw;
                    final float q0 = pyWt[0], q1 = pyWt[1], q2 = pyWt[2], q3 = pyWt[3];
                    for (int x = 0; x < pw; x++) {
                        qr[x] = pR[pa + x] * q0 + pR[pb + x] * q1 + pR[pc + x] * q2 + pR[pd + x] * q3;
                        qg[x] = pG[pa + x] * q0 + pG[pb + x] * q1 + pG[pc + x] * q2 + pG[pd + x] * q3;
                        qb[x] = pB[pa + x] * q0 + pB[pb + x] * q1 + pB[pc + x] * q2 + pB[pd + x] * q3;
                        qa[x] = pCov[pa + x] * q0 + pCov[pb + x] * q1 + pCov[pc + x] * q2 + pCov[pd + x] * q3;
                    }
                }
                final float v = (y + 0.5f) * invH;
                final float vEdge = Math.min(v, 1f - v);
                for (int x = 0; x < w; x++) {
                    final int i0 = xIdx[x];
                    final int xa = Math.max(0, i0 - 1), xb = Math.max(0, i0);
                    final int xc = Math.min(fw - 1, i0 + 1), xd = Math.min(fw - 1, i0 + 2);
                    final int q = x * 4;
                    final float ka = xWt[q], kb = xWt[q + 1], kc = xWt[q + 2], kd = xWt[q + 3];
                    float r = tr[xa] * ka + tr[xb] * kb + tr[xc] * kc + tr[xd] * kd;
                    float g = tg[xa] * ka + tg[xb] * kb + tg[xc] * kc + tg[xd] * kd;
                    float b = tb[xa] * ka + tb[xb] * kb + tb[xc] * kc + tb[xd] * kd;

                    float wa, wr, wg, wb;
                    if (wallsNative) {
                        final int pi = rowP + x;
                        wa = pCov[pi];
                        wr = pR[pi];
                        wg = pG[pi];
                        wb = pB[pi];
                    } else {
                        final int pi = pxIdx[x];
                        final int a0 = Math.max(0, pi - 1), a1 = Math.max(0, pi);
                        final int a2 = Math.min(pw - 1, pi + 1), a3 = Math.min(pw - 1, pi + 2);
                        final float pa = pxWt[q], pb = pxWt[q + 1], pc = pxWt[q + 2], pd = pxWt[q + 3];
                        wa = qa[a0] * pa + qa[a1] * pb + qa[a2] * pc + qa[a3] * pd;
                        wr = qr[a0] * pa + qr[a1] * pb + qr[a2] * pc + qr[a3] * pd;
                        wg = qg[a0] * pa + qg[a1] * pb + qg[a2] * pc + qg[a3] * pd;
                        wb = qb[a0] * pa + qb[a1] * pb + qb[a2] * pc + qb[a3] * pd;
                    }
                    if (wa > 0.002f) {
                        final float ia = 1f - sat(wa);
                        r = r * ia + Math.max(wr, 0f);
                        g = g * ia + Math.max(wg, 0f);
                        b = b * ia + Math.max(wb, 0f);
                    }

                    // The corner behind the action button sinks into a flat, quiet tone
                    float hush = 1f;
                    if (x >= quietX && y >= quietY) {
                        final float qk = quietAt(x * invX, v) * 0.82f;
                        r = mix(r, m.quiet[0], qk);
                        g = mix(g, m.quiet[1], qk);
                        b = mix(b, m.quiet[2], qk);
                        hush = 1f - qk;
                    }

                    // Fine grit and grain at the output size, this is also what stops the gradients banding
                    seed ^= seed << 13;
                    seed ^= seed >>> 17;
                    seed ^= seed << 5;
                    final float grit = tile[((y & TILE_MASK) << 9) | ((x + 131) & TILE_MASK)] * (1.8f + 2.4f * sat(wa)) * hush;
                    final float gn = ((seed & 0xFF) - 127.5f) * (2.6f / 127.5f) + grit;
                    line[x] = (clampByte(r + gn + 0.5f) << 16) | (clampByte(g + gn + 0.5f) << 8) | clampByte(b + gn + 0.5f);
                }
                img.getRaster().setDataElements(0, y, w, 1, line);
            }

            final Graphics2D g = img.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                final BufferedImage creepers = new BufferedImage(Math.max(1, w / 2), Math.max(1, h / 2), BufferedImage.TYPE_INT_ARGB);
                final Graphics2D cg = creepers.createGraphics();
                cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                cg.scale(creepers.getWidth() / (double) w, creepers.getHeight() / (double) h);
                ivy(cg);
                cg.dispose();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(creepers, 0, 0, w, h, null);
                litter(g);
            } finally {
                g.dispose();
            }
            return img;
        }

        /** Twigs and small drifts of fallen leaves blown against the foot of the wall, never out into the open floor. */
        private void litter(final Graphics2D g) {
            final Random r = new Random(0x51AB);
            final float unitY = h, unitX = w / aspect;
            final float[] o = new float[4];
            final Path2D leaf = new Path2D.Float();
            leaf.moveTo(-1f, 0f);
            leaf.curveTo(-0.62f, -0.62f, 0.34f, -0.58f, 1f, 0f);
            leaf.curveTo(0.30f, 0.46f, -0.58f, 0.56f, -1f, 0f);
            leaf.closePath();
            final Line2D rib = new Line2D.Float(-1.05f, 0f, 0.72f, 0f);
            final AffineTransform at = new AffineTransform();
            final Color shade = new Color(0, 0, 0, 92);
            g.setStroke(new BasicStroke(Math.max(1f, h * 0.0011f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Twigs first, so leaves settle over them
            for (int n = 0; n < 8; n++) {
                final float s = r.nextFloat() * perim;
                perimPoint(s, o);
                final float reach = lut(footL, s) + 0.012f + r.nextFloat() * 0.040f;
                final float x = o[0] + o[2] * reach, y = o[1] + o[3] * reach;
                if (!litterSpot(x, y, 0.002f, 0.075f)) {
                    continue;
                }
                final float ang = r.nextFloat() * 6.2832f, len = 0.012f + r.nextFloat() * 0.030f, bend = (r.nextFloat() - 0.5f) * 0.016f;
                final float x1 = x + cosf(ang) * len, y1 = y + sinf(ang) * len;
                final float mx = (x + x1) * 0.5f - sinf(ang) * bend, my = (y + y1) * 0.5f + cosf(ang) * bend;
                final QuadCurve2D twig = new QuadCurve2D.Float(x * unitX, y * unitY, mx * unitX, my * unitY, x1 * unitX, y1 * unitY);
                final float off = h * 0.0014f;
                g.setColor(shade);
                g.draw(AffineTransform.getTranslateInstance(off * 0.7f, off).createTransformedShape(twig));
                final float t = 0.66f + r.nextFloat() * 0.34f;
                g.setColor(new Color((int) (58 * t), (int) (44 * t), (int) (32 * t)));
                g.draw(twig);
            }

            for (int drift = 0; drift < 11; drift++) {
                final float s = r.nextFloat() * perim;
                final int count = 1 + r.nextInt(5);
                final float spreadS = 0.016f + r.nextFloat() * 0.055f;
                final float tone = 0.78f + r.nextFloat() * 0.28f;
                for (int n = 0; n < count; n++) {
                    final float along = s + (r.nextFloat() + r.nextFloat() - 1f) * spreadS;
                    perimPoint(along, o);
                    final float out = r.nextFloat();
                    final float reach = lut(footL, along) + 0.004f + out * out * 0.070f;
                    final float x = o[0] + o[2] * reach, y = o[1] + o[3] * reach;
                    if (!litterSpot(x, y, -1f, 0.085f)) {
                        continue;
                    }
                    final float big = r.nextFloat();
                    final float sz = (0.0036f + big * big * big * 0.0135f) * unitY;
                    final float shad = bil(shadow, Math.min(gh - 2, (int) (y * gridY)) * gw + Math.min(gw - 2, (int) (x * gridX)), gw, 0f, 0f);
                    final float wd = wallDistAt(x, y);
                    final float ct = sat(1f - wd / (0.03f * aspect));
                    final float k = tone * (0.68f + r.nextFloat() * 0.40f) * (1f - shad * 0.42f) * (1f - ct * 0.45f);
                    final Color c = m.leaf[r.nextInt(m.leaf.length)];
                    at.setToTranslation(x * unitX, y * unitY);
                    at.rotate(r.nextFloat() * 6.2832f);
                    at.scale(sz, sz * (0.46f + r.nextFloat() * 0.62f));
                    final Shape sh = at.createTransformedShape(leaf);
                    g.setColor(shade);
                    g.fill(AffineTransform.getTranslateInstance(sz * 0.20f, sz * 0.28f).createTransformedShape(sh));
                    g.setColor(new Color(clampByte(c.getRed() * k), clampByte(c.getGreen() * k), clampByte(c.getBlue() * k)));
                    g.fill(sh);
                    if (sz > h * 0.0072f) {
                        g.setColor(new Color(clampByte(c.getRed() * k * 0.56f), clampByte(c.getGreen() * k * 0.56f), clampByte(c.getBlue() * k * 0.56f)));
                        g.draw(at.createTransformedShape(rib));
                    }
                }
            }
        }

        /** Litter stays on the board, off the quiet corner, and within reach of a wall; a negative minimum lets it lie on the wall too. */
        private boolean litterSpot(final float x, final float y, final float minDist, final float maxDist) {
            if (x < 0f || y < 0f || x >= aspect || y >= 1f || quietAt(x, y) > 0.01f) {
                return false;
            }
            final float wd = wallDistAt(x, y);
            return wd >= minDist && wd <= maxDist;
        }

        /** Creepers spilling off the ledges in a few uneven drifts, lit by the same shadow map as the stone. */
        private void ivy(final Graphics2D g) {
            final Random r = new Random(0x1F7);
            final float unitY = h, unitX = w / aspect;
            final float[] o = new float[4];
            final float[][] leaves = new float[1600][6];
            int count = 0;
            final int clumps = 6;
            for (int c = 0; c < clumps; c++) {
                final float s = r.nextFloat() * perim;
                perimPoint(s, o);
                final float foot = lut(footL, s);
                final float ax = o[2], ay = o[3];
                final float tx = -ay, ty = ax;
                final float px0 = o[0] + ax * foot, py0 = o[1] + ay * foot;
                if (quietAt(px0, py0) > 0.01f) {
                    continue;
                }
                // Mostly small tufts, the odd long curtain
                final float size = r.nextFloat();
                final int vines = 2 + (int) (size * size * 9f);
                final float spreadS = 0.012f + size * size * 0.10f;
                final float tone = 0.72f + r.nextFloat() * 0.26f;
                for (int n = 0; n < vines; n++) {
                    final float start = (r.nextFloat() + r.nextFloat() - 1f) * spreadS;
                    float x = px0 + tx * start - ax * (0.030f + r.nextFloat() * 0.022f);
                    float y = py0 + ty * start - ay * (0.030f + r.nextFloat() * 0.022f);
                    float dt = (r.nextFloat() - 0.5f) * 0.0065f;
                    final int steps = 7 + r.nextInt(12);
                    for (int k = 0; k < steps && count < leaves.length - 2; k++) {
                        x += ax * 0.0052f + tx * dt;
                        y += ay * 0.0052f + ty * dt;
                        dt += (r.nextFloat() - 0.5f) * 0.0022f;
                        if ((x - px0) * ax + (y - py0) * ay > 0.006f + r.nextFloat() * 0.006f) {
                            break;
                        }
                        final int pairs = 1 + r.nextInt(2);
                        for (int p = 0; p < pairs; p++) {
                            final float[] L = leaves[count++];
                            L[0] = x + (r.nextFloat() - 0.5f) * 0.0065f;
                            L[1] = y + (r.nextFloat() - 0.5f) * 0.0065f;
                            L[2] = r.nextFloat() * 6.2832f;
                            L[3] = 0.0030f + r.nextFloat() * 0.0030f;
                            L[4] = r.nextInt(m.ivy.length);
                            L[5] = tone * (0.52f + r.nextFloat() * 0.48f);
                        }
                    }
                }
            }
            final Path2D unit = new Path2D.Float();
            unit.moveTo(-1f, 0f);
            unit.curveTo(-0.55f, -0.72f, 0.45f, -0.62f, 1f, 0f);
            unit.curveTo(0.45f, 0.62f, -0.55f, 0.72f, -1f, 0f);
            unit.closePath();
            final AffineTransform at = new AffineTransform();
            // Shadow pass first so the mass sits on the stone
            g.setColor(new Color(0, 0, 0, 84));
            for (int i = 0; i < count; i++) {
                final float[] L = leaves[i];
                final float sz = L[3] * unitY;
                at.setToTranslation((L[0] + 0.0018f) * unitX, (L[1] + 0.0024f) * unitY);
                at.rotate(L[2]);
                at.scale(sz, sz);
                g.fill(at.createTransformedShape(unit));
            }
            for (int i = 0; i < count; i++) {
                final float[] L = leaves[i];
                final float sz = L[3] * unitY;
                at.setToTranslation(L[0] * unitX, L[1] * unitY);
                at.rotate(L[2]);
                at.scale(sz, sz);
                final Color c = m.ivy[(int) L[4]];
                final float u = Math.max(0f, Math.min(aspect - 0.001f, L[0])), v = Math.max(0f, Math.min(0.999f, L[1]));
                final float qxf = u * gridX, qyf = v * gridY;
                final int qx0 = (int) qxf, qy0 = (int) qyf;
                final float shad = bil(shadow, qy0 * gw + qx0, gw, qxf - qx0, qyf - qy0);
                final float rimDist = Math.min(Math.min(u, aspect - u), Math.min(v, 1f - v));
                final float k = L[5] * (1f - shad * 0.55f) * (0.62f + 0.38f * smooth(0f, 0.045f, rimDist)) * mix(1f, keyFallAt(u, v), 0.5f);
                g.setColor(new Color(Math.min(255, (int) (c.getRed() * k)), Math.min(255, (int) (c.getGreen() * k)), Math.min(255, (int) (c.getBlue() * k)), 224));
                g.fill(at.createTransformedShape(unit));
            }
        }
    }

    private static Path2D poly(final float... pts) {
        final Path2D p = new Path2D.Float();
        p.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) {
            p.lineTo(pts[i], pts[i + 1]);
        }
        p.closePath();
        return p;
    }

    private static void catmull(final float t, final float[] out, final int at) {
        final float t2 = t * t, t3 = t2 * t;
        out[at] = -0.5f * t3 + t2 - 0.5f * t;
        out[at + 1] = 1.5f * t3 - 2.5f * t2 + 1f;
        out[at + 2] = -1.5f * t3 + 2f * t2 + 0.5f * t;
        out[at + 3] = 0.5f * t3 - 0.5f * t2;
    }

    private static int clampByte(final float v) {
        return v <= 0f ? 0 : v >= 255f ? 255 : (int) v;
    }

    private static int lerpC(final int a, final int b, final float t) {
        return Math.round(a + (b - a) * t);
    }

    // Metal: matte cast surfaces with a broad sheen and a small, restrained highlight

    private static final float MK_X = -0.46f, MK_Y = -0.62f, MK_Z = 0.635f;
    private static final float MH_X = -0.254f, MH_Y = -0.342f, MH_Z = 0.904f;

    /** Lights a unit normal; out receives display colour from 0 to 1. */
    private static void litMetal(final float nx, final float ny, final float nz, final float[] alb, final float specA, final float specB, final float[] out) {
        final float kd = Math.max(0f, nx * MK_X + ny * MK_Y + nz * MK_Z);
        final float fd = Math.max(0f, nx * 0.60f + ny * 0.20f + nz * 0.77f);
        final float hemi = 0.5f - 0.5f * ny;
        final float hd = Math.max(0f, nx * MH_X + ny * MH_Y + nz * MH_Z);
        final float s4 = hd * hd * hd * hd;
        final float s16 = s4 * s4 * s4 * s4;
        final float s64 = s16 * s16 * s16 * s16;
        final float diff = 0.06f + hemi * 0.16f + kd * 1.12f + fd * 0.08f;
        final float bounce = Math.max(0f, ny) * 0.06f;
        final float spec = s16 * specA + s64 * specB;
        for (int c = 0; c < 3; c++) {
            final float x = alb[c] * (diff + bounce) + (alb[c] * 1.1f + 0.05f) * spec;
            out[c] = sqrtf(aces(x));
        }
    }

    private static final Map<String, BufferedImage> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, BufferedImage>(64, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, BufferedImage> e) {
                    return size() > 96;
                }
            });

    /** Premultiplied float canvas the metal parts are composited on. */
    private static final class Canvas {
        final int w, h;
        final float[] r, g, b, a;

        Canvas(final int w, final int h) {
            this.w = w;
            this.h = h;
            r = new float[w * h];
            g = new float[w * h];
            b = new float[w * h];
            a = new float[w * h];
        }

        void over(final int i, final float cr, final float cg, final float cb, final float alpha) {
            final float ia = 1f - alpha;
            r[i] = cr * alpha + r[i] * ia;
            g[i] = cg * alpha + g[i] * ia;
            b[i] = cb * alpha + b[i] * ia;
            a[i] = alpha + a[i] * ia;
        }

        float[] coverage(final Shape s) {
            final BufferedImage m = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2 = m.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(Color.WHITE);
            g2.fill(s);
            g2.dispose();
            final int[] px = new int[w * h];
            m.getRaster().getDataElements(0, 0, w, h, px);
            final float[] cov = new float[w * h];
            for (int i = 0; i < cov.length; i++) {
                cov[i] = (px[i] >>> 24) / 255f;
            }
            return cov;
        }

        void shadow(final float[] cov, final int blur, final int dx, final int dy, final float strength, final Color c) {
            final float[] b2 = boxBlur(cov, w, h, blur, 2);
            final float cr = c.getRed() / 255f, cg = c.getGreen() / 255f, cb = c.getBlue() / 255f;
            for (int y = 0; y < h; y++) {
                final int sy = y - dy;
                if (sy < 0 || sy >= h) {
                    continue;
                }
                for (int x = 0; x < w; x++) {
                    final int sx = x - dx;
                    if (sx < 0 || sx >= w) {
                        continue;
                    }
                    final float al = b2[sy * w + sx] * strength;
                    if (al > 0.002f) {
                        over(y * w + x, cr, cg, cb, Math.min(1f, al));
                    }
                }
            }
        }

        /** Distance in pixels from each covered pixel to the edge of the shape. */
        private float[] inside(final float[] cov) {
            final float[] t = new float[w * h];
            for (int i = 0; i < t.length; i++) {
                t[i] = cov[i] >= 0.5f ? 1e6f : 0f;
            }
            final float dg = 1.4142f;
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    final int i = y * w + x;
                    if (t[i] > 0f) {
                        t[i] = Math.min(t[i], Math.min(Math.min(t[i - 1] + 1f, t[i - w] + 1f), Math.min(t[i - w - 1] + dg, t[i - w + 1] + dg)));
                    }
                }
            }
            for (int y = h - 2; y >= 1; y--) {
                for (int x = w - 2; x >= 1; x--) {
                    final int i = y * w + x;
                    if (t[i] > 0f) {
                        t[i] = Math.min(t[i], Math.min(Math.min(t[i + 1] + 1f, t[i + w] + 1f), Math.min(t[i + w + 1] + dg, t[i + w - 1] + dg)));
                    }
                }
            }
            return t;
        }

        /**
         * Worked metal: height rises in a straight chamfer from the edge up to chamfer pixels, and a
         * gentle crown over crown pixels moves the sheen across the plate. A chased line runs just inside
         * the outline, and within edge pixels of the outline the dark body wears through to the metal below.
         */
        void forge(final float[] cov, final float chamfer, final float crown, final float[] body, final float[] worn, final float edge, final int scale, final float micro) {
            final float[] dist = inside(cov);
            final float[] ht = new float[w * h];
            final float g0 = scale * 0.052f, gw = Math.max(1f, scale * 0.0095f);
            for (int i = 0; i < ht.length; i++) {
                final float di = dist[i];
                if (di > 0f) {
                    final float q = 1f - sat(di / crown);
                    final float g = 1f - sat(Math.abs(di - g0) / gw);
                    ht[i] = Math.min(di, chamfer) * 0.62f + crown * 0.34f * (1f - q * q) - g * g * gw * 0.42f;
                }
            }
            final float[] hs = boxBlur(ht, w, h, Math.max(1, Math.round(scale * 0.0075f)), 2);
            final float[] t = tile();
            final float[] rgb = new float[3], alb = new float[3];
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    final int i = y * w + x;
                    final float c = cov[i];
                    if (c <= 0f) {
                        continue;
                    }
                    float gx = (hs[i + 1] - hs[i - 1]) * 0.5f, gy = (hs[i + w] - hs[i - w]) * 0.5f;
                    final float n1 = t[((y * 2 & TILE_MASK) << 9) | (x * 2 & TILE_MASK)];
                    final float n2 = t[(((y * 2 + 77) & TILE_MASK) << 9) | ((x * 2 + 31) & TILE_MASK)];
                    final float n3 = t[(((y + 200) & TILE_MASK) << 9) | ((x + 90) & TILE_MASK)];
                    gx += n1 * micro + n3 * micro * 0.9f;
                    gy += n2 * micro - n3 * micro * 0.7f;
                    final float nl = 1f / sqrtf(gx * gx + gy * gy + 1f);
                    // Brass shows through unevenly along the edge, a little wider where the casting is proud
                    final float facing = -(gx * MK_X + gy * MK_Y);
                    final float wear = smooth(edge * (1.1f + n3 * 1.1f), edge * 0.35f, dist[i]) * (0.16f + 0.84f * smooth(-0.05f, 0.30f, facing + n3 * 0.5f));
                    final float pit = 1f + n1 * 0.26f;
                    alb[0] = mix(body[0], worn[0], wear) * pit;
                    alb[1] = mix(body[1], worn[1], wear) * pit;
                    alb[2] = mix(body[2], worn[2], wear) * pit;
                    litMetal(-gx * nl, -gy * nl, nl, alb, 0.42f + wear * 0.3f, 0.58f + wear * 0.5f, rgb);
                    over(i, rgb[0], rgb[1], rgb[2], c);
                }
            }
        }

        /** Engraves shapes into the metal: a dark cut with the light catching its lower wall; strength 1 is a full cut. */
        void carve(final float[] cov, final int lift, final float[] worn, final float strength) {
            final float lipR = sqrtf(aces(worn[0] * 1.1f)), lipG = sqrtf(aces(worn[1] * 1.1f)), lipB = sqrtf(aces(worn[2] * 1.1f));
            for (int y = lift; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    final int i = y * w + x;
                    final float c = cov[i];
                    if (c > 0f) {
                        over(i, 0.055f, 0.050f, 0.045f, c * 0.74f * strength);
                    }
                    final float lip = cov[(y - lift) * w + x] - c;
                    if (lip > 0f && a[i] > 0.5f) {
                        over(i, lipR, lipG, lipB, lip * 0.70f * strength);
                    }
                }
            }
        }

        /**
         * Light from the turn burst behind the ring: a warm wash over the plate that falls away from the
         * ring, and a hot line along the edges that face the burst. d is the diameter in canvas pixels;
         * flip is for the frame that hangs from the top, whose burst points down.
         */
        void ember(final float[] cov, final int width, final int soft, final float d, final boolean flip, final float wash, final float strength) {
            final float[] line = new float[w * h];
            final int step = flip ? -width : width;
            for (int y = width; y < h - width; y++) {
                for (int x = 0; x < w; x++) {
                    final int i = y * w + x;
                    line[i] = cov[i] * (1f - cov[(y - step) * w + x]);
                }
            }
            final float[] glow = boxBlur(line, w, h, soft, 1);
            final float hx = w * 0.5f, hy = h * 0.5f;
            for (int y = 0; y < h; y++) {
                final float dy = (y + 0.5f - hy) / d * (flip ? -1f : 1f);
                final float upper = smooth(0.10f, -0.12f, dy);
                for (int x = 0; x < w; x++) {
                    final int i = y * w + x;
                    final float c = cov[i];
                    if (c <= 0f) {
                        continue;
                    }
                    final float dx = (x + 0.5f - hx) / d;
                    final float reach = Math.max(0f, sqrtf(dx * dx + dy * dy) - 0.50f);
                    // Roughly exp(-2.4 r), cheap and close enough for a glow
                    final float q = 1f + reach * 0.80f;
                    final float fall = 1f / (q * q * q);
                    final float v = sat(Math.max(line[i], glow[i] * 0.7f) * fall * strength) * smooth(0.06f, -0.02f, dy);
                    if (v > 0.004f) {
                        over(i, 1f, 0.62f + 0.28f * fall * fall, 0.20f + 0.34f * fall * fall, v);
                    }
                    final float wa = wash * fall * upper * c;
                    if (wa > 0.004f) {
                        over(i, 1f, 0.50f, 0.13f, wa);
                    }
                }
            }
        }

        /**
         * A hard highlight along every edge of the shape that faces up in the image, whichever way the
         * frame is turned, broken here and there by the casting's surface.
         */
        void specular(final float[] cov, final int width, final int soft, final float cr, final float cg, final float cb, final float strength) {
            final float[] line = new float[w * h];
            for (int y = width; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    final int i = y * w + x;
                    line[i] = cov[i] * (1f - cov[(y - width) * w + x]);
                }
            }
            final float[] glow = boxBlur(line, w, h, soft, 1);
            final float[] t = tile();
            for (int i = 0; i < line.length; i++) {
                final float v = Math.max(line[i], glow[i] * 0.55f) * cov[i];
                if (v > 0.004f) {
                    final float broken = 0.72f + 0.42f * t[((i / w & TILE_MASK) << 9) | (i % w / 3 & TILE_MASK)];
                    over(i, cr, cg, cb, sat(v * strength * broken));
                }
            }
        }

        BufferedImage image() {
            final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            final int[] px = new int[w * h];
            for (int i = 0; i < px.length; i++) {
                final float al = a[i];
                if (al <= 0.002f) {
                    continue;
                }
                final float ia = 1f / al;
                px[i] = (clampByte(al * 255f + 0.5f) << 24)
                        | (clampByte(r[i] * ia * 255f + 0.5f) << 16)
                        | (clampByte(g[i] * ia * 255f + 0.5f) << 8)
                        | clampByte(b[i] * ia * 255f + 0.5f);
            }
            out.getRaster().setDataElements(0, 0, w, h, px);
            return out;
        }
    }

    private static double deviceScale(final Graphics2D g) {
        final AffineTransform t = g.getTransform();
        final double s = Math.max(Math.hypot(t.getScaleX(), t.getShearY()), Math.hypot(t.getScaleY(), t.getShearX()));
        return s <= 0d ? 1d : Math.max(1d, Math.min(4d, s));
    }

    private static Graphics2D smoothCopy(final Graphics2D g) {
        final Graphics2D c = (Graphics2D) g.create();
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        c.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return c;
    }

    // Portrait furniture

    /**
     * Draws the mount behind a circular portrait centred on (cx, cy): two low stepped wings reaching
     * 1.6 diameters either side, with a fine engraved line framing the plain metal the phase icons sit
     * on from 0.62 to 1.57 diameters out. Active adds a white hot burst hugging the ring and orange light
     * on the frame's own edges facing it, all of the burst inside 0.853 diameters either side and 1.2
     * above and below, so the caller's clip never cuts it. Opponent turns it over so it hangs from the top
     * edge. Compositing order: this frame, the portrait clipped to a circle, then
     * {@link #paintPortraitRing}, then {@link #paintLifePlate}.
     */
    public static void paintPortraitFrame(final Graphics2D g, final int cx, final int cy, final int diameter, final boolean active, final boolean opponent) {
        if (diameter <= 0) {
            return;
        }
        final Graphics2D c = smoothCopy(g);
        try {
            final double sc = deviceScale(g);
            final int d = Math.max(8, (int) Math.round(diameter * sc));
            if (active) {
                drawCentered(c, cached("burst" + d + opponent, () -> sunburst(d, opponent)), cx, cy, sc, 1d);
            }
            drawCentered(c, cached("frame" + d + active + opponent, () -> halved(frameImage(d * 2, active, opponent))), cx, cy, sc, 1d);
        } finally {
            c.dispose();
        }
    }

    private interface Maker {
        BufferedImage make();
    }

    /** Draws a cached image centred on (cx, cy); imageScale is how many device pixels one image pixel covers. */
    private static void drawCentered(final Graphics2D c, final BufferedImage img, final int cx, final int cy, final double deviceScale, final double imageScale) {
        if (deviceScale == 1d && imageScale == 1d) {
            c.drawImage(img, cx - img.getWidth() / 2, cy - img.getHeight() / 2, null);
            return;
        }
        final AffineTransform t = AffineTransform.getTranslateInstance(cx, cy);
        t.scale(imageScale / deviceScale, imageScale / deviceScale);
        t.translate(-img.getWidth() / 2d, -img.getHeight() / 2d);
        c.drawImage(img, t, null);
    }

    private static int even(final double v) {
        return ((int) Math.ceil(v) + 1) & ~1;
    }

    /**
     * Takes a 2x supersampled image down to its final size with bicubic filtering. It is done once and
     * the result cached, so a repaint is a plain blit rather than a software bicubic of the big image.
     */
    private static BufferedImage halved(final BufferedImage big) {
        final BufferedImage out = new BufferedImage(big.getWidth() / 2, big.getHeight() / 2, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(big, AffineTransform.getScaleInstance(0.5d, 0.5d), null);
        g.dispose();
        return out;
    }

    private static BufferedImage cached(final String key, final Maker maker) {
        BufferedImage img = CACHE.get(key);
        if (img == null) {
            img = premultiplied(maker.make());
            CACHE.put(key, img);
        }
        return img;
    }

    /** The same picture with its colours premultiplied, which lays over the screen a good deal faster in software. */
    private static BufferedImage premultiplied(final BufferedImage img) {
        if (img == null || img.getType() == BufferedImage.TYPE_INT_ARGB_PRE || !img.getColorModel().hasAlpha()) {
            return img;
        }
        final BufferedImage pre = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D g = pre.createGraphics();
        try {
            g.setComposite(java.awt.AlphaComposite.Src);
            g.drawImage(img, 0, 0, null);
        } finally {
            g.dispose();
        }
        return pre;
    }

    private static Path2D blade(final double x0, final double y0, final double c1x, final double c1y, final double c2x, final double c2y,
            final double tx, final double ty, final double c3x, final double c3y, final double c4x, final double c4y, final double x1, final double y1) {
        final Path2D p = new Path2D.Double();
        p.moveTo(x0, y0);
        p.curveTo(c1x, c1y, c2x, c2y, tx, ty);
        p.curveTo(c3x, c3y, c4x, c4y, x1, y1);
        p.closePath();
        return p;
    }

    private static Area both(final Shape right) {
        final Area a = new Area(right);
        a.add(new Area(AffineTransform.getScaleInstance(-1, 1).createTransformedShape(right)));
        return a;
    }

    // Cast steel in three tones, one for each plate, with brass only where wear has exposed it along the edges
    private static final float[] STEEL_ACTIVE = lin(0x524C42), STEEL_IDLE = lin(0x34373B);
    private static final float[] STEEL_TOP_ACTIVE = lin(0x786C56), STEEL_TOP_IDLE = lin(0x4A4D50);
    private static final float[] BRASS_ACTIVE = lin(0xE8BC62), BRASS_IDLE = lin(0x7E7C74);
    private static final float[] STEEL_DEEP = lin(0x2A2A2E), STEEL_BACK_IDLE = lin(0x212327);

    /** The shape shrunk by inset on all sides, so a plate stacked on another keeps an even step around it. */
    private static Area eroded(final Area a, final double inset) {
        final Area out = new Area(a);
        out.subtract(new Area(new BasicStroke((float) (inset * 2d), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4f).createStrokedShape(a)));
        return out;
    }

    /** A chevron cut pointing along the wing, apex at (x, y) leaning with the shoulder's slope. */
    private static Path2D chevron(final double x, final double y, final double back, final double hh, final double thick) {
        final double lean = 0.42;
        final Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x - back + hh * 0.30, y - back * lean - hh);
        p.lineTo(x - back + hh * 0.30 - thick, y - back * lean - hh + thick * 0.3);
        p.lineTo(x - thick, y - thick * lean);
        p.lineTo(x - back + hh * 0.30 - thick * 0.4, y - back * lean + hh - thick * 0.2);
        p.lineTo(x - back + hh * 0.30 + thick * 0.4, y - back * lean + hh);
        p.closePath();
        return p;
    }

    /** Draws the frame at d pixels to the diameter; callers pass twice the final size and halve the result. */
    private static BufferedImage frameImage(final int d, final boolean active, final boolean opponent) {
        // A multiple of four, so the centre stays on a whole pixel once this is halved
        final int W = (even(d * 3.5) + 2) & ~3, H = (even(d * 2.0) + 2) & ~3;
        final Canvas cv = new Canvas(W, H);
        final AffineTransform at = new AffineTransform();
        at.translate(W / 2d, H / 2d);
        at.scale(d, opponent ? -d : d);

        // The back plate: two low wings leaving the collar below its top, each stepped by two barbs and running out to a point at 1.6
        final Path2D backP = new Path2D.Double();
        backP.moveTo(0.30, -0.40);
        backP.curveTo(0.46, -0.345, 0.64, -0.285, 0.80, -0.215);
        backP.lineTo(0.852, -0.236);
        backP.lineTo(0.878, -0.160);
        backP.lineTo(1.19, -0.118);
        backP.lineTo(1.238, -0.134);
        backP.lineTo(1.262, -0.094);
        backP.lineTo(1.48, -0.066);
        backP.lineTo(1.565, -0.034);
        backP.lineTo(1.60, 0.0);
        backP.lineTo(1.565, 0.034);
        backP.lineTo(1.48, 0.064);
        backP.lineTo(1.22, 0.098);
        backP.lineTo(0.98, 0.140);
        backP.lineTo(0.92, 0.195);
        backP.curveTo(0.80, 0.285, 0.58, 0.395, 0.30, 0.47);
        backP.closePath();
        final Area back = both(backP);

        // The middle plate steps in evenly from the back one, and the top plate steps in again as shoulders
        final Area mid = eroded(back, 0.028);
        final Path2D shoulderP = new Path2D.Double();
        shoulderP.moveTo(0.30, -0.345);
        shoulderP.curveTo(0.46, -0.30, 0.60, -0.25, 0.765, -0.178);
        shoulderP.lineTo(0.70, -0.112);
        shoulderP.lineTo(0.30, -0.112);
        shoulderP.closePath();
        shoulderP.moveTo(0.30, 0.112);
        shoulderP.lineTo(0.72, 0.112);
        shoulderP.lineTo(0.80, 0.175);
        shoulderP.curveTo(0.70, 0.245, 0.52, 0.335, 0.30, 0.405);
        shoulderP.closePath();
        final Area top = both(shoulderP);
        top.intersect(mid);

        // The engraving: three chevrons stepping down each shoulder
        final Area ornamentR = new Area();
        ornamentR.add(new Area(chevron(0.640, -0.176, 0.040, 0.034, 0.0135)));
        ornamentR.add(new Area(chevron(0.682, -0.160, 0.032, 0.027, 0.0115)));
        ornamentR.add(new Area(chevron(0.718, -0.146, 0.024, 0.020, 0.0095)));
        final Area ornament = both(ornamentR);

        // One fine line cut into the metal itself, from each shoulder out to the tip, framing the plain run the glyphs sit on
        final Path2D scribeP = new Path2D.Double();
        scribeP.moveTo(0.705, -0.112);
        scribeP.lineTo(1.19, -0.092);
        scribeP.lineTo(1.47, -0.048);
        scribeP.lineTo(1.552, 0.0);
        scribeP.lineTo(1.47, 0.046);
        scribeP.lineTo(1.19, 0.078);
        scribeP.lineTo(0.725, 0.112);
        final Area scribe = both(new BasicStroke(0.0060f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(scribeP));

        // A low cradle under the portrait, tucked up behind the plates; its foot carries the life plate
        final Path2D cradleP = new Path2D.Double();
        cradleP.moveTo(-0.55, 0.12);
        cradleP.curveTo(-0.57, 0.44, -0.38, 0.72, 0, 0.74);
        cradleP.curveTo(0.38, 0.72, 0.57, 0.44, 0.55, 0.12);
        cradleP.closePath();
        final Area cradle = new Area(cradleP);
        final Area collar = new Area(new Ellipse2D.Double(-0.565, -0.565, 1.13, 1.13));

        final Area silhouette = new Area();
        silhouette.add(back);
        silhouette.add(cradle);
        silhouette.add(collar);

        final float[] steel = active ? STEEL_ACTIVE : STEEL_IDLE;
        final float[] steelTop = active ? STEEL_TOP_ACTIVE : STEEL_TOP_IDLE;
        final float[] steelBack = active ? STEEL_DEEP : STEEL_BACK_IDLE;
        final float[] brass = active ? BRASS_ACTIVE : BRASS_IDLE;
        final float micro = 0.030f;
        final float edge = Math.max(2.8f, d * 0.015f);
        // The contact shadow underneath: a tight dark line where the plate meets the floor inside a wide soft fall off
        final float[] silCov = cv.coverage(at.createTransformedShape(silhouette));
        cv.shadow(silCov, Math.max(3, Math.round(d * 0.046f)), 0, Math.round(d * 0.050f), 0.62f, Color.BLACK);
        cv.shadow(silCov, Math.max(2, Math.round(d * 0.012f)), Math.max(1, d / 180), Math.max(1, d / 80), 0.58f, Color.BLACK);

        final float[] backCov = cv.coverage(at.createTransformedShape(back));
        cv.forge(backCov, d * 0.030f, d * 0.08f, steelBack, brass, edge, d, micro);

        final float[] midCov = cv.coverage(at.createTransformedShape(mid));
        cv.shadow(midCov, Math.max(1, Math.round(d * 0.010f)), Math.max(1, d / 200), Math.max(1, d / 120), 0.66f, Color.BLACK);
        cv.forge(midCov, d * 0.026f, d * 0.09f, steel, brass, edge * 0.8f, d, micro);

        final float[] topCov = cv.coverage(at.createTransformedShape(top));
        cv.shadow(topCov, Math.max(1, Math.round(d * 0.010f)), Math.max(1, d / 200), Math.max(1, d / 120), 0.66f, Color.BLACK);
        cv.forge(topCov, d * 0.024f, d * 0.08f, steelTop, brass, edge * 0.7f, d, micro);

        cv.carve(cv.coverage(at.createTransformedShape(ornament)), Math.max(1, Math.round(d * 0.0045f)), brass, 1f);
        cv.carve(cv.coverage(at.createTransformedShape(scribe)), Math.max(1, Math.round(d * 0.0035f)), brass, 0.58f);

        // The specular run along the top of each wing, on the outer rim and again on the step inside it
        final float sr = active ? 1f : 0.86f, sg = active ? 0.93f : 0.89f, sb = active ? 0.72f : 0.92f;
        final int specW = Math.max(1, Math.round(d * 0.0075f)), specSoft = Math.max(1, Math.round(d * 0.006f));
        cv.specular(backCov, specW, specSoft, sr, sg, sb, active ? 0.92f : 0.66f);
        // Only the shoulders above the centre line: the long straight edge below it would read as a stray bright bar
        final float[] topUpper = topCov.clone();
        Arrays.fill(topUpper, (H / 2) * W, topUpper.length, 0f);
        cv.specular(topUpper, specW, specSoft, sr, sg, sb, active ? 0.62f : 0.40f);
        if (active) {
            cv.ember(backCov, specW * 2, specSoft * 2, d, opponent, 0.30f, 1.35f);
            cv.ember(topCov, specW * 2, specSoft * 2, d, opponent, 0f, 1.20f);
        }

        final float[] cradleCov = cv.coverage(at.createTransformedShape(cradle));
        cv.shadow(cradleCov, Math.max(1, Math.round(d * 0.018f)), 0, Math.max(1, d / 60), 0.60f, Color.BLACK);
        cv.forge(cradleCov, d * 0.060f, d * 0.22f, steel, brass, edge, d, micro);

        final float[] collarCov = cv.coverage(at.createTransformedShape(collar));
        cv.shadow(collarCov, Math.max(1, Math.round(d * 0.014f)), 0, 0, 0.58f, Color.BLACK);
        cv.forge(collarCov, d * 0.020f, d * 0.08f, STEEL_DEEP, brass, edge * 0.6f, d, micro);
        if (active) {
            cv.ember(collarCov, specW * 2, specSoft * 2, d, opponent, 0.22f, 1.25f);
        }
        return cv.image();
    }

    // The burst's reach from the portrait centre, in diameters. Straight up is limited by the avatar panel's clip at 1.2
    private static final float BURST_RING = 0.565f, BURST_SIDE = 0.80f, BURST_UP = 1.12f, BURST_DOWN = 0.86f;
    // Nothing of the burst passes these: 0.853 across keeps it under 12% of a 1920 window at d = 135
    private static final float BURST_MAX_SIDE = 0.853f, BURST_MAX_UP = 1.18f;
    // Each flare: degrees from straight up, reach, half width at the base, strength. Long and short alternate, thickest upwards
    private static final float[][] FLARES = {
        {-7f, 1.16f, 0.046f, 0.92f},
        {5f, 0.90f, 0.030f, 0.46f},
        {14f, 1.18f, 0.040f, 0.78f},
        {24f, 0.97f, 0.034f, 0.50f},
        {34f, 1.30f, 0.048f, 0.86f},
        {45f, 1.02f, 0.034f, 0.52f},
        {57f, 0.97f, 0.040f, 0.62f},
        {70f, 0.84f, 0.030f, 0.34f},
        {-16f, 0.98f, 0.032f, 0.50f},
        {-27f, 1.30f, 0.048f, 0.90f},
        {-37f, 0.94f, 0.030f, 0.42f},
        {-48f, 1.08f, 0.042f, 0.72f},
        {-62f, 0.93f, 0.034f, 0.42f},
        {-75f, 0.86f, 0.030f, 0.32f},
        {96f, 0.80f, 0.028f, 0.26f},
        {-100f, 0.80f, 0.028f, 0.26f},
        {126f, 0.78f, 0.026f, 0.20f},
        {-128f, 0.78f, 0.026f, 0.20f},
    };

    /**
     * The turn burst: a white core right at the ring, falling off fast through orange gold to nothing,
     * with eighteen soft flares of very different lengths standing out of it, mostly upwards. The alpha
     * reaches exactly zero well inside the image, so no bitmap or clip edge ever shows. It is plain alpha
     * over, in colours pure enough to read as added light: a real additive composite would read the
     * destination back pixel by pixel on an accelerated Swing surface.
     */
    private static BufferedImage sunburst(final int d, final boolean opponent) {
        final int W = even(d * 1.76f + 4f), H = even(d * 2.40f);
        final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        final int[] px = new int[W * H];
        final float ring = d * BURST_RING, side = d * BURST_SIDE, up = d * BURST_UP, down = d * BURST_DOWN;
        final float maxSide = d * BURST_MAX_SIDE, maxUp = d * BURST_MAX_UP;
        final float coreLen = d * 0.040f;
        final float halfW = W * 0.5f, halfH = H * 0.5f;
        final int nf = FLARES.length;
        final float[] fx = new float[nf], fy = new float[nf], fLen = new float[nf], fWid = new float[nf];
        for (int i = 0; i < nf; i++) {
            final double a = Math.toRadians(FLARES[i][0]);
            fx[i] = (float) Math.sin(a);
            fy[i] = (float) -Math.cos(a);
            fLen[i] = d * FLARES[i][1];
            fWid[i] = d * FLARES[i][2];
        }
        final float start = ring * 0.86f;
        final int spokes = 720;
        final float[] spoke = spokeTable(spokes, 0x7B2L, 26, 0.012f, 0.030f);
        for (int y = 0; y < H; y++) {
            final float dy = (y + 0.5f - halfH) * (opponent ? -1f : 1f);
            for (int x = 0; x < W; x++) {
                final float dx = x + 0.5f - halfW;
                final float r = sqrtf(dx * dx + dy * dy);
                final float nx = dx / Math.max(r, 0.5f), ny = dy / Math.max(r, 0.5f);
                final float qx = nx / side, qy = ny / (ny < 0f ? up : down);
                final float limit = 1f / sqrtf(qx * qx + qy * qy);
                final float t = sat((r - ring) / (limit - ring));
                float heat = (1f - t) * (1f - t) * (1f - t);
                // The soft glow is broken up around the circle, so it does not read as a drawn disc
                final int sp = Math.floorMod((int) ((atan2f(dy, dx) + 3.1415927f) / 6.2831855f * spokes), spokes);
                heat *= 0.62f + 0.62f * spoke[sp];
                for (int i = 0; i < nf; i++) {
                    final float along = dx * fx[i] + dy * fy[i];
                    if (along <= start || along >= fLen[i]) {
                        continue;
                    }
                    final float q = (along - start) / (fLen[i] - start);
                    final float across = Math.abs(dx * fy[i] - dy * fx[i]);
                    final float off = across / (fWid[i] * 1.32f * (1f - q * 0.60f) + 0.45f);
                    if (off < 3.2f) {
                        final float fade = (1f - q) * sqrtf(sqrtf(1f - q));
                        // A broad soft body with a thinner, hotter line inside it
                        final float f = FLARES[i][3] * fade * ((float) Math.exp(-off * off) * 0.74f + (float) Math.exp(-off * off * 6f) * 0.26f);
                        heat = 1f - (1f - heat) * (1f - f);
                    }
                }
                // The white core sits tight against the ring
                if (r >= ring * 0.9f) {
                    final float beyond = Math.max(0f, r - ring) / coreLen;
                    heat = Math.max(heat, 1f / (1f + beyond * beyond * beyond));
                } else {
                    heat = 1f;
                }
                // A safety envelope: whatever got this far has faded to nothing before any straight edge could show
                final float reachY = ny < 0f ? maxUp : maxUp * 0.92f;
                heat *= smooth(maxSide, maxSide * 0.84f, Math.abs(dx)) * smooth(reachY, reachY * 0.86f, Math.abs(dy));
                final float alpha = heat <= 0f ? 0f : 0.97f * (float) Math.pow(heat, 0.62);
                // Hue is set for every pixel, even fully clear ones, so bilinear scaling cannot leave a dark fringe
                final int gg, bb;
                if (heat > 0.70f) {
                    final float k = smooth(0.70f, 0.96f, heat);
                    gg = lerpC(0xDC, 0xFD, k);
                    bb = lerpC(0x88, 0xF2, k);
                } else if (heat > 0.26f) {
                    final float k = (heat - 0.26f) / 0.44f;
                    gg = lerpC(0xA6, 0xDC, k);
                    bb = lerpC(0x38, 0x88, k);
                } else {
                    final float k = heat / 0.26f;
                    gg = lerpC(0x70, 0xA6, k);
                    bb = lerpC(0x08, 0x38, k);
                }
                px[y * W + x] = (clampByte(alpha * 255f) << 24) | (0xFF << 16) | (gg << 8) | bb;
            }
        }
        img.getRaster().setDataElements(0, 0, W, H, px);
        return img;
    }

    /** Evenly spread spokes with jittered angle, width and strength; values 0 to 1 around the circle. */
    private static float[] spokeTable(final int n, final long seed, final int rays, final float minW, final float maxW) {
        final Random r = new Random(seed);
        final float[] t = new float[n];
        for (int i = 0; i < rays; i++) {
            final float at = (i + 0.5f + (r.nextFloat() - 0.5f) * 0.6f) / rays * n;
            final float width = n * (minW + r.nextFloat() * (maxW - minW));
            final float gain = 0.30f + r.nextFloat() * 0.70f;
            for (int k = 0; k < n; k++) {
                float dist = Math.abs(k - at);
                dist = Math.min(dist, n - dist);
                final float q = dist / width;
                if (q < 3f) {
                    t[k] = Math.max(t[k], gain * (float) Math.exp(-q * q));
                }
            }
        }
        return t;
    }

    /**
     * The medallion for the end of a game, drawn behind a round portrait of diameter d centred on
     * (cx, cy): a reeded ring inside a laurel wreath, with a spearhead crest above and a tied ribbon below.
     * Gold when victory, cold pewter otherwise. Everything stays inside about 0.95 diameters of the centre.
     * Supersampled once and cached, so it is safe to call on every repaint.
     */
    public static void paintTrophyFrame(final Graphics2D g, final int cx, final int cy, final int d, final boolean victory) {
        if (d <= 0) {
            return;
        }
        final Graphics2D c = smoothCopy(g);
        try {
            final double sc = deviceScale(g);
            final int dp = Math.max(8, (int) Math.round(d * sc));
            drawCentered(c, cached("trophy" + dp + victory, () -> halved(trophyImage(dp * 2, victory))), cx, cy, sc, 1d);
        } finally {
            c.dispose();
        }
    }

    /** One laurel leaf, its base at (x, y) pointing along the angle: pointed at both ends and fuller on one side. */
    private static Shape laurelLeaf(final double x, final double y, final double len, final double wid, final double angle) {
        final Path2D p = new Path2D.Double();
        p.moveTo(0, 0);
        p.curveTo(len * 0.22, -wid * 1.10, len * 0.66, -wid * 0.95, len, 0);
        p.curveTo(len * 0.62, wid * 0.72, len * 0.20, wid * 0.80, 0, 0);
        p.closePath();
        final AffineTransform t = AffineTransform.getTranslateInstance(x, y);
        t.rotate(angle);
        return t.createTransformedShape(p);
    }

    private static Path2D mirrored(final Path2D right) {
        final Path2D out = new Path2D.Double(Path2D.WIND_NON_ZERO);
        out.append(right, false);
        out.append(AffineTransform.getScaleInstance(-1, 1).createTransformedShape(right), false);
        return out;
    }

    /** Draws the trophy at d pixels to the diameter; callers pass twice the final size and halve the result. */
    private static BufferedImage trophyImage(final int d, final boolean victory) {
        final int S = (even(d * 2.1) + 2) & ~3;
        final Canvas cv = new Canvas(S, S);
        final AffineTransform at = AffineTransform.getTranslateInstance(S / 2d, S / 2d);
        at.scale(d, d);

        final float[] body = victory ? lin(0xB88E3C) : lin(0x666C72);
        final float[] deep = victory ? lin(0x6E5024) : lin(0x3C4046);
        final float[] worn = victory ? lin(0xFFE8AE) : lin(0xBAC2CA);
        final float sr = victory ? 1f : 0.80f, sg = victory ? 0.91f : 0.87f, sb = victory ? 0.64f : 0.95f;
        final float micro = 0.030f;
        final float edge = Math.max(2.8f, d * 0.012f);

        // The wreath, one side at a time: leaves in two rows along a stem, alternating over and under
        final Path2D under = new Path2D.Double(Path2D.WIND_NON_ZERO), over = new Path2D.Double(Path2D.WIND_NON_ZERO);
        final Path2D ribs = new Path2D.Double(), stemP = new Path2D.Double();
        final int n = 11;
        for (int i = 0; i < n; i++) {
            final double t = i / (double) (n - 1);
            final double a = Math.toRadians(168 - t * 146);
            final double rad = 0.742 - t * 0.022;
            final double x = Math.sin(a) * rad, y = -Math.cos(a) * rad;
            // The stem's direction, heading up towards the crest
            final double tang = Math.atan2(-Math.sin(a), -Math.cos(a));
            if (i == 0) {
                stemP.moveTo(x, y);
            } else {
                stemP.lineTo(x, y);
            }
            final double len = 0.235 - t * 0.085, wid = 0.060 - t * 0.022;
            final double outA = tang + 0.56, inA = tang - 0.50;
            (i % 2 == 0 ? over : under).append(laurelLeaf(x, y, len, wid, outA), false);
            (i % 2 == 0 ? under : over).append(laurelLeaf(x, y, len * 0.70, wid * 0.82, inA), false);
            ribs.moveTo(x + Math.cos(outA) * len * 0.10, y + Math.sin(outA) * len * 0.10);
            ribs.lineTo(x + Math.cos(outA) * len * 0.80, y + Math.sin(outA) * len * 0.80);
            ribs.moveTo(x + Math.cos(inA) * len * 0.08, y + Math.sin(inA) * len * 0.08);
            ribs.lineTo(x + Math.cos(inA) * len * 0.54, y + Math.sin(inA) * len * 0.54);
            if (i == n - 1) {
                // One last leaf finishes the branch, pointing at the crest
                over.append(laurelLeaf(x, y, len * 0.92, wid * 0.86, tang - 0.06), false);
            }
        }
        final Path2D underAll = mirrored(under), overAll = mirrored(over);
        final Shape stem = new BasicStroke(0.016f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(mirrored(stemP));
        final Shape rib = new BasicStroke(0.0060f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(mirrored(ribs));

        // The crest: a spearhead with a smaller blade leaning out to each side
        final Path2D flankR = poly(0.040f, -0.640f, 0.128f, -0.700f, 0.142f, -0.838f, 0.066f, -0.752f);
        final Path2D crestAll = mirrored(flankR);
        final Path2D spear = poly(0f, -0.945f, 0.058f, -0.762f, 0f, -0.610f, -0.058f, -0.762f);
        // The tied ribbon: two swallowtail ends and a knot where the stems meet
        final Path2D tailR = poly(0.020f, 0.700f, 0.120f, 0.732f, 0.246f, 0.868f, 0.178f, 0.862f, 0.170f, 0.930f, 0.060f, 0.806f);
        final Path2D tails = mirrored(tailR);
        final Path2D knot = poly(-0.098f, 0.712f, 0f, 0.662f, 0.098f, 0.712f, 0f, 0.778f);

        final Shape ring = new Ellipse2D.Double(-0.628, -0.628, 1.256, 1.256);
        final Shape bezel = new Ellipse2D.Double(-0.532, -0.532, 1.064, 1.064);

        // Reeding around the ring, like the milled edge of a coin
        final Path2D reeds = new Path2D.Double();
        final int cuts = 64;
        for (int i = 0; i < cuts; i++) {
            final double a = i * Math.PI * 2 / cuts;
            final double sa = Math.sin(a), ca = Math.cos(a), hw = 0.0058;
            reeds.moveTo(sa * 0.568 - ca * hw, -ca * 0.568 - sa * hw);
            reeds.lineTo(sa * 0.606 - ca * hw, -ca * 0.606 - sa * hw);
            reeds.lineTo(sa * 0.606 + ca * hw, -ca * 0.606 + sa * hw);
            reeds.lineTo(sa * 0.568 + ca * hw, -ca * 0.568 + sa * hw);
            reeds.closePath();
        }

        final Path2D silhouette = new Path2D.Double(Path2D.WIND_NON_ZERO);
        silhouette.append(underAll, false);
        silhouette.append(overAll, false);
        silhouette.append(crestAll, false);
        silhouette.append(spear, false);
        silhouette.append(tails, false);
        silhouette.append(knot, false);
        silhouette.append(ring, false);
        final float[] silCov = cv.coverage(at.createTransformedShape(silhouette));
        if (victory) {
            cv.shadow(silCov, Math.max(3, Math.round(d * 0.030f)), 0, 0, 0.38f, new Color(0xFFBE5A));
        }
        cv.shadow(silCov, Math.max(3, Math.round(d * 0.020f)), 0, Math.round(d * 0.022f), 0.60f, Color.BLACK);
        cv.shadow(silCov, Math.max(1, Math.round(d * 0.006f)), Math.max(1, d / 240), Math.max(1, d / 160), 0.52f, Color.BLACK);

        final int leafScale = Math.round(d * 0.30f);
        final int stepBlur = Math.max(1, Math.round(d * 0.006f)), stepOff = Math.max(1, d / 220);
        final float[] tailCov = cv.coverage(at.createTransformedShape(tails));
        cv.forge(tailCov, d * 0.012f, d * 0.04f, deep, worn, edge * 0.8f, leafScale, micro);

        final float[] underCov = cv.coverage(at.createTransformedShape(underAll));
        cv.forge(underCov, d * 0.012f, d * 0.04f, deep, worn, edge * 0.7f, leafScale, micro);
        final float[] stemCov = cv.coverage(at.createTransformedShape(stem));
        cv.forge(stemCov, d * 0.006f, d * 0.012f, deep, worn, edge * 0.5f, leafScale, micro);
        final float[] overCov = cv.coverage(at.createTransformedShape(overAll));
        cv.shadow(overCov, stepBlur, stepOff, stepOff * 2, 0.62f, Color.BLACK);
        cv.forge(overCov, d * 0.014f, d * 0.045f, body, worn, edge * 0.8f, leafScale, micro);
        cv.carve(cv.coverage(at.createTransformedShape(rib)), Math.max(1, Math.round(d * 0.0030f)), worn, 0.62f);

        final float[] crestCov = cv.coverage(at.createTransformedShape(crestAll));
        cv.shadow(crestCov, stepBlur, stepOff, stepOff * 2, 0.50f, Color.BLACK);
        cv.forge(crestCov, d * 0.012f, d * 0.04f, deep, worn, edge * 0.7f, leafScale, micro);
        final float[] spearCov = cv.coverage(at.createTransformedShape(spear));
        cv.shadow(spearCov, stepBlur, stepOff, stepOff * 2, 0.62f, Color.BLACK);
        cv.forge(spearCov, d * 0.030f, d * 0.05f, body, worn, edge, leafScale, micro);

        final float[] knotCov = cv.coverage(at.createTransformedShape(knot));
        cv.shadow(knotCov, stepBlur, stepOff, stepOff * 2, 0.60f, Color.BLACK);
        cv.forge(knotCov, d * 0.022f, d * 0.05f, body, worn, edge, leafScale, micro);

        final float[] ringCov = cv.coverage(at.createTransformedShape(ring));
        cv.shadow(ringCov, Math.max(2, Math.round(d * 0.012f)), stepOff, stepOff * 3, 0.66f, Color.BLACK);
        cv.forge(ringCov, d * 0.028f, d * 0.10f, body, worn, edge, d, micro);
        cv.carve(cv.coverage(at.createTransformedShape(reeds)), Math.max(1, Math.round(d * 0.0036f)), worn, 0.66f);

        final float[] bezelCov = cv.coverage(at.createTransformedShape(bezel));
        cv.shadow(bezelCov, stepBlur, 0, stepOff, 0.60f, Color.BLACK);
        cv.forge(bezelCov, d * 0.012f, d * 0.03f, deep, worn, edge * 0.6f, d, micro);

        final int specW = Math.max(1, Math.round(d * 0.0060f)), specSoft = Math.max(1, Math.round(d * 0.0050f));
        final float gleam = victory ? 0.82f : 0.52f;
        cv.specular(ringCov, specW, specSoft, sr, sg, sb, gleam);
        cv.specular(overCov, specW, specSoft, sr, sg, sb, gleam * 0.80f);
        cv.specular(spearCov, specW, specSoft, sr, sg, sb, gleam);
        cv.specular(knotCov, specW, specSoft, sr, sg, sb, gleam * 0.80f);
        return cv.image();
    }

    /**
     * A small floating crystal shard centred on (cx, cy), a faceted angular sliver with a bright core and a
     * soft glow. size is its length from tip to tail, angle is in radians clockwise with 0 pointing up, and
     * the colour's alpha fades the whole shard, so it can be animated without touching the cache.
     */
    public static void paintShard(final Graphics2D g, final float cx, final float cy, final float size, final float angle, final Color colour) {
        if (size <= 0f) {
            return;
        }
        final Color col = colour == null ? new Color(0x8FD8FF) : colour;
        final Graphics2D c = smoothCopy(g);
        try {
            final double sc = deviceScale(g);
            // Sizes share images in steps of four device pixels; the small remainder is taken up by scaling
            final int px = Math.max(8, Math.min(256, (Math.round((float) (size * sc)) + 3) & ~3));
            final int rgb = col.getRGB() & 0xFFFFFF;
            final BufferedImage img = cached("shard" + px + "_" + rgb, () -> halved(halved(shardImage(px * 4, new Color(rgb)))));
            if (col.getAlpha() < 255) {
                c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, col.getAlpha() / 255f));
            }
            final double k = size / (double) px;
            final AffineTransform t = AffineTransform.getTranslateInstance(cx, cy);
            t.rotate(angle);
            t.scale(k, k);
            t.translate(-img.getWidth() / 2d, -img.getHeight() / 2d);
            c.drawImage(img, t, null);
        } finally {
            c.dispose();
        }
    }

    /** The shard at L pixels long, tip up, on a square canvas big enough for its glow at any turn. */
    private static BufferedImage shardImage(final int L, final Color col) {
        final int S = ((int) Math.ceil(L * 1.5) + 3) & ~3;
        final BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        final float cr = col.getRed() / 255f, cg = col.getGreen() / 255f, cb = col.getBlue() / 255f;
        final float hs = S * 0.5f;
        // The glow first: an elongated falloff that is exactly zero well inside the canvas
        final int[] px = new int[S * S];
        final int grgb = (col.getRed() << 16) | (col.getGreen() << 8) | col.getBlue();
        for (int y = 0; y < S; y++) {
            final float qy = (y + 0.5f - hs) / (L * 0.72f);
            for (int x = 0; x < S; x++) {
                final float qx = (x + 0.5f - hs) / (L * 0.42f);
                final float q = sat(1f - sqrtf(qx * qx + qy * qy));
                px[y * S + x] = (clampByte(q * q * 0.85f * 255f) << 24) | grgb;
            }
        }
        img.getRaster().setDataElements(0, 0, S, S, px);

        final Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(hs, hs);
        g.scale(L, L);
        // An uneven sliver: the tip off centre, one shoulder higher than the other, a slanted tail
        final float tx = 0.045f, ty = -0.50f, rx = 0.118f, ry = -0.13f, r2x = 0.078f, r2y = 0.20f;
        final float bx = -0.030f, by = 0.50f, l2x = -0.098f, l2y = 0.10f, lx = -0.122f, ly = -0.21f;
        final float mx = -0.012f, my = -0.05f;
        final Path2D outline = poly(tx, ty, rx, ry, r2x, r2y, bx, by, l2x, l2y, lx, ly);
        // Facets lit from the upper left, every one a tint of the shard's own colour
        facet(g, cr, cg, cb, 0.82f, 1f, tx, ty, lx, ly, mx, my);
        facet(g, cr, cg, cb, 0.40f, 1f, lx, ly, l2x, l2y, bx, by, mx, my);
        facet(g, cr, cg, cb, 0.56f, 1f, tx, ty, rx, ry, mx, my);
        facet(g, cr, cg, cb, 0f, 0.70f, rx, ry, r2x, r2y, bx, by, mx, my);
        // The bright core inside, clipped to the crystal
        final Shape oldClip = g.getClip();
        g.clip(outline);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(mx, my), 0.30f, new float[] {0f, 0.34f, 1f},
                new Color[] {new Color(1f, 1f, 1f, 0.96f), new Color(1f, 1f, 1f, 0.60f), new Color(1f, 1f, 1f, 0f)}));
        g.fill(new Rectangle2D.Float(-0.5f, -0.6f, 1f, 1.2f));
        g.setClip(oldClip);
        // One hot edge down the ridge that faces the light
        g.setStroke(new BasicStroke(0.012f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(1f, 1f, 1f, 0.88f));
        g.draw(new Line2D.Float(tx, ty + 0.03f, mx, my));
        g.dispose();
        return img;
    }

    /** Fills a polygon in the colour taken towards white by tint and multiplied by value. */
    private static void facet(final Graphics2D g, final float r, final float gr, final float b, final float tint, final float value, final float... pts) {
        g.setColor(new Color(sat(mix(r, 1f, tint) * value), sat(mix(gr, 1f, tint) * value), sat(mix(b, 1f, tint) * value), 0.94f));
        g.fill(poly(pts));
    }

    /**
     * Draws a matte metal ring over the portrait. A null accent is plain metal; otherwise the ring
     * glows in that colour (selectable, chosen, hover).
     */
    public static void paintPortraitRing(final Graphics2D g, final int cx, final int cy, final int diameter, final Color accent) {
        if (diameter <= 0) {
            return;
        }
        final Graphics2D c = smoothCopy(g);
        try {
            final double sc = deviceScale(g);
            final int d = Math.max(8, (int) Math.round(diameter * sc));
            drawCentered(c, cached("ring" + d + (accent == null ? "" : Integer.toHexString(accent.getRGB())), () -> ringImage(d, accent)), cx, cy, sc, 1d);
        } finally {
            c.dispose();
        }
    }

    private static final float[] RING_METAL = lin(0x8E7E5C);

    private static BufferedImage ringImage(final int d, final Color accent) {
        final float width = Math.max(3f, d * 0.074f);
        final float rc = d * 0.5f + width * 0.12f;
        final float glowR = accent == null ? d * 0.03f : d * 0.20f;
        final int size = even((rc + width + glowR) * 2f + 4f);
        final BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final int[] px = new int[size * size];
        final float half = size / 2f;
        final float ro = rc + width * 0.5f, ri = rc - width * 0.5f;
        final float shadowW = Math.max(2f, d * 0.06f);
        final float ar = accent == null ? 0f : accent.getRed() / 255f;
        final float ag = accent == null ? 0f : accent.getGreen() / 255f;
        final float ab = accent == null ? 0f : accent.getBlue() / 255f;
        final float[] t = tile();
        final float[] rgb = new float[3];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                final float dx = x + 0.5f - half, dy = y + 0.5f - half;
                final float r = sqrtf(dx * dx + dy * dy);
                final float ndx = dx / Math.max(r, 0.001f), ndy = dy / Math.max(r, 0.001f);
                float a = 0f, cr = 0f, cg = 0f, cb = 0f;
                // Glow or contact shadow outside the ring
                if (r > ro - 1f) {
                    final float q = sat((r - ro) / glowR);
                    if (accent != null) {
                        a = (1f - q) * (1f - q) * 0.92f;
                        cr = ar;
                        cg = ag;
                        cb = ab;
                    } else {
                        a = (1f - q) * (1f - q) * 0.50f;
                    }
                }
                // Inner shadow falling on the portrait
                if (r < ri + 1f) {
                    final float q = sat((ri - r) / shadowW);
                    a = (1f - q) * (1f - q) * 0.58f;
                    cr = 0f;
                    cg = 0f;
                    cb = 0f;
                }
                final float tr = (r - rc) / (width * 0.5f);
                if (Math.abs(tr) < 1f + 2f / width) {
                    final float cov = sat((1f - Math.abs(tr)) * width * 0.5f + 0.5f);
                    // A fat outer torus with a narrow step down onto the portrait
                    final float local = tr > -0.40f ? (tr - 0.30f) / 0.70f : (tr + 0.70f) / 0.30f;
                    final float rad = Math.max(-0.96f, Math.min(0.96f, local));
                    final float nz = sqrtf(1f - rad * rad);
                    final float jx = t[(((y * 2) & TILE_MASK) << 9) | ((x * 2) & TILE_MASK)] * 0.05f;
                    final float jy = t[(((y * 2 + 77) & TILE_MASK) << 9) | ((x * 2 + 31) & TILE_MASK)] * 0.05f;
                    litMetal(rad * ndx + jx, rad * ndy + jy, nz, RING_METAL, 0.22f, 0.34f, rgb);
                    final float groove = Math.abs(tr + 0.40f) < 0.09f ? 0.45f : 1f;
                    float mr = rgb[0] * groove, mg = rgb[1] * groove, mb = rgb[2] * groove;
                    if (accent != null) {
                        mr = mix(mr, Math.min(1f, ar * (0.35f + mr)), 0.42f);
                        mg = mix(mg, Math.min(1f, ag * (0.35f + mg)), 0.42f);
                        mb = mix(mb, Math.min(1f, ab * (0.35f + mb)), 0.42f);
                    }
                    final float na = cov + a * (1f - cov);
                    if (na > 0f) {
                        cr = (cr * a * (1f - cov) + mr * cov) / na;
                        cg = (cg * a * (1f - cov) + mg * cov) / na;
                        cb = (cb * a * (1f - cov) + mb * cov) / na;
                    }
                    a = na;
                }
                if (a > 0.004f) {
                    px[y * size + x] = (clampByte(a * 255f) << 24) | (clampByte(cr * 255f) << 16) | (clampByte(cg * 255f) << 8) | clampByte(cb * 255f);
                }
            }
        }
        img.getRaster().setDataElements(0, 0, size, size, px);
        return img;
    }

    /**
     * Dark plate with a metal rim centred at (cx, cy), about 1.5 times its height wide, with the number
     * centred on it. Values of 5 or lower turn red.
     */
    public static void paintLifePlate(final Graphics2D g, final int cx, final int cy, final int height, final String life, final Font font) {
        if (height <= 0) {
            return;
        }
        final Graphics2D c = smoothCopy(g);
        try {
            final double sc = deviceScale(g);
            final int hpx = Math.max(10, (int) Math.round(height * sc));
            drawCentered(c, cached("plate" + hpx, () -> plateImage(hpx)), cx, cy, sc, 1d);

            if (life != null && !life.isEmpty()) {
                c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                c.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                Font f = font != null ? font : c.getFont();
                FontMetrics fm = c.getFontMetrics(f);
                final float maxW = height * 1.5f * 0.70f;
                final int tw = fm.stringWidth(life);
                if (tw > maxW) {
                    f = f.deriveFont(f.getSize2D() * maxW / tw);
                    fm = c.getFontMetrics(f);
                }
                c.setFont(f);
                final float tx = cx - fm.stringWidth(life) / 2f;
                final float ty = cy + (fm.getAscent() - fm.getDescent()) / 2f;
                final float sh = Math.max(1f, height / 26f);
                c.setColor(new Color(0, 0, 0, 70));
                c.drawString(life, tx - sh, ty + sh);
                c.drawString(life, tx + sh, ty + sh);
                c.drawString(life, tx, ty + sh * 2f);
                c.setColor(new Color(0, 0, 0, 140));
                c.drawString(life, tx, ty + sh);
                final boolean low = isLow(life);
                c.setPaint(low ? new GradientPaint(0, ty - fm.getAscent(), new Color(0xFF8A7A), 0, ty, new Color(0xF0352E))
                        : new GradientPaint(0, ty - fm.getAscent(), Color.WHITE, 0, ty, new Color(0xE9E2D2)));
                c.drawString(life, tx, ty);
            }
        } finally {
            c.dispose();
        }
    }

    private static boolean isLow(final String life) {
        try {
            return Integer.parseInt(life.trim()) <= 5;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    private static BufferedImage plateImage(final int hpx) {
        final float pw = hpx * 1.5f;
        final int W = even(pw + hpx * 0.3f), H = even(hpx * 1.3f);
        final Canvas cv = new Canvas(W, H);
        final AffineTransform at = new AffineTransform();
        at.translate(W / 2d, H / 2d);
        at.scale(hpx, hpx);
        final Shape outer = at.createTransformedShape(lozenge(0.75, 0.50));
        final Shape inner = at.createTransformedShape(lozenge(0.67, 0.42));
        final float[] covO = cv.coverage(outer), covI = cv.coverage(inner);
        cv.shadow(covO, Math.max(1, hpx / 12), 0, Math.max(1, hpx / 16), 0.8f, Color.BLACK);
        cv.forge(covO, Math.max(1.5f, hpx * 0.07f), hpx * 0.2f, STEEL_IDLE, RING_METAL, Math.max(1.2f, hpx * 0.05f), hpx * 3, 0.03f);
        cv.shadow(covI, Math.max(1, hpx / 28), 0, 0, 0.8f, Color.BLACK);
        for (int y = 0; y < H; y++) {
            final float t = y / (float) H;
            for (int x = 0; x < W; x++) {
                final int i = y * W + x;
                if (covI[i] > 0f) {
                    final float dx = (x - W / 2f) / W, dy = t - 0.42f;
                    final float gl = sat(1f - sqrtf(dx * dx * 2.4f + dy * dy * 2.4f) * 1.6f);
                    cv.over(i, 0.045f + gl * 0.075f, 0.050f + gl * 0.085f, 0.060f + gl * 0.10f, covI[i]);
                }
            }
        }
        return cv.image();
    }

    /** Elongated hexagon with softened corners. */
    private static Path2D lozenge(final double hw, final double hh) {
        final double sh = hw * 0.58;
        final double k = 0.12;
        final Path2D p = new Path2D.Double();
        p.moveTo(-hw, 0);
        p.curveTo(-hw + (hw - sh) * 0.35, -hh * 0.5, -sh - (hw - sh) * 0.1, -hh * 0.92, -sh + k * hw * 0.3, -hh);
        p.lineTo(sh - k * hw * 0.3, -hh);
        p.curveTo(sh + (hw - sh) * 0.1, -hh * 0.92, hw - (hw - sh) * 0.35, -hh * 0.5, hw, 0);
        p.curveTo(hw - (hw - sh) * 0.35, hh * 0.5, sh + (hw - sh) * 0.1, hh * 0.92, sh - k * hw * 0.3, hh);
        p.lineTo(-sh + k * hw * 0.3, hh);
        p.curveTo(-sh - (hw - sh) * 0.1, hh * 0.92, -hw + (hw - sh) * 0.35, hh * 0.5, -hw, 0);
        p.closePath();
        return p;
    }

    // Phase glyphs

    private static final Shape[] GLYPHS = {diamondGlyph(), cardGlyph(), swordsGlyph(false), shieldGlyph()};
    private static final Shape BOLD_SWORDS = swordsGlyph(true);

    private static Shape diamondGlyph() {
        return poly(0.5f, 0.10f, 0.90f, 0.5f, 0.5f, 0.90f, 0.10f, 0.5f);
    }

    private static Shape cardGlyph() {
        return new RoundRectangle2D.Float(0.17f, 0f, 0.66f, 1f, 0.17f, 0.17f);
    }

    private static Shape swordsGlyph(final boolean bold) {
        final Area out = new Area();
        final AffineTransform r1 = AffineTransform.getRotateInstance(Math.toRadians(bold ? 45 : 42), 0.5, 0.5);
        final AffineTransform r2 = AffineTransform.getRotateInstance(Math.toRadians(bold ? -45 : -42), 0.5, 0.5);
        final float bw = bold ? 0.098f : 0.074f;
        final Area sword = new Area(poly(0.5f, -0.10f, 0.5f + bw, 0.08f, 0.5f + bw, 0.70f, 0.5f - bw, 0.70f, 0.5f - bw, 0.08f));
        sword.add(new Area(new RoundRectangle2D.Float(bold ? 0.29f : 0.26f, 0.67f, bold ? 0.42f : 0.48f, bold ? 0.11f : 0.09f, 0.04f, 0.04f)));
        sword.add(new Area(new Rectangle2D.Float(0.455f, 0.74f, 0.09f, 0.22f)));
        sword.add(new Area(new Ellipse2D.Float(0.43f, 0.93f, 0.14f, 0.14f)));
        out.add(new Area(r1.createTransformedShape(sword)));
        out.add(new Area(r2.createTransformedShape(sword)));
        return out;
    }

    private static Shape shieldGlyph() {
        final Path2D p = new Path2D.Float();
        p.moveTo(0.5f, 0f);
        p.curveTo(0.62f, 0.09f, 0.78f, 0.12f, 0.92f, 0.12f);
        p.curveTo(0.94f, 0.50f, 0.82f, 0.80f, 0.5f, 1f);
        p.curveTo(0.18f, 0.80f, 0.06f, 0.50f, 0.08f, 0.12f);
        p.curveTo(0.22f, 0.12f, 0.38f, 0.09f, 0.5f, 0f);
        p.closePath();
        return p;
    }

    /**
     * Draws a phase icon about 60% of the size box at (x, y), so its bloom has room: kind is 0 small
     * diamond, 1 card pip, 2 crossed swords, 3 shield. Lit is the current phase, with a candle glow
     * that fades to nothing inside the box. Otherwise the pip, swords and shield are about 70% white
     * and the small diamonds dimmer, filled when a stop is set and hollow when not, with a dark edge so
     * they read on both dark metal and grey stone.
     */
    public static void paintPhaseGlyph(final Graphics2D g, final int x, final int y, final int size, final int kind, final boolean lit, final boolean stopEnabled, final boolean hover) {
        if (size <= 0) {
            return;
        }
        final Graphics2D c = smoothCopy(g);
        try {
            final float cx = x + size * 0.5f, cy = y + size * 0.5f;
            final float gs = size * 0.60f;
            final AffineTransform at = new AffineTransform();
            at.translate(cx - gs * 0.5f, cy - gs * 0.5f);
            at.scale(gs, gs);
            final int k = Math.floorMod(kind, GLYPHS.length);
            final Shape s = at.createTransformedShape(k == 2 && (gs < 14f || lit) ? BOLD_SWORDS : GLYPHS[k]);
            final float line = Math.max(0.8f, size / 26f);
            final float bloom = size * 0.5f - 1.5f;

            if (lit) {
                c.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), bloom, new float[] {0f, 0.42f, 0.74f, 1f},
                        new Color[] {new Color(255, 206, 120, 255), new Color(255, 176, 76, 225), new Color(255, 144, 44, 92), new Color(255, 124, 30, 0)}));
                c.fill(new Ellipse2D.Float(cx - bloom, cy - bloom, bloom * 2f, bloom * 2f));
                if (k != 2) {
                    c.setStroke(new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    c.setColor(new Color(92, 48, 8, 150));
                    c.draw(s);
                }
                c.setPaint(new GradientPaint(0, cy - gs * 0.5f, new Color(0xFFFFFA), 0, cy + gs * 0.5f, new Color(0xFFE9B0)));
                c.fill(s);
                return;
            }
            if (hover) {
                c.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), bloom, new float[] {0f, 1f},
                        new Color[] {new Color(230, 238, 255, 90), new Color(230, 238, 255, 0)}));
                c.fill(new Ellipse2D.Float(cx - bloom, cy - bloom, bloom * 2f, bloom * 2f));
            }
            final boolean major = k != 0;
            final int fillA = hover ? 225 : major ? (stopEnabled ? 178 : 52) : (stopEnabled ? 112 : 40);
            final int edgeA = hover ? 235 : major ? 178 : (stopEnabled ? 96 : 110);
            // Dark edge outside, pale body inside; between them they read on any ground the board has
            c.setStroke(new BasicStroke(line * 1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            c.setColor(new Color(0, 0, 0, hover ? 150 : major ? 120 : 88));
            c.draw(s);
            final int pale = major ? 0xFFFEFA : 0xF0ECE0;
            c.setColor(new Color((pale >> 16) & 255, (pale >> 8) & 255, pale & 255, fillA));
            c.fill(s);
            c.setStroke(new BasicStroke(line * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            c.setColor(new Color((pale >> 16) & 255, (pale >> 8) & 255, pale & 255, edgeA));
            c.draw(s);
        } finally {
            c.dispose();
        }
    }

    // Card back

    /**
     * Paints an original card back into the box at (x, y): a deep indigo field, a bronze inner border
     * and an engraved compass star echoing the floor medallion. It is drawn at 2x, once per size, and
     * taken down with bicubic filtering, so it stays crisp however large or small the pile is.
     */
    public static void paintCardBack(final Graphics2D g, final int x, final int y, final int w, final int h, final int arc) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final Graphics2D c = smoothCopy(g);
        try {
            final double sc = deviceScale(g);
            final int pw = Math.max(1, (int) Math.round(w * sc)), ph = Math.max(1, (int) Math.round(h * sc));
            final int pa = Math.max(0, (int) Math.round(arc * sc));
            final BufferedImage img = cached("cardback" + pw + "x" + ph + "r" + pa, () -> halved(cardBackImage(pw * 2, ph * 2, pa * 2)));
            if (pw == w && ph == h) {
                c.drawImage(img, x, y, null);
            } else {
                final AffineTransform t = AffineTransform.getTranslateInstance(x, y);
                t.scale(w / (double) pw, h / (double) ph);
                c.drawImage(img, t, null);
            }
        } finally {
            c.dispose();
        }
    }

    private static Color bronze(final float t) {
        return new Color(lerpC(0x6A, 0xEC, t), lerpC(0x48, 0xC8, t), lerpC(0x24, 0x88, t));
    }

    /** One facet of the star, toned by how squarely it faces the light in the top left. */
    private static void starFacet(final Graphics2D g, final float x0, final float y0, final float x1, final float y1, final float x2, final float y2) {
        final float ex = x2 - x1, ey = y2 - y1;
        final float len = Math.max(0.001f, sqrtf(ex * ex + ey * ey));
        // The facet's outward normal in the plane, turned to point away from the star's centre
        float nx = ey / len, ny = -ex / len;
        if (nx * (x1 - x0) + ny * (y1 - y0) < 0f) {
            nx = -nx;
            ny = -ny;
        }
        g.setColor(bronze(sat(0.46f + 0.54f * (nx * -0.60f + ny * -0.80f))));
        g.fill(poly(x0, y0, x1, y1, x2, y2));
    }

    private static BufferedImage cardBackImage(final int W, final int H, final int arc) {
        final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            final float u = Math.min(W, H), cx = W * 0.5f, cy = H * 0.5f;
            final float cap = Math.max(0f, arc);

            // The card's own dark edge, then the field inside it
            g.setColor(new Color(0x0A0B18));
            g.fill(new RoundRectangle2D.Float(0f, 0f, W, H, cap, cap));
            final float e1 = Math.max(1f, u * 0.028f);
            final Shape field = new RoundRectangle2D.Float(e1, e1, W - e1 * 2f, H - e1 * 2f, Math.max(0f, cap - e1 * 2f), Math.max(0f, cap - e1 * 2f));
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), Math.max(W, H) * 0.62f, new float[] {0f, 0.52f, 1f},
                    new Color[] {new Color(0x2C3082), new Color(0x1B1E5A), new Color(0x0F1138)}));
            g.fill(field);

            // Fine engraved rays across the field, faint enough to read as a texture
            final Shape oldClip = g.getClip();
            final float e2 = u * 0.086f;
            final Shape inner = new RoundRectangle2D.Float(e2, e2, W - e2 * 2f, H - e2 * 2f, Math.max(0f, cap - e2 * 1.2f), Math.max(0f, cap - e2 * 1.2f));
            g.clip(inner);
            final float reach = (W + H) * 0.6f;
            if (u > 90f) {
                g.setStroke(new BasicStroke(Math.max(0.9f, u * 0.0028f)));
                g.setColor(new Color(150, 160, 240, 34));
                for (int i = 0; i < 96; i++) {
                    final double a = i * Math.PI / 48d;
                    g.draw(new Line2D.Float(cx, cy, cx + (float) Math.cos(a) * reach, cy + (float) Math.sin(a) * reach));
                }
            }
            g.setClip(oldClip);

            // The bronze inner border: a heavy band lit from the top left, a dark cut outside it and a fine line within
            g.setStroke(new BasicStroke(Math.max(1.6f, u * 0.024f)));
            g.setColor(new Color(0, 0, 0, 120));
            g.draw(AffineTransform.getTranslateInstance(u * 0.004f, u * 0.006f).createTransformedShape(inner));
            g.setPaint(new GradientPaint(0f, 0f, new Color(0xEAC688), W, H, new Color(0x7A5428)));
            g.draw(inner);
            final float e3 = u * 0.118f;
            final Shape hair = new RoundRectangle2D.Float(e3, e3, W - e3 * 2f, H - e3 * 2f, Math.max(0f, cap - e3 * 1.2f), Math.max(0f, cap - e3 * 1.2f));
            g.setStroke(new BasicStroke(Math.max(0.9f, u * 0.0065f)));
            g.setPaint(new GradientPaint(0f, 0f, new Color(0xB89458), W, H, new Color(0x5E4220)));
            g.draw(hair);

            // Small lozenges closing each corner of the border
            final float ins = u * 0.168f, lo = u * 0.030f;
            final float[][] corners = {{ins, ins}, {W - ins, ins}, {ins, H - ins}, {W - ins, H - ins}};
            for (final float[] p : corners) {
                g.setColor(bronze(0.62f));
                g.fill(poly(p[0], p[1] - lo, p[0] + lo, p[1], p[0], p[1] + lo, p[0] - lo, p[1]));
            }

            // Slender spears running out of the star to the top and bottom of the border, ending in the same lozenge
            final float R = u * 0.355f;
            final float needle = H * 0.5f - u * 0.170f;
            if (needle > R) {
                final float nw = u * 0.0125f;
                g.setPaint(new GradientPaint(0f, cy - needle, bronze(0.82f), 0f, cy + needle, bronze(0.25f)));
                g.fill(poly(cx, cy - needle, cx + nw, cy - R, cx, cy - R * 0.8f, cx - nw, cy - R));
                g.fill(poly(cx, cy + needle, cx + nw, cy + R, cx, cy + R * 0.8f, cx - nw, cy + R));
                g.setColor(bronze(0.62f));
                g.fill(poly(cx, cy - needle - lo * 0.9f, cx + lo * 0.7f, cy - needle, cx, cy - needle + lo * 0.9f, cx - lo * 0.7f, cy - needle));
                g.fill(poly(cx, cy + needle - lo * 0.9f, cx + lo * 0.7f, cy + needle, cx, cy + needle + lo * 0.9f, cx - lo * 0.7f, cy + needle));
            }

            // A dark seat for the medallion so its lines stand off the field
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), R * 1.12f, new float[] {0f, 0.8f, 1f},
                    new Color[] {new Color(10, 11, 38, 170), new Color(10, 11, 38, 130), new Color(10, 11, 38, 0)}));
            g.fill(new Ellipse2D.Float(cx - R * 1.12f, cy - R * 1.12f, R * 2.24f, R * 2.24f));

            // The medallion's double ring with its notched band, in the floor's proportions
            final float offX = u * 0.0035f, offY = u * 0.0050f;
            final float ringW = Math.max(1.2f, u * 0.0105f);
            for (final float rn : new float[] {1f, 0.89f}) {
                final Ellipse2D.Float circle = new Ellipse2D.Float(cx - R * rn, cy - R * rn, R * rn * 2f, R * rn * 2f);
                g.setStroke(new BasicStroke(ringW));
                g.setColor(new Color(0, 0, 0, 140));
                g.draw(AffineTransform.getTranslateInstance(offX, offY).createTransformedShape(circle));
                g.setPaint(new GradientPaint(cx - R, cy - R, new Color(0xE6C283), cx + R, cy + R, new Color(0x7A5428)));
                g.draw(circle);
            }
            if (u > 70f) {
                g.setStroke(new BasicStroke(Math.max(1f, u * 0.0075f)));
                g.setColor(bronze(0.48f));
                for (int i = 0; i < 32; i++) {
                    final double a = i * Math.PI / 16d;
                    final float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    g.draw(new Line2D.Float(cx + ca * R * 0.90f, cy + sa * R * 0.90f, cx + ca * R * 0.99f, cy + sa * R * 0.99f));
                }
            }

            // The compass star: long points on the axes and short ones on the diagonals, each split into a lit and a dark facet
            final float lx = 0.875f, vx = 0.2772f, vy = 0.1148f, sx = 0.4243f;
            final float vr = sqrtf(vx * vx + vy * vy) * R, va = 0.3926991f;
            final Path2D outline = new Path2D.Float();
            for (int q = 0; q < 8; q++) {
                final float ta = q * 0.7853982f;
                final float tipR = (q % 2 == 0 ? lx : sx * 1.4142f) * R;
                final float tipX = cx + (float) Math.cos(ta) * tipR, tipY = cy + (float) Math.sin(ta) * tipR;
                final float aX = cx + (float) Math.cos(ta - va) * vr, aY = cy + (float) Math.sin(ta - va) * vr;
                final float bX = cx + (float) Math.cos(ta + va) * vr, bY = cy + (float) Math.sin(ta + va) * vr;
                starFacet(g, cx, cy, tipX, tipY, aX, aY);
                starFacet(g, cx, cy, tipX, tipY, bX, bY);
                if (q == 0) {
                    outline.moveTo(tipX, tipY);
                } else {
                    outline.lineTo(tipX, tipY);
                }
                outline.lineTo(bX, bY);
            }
            outline.closePath();
            g.setStroke(new BasicStroke(Math.max(1f, u * 0.0060f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
            g.setColor(new Color(0x12122E));
            g.draw(outline);

            // A round boss closes the centre, cut with a ring and a dark pip like the floor's
            final float boss = R * 0.19f;
            g.setPaint(new GradientPaint(cx - boss, cy - boss, new Color(0xF0D49A), cx + boss, cy + boss, new Color(0x6E4A22)));
            g.fill(new Ellipse2D.Float(cx - boss, cy - boss, boss * 2f, boss * 2f));
            g.setStroke(new BasicStroke(Math.max(1f, u * 0.006f)));
            g.setColor(new Color(0x12122E));
            g.draw(new Ellipse2D.Float(cx - boss, cy - boss, boss * 2f, boss * 2f));
            final float pip = R * 0.058f;
            g.fill(new Ellipse2D.Float(cx - pip, cy - pip, pip * 2f, pip * 2f));
        } finally {
            g.dispose();
        }
        return img;
    }

    // Small icons

    /** A bold three-quarter circular arrow, white at about 45% with a soft dark outline. */
    public static void paintTapArrow(final Graphics2D g, final int cx, final int cy, final int size) {
        if (size <= 0) {
            return;
        }
        final Graphics2D c = smoothCopy(g);
        try {
            final double sc = deviceScale(g);
            final int px = Math.max(8, (int) Math.round(size * sc));
            drawCentered(c, cached("tap" + px, () -> tapImage(px)), cx, cy, sc, 1d);
        } finally {
            c.dispose();
        }
    }

    private static BufferedImage tapImage(final int size) {
        final int pad = Math.max(3, size / 8);
        final int W = even(size + pad * 2);
        final BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        final double cc = W / 2d;
        final double r = size * 0.335, wid = size * 0.17;
        final double start = 305, extent = -262;
        final Arc2D arc = new Arc2D.Double(cc - r, cc - r, 2 * r, 2 * r, start, extent, Arc2D.OPEN);
        final Area area = new Area(new BasicStroke((float) wid, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(arc));
        final double a = Math.toRadians(start + extent);
        final double ex = cc + r * Math.cos(a), ey = cc - r * Math.sin(a);
        final double tx = Math.sin(a), ty = Math.cos(a), nx = Math.cos(a), ny = -Math.sin(a);
        final double hw = wid * 1.26, hl = wid * 1.75;
        final Path2D head = new Path2D.Double();
        head.moveTo(ex + nx * hw - tx * 0.5, ey + ny * hw - ty * 0.5);
        head.lineTo(ex + tx * hl, ey + ty * hl);
        head.lineTo(ex - nx * hw - tx * 0.5, ey - ny * hw - ty * 0.5);
        head.closePath();
        area.add(new Area(head));
        for (int i = 3; i >= 1; i--) {
            final Area ol = new Area(new BasicStroke((float) (size * 0.018 * i + 1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(area));
            ol.subtract(area);
            g.setColor(new Color(0, 0, 0, 60));
            g.fill(ol);
        }
        g.setColor(new Color(255, 255, 255, 115));
        g.fill(area);
        g.dispose();
        return img;
    }

    /** A settings gear centred on (cx, cy). */
    public static void paintGearIcon(final Graphics2D g, final int cx, final int cy, final int size, final Color c) {
        if (size <= 0) {
            return;
        }
        final Graphics2D g2 = smoothCopy(g);
        try {
            final int teeth = 8;
            final double ro = size * 0.5, rr = size * 0.37, hole = size * 0.16;
            final Path2D p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            final double step = Math.PI * 2 / teeth;
            for (int i = 0; i < teeth; i++) {
                final double a = i * step - Math.PI / 2;
                final double t1 = step * 0.17, t2 = step * 0.29;
                addPolar(p, cx, cy, rr, a - t2, i == 0);
                addPolar(p, cx, cy, ro, a - t1, false);
                addPolar(p, cx, cy, ro, a + t1, false);
                addPolar(p, cx, cy, rr, a + t2, false);
            }
            p.closePath();
            p.append(new Ellipse2D.Double(cx - hole, cy - hole, hole * 2, hole * 2), false);
            iconFill(g2, p, c, size);
        } finally {
            g2.dispose();
        }
    }

    private static void addPolar(final Path2D p, final double cx, final double cy, final double r, final double a, final boolean first) {
        final double x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
        if (first) {
            p.moveTo(x, y);
        } else {
            p.lineTo(x, y);
        }
    }

    /** A rolled scroll with lines of text, for the game log. */
    public static void paintLogIcon(final Graphics2D g, final int cx, final int cy, final int size, final Color c) {
        if (size <= 0) {
            return;
        }
        final Graphics2D g2 = smoothCopy(g);
        try {
            final float s = size;
            final float x0 = cx - s * 0.5f, y0 = cy - s * 0.5f;
            final Area a = new Area(new RoundRectangle2D.Float(x0 + s * 0.20f, y0 + s * 0.12f, s * 0.60f, s * 0.76f, s * 0.05f, s * 0.05f));
            a.add(new Area(new RoundRectangle2D.Float(x0 + s * 0.10f, y0 + s * 0.05f, s * 0.80f, s * 0.15f, s * 0.15f, s * 0.15f)));
            a.add(new Area(new RoundRectangle2D.Float(x0 + s * 0.10f, y0 + s * 0.80f, s * 0.80f, s * 0.15f, s * 0.15f, s * 0.15f)));
            final float lh = Math.max(1f, s * 0.07f);
            a.subtract(new Area(new Rectangle2D.Float(x0 + s * 0.30f, y0 + s * 0.31f, s * 0.40f, lh)));
            a.subtract(new Area(new Rectangle2D.Float(x0 + s * 0.30f, y0 + s * 0.46f, s * 0.40f, lh)));
            a.subtract(new Area(new Rectangle2D.Float(x0 + s * 0.30f, y0 + s * 0.61f, s * 0.26f, lh)));
            iconFill(g2, a, c, size);
        } finally {
            g2.dispose();
        }
    }

    private static void iconFill(final Graphics2D g, final Shape s, final Color c, final int size) {
        final Color col = c != null ? c : Color.WHITE;
        final float off = Math.max(0.6f, size / 26f);
        final AffineTransform t = AffineTransform.getTranslateInstance(0, off);
        g.setColor(new Color(0, 0, 0, Math.min(150, col.getAlpha())));
        g.fill(t.createTransformedShape(s));
        g.setColor(col);
        g.fill(s);
    }

    // End of game

    private static final float BURST_Y = 0.32f;
    private static final float BURST_RADIUS = 0.35f;
    private static String burstKey;
    private static BufferedImage burstImage;

    /**
     * A translucent burst for the end of game, painted with alpha over whatever g already holds: soft
     * rays at about 25% opacity from a point at the horizontal centre, 32% of the way down, reaching
     * zero alpha by 35% of the width from that point and leaving everything beyond it untouched.
     */
    public static void paintVictoryBurst(final Graphics2D g, final int w, final int h, final Color core) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final Color col = core != null ? core : new Color(0xFFC860);
        final Graphics2D c = smoothCopy(g);
        try {
            // Half size, rounded up so a resize drag does not rebuild it on every step
            final int bw = (Math.max(16, w / 2) + 31) & ~31, bh = (Math.max(16, h / 2) + 31) & ~31;
            final String key = bw + "x" + bh + Integer.toHexString(col.getRGB());
            final BufferedImage img;
            synchronized (CACHE) {
                if (!key.equals(burstKey)) {
                    burstImage = burstImage(bw, bh, col);
                    burstKey = key;
                }
                img = burstImage;
            }
            c.drawImage(img, 0, 0, w, h, null);
        } finally {
            c.dispose();
        }
    }

    private static BufferedImage burstImage(final int w, final int h, final Color core) {
        final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final int[] line = new int[w];
        final float[] broad = spokeTable(2048, 0x71C, 13, 0.012f, 0.028f);
        final float[] fine = spokeTable(2048, 0x9A3, 34, 0.0030f, 0.0080f);
        final int cr = core.getRed(), cg = core.getGreen(), cb = core.getBlue();
        final float ox = w * 0.5f, oy = h * BURST_Y;
        final float radius = w * BURST_RADIUS;
        int seed = 0x13579B;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final float dx = x + 0.5f - ox, dy = y + 0.5f - oy;
                final float r = sqrtf(dx * dx + dy * dy);
                final float e = r / radius;
                int alpha = 0;
                float heat = 0f;
                if (e < 1f) {
                    final float th = (atan2f(dy, dx) / 6.2831855f + 0.5f) * 2047f;
                    final float ray = broad[(int) th] * 0.72f + fine[(int) th] * 0.48f;
                    final float fall = (1f - e) * (1f - e) * (1f + 2f * e);
                    final float heart = sat(1f - e * 3.4f);
                    final float al = 0.25f * fall * (0.30f + 0.70f * sat(ray)) + heart * heart * 0.16f;
                    seed ^= seed << 13;
                    seed ^= seed >>> 17;
                    seed ^= seed << 5;
                    // A little noise in the alpha stops the long shallow ramp banding
                    alpha = clampByte(al * 255f + ((seed & 0xFF) / 255f) * Math.min(1f, fall * 8f));
                    heat = sat(1f - e * 1.8f);
                }
                final int rr = lerpC(cr, 255, heat * 0.55f), gg = lerpC(cg, 255, heat * 0.55f), bb = lerpC(cb, 255, heat * 0.55f);
                line[x] = (alpha << 24) | (rr << 16) | (gg << 8) | bb;
            }
            img.getRaster().setDataElements(0, y, w, 1, line);
        }
        return img;
    }
}
