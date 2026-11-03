package forge.toolbox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import forge.Singletons;
import forge.card.ColorSet;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;

/**
 * Arena-style look for the desktop client: a dark navy and gold palette, a fixed
 * match board (see res/defaults/match_arena.xml) and the painting helpers shared by
 * the restyled cards, buttons and player panels. Toggled by {@link FPref#UI_ARENA_THEME}.
 */
public final class ArenaTheme {
    private ArenaTheme() { }

    // Palette
    public static final Color NIGHT       = new Color(0x080D19);
    public static final Color NAVY        = new Color(0x0F1A2E);
    public static final Color NAVY_LIGHT  = new Color(0x1B2A44);
    public static final Color SLATE       = new Color(0x2B3D5C);
    public static final Color GOLD        = new Color(0xD8B26A);
    public static final Color GOLD_BRIGHT = new Color(0xF6DC9A);
    public static final Color GOLD_DIM    = new Color(0x8C6D3B);
    public static final Color TEXT        = new Color(0xF2ECDF);
    public static final Color TEXT_DIM    = new Color(0x9DA9BD);
    public static final Color PLAYABLE    = new Color(0x3DA9FF);
    public static final Color SELECTABLE  = new Color(0xFFD45A);
    public static final Color CHOSEN      = new Color(0xFF7A2E);
    public static final Color HOVER       = new Color(0xE6F1FF);
    public static final Color DANGER      = new Color(0xE5484D);
    public static final Color BUFF        = new Color(0x6FE07A);
    public static final Color ACTION_TOP    = new Color(0xF8A63E);
    public static final Color ACTION_BOTTOM = new Color(0xC2501B);
    public static final Color GLASS       = new Color(8, 13, 25, 205);
    public static final Color GLASS_EDGE  = new Color(216, 178, 106, 70);

    // Board geometry as fractions of the match area; must match res/defaults/match_arena.xml.
    public static final float RAIL_RIGHT = 1f;
    public static final float FIELD_SPLIT = 0.39f;
    public static final float HAND_TOP = 0.85f;

    /** Cells that float over the board on the Arena layout rather than tiling it. */
    public static boolean isFloatingDoc(final EDocID id) {
        return id == EDocID.REPORT_MESSAGE || id == EDocID.BUTTON_DOCK || id == EDocID.REPORT_LOG;
    }

    public enum BoardVariant { DEFAULT, DAY, NIGHT }

    public static boolean isEnabled() {
        final ForgePreferences prefs = FModel.getPreferences();
        return prefs != null && prefs.getPrefBoolean(FPref.UI_ARENA_THEME);
    }

    /** True while the fixed Arena board is showing: theme on and the current screen is a match. */
    public static boolean isMatchBoard() {
        if (!isEnabled() || Singletons.getControl() == null) {
            return false;
        }
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        return screen != null && screen.isMatchScreen();
    }

    /** Arena replacement for a skin swatch, or null to keep the skin's own colour. */
    public static Color getSkinColor(final FSkinProp prop) {
        switch (prop) {
            case CLR_THEME:                   return new Color(15, 26, 46, 228);
            case CLR_BORDERS:                 return new Color(216, 178, 106, 110);
            case CLR_ZEBRA:                   return new Color(0x16243C);
            case CLR_HOVER:                   return new Color(0x2B3D5C);
            case CLR_ACTIVE:                  return new Color(0x8C6D3B);
            case CLR_INACTIVE:                return new Color(0x1B2A44);
            case CLR_TEXT:                    return TEXT;
            case CLR_PHASE_INACTIVE_ENABLED:  return new Color(0x2C4F78);
            case CLR_PHASE_INACTIVE_DISABLED: return new Color(0x232B38);
            case CLR_PHASE_ACTIVE_ENABLED:    return GOLD;
            case CLR_PHASE_ACTIVE_DISABLED:   return GOLD_DIM;
            case CLR_THEME2:                  return NIGHT;
            case CLR_OVERLAY:                 return new Color(4, 7, 14, 215);
            default:                          return null;
        }
    }

    private static Dimension boardSize() {
        final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension(Math.max(1280, Math.min(screen.width, 2560)), Math.max(720, Math.min(screen.height, 1440)));
    }

    /** Match board art, sized to the screen; it is stretched over the match area. */
    public static BufferedImage createBoardImage(final BoardVariant variant) {
        final Dimension d = boardSize();
        return createBoardImage(d.width, d.height, variant);
    }

