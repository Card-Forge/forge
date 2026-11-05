package forge.toolbox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import forge.Singletons;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.card.CardView.CardStateView;
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
    public static final Color PLAYABLE    = new Color(0x38C8FF);
    public static final Color SELECTABLE  = new Color(0x38C8FF);
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
    public static final float FIELD_SPLIT = 0.38f;
    public static final float HAND_TOP = 0.85f;

    /** Cells that float over the board on the Arena layout rather than tiling it. */
    public static boolean isFloatingDoc(final EDocID id) {
        return id == EDocID.REPORT_MESSAGE || id == EDocID.BUTTON_DOCK || id == EDocID.REPORT_LOG || Arrays.asList(EDocID.Hands).contains(id);
    }

    public enum BoardVariant { DEFAULT, DAY, NIGHT }

    public static boolean isEnabled() {
        final ForgePreferences prefs = FModel.getPreferences();
        return prefs != null && prefs.getPrefBoolean(FPref.UI_ARENA_THEME);
    }

    /** True while the fixed Arena board is showing: theme on and the current screen is a match. */
    /** True while a dialog photographs the board for its backdrop: nothing on the board glows for that picture. */
    public static volatile boolean snapshotting = false;

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
        return ArenaArt.createBoard(w, h, switch (variant) {
            case DAY -> 0;
            case NIGHT -> 2;
            default -> 1;
        });
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

    // one voice for words and figures alike: compact and sturdy, with figures that line up. Rules text has its own, see below
    private static final String[] DISPLAY_FACES = {"Cambria", "Palatino Linotype", "Book Antiqua", "Palatino", "Constantia", "Georgia"};
    private static final String[] NUMERAL_FACES = {"Cambria", "Palatino Linotype", "Book Antiqua", "Palatino", "Georgia"};
    private static final String[] RULES_FACES = {"Palatino Linotype", "Book Antiqua", "Palatino", "Cambria", "Georgia"};
    private static String numeralFace;
    private static String rulesFace;

    private static String firstInstalled(final String[] faces) {
        final List<String> installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (final String face : faces) {
            if (installed.contains(face)) {
                return face;
            }
        }
        return Font.SERIF;
    }

    /** For life totals, power/toughness and counts: the display face's figures sit at uneven heights. */
    public static Font numeralFont(final int style, final float size) {
        if (numeralFace == null) {
            numeralFace = firstInstalled(NUMERAL_FACES);
        }
        return new Font(numeralFace, style, 1).deriveFont(size);
    }

    /** Words in the display face and figures in the numeral face, whose digits line up. Returns the width; draws it if asked. */
    public static float paintLabel(final Graphics2D g, final String text, final float size, final float x, final float y, final boolean draw) {
        float at = x;
        int start = 0;
        while (start < text.length()) {
            final boolean digits = Character.isDigit(text.charAt(start));
            int end = start + 1;
            while (end < text.length() && Character.isDigit(text.charAt(end)) == digits) {
                end++;
            }
            final String run = text.substring(start, end);
            g.setFont(digits ? numeralFont(Font.BOLD, size) : displayFont(Font.BOLD, size));
            if (draw) {
                g.drawString(run, at, y);
            }
            at += g.getFontMetrics().stringWidth(run);
            start = end;
        }
        return at - x;
    }

    /** For the words of rules: a card's text stays in a book face. */
    public static Font rulesFont(final int style, final float size) {
        if (rulesFace == null) {
            rulesFace = firstInstalled(RULES_FACES);
        }
        return new Font(rulesFace, style, 1).deriveFont(size);
    }

    private static String displayFace;

    /**
     * The face for everything that speaks in Arena's voice: titles, life totals, card names on
     * tiles, power/toughness, buttons. A bookish serif with lining figures, taken from the
     * fonts the system already has, falling back to the platform's serif.
     */
    public static Font displayFont(final int style, final float size) {
        if (displayFace == null) {
            displayFace = Font.SERIF;
            final List<String> installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (final String face : DISPLAY_FACES) {
                if (installed.contains(face)) {
                    displayFace = face;
                    break;
                }
            }
        }
        return new Font(displayFace, style, 1).deriveFont(size);
    }

    /**
     * Paints a button body, always a full pill. Primary buttons are the luminous orange action
     * button; the rest are near-black with a white outline; a disabled one is flat navy.
     */
    public static void paintButton(final Graphics2D g, final int w, final int h, final boolean primary,
            final boolean hovered, final boolean pressed, final boolean focused, final boolean toggled, final boolean enabled) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // the same pill whatever its kind, with room inside the bounds for the bloom
        final int pad = Math.min(8, Math.max(3, h / 10));
        final float bx = pad, by = pad, bw = w - 2f * pad, bh = h - 2f * pad;
        if (bw <= 0 || bh <= 0) {
            return;
        }
        final float arc = bh;
        final RoundRectangle2D body = new RoundRectangle2D.Float(bx, by, bw, bh, arc, arc);
        final boolean molten = primary && enabled;

        if (molten) {
            paintGlow(g, Math.round(bx), Math.round(by), Math.round(bw), Math.round(bh), Math.round(arc),
                    new Color(0xFF8A2A), pad, hovered ? 255 : 215);
            final float[] stops = {0f, 0.45f, 1f};
            final Color[] colours = pressed
                    ? new Color[] {new Color(0xE8862A), new Color(0xC85E18), new Color(0x93400F)}
                    : hovered
                    ? new Color[] {new Color(0xFFC860), new Color(0xFF9426), new Color(0xF06408)}
                    : new Color[] {new Color(0xFFB238), new Color(0xFF8418), new Color(0xE85A08)};
            g.setPaint(new java.awt.LinearGradientPaint(0, by, 0, by + bh, stops, colours));
            g.fill(body);
            final java.awt.Shape oldClip = g.getClip();
            g.clip(body);
            // molten at the centre
            g.setPaint(new java.awt.RadialGradientPaint(bx + bw / 2f, by + bh * 0.45f, Math.max(1f, bw * 0.42f), new float[] {0f, 1f},
                    new Color[] {new Color(255, 214, 130, 55), new Color(255, 214, 130, 0)}));
            g.fillRect(Math.round(bx), Math.round(by), Math.round(bw), Math.round(bh));
            // the hot upper edge
            g.setPaint(new GradientPaint(0, by, new Color(255, 240, 200, 120), 0, by + bh * 0.28f, new Color(255, 240, 200, 0)));
            g.fillRect(Math.round(bx), Math.round(by), Math.round(bw), Math.round(bh * 0.3f));
            g.setClip(oldClip);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(new Color(0xFFCB6E));
            g.draw(new RoundRectangle2D.Float(bx + 0.5f, by + 0.5f, bw - 1f, bh - 1f, arc, arc));
            return;
        }

        Color top, bottom, rim;
        if (!enabled) {
            // not ready yet: deep blue with a lit rim
            top = new Color(0x2B4C8C);
            bottom = new Color(0x18305E);
            rim = new Color(150, 195, 245, 210);
        } else if (toggled) {
            top = new Color(0xA8843F);
            bottom = new Color(0x6A4E22);
            rim = GOLD_BRIGHT;
        } else {
            top = hovered ? new Color(0x2C303A) : new Color(0x15171D);
            bottom = hovered ? new Color(0x181A21) : new Color(0x08090C);
            rim = new Color(255, 255, 255, hovered ? 190 : 128);
            if (pressed) { top = new Color(0x08090C); bottom = Color.BLACK; }
        }
        g.setPaint(new GradientPaint(0, by, top, 0, by + bh, bottom));
        g.fill(body);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(rim);
        g.draw(new RoundRectangle2D.Float(bx + 0.5f, by + 0.5f, bw - 1f, bh - 1f, arc, arc));
    }

    /** The colour of a tile's frame and name bar. Lands take the colour of the mana they make. */
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
                return new Color(0xA89E8C);
            }
            colors = ColorSet.fromMask(mask);
        }
        if (colors != null && colors.isMulticolor()) {
            return new Color(0xC9A640);
        }
        if (colors != null && colors.isMonoColor()) {
            if (colors.hasWhite()) { return new Color(0xE2DAC0); }
            if (colors.hasBlue())  { return new Color(0x3A8AD6); }
            if (colors.hasBlack()) { return new Color(0x37333D); }
            if (colors.hasRed())   { return new Color(0xD0432B); }
            if (colors.hasGreen()) { return new Color(0x2FA562); }
        }
        return state.isArtifact() ? new Color(0xA9B8C6) : new Color(0xB9B4AA);
    }

    /** The thin line of colour round a tile's art; pale and dark cards still need it to show against black. */
    public static Color frameLine(final Color tint) {
        final int l = (tint.getRed() * 30 + tint.getGreen() * 59 + tint.getBlue() * 11) / 100;
        return l < 70 ? new Color(0x8D8794) : tint;
    }

    /** A colour a little lighter, for the top of a bar. */
    public static Color lift(final Color c) {
        return new Color(Math.min(255, c.getRed() + 26), Math.min(255, c.getGreen() + 26), Math.min(255, c.getBlue() + 26));
    }

    private static final java.util.Map<String, BufferedImage> PIPS = new java.util.HashMap<>();
    private static final java.util.Map<String, BufferedImage> GLYPHS = new java.util.HashMap<>();
    private static final java.util.Map<String, BufferedImage> DARK_GLYPHS = new java.util.HashMap<>();

    /** The same glyph as {@link #monochromeIcon}, in near-black, for a pale chip. */
    public static BufferedImage darkIcon(final String key, final java.awt.Image source) {
        BufferedImage dark = DARK_GLYPHS.get(key);
        if (dark == null) {
            final BufferedImage white = monochromeIcon(key, source);
            dark = new BufferedImage(white.getWidth(), white.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < white.getHeight(); y++) {
                for (int x = 0; x < white.getWidth(); x++) {
                    dark.setRGB(x, y, (white.getRGB(x, y) & 0xFF000000) | 0x121216);
                }
            }
            DARK_GLYPHS.put(key, dark);
        }
        return dark;
    }

    /** A card's cost as bright pips ready to place on its top edge: lifted colours, a black ring round the row. */
    /**
     * The symbols' colour has to read at a glance at this size, so each single-colour symbol is painted
     * in its colour, keeping the shape of its glyph. Numerals and hybrids are only brightened.
     */
    private static void tintPips(final BufferedImage raw, final forge.card.mana.ManaCost cost, final int size) {
        final java.util.List<forge.card.mana.ManaCostShard> order = new java.util.ArrayList<>();
        if (cost.getGenericCost() > 0 || cost.isPureGeneric()) {
            for (final forge.card.mana.ManaCostShard s : cost) {
                if (s == forge.card.mana.ManaCostShard.X) {
                    order.add(s);
                }
            }
            order.add(null); // the numeral
            for (final forge.card.mana.ManaCostShard s : cost) {
                if (s != forge.card.mana.ManaCostShard.X) {
                    order.add(s);
                }
            }
        } else {
            for (final forge.card.mana.ManaCostShard s : cost) {
                order.add(s);
            }
        }
        for (int i = 0; i < order.size(); i++) {
            final int tint = pipTint(order.get(i));
            final int x0 = 2 + i * (size + 1);
            if (order.get(i) == null) {
                // a numeral in the sprite goes hollow at this size, so it is drawn crisp: a pale disc and a bold figure
                final Graphics2D ng = raw.createGraphics();
                ng.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ng.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                ng.setPaint(new GradientPaint(0, 2, new Color(0xF4EFE2), 0, 2 + size, new Color(0xC6BFAD)));
                ng.fillOval(x0, 2, size, size);
                final String figure = String.valueOf(cost.getGenericCost());
                ng.setFont(numeralFont(Font.BOLD, size * (figure.length() > 1 ? 0.68f : 0.84f)));
                final java.awt.FontMetrics fm = ng.getFontMetrics();
                ng.setColor(new Color(12, 12, 14));
                ng.drawString(figure, x0 + (size - fm.stringWidth(figure)) / 2f, 2 + (size + fm.getAscent() - fm.getDescent()) / 2f);
                ng.dispose();
                continue;
            }
            for (int y = 0; y < raw.getHeight(); y++) {
                for (int x = x0; x < Math.min(raw.getWidth(), x0 + size + 1); x++) {
                    final int argb = raw.getRGB(x, y);
                    final int a = argb >>> 24;
                    if (a == 0) {
                        continue;
                    }
                    final int r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
                    final int nr, ng, nb;
                    if (tint == 0) {
                        nr = Math.min(255, r * 5 / 4 + 10);
                        ng = Math.min(255, gr * 5 / 4 + 10);
                        nb = Math.min(255, b * 5 / 4 + 10);
                    } else {
                        final float f = Math.min(1f, (r * 30 + gr * 59 + b * 11) / 100f / 190f);
                        nr = Math.round(((tint >> 16) & 255) * f);
                        ng = Math.round(((tint >> 8) & 255) * f);
                        nb = Math.round((tint & 255) * f);
                    }
                    raw.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                }
            }
        }
    }

    private static int pipTint(final forge.card.mana.ManaCostShard s) {
        if (s == forge.card.mana.ManaCostShard.WHITE) { return 0xFFF4B8; }
        if (s == forge.card.mana.ManaCostShard.BLUE) { return 0x7CC8F4; }
        if (s == forge.card.mana.ManaCostShard.BLACK) { return 0xB4A8AC; }
        if (s == forge.card.mana.ManaCostShard.RED) { return 0xF58A64; }
        if (s == forge.card.mana.ManaCostShard.GREEN) { return 0x6FCB8C; }
        return 0;
    }

    public static BufferedImage brightPips(final forge.card.mana.ManaCost cost, final int size) {
        final String key = cost + "@" + size;
        BufferedImage img = PIPS.get(key);
        if (img == null) {
            final int w = cost.getGlyphCount() * (size + 1) + 4, h = size + 4;
            final BufferedImage raw = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = raw.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            CardFaceSymbols.draw(g, cost, 2, 2, size);
            g.dispose();
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D out = img.createGraphics();
            out.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            out.setColor(new Color(0, 0, 0, 215));
            for (int i = 0; i < cost.getGlyphCount(); i++) {
                out.fillOval(1 + i * (size + 1), 1, size + 2, size + 2);
            }
            tintPips(raw, cost, size);
            out.drawImage(raw, 0, 0, null);
            out.dispose();
            PIPS.put(key, img);
        }
        return img;
    }

    /**
     * A keyword icon as a white glyph on nothing: the sprite's background colour is read from its
     * corners and whatever differs from it becomes the glyph, so every icon speaks in one style.
     */
    public static BufferedImage monochromeIcon(final String key, final Image src) {
        BufferedImage img = GLYPHS.get(key);
        if (img == null) {
            final int s = 48;
            final BufferedImage raw = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = raw.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, 0, 0, s, s, null);
            g.dispose();
            // the background: the commonest colour among pixels just inside the edge
            long br = 0, bg = 0, bb = 0;
            int n = 0;
            for (int i = 6; i < s - 6; i += 3) {
                for (final int rgb : new int[] {raw.getRGB(i, 5), raw.getRGB(i, s - 6), raw.getRGB(5, i), raw.getRGB(s - 6, i)}) {
                    if (((rgb >>> 24) & 255) > 128) {
                        br += (rgb >> 16) & 255;
                        bg += (rgb >> 8) & 255;
                        bb += rgb & 255;
                        n++;
                    }
                }
            }
            img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < s; y++) {
                for (int x = 0; x < s; x++) {
                    final int rgb = raw.getRGB(x, y);
                    final int a = (rgb >>> 24) & 255;
                    int alpha = a;
                    if (n > 0 && a > 0) {
                        final int d = Math.abs(((rgb >> 16) & 255) - (int) (br / n)) + Math.abs(((rgb >> 8) & 255) - (int) (bg / n)) + Math.abs((rgb & 255) - (int) (bb / n));
                        alpha = Math.min(a, Math.max(0, (d - 45) * 3));
                    }
                    img.setRGB(x, y, (alpha << 24) | 0xFFFFFF);
                }
            }
            GLYPHS.put(key, img);
        }
        return img;
    }


    /** A colour darkened to the given brightness. */
    public static Color shade(final Color c, final float factor) {
        return new Color(Math.round(c.getRed() * factor), Math.round(c.getGreen() * factor), Math.round(c.getBlue() * factor));
    }

    /** A colour with most of its saturation and some of its light gone, for tapped tiles. */
    public static Color fade(final Color c) {
        final int l = (c.getRed() * 30 + c.getGreen() * 59 + c.getBlue() * 11) / 100;
        return new Color((3 * c.getRed() + l) * 3 / 20, (3 * c.getGreen() + l) * 3 / 20, (3 * c.getBlue() + l) * 3 / 20);
    }

    public static boolean isLight(final Color c) {
        return (c.getRed() * 30 + c.getGreen() * 59 + c.getBlue() * 11) / 100 > 118;
    }

    /** The light plate power and toughness sit on, faintly tinted with the card's colour. */
    public static Color plateColor(final Color tint) {
        return new Color((tint.getRed() + 3 * 244) / 4, (tint.getGreen() + 3 * 240) / 4, (tint.getBlue() + 3 * 230) / 4);
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
                // avatars are small pictures: enlarge first, then bring the edges back
                final BufferedImage big = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                final Graphics2D bg = big.createGraphics();
                bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                bg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                bg.drawImage(src, 0, 0, size, size, null);
                bg.dispose();
                final java.awt.image.Kernel sharpen = new java.awt.image.Kernel(3, 3, new float[] {0f, -0.32f, 0f, -0.32f, 2.28f, -0.32f, 0f, -0.32f, 0f});
                g.drawImage(new java.awt.image.ConvolveOp(sharpen, java.awt.image.ConvolveOp.EDGE_NO_OP, null).filter(big, null),
                        Math.round(rim), Math.round(rim), Math.round(size - 2 * rim), Math.round(size - 2 * rim), null);
            } else {
                g.setColor(NAVY_LIGHT);
                g.fill(inner);
            }
            g.setComposite(AlphaComposite.SrcOver);
            // Inner shadow so the portrait sits inside the frame
            g.setPaint(new RadialGradientPaint(new Point2D.Float(size / 2f, size / 2f), size / 2f,
                    new float[] {0.68f, 1f}, new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 165)}));
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
