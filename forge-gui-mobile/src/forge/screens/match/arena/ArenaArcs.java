package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;

import forge.Graphics;

/**
 * The Arena's targeting arcs (port of the Swing TargetingOverlay's Arena arcs): a glowing curve from what aims to what
 * it aims at, bowed away from the straight line, with a bright core, a soft glow and a pulse of light running along it
 * towards the target, which wears a small ring. Drawn as a trail of batched soft discs, so a board full of arcs costs
 * a few hundred quads and no transforms or extra draw calls.
 */
public final class ArenaArcs {
    private ArenaArcs() { }

    /** What the arc means: a friendly aim, an attack, a block, or a spell or ability aimed at an opponent's things. */
    public static final int FRIEND = 0, ATTACK = 1, BLOCK = 2, FOE = 3;
    private static final Color[] COLOR = {ArenaTheme.rgb(0x6ED2FF), ArenaTheme.rgb(0xFF7A2E), ArenaTheme.rgb(0x5C9BFF), ArenaTheme.rgb(0xFF5A4A)};
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color();

    /** One arc from x1, y1 to x2, y2 (the coordinates of the Graphics in use). */
    public static void draw(final Graphics g, final float x1, final float y1, final float x2, final float y2, final int kind) {
        final float dx = x2 - x1, dy = y2 - y1;
        final float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 2f) {
            return;
        }
        final Color c = COLOR[Math.max(0, Math.min(COLOR.length - 1, kind))];
        // bow it: perpendicular to the line, upwards for a mostly level arc, to the left for a mostly upright one
        float nx = -dy / len, ny = dx / len;
        if (Math.abs(dx) >= Math.abs(dy) ? ny > 0 : nx > 0) {
            nx = -nx;
            ny = -ny;
        }
        final float bow = Math.min(len * 0.22f, ArenaTheme.px(220));
        final float cx = (x1 + x2) / 2f + nx * bow, cy = (y1 + y2) / 2f + ny * bow;
        final float glowR = Math.max(3f, ArenaTheme.px(10)), coreR = Math.max(1.2f, ArenaTheme.px(2.6f));
        // the discs overlap well, so the core reads as one line
        final int n = Math.max(24, Math.min(420, Math.round(len * 1.15f / (coreR * 1.1f))));
        // a pulse running towards the target, once every 0.9 s
        final float run = (ArenaMotion.seconds() % 0.9f) / 0.9f;
        ArenaMotion.requestFrame();
        final Color glow = ArenaTheme.alpha(c, Math.min(0.5f, 6.5f / n * 2.2f), S1);
        for (int i = 0; i <= n; i++) {
            final float t = i / (float) n;
            final float u = 1f - t;
            final float px = u * u * x1 + 2 * u * t * cx + t * t * x2, py = u * u * y1 + 2 * u * t * cy + t * t * y2;
            final float taper = 0.55f + 0.45f * Math.min(1f, t * 4f); // thin where it leaves, full where it arrives
            g.drawSoftShadow(glow, px - glowR * 0.25f * taper, py - glowR * 0.25f * taper, glowR * 0.5f * taper, glowR * 0.5f * taper, glowR * 0.25f * taper, glowR * 0.75f * taper);
        }
        for (int i = 0; i <= n; i++) {
            final float t = i / (float) n;
            final float u = 1f - t;
            final float px = u * u * x1 + 2 * u * t * cx + t * t * x2, py = u * u * y1 + 2 * u * t * cy + t * t * y2;
            final float dist = Math.abs(t - run);
            final float lit = Math.max(0f, 1f - dist / 0.08f);
            final float taper = 0.55f + 0.45f * Math.min(1f, t * 4f);
            final float r = coreR * taper * (1f + 0.6f * lit);
            S2.set(c.r + (1f - c.r) * (0.55f + 0.45f * lit), c.g + (1f - c.g) * (0.55f + 0.45f * lit), c.b + (1f - c.b) * (0.55f + 0.45f * lit), 0.9f);
            g.fillSmoothCircle(S2, px, py, r);
        }
        // the mark where it lands
        final float ring = Math.max(4f, ArenaTheme.px(11));
        g.drawGlow(ArenaTheme.alpha(c, 0.7f, S3), x2 - ring, y2 - ring, 2 * ring, 2 * ring, ring, ArenaTheme.px(10));
        g.drawSmoothCircle(Math.max(1.5f, ArenaTheme.px(2.5f)), ArenaTheme.alpha(ArenaTheme.WHITE, 0.92f, S3), x2, y2, ring);
        g.fillSmoothCircle(ArenaTheme.alpha(c, 0.95f, S3), x2, y2, ring * 0.38f);
    }
}