    public static BufferedImage createBoardImage(final int w, final int h, final BoardVariant variant) {
        final Color top, bottom;
        switch (variant) {
            case DAY:
                top = new Color(0x5C6168); bottom = new Color(0x3A3E44);
                break;
            case NIGHT:
                top = new Color(0x2A3142); bottom = new Color(0x161B26);
                break;
            default:
                top = new Color(0x414955); bottom = new Color(0x262C35);
                break;
        }

        final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // A plain slate surface, a little lighter in the middle, so the cards carry the scene
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g.fillRect(0, 0, w, h);

            final float right = w * RAIL_RIGHT;
            final float split = h * FIELD_SPLIT, handTop = h * HAND_TOP;

            g.setPaint(new RadialGradientPaint(new Point2D.Float(right / 2f, split), Math.max(right, h) * 0.55f,
                    new float[] {0f, 1f}, new Color[] {new Color(255, 255, 255, 22), new Color(255, 255, 255, 0)}));
            g.fillRect(0, 0, w, h);

            // Carved emblem where the two battlefields meet
            paintEmblem(g, right / 2f, split, h * 0.27f);

            // Hand tray darkens towards the bottom edge so the cards in hand read clearly
            g.setPaint(new GradientPaint(0, handTop - h * 0.05f, new Color(6, 8, 12, 0), 0, h, new Color(6, 8, 12, 200)));
            g.fillRect(0, Math.round(handTop - h * 0.05f), w, h);

            // Vignette
            final float radius = (float) Math.hypot(w, h) * 0.6f;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(right / 2f, h * 0.45f), radius,
                    new float[] {0f, 0.5f, 1f},
                    new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 45), new Color(0, 0, 0, 190)}));
            g.fillRect(0, 0, w, h);
        } finally {
            g.dispose();
        }
        addGrain(img, 3, 0xA2E7A);
        return img;
    }

    /** A faint engraved emblem: concentric grooves and spokes, a dark line with a light lip below it. */
    private static void paintEmblem(final Graphics2D g, final float cx, final float cy, final float r) {
        final float[] rings = {1f, 0.86f, 0.55f, 0.18f};
        for (final float ring : rings) {
            final float rr = r * ring;
            g.setStroke(new BasicStroke(Math.max(2f, r / 90f)));
            g.setColor(new Color(0, 0, 0, 34));
            g.draw(new Ellipse2D.Float(cx - rr, cy - rr, rr * 2, rr * 2));
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255, 255, 255, 12));
            g.draw(new Ellipse2D.Float(cx - rr, cy - rr + 2, rr * 2, rr * 2));
        }
        for (int i = 0; i < 16; i++) {
            final double a = Math.PI * 2 * i / 16;
            final double r0 = r * (i % 2 == 0 ? 0.55 : 0.86), r1 = r;
            g.setStroke(new BasicStroke(Math.max(2f, r / 110f)));
            g.setColor(new Color(0, 0, 0, 28));
            g.draw(new Line2D.Double(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, cx + Math.cos(a) * r1, cy + Math.sin(a) * r1));
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255, 255, 255, 10));
            g.draw(new Line2D.Double(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0 + 2, cx + Math.cos(a) * r1, cy + Math.sin(a) * r1 + 2));
        }
        // a faint wash of light over the centre
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), r,
                new float[] {0f, 1f}, new Color[] {new Color(255, 248, 230, 34), new Color(255, 248, 230, 0)}));
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }

    /** Dark navy tile used wherever the skin asks for its background texture. */
    public static BufferedImage createTexture() {
        final int size = 256;
        final BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x0D1628));
        g.fillRect(0, 0, size, size);
        g.dispose();
        addGrain(img, 6, 0x5EED);
        return img;
    }

    private static void addGrain(final BufferedImage img, final int amount, final long seed) {
        final Random rnd = new Random(seed);
        final int w = img.getWidth(), h = img.getHeight();
        final int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                final int n = rnd.nextInt(amount * 2 + 1) - amount;
                final int p = row[x];
                row[x] = (clamp(((p >> 16) & 0xFF) + n) << 16) | (clamp(((p >> 8) & 0xFF) + n) << 8) | clamp((p & 0xFF) + n);
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
    }

    private static int clamp(final int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    public static Color alpha(final Color c, final int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /**
     * Soft glow around a rounded rectangle, strongest at its edge. Paint before the shape itself.
     */
    public static void paintGlow(final Graphics2D g, final int x, final int y, final int w, final int h,
            final int arc, final Color c, final int spread, final int maxAlpha) {
        if (spread <= 0) {
            return;
        }
        final Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = spread; i >= 1; i--) {
            final float t = 1f - (i - 1f) / spread;
            g.setColor(alpha(c, Math.round(maxAlpha * t * t / 2.2f)));
            g.fill(new RoundRectangle2D.Float(x - i, y - i, w + 2f * i, h + 2f * i, arc + 2f * i, arc + 2f * i));
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    /** Translucent dark panel with a thin gold rim, used behind the side-rail views. */
    public static void paintGlassPanel(final Graphics2D g, final int x, final int y, final int w, final int h) {
        final Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f, 14, 14);
        g.setPaint(new GradientPaint(0, y, new Color(16, 26, 46, 215), 0, y + h, new Color(7, 11, 22, 225)));
        g.fill(r);
        g.setColor(GLASS_EDGE);
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    /**
     * Paints a button body. Primary buttons are the amber Arena action button;
     * the rest are navy with a gold rim.
     */
    public static void paintButton(final Graphics2D g, final int w, final int h, final boolean primary,
            final boolean hovered, final boolean pressed, final boolean focused, final boolean toggled, final boolean enabled) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final int pad = primary && enabled ? Math.min(5, Math.max(2, h / 10)) : 1;
        final float bx = pad, by = pad, bw = w - 2f * pad, bh = h - 2f * pad;
        if (bw <= 0 || bh <= 0) {
            return;
        }
        final float arc = Math.min(bh, 26f);
        final RoundRectangle2D body = new RoundRectangle2D.Float(bx, by, bw, bh, arc, arc);

        Color top, bottom, rim;
        if (primary) {
            top = ACTION_TOP;
            bottom = ACTION_BOTTOM;
            rim = GOLD_BRIGHT;
            if (hovered) { top = top.brighter(); bottom = new Color(0xD8601F); }
            if (pressed) { top = new Color(0xD8862E); bottom = new Color(0x9E3F14); }
            if (enabled) {
                paintGlow(g, Math.round(bx), Math.round(by), Math.round(bw), Math.round(bh), Math.round(arc),
                        new Color(0xFF8A2A), pad, hovered ? 190 : 140);
            }
        } else if (toggled) {
            top = new Color(0xA8843F);
            bottom = new Color(0x6A4E22);
            rim = GOLD_BRIGHT;
        } else {
            top = hovered ? new Color(0x2A3F62) : NAVY_LIGHT;
            bottom = hovered ? new Color(0x16243D) : NAVY;
            rim = hovered || focused ? GOLD : GOLD_DIM;
            if (pressed) { top = NAVY; bottom = NIGHT; }
        }

        g.setPaint(new GradientPaint(0, by, top, 0, by + bh, bottom));
        g.fill(body);

        // Glossy top sheen
        final java.awt.Shape oldClip = g.getClip();
        g.clip(body);
        g.setPaint(new GradientPaint(0, by, new Color(255, 255, 255, primary ? 70 : 34), 0, by + bh * 0.55f, new Color(255, 255, 255, 0)));
        g.fill(new RoundRectangle2D.Float(bx, by, bw, bh * 0.55f, arc, arc));
        g.setClip(oldClip);

        g.setStroke(new BasicStroke(focused && !primary ? 1.6f : 1.1f));
        g.setColor(rim);
        g.draw(new RoundRectangle2D.Float(bx + 0.5f, by + 0.5f, bw - 1f, bh - 1f, arc, arc));
    }

    /** Frame colour of a battlefield tile: the card's colour, gold for multicolour, stone for lands, steel for artifacts. */
    public static Color frameColor(final ColorSet colors, final boolean isLand, final boolean isArtifact) {
        if (colors != null && colors.isMulticolor()) {
            return new Color(0xC9A24A);
        }
        if (colors != null && colors.isMonoColor()) {
            if (colors.hasWhite()) { return new Color(0xD8D0B0); }
            if (colors.hasBlue())  { return new Color(0x2F6FB0); }
            if (colors.hasBlack()) { return new Color(0x4C4350); }
            if (colors.hasRed())   { return new Color(0xB8432F); }
            if (colors.hasGreen()) { return new Color(0x3F8048); }
        }
        if (isLand) {
            return new Color(0x8B7458);
        }
        return isArtifact ? new Color(0x8D939C) : new Color(0x7A7F88);
    }

    /** Crops an avatar to a circle with a gold rim, the Arena portrait style. */
    public static BufferedImage createCircleAvatar(final Image src, final int size) {
        final BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            final float rim = Math.max(2f, size / 26f);
            final Ellipse2D inner = new Ellipse2D.Float(rim, rim, size - 2 * rim, size - 2 * rim);
            g.setColor(Color.WHITE);
            g.fill(inner);
            g.setComposite(AlphaComposite.SrcIn);
            if (src != null) {
                g.drawImage(src, Math.round(rim), Math.round(rim), Math.round(size - 2 * rim), Math.round(size - 2 * rim), null);
            } else {
                g.setColor(NAVY_LIGHT);
                g.fill(inner);
            }
            g.setComposite(AlphaComposite.SrcOver);
            // Inner shadow so the portrait sits inside the frame
            g.setPaint(new RadialGradientPaint(new Point2D.Float(size / 2f, size / 2f), size / 2f,
                    new float[] {0.7f, 1f}, new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
            g.fill(inner);
            g.setStroke(new BasicStroke(rim));
            g.setPaint(new GradientPaint(0, 0, GOLD_BRIGHT, 0, size, GOLD_DIM));
            g.draw(new Ellipse2D.Float(rim / 2, rim / 2, size - rim, size - rim));
        } finally {
            g.dispose();
        }
        return out;
    }
}
