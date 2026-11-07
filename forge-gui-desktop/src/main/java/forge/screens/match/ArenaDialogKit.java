package forge.screens.match;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.toolbox.ArenaArt;
import forge.toolbox.ArenaCurtain;
import forge.toolbox.ArenaText;
import forge.toolbox.ArenaMotion;
import forge.toolbox.ArenaTheme;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FButton;
import forge.util.Localizer;
import forge.view.FView;

/**
 * What every Arena overlay shares, so all of them have one backdrop, one heading, one button
 * slot and one way of drawing a card: the dimmed scene behind, the title anchored near the top,
 * the row language of cards with a soft glow, cast shadow and lit rim, and the small controls.
 * All sizes are fractions of the window, so the overlays look the same at any resolution.
 */
final class ArenaDialogKit {
    private ArenaDialogKit() { }

    /** The saturated rim of anything that can be picked, and the bloom it throws. */
    static final Color CYAN = new Color(0x19C8FF);
    /** The thin bright core down the middle of that rim. */
    static final Color CYAN_CORE = new Color(0xD2F6FF);
    /** The rim of what has been picked, and its bloom. */
    static final Color PICKED = new Color(0xFFB52E);
    /** The thin bright core down the middle of that rim. */
    static final Color PICKED_CORE = new Color(0xFFF2CC);
    /** The corner radius of a card-like tile is half of this share of its width: about 5%. */
    static final float TILE_ARC = 0.1f;
    /** Transparent margin around card faces, so they stay smooth when rotated. */
    static final int FACE_PAD = 2;
    /** Where the middle of a row of cards or tiles sits. */
    static final float ROW_CENTRE_Y = 0.47f;
    /** Top and bottom of the area that holds rows of cards. */
    static final float ROW_AREA_TOP = 0.175f;
    static final float ROW_AREA_BOTTOM = 0.775f;

    private static final float BUTTON_W = 0.14f;
    private static final float BUTTON_H = 0.058f;
    private static final float BUTTON_CY = 0.81f;
    private static final float TITLE_CY = 0.09f;
    private static final float TITLE_SIZE = 0.055f;
    private static final int TITLE_MOST_CHARS = 30;
    private static final float TITLE_NARROW = 0.95f;
    private static final float RULE_Y = 0.13f;
    private static final float RULE_W = 0.42f;
    private static final float SUBTITLE_SIZE = 0.026f;
    private static final float DIM = 0.6f;
    /** How much colour the scene behind a dialog loses. */
    private static final float DESATURATE = 0.12f;
    /** How dark the vignette gets, part way to the edges and in the corners. */
    private static final float VIGNETTE_MID = 0.28f;
    private static final float VIGNETTE_CORNER = 0.58f;
    /** The end of the game lies on the same board, a little darker. */
    private static final float STAGE_DIM = 0.7f;
    private static final float SOURCE_H = 0.44f;
    /** The width of the tab beside the prompt's card, as a share of its height. */
    private static final float SOURCE_TAB_W = 0.82f;
    /** The number under a card where order matters, as a share of the window's height. */
    private static final float BADGE = 0.036f;
    /** What a row keeps clear of the tab of the prompt's card, beyond its own glow, as a share of the width. */
    private static final float SOURCE_CLEAR = 0.012f;
    /** A question as the title: the size of one that takes two lines at most and at least, and how wide it goes. */
    private static final float QUESTION_MOST = 0.042f;
    private static final float QUESTION_LEAST = 0.035f;
    private static final float QUESTION_W = 0.4f;
    /** The middle of a question's two lines. */
    private static final float QUESTION_CY = 0.078f;
    /** How far under a card the rail of a row in order runs. */
    private static final float RAIL_DROP = 0.025f;

    // ---- geometry

    static int buttonW(final int w) {
        return Math.round(w * BUTTON_W);
    }

    static int buttonH(final int h) {
        return Math.round(h * BUTTON_H);
    }

    /** The y of the top of the button slot. */
    static int buttonY(final int h) {
        return Math.round(h * BUTTON_CY) - buttonH(h) / 2;
    }

    /** Puts the visible ones of these side by side in the shared slot, all the same size. */
    static void placeButtons(final int w, final int h, final JComponent... buttons) {
        final List<JComponent> shown = new ArrayList<>();
        for (final JComponent b : buttons) {
            if (b.isVisible()) {
                shown.add(b);
            }
        }
        placeRow(w, h, buttonW(w), shown);
    }

    /** Puts these side by side in the shared slot, each {@code bw} wide; extra rows stack upwards. */
    static void placeRow(final int w, final int h, final int bw, final List<? extends JComponent> shown) {
        placeRowAt(w / 2, w, h, bw, shown);
    }

    /** As {@link #placeRow}, around {@code cx} rather than the middle of the window. */
    static void placeRowAt(final int cx, final int w, final int h, final int bw, final List<? extends JComponent> shown) {
        final int bh = buttonH(h), gap = Math.round(w * 0.012f);
        final int perRow = Math.max(1, (w - 2 * Math.round(w * 0.04f) + gap) / Math.max(1, bw + gap));
        final int rows = (shown.size() + perRow - 1) / Math.max(1, perRow);
        for (int i = 0; i < shown.size(); i++) {
            final int row = i / perRow;
            final int inRow = Math.min(perRow, shown.size() - row * perRow);
            final int rowW = inRow * bw + (inRow - 1) * gap;
            final int x = cx - rowW / 2 + (i % perRow) * (bw + gap);
            shown.get(i).setBounds(x, buttonY(h) - (rows - 1 - row) * (bh + gap), bw, bh);
        }
    }

    /** View Battlefield: top right, at the height of the title. */
    static Rectangle viewButtonBounds(final int w, final int h) {
        final int bw = Math.round(w * 0.125f), bh = buttonH(h);
        return new Rectangle(w - bw - Math.round(w * 0.024f), Math.round(h * TITLE_CY) - bh / 2, bw, bh);
    }

    /** How wide a row of cards may be, between the window's edges or in the space left of the prompt's card. */
    static int rowRoom(final int w, final int h, final boolean source) {
        // beside the prompt's card a row still centres on the window, so it has twice the reach to its right
        return source ? 2 * (rowRight(w, h, true) - w / 2) : w - 2 * Math.round(w * 0.03f);
    }

    /** How far right a row may reach. */
    static int rowRight(final int w, final int h, final boolean source) {
        return source ? sourceTabLeft(w, h) - sourceClear(w, h) : w - Math.round(w * 0.03f);
    }

    /**
     * Where a row this wide has its middle: in the middle of the window while that keeps it clear of
     * the prompt's card, else in the middle of the space left of that card.
     */
    static int rowCentre(final int w, final int h, final int rowW, final boolean source) {
        return !source || w / 2 + (rowW + 1) / 2 <= rowRight(w, h, true) ? w / 2 : sourceTabLeft(w, h) / 2;
    }

    /** The left end of the tab of the prompt's card, the first thing of it that a row comes to. */
    static int sourceTabLeft(final int w, final int h) {
        final Rectangle r = sourceBounds(w, h);
        return r.x - Math.round(sourceTabSize(r.height) * SOURCE_TAB_W);
    }

    /** That gap and the widest glow together. */
    private static int sourceClear(final int w, final int h) {
        final Glow glow = chosenGlow(h);
        return Math.round(w * SOURCE_CLEAR) + glow.rimWidth() + glow.fall();
    }

    private static int sourceTabSize(final int cardH) {
        return Math.round(cardH * 0.075f);
    }

    static int sourceHeight(final int h) {
        return Math.round(h * SOURCE_H);
    }

    static int sourceWidth(final int h) {
        return Math.round(sourceHeight(h) / forge.view.arcane.CardPanel.ASPECT_RATIO);
    }

    /** Where the prompt's card sits: on the right, level with the row. */
    static Rectangle sourceBounds(final int w, final int h) {
        final int sh = sourceHeight(h), sw = sourceWidth(h);
        return new Rectangle(w - sw - Math.round(w * 0.014f), Math.round(h * ROW_CENTRE_Y) - sh / 2, sw, sh);
    }

    // ---- coming into view

    /**
     * A dialog's way in: its backdrop fades over the board, then what it holds rises into place. While the
     * backdrop is fading the board has to be painted underneath it, so only then is the dialog not opaque.
     */
    static final class Entrance {
        private final JComponent owner;
        private final java.util.function.BooleanSupplier opaqueAfter;
        private final ArenaMotion.Fade fade;

        Entrance(final JComponent owner, final java.util.function.BooleanSupplier opaqueAfter) {
            this.owner = owner;
            this.opaqueAfter = opaqueAfter;
            this.fade = new ArenaMotion.Fade(owner::repaint, 1f);
        }

        void begin(final int millis) {
            owner.setOpaque(false);
            fade.set(0f);
            fade.to(1f, millis, () -> {
                owner.setOpaque(opaqueAfter.getAsBoolean());
                owner.repaint();
            });
        }

        /** Shows nothing until it begins. */
        void hide() {
            owner.setOpaque(false);
            fade.set(0f);
            owner.repaint();
        }

        boolean isOver() {
            return fade.get() >= 1f;
        }

        /** How far the backdrop and heading are in. */
        float scene() {
            return ArenaMotion.clamp01(fade.get() / 0.55f);
        }

        /** How far what the dialog holds is in. */
        float content() {
            return ArenaMotion.clamp01((fade.get() - 0.2f) / 0.8f);
        }

        /** The same for one of a row of things, each a little behind the one before. */
        float inTurn(final int index, final int count) {
            final float lag = count <= 1 ? 0f : Math.min(0.5f / (count - 1), 0.08f);
            return ArenaMotion.OUT.at(ArenaMotion.clamp01((content() - index * lag) / Math.max(0.1f, 1f - lag * Math.max(0, count - 1))));
        }

        /** Painting for the backdrop: the caller disposes of what it gets. */
        Graphics2D sceneGraphics(final Graphics g) {
            final Graphics2D c = (Graphics2D) g.create();
            if (scene() < 1f) {
                c.setComposite(java.awt.AlphaComposite.SrcOver.derive(scene()));
            }
            return c;
        }

        /** Painting for the contents: the caller disposes of what it gets. */
        Graphics2D contentGraphics(final Graphics g) {
            final Graphics2D c = (Graphics2D) g.create();
            final float in = content();
            if (in < 1f) {
                c.setComposite(java.awt.AlphaComposite.SrcOver.derive(in));
                c.translate(0, Math.round((1f - in) * owner.getHeight() * 0.025f));
            }
            return c;
        }
    }

    // ---- backdrop

    /** The window as it is, without the focus card (a dialog shows its own) or the board's action cluster (what the dialog asks comes first). */
    private static BufferedImage snapshot(final Component hide, final boolean bare) {
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        final int w = lpn.getWidth(), h = lpn.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D sg = out.createGraphics();
        final List<Component> hidden = new ArrayList<>();
        for (final Component c : lpn.getComponents()) {
            // a card preview may still be fading out, having just been dismissed for this dialog
            if (c.isVisible() && (c == hide || c instanceof ArenaFocusCard || c instanceof ArenaCardPreview || c instanceof TabBarDim || ArenaCurtain.isCurtain(c))) {
                hidden.add(c);
            }
        }
        final IVDoc<?> prompt = EDocID.REPORT_MESSAGE.getDoc();
        final DragCell cell = prompt == null ? null : prompt.getParentCell();
        if (cell != null && cell.isVisible()) {
            hidden.add(cell);
        }
        if (bare) {
            for (final Component c : FView.SINGLETON_INSTANCE.getPnlContent().getComponents()) {
                if (c.isVisible() && c instanceof ArenaCornerMenu) {
                    hidden.add(c);
                }
            }
        }
        for (final Component c : hidden) {
            c.setVisible(false);
        }
        try {
            // the board leaves its glows and bursts out of this one paint, so that the picture is unlit
            ArenaTheme.snapshotting = true;
            lpn.paint(sg);
        } finally {
            ArenaTheme.snapshotting = false;
            for (final Component c : hidden) {
                c.setVisible(true);
            }
            sg.dispose();
        }
        return out;
    }

    /**
     * Photographs the window sharp and dark, to sit opaque behind a dialog: nothing blurred, only
     * darkened, so the board keeps its colour and can still be read.
     */
    static BufferedImage captureBackdrop(final Component hide) {
        final BufferedImage out = snapshot(hide, false);
        return out == null ? null : graded(out, DIM);
    }

    /** The window as the end of the game shows it, without the board's corner menu: to be darkened by {@link #gradeStage}. */
    static BufferedImage snapshotStage() {
        return snapshot(null, true);
    }

    /** Darkens that picture as the end of the game shows it, more than a dialog's. Safe off the event thread. */
    static BufferedImage gradeStage(final BufferedImage photo) {
        return photo == null ? null : graded(photo, STAGE_DIM);
    }

    /** Takes a little of the colour out, lays the near-black dim over it, and darkens the edges in an oval the shape of the window. */
    private static BufferedImage graded(final BufferedImage out, final float dim) {
        final int w = out.getWidth(), h = out.getHeight();
        final int[] px = out.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; i++) {
            final int r = (px[i] >> 16) & 0xFF, gr = (px[i] >> 8) & 0xFF, b = px[i] & 0xFF;
            final float lum = (r * 3 + gr * 6 + b) / 10f;
            px[i] = (Math.round(r + (lum - r) * DESATURATE) << 16) | (Math.round(gr + (lum - gr) * DESATURATE) << 8) | Math.round(b + (lum - b) * DESATURATE);
        }
        out.setRGB(0, 0, w, h, px, 0, w);

        final Graphics2D g = out.createGraphics();
        try {
            g.setColor(new Color(4, 5, 7, Math.round(255 * dim)));
            g.fillRect(0, 0, w, h);
            final float cx = w / 2f, cy = h * ROW_CENTRE_Y;
            final AffineTransform squash = AffineTransform.getTranslateInstance(cx, cy);
            squash.scale(1, h / (double) w);
            squash.translate(-cx, -cy);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), w * 0.74f, new Point2D.Float(cx, cy), new float[] {0f, 0.42f, 0.72f, 1f},
                    new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 0, 0, Math.round(255 * VIGNETTE_MID)), new Color(0, 0, 0, Math.round(255 * VIGNETTE_CORNER))},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, squash));
            g.fillRect(0, 0, w, h);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** Three box blurs, which together fall off as softly as a Gaussian about {@code r} wide. */
    static void softBlur(final int[] px, final int w, final int h, final int r) {
        for (int i = 0; i < 3; i++) {
            boxBlur(px, w, h, r);
        }
    }

    static void boxBlur(final int[] px, final int w, final int h, final int r) {
        final int[] tmp = new int[px.length];
        final int span = 2 * r + 1;
        for (int y = 0; y < h; y++) {
            int cr = 0, cg = 0, cb = 0;
            for (int i = -r; i <= r; i++) {
                final int p = px[y * w + clamp(i, w)];
                cr += (p >> 16) & 0xFF;
                cg += (p >> 8) & 0xFF;
                cb += p & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                tmp[y * w + x] = ((cr / span) << 16) | ((cg / span) << 8) | (cb / span);
                final int add = px[y * w + clamp(x + r + 1, w)], sub = px[y * w + clamp(x - r, w)];
                cr += ((add >> 16) & 0xFF) - ((sub >> 16) & 0xFF);
                cg += ((add >> 8) & 0xFF) - ((sub >> 8) & 0xFF);
                cb += (add & 0xFF) - (sub & 0xFF);
            }
        }
        for (int x = 0; x < w; x++) {
            int cr = 0, cg = 0, cb = 0;
            for (int i = -r; i <= r; i++) {
                final int p = tmp[clamp(i, h) * w + x];
                cr += (p >> 16) & 0xFF;
                cg += (p >> 8) & 0xFF;
                cb += p & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                px[y * w + x] = ((cr / span) << 16) | ((cg / span) << 8) | (cb / span);
                final int add = tmp[clamp(y + r + 1, h) * w + x], sub = tmp[clamp(y - r, h) * w + x];
                cr += ((add >> 16) & 0xFF) - ((sub >> 16) & 0xFF);
                cg += ((add >> 8) & 0xFF) - ((sub >> 8) & 0xFF);
                cb += (add & 0xFF) - (sub & 0xFF);
            }
        }
    }

    private static int clamp(final int v, final int size) {
        return v < 0 ? 0 : Math.min(size - 1, v);
    }

    static void paintBackdrop(final Graphics2D g, final BufferedImage backdrop, final int w, final int h) {
        if (backdrop == null) {
            g.setColor(new Color(0x101114));
            g.fillRect(0, 0, w, h);
        } else if (backdrop.getWidth() == w && backdrop.getHeight() == h) {
            g.drawImage(backdrop, 0, 0, null);
        } else {
            final Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(backdrop, 0, 0, w, h, null);
            if (old != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
            }
        }
    }

    /**
     * The navigation bar sits above the overlay layer and has to stay usable, so it is dimmed by
     * a pane in front of it that lets every click through.
     */
    @SuppressWarnings("serial")
    static final class TabBarDim extends JComponent {
        private final Component bar = FView.SINGLETON_INSTANCE.getNavigationBar();
        /** It darkens and clears with the dialog it belongs to rather than switching. */
        private final ArenaMotion.Fade shown = new ArenaMotion.Fade(this::repaint, 0f);

        void attach() {
            final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
            if (getParent() == null) {
                lpn.add(this, FView.NAVIGATION_BAR_LAYER);
                shown.set(0f);
            }
            lpn.moveToFront(this);
            setBounds(bar.getBounds());
            setVisible(bar.isVisible());
            shown.to(1f, 260);
        }

        void detach() {
            if (getParent() == null) {
                return;
            }
            shown.to(0f, 200, () -> {
                final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
                if (getParent() == lpn) {
                    final Rectangle r = getBounds();
                    lpn.remove(this);
                    lpn.repaint(r);
                }
            });
        }

        @Override
        public boolean contains(final int x, final int y) {
            return false;
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final float a = DIM * shown.get();
            if (a > 0f) {
                g.setColor(new Color(0f, 0f, 0f, Math.min(1f, a)));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    // ---- heading

    static void quality(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** Message text as it comes from the game: HTML tags off, line breaks kept. */
    static String plainText(final String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("(?s)<head>.*?</head>", "").replaceAll("<br\\s*/?>", "\n").replaceAll("<[^>]*>", "").trim();
    }

    /** Whether two texts say the same thing, so one of them need not be shown. */
    static boolean sameWords(final String a, final String b) {
        return b != null && a.replaceAll("[^\\p{L}\\p{N}]", "").equalsIgnoreCase(b.replaceAll("[^\\p{L}\\p{N}]", ""));
    }

    /** The y of the rule under every title. */
    static int ruleY(final int h) {
        return Math.round(h * RULE_Y);
    }

    /** The face of titles: the display face, drawn a little narrower. */
    static Font titleFont(final float size) {
        return ArenaTheme.displayFont(Font.BOLD, size).deriveFont(AffineTransform.getScaleInstance(TITLE_NARROW, 1));
    }

    /** How tall capitals are in the current font, to centre words by what the eye sees. */
    static float capHeight(final Graphics2D g) {
        return (float) g.getFont().createGlyphVector(g.getFontRenderContext(), "H").getVisualBounds().getHeight();
    }

    /** A title too long for the one line gives way to {@code fallback}, which may be none, and moves into the subtitle: {title, subtitle}. */
    private static String[] fitHeading(final Graphics2D g, final String title, final String subtitle, final String fallback, final int w, final int h) {
        // a card's name arrives with the game's internal id after it, which means nothing to a player
        final String heading = title == null ? "" : title.replaceAll("\\s*\\n\\s*", " ").replaceAll("\\s*\\(\\d+\\)\\s*$", "");
        g.setFont(titleFont(h * TITLE_SIZE));
        if (heading.isEmpty()) {
            return new String[] {fallback, subtitle};
        }
        final int room = Math.round(w * RULE_W * 0.9f);
        if (heading.length() <= TITLE_MOST_CHARS && lineWidth(wrap(heading, g, room * 2).get(0), g) <= room) {
            return new String[] {heading, subtitle};
        }
        return new String[] {fallback, subtitle == null || subtitle.isEmpty() ? heading : heading + " \u00B7 " + subtitle};
    }

    /** The subtitle's lines, at most two; none when there is no subtitle, so it never leaves a gap. Sets the font. */
    private static List<List<String>> subtitleLines(final Graphics2D g, final String subtitle, final int w, final int h) {
        final Font font = ArenaTheme.displayFont(Font.PLAIN, h * SUBTITLE_SIZE);
        g.setFont(font);
        if (subtitle == null || subtitle.isEmpty()) {
            return List.of();
        }
        List<List<String>> lines = wrap(font.canDisplay('\u00B7') ? subtitle : subtitle.replace('\u00B7', '-'), g, Math.round(w * 0.62f));
        if (lines.size() > 2) {
            lines = new ArrayList<>(lines.subList(0, 2));
            lines.get(1).add("…");
        }
        return lines;
    }

    private static int subtitleTop(final int h) {
        return ruleY(h) + Math.round(h * 0.008f);
    }

    /** The y just below the heading these would make: under the subtitle if there is one, else under the rule. */
    static int headingBottom(final String title, final String subtitle, final String fallback, final int w, final int h) {
        final Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            quality(g);
            final List<List<String>> lines = subtitleLines(g, fitHeading(g, title, subtitle, fallback, w, h)[1], w, h);
            return lines.isEmpty() ? ruleY(h) : subtitleTop(h) + lines.size() * g.getFontMetrics().getHeight();
        } finally {
            g.dispose();
        }
    }

    /**
     * The one heading: one line of title at one fixed size and height, a rule of fixed width that
     * is always wider than it, and the instruction centred beneath. A title that would not fit on
     * the line becomes {@code fallback}, with its words moved down into the instruction.
     */
    static void paintHeading(final Graphics2D g, final String title, final String subtitle, final String fallback, final int w, final int h) {
        quality(g);
        final String[] fitted = fitHeading(g, title, subtitle, fallback, w, h);
        if (fitted[0] != null) {
            final List<String> titleLine = wrap(fitted[0], g, w).get(0);
            paintTitleLine(g, titleLine, (w - lineWidth(titleLine, g)) / 2, Math.round(h * TITLE_CY + capHeight(g) / 2f));
            paintRule(g, w / 2, ruleY(h), Math.round(w * RULE_W), h, Color.WHITE);
        }

        final List<List<String>> lines = subtitleLines(g, fitted[1], w, h);
        final FontMetrics fm = g.getFontMetrics();
        int ly = subtitleTop(h) + fm.getAscent();
        for (final List<String> line : lines) {
            final int lx = (w - lineWidth(line, g)) / 2;
            drawHalo(g, line, lx, ly, 2);
            g.setColor(new Color(0, 0, 0, 190));
            drawLine(g, line, lx, ly + 1);
            g.setColor(Color.WHITE);
            drawLine(g, line, lx, ly);
            ly += fm.getHeight();
        }
    }

    /** One line of a title: a shadow of 2px and no more. */
    private static void paintTitleLine(final Graphics2D g, final List<String> line, final int x, final int baseline) {
        drawHalo(g, line, x, baseline, 2);
        g.setColor(new Color(0, 0, 0, 190));
        drawLine(g, line, x, baseline + 2);
        g.setColor(Color.WHITE);
        drawLine(g, line, x, baseline);
    }

    /** The size at which a question makes a title of at most two lines, or below zero when it is too long for that. */
    static float questionSize(final String question, final int w, final int h) {
        final Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            quality(g);
            final int room = Math.round(w * QUESTION_W);
            g.setFont(titleFont(h * TITLE_SIZE));
            if (wrap(question, g, room).size() == 1) {
                return h * TITLE_SIZE;
            }
            for (float size = h * QUESTION_MOST; size >= h * QUESTION_LEAST; size -= 1f) {
                g.setFont(titleFont(size));
                if (wrap(question, g, room).size() <= 2) {
                    return size;
                }
            }
            return -1f;
        } finally {
            g.dispose();
        }
    }

    /** A question as the whole heading, its two lines about equal, over the rule of every title. */
    static void paintQuestionTitle(final Graphics2D g, final String question, final float size, final int w, final int h) {
        quality(g);
        g.setFont(titleFont(size));
        int room = Math.round(w * QUESTION_W);
        List<List<String>> lines = wrap(question, g, room);
        while (lines.size() == 2 && room > w / 4) {
            final List<List<String>> tighter = wrap(question, g, room - Math.round(w * 0.01f));
            if (tighter.size() != 2) {
                break;
            }
            room -= Math.round(w * 0.01f);
            lines = tighter;
        }
        final int pitch = Math.round(size * 1.16f);
        final float cap = capHeight(g);
        int baseline = lines.size() == 1 ? Math.round(h * TITLE_CY + cap / 2f) : Math.round(h * QUESTION_CY - pitch / 2f + cap / 2f);
        int widest = 0;
        for (final List<String> line : lines) {
            final int lineW = lineWidth(line, g);
            widest = Math.max(widest, lineW);
            paintTitleLine(g, line, (w - lineW) / 2, baseline);
            baseline += pitch;
        }
        // the one rule of every title
        paintRule(g, w / 2, ruleY(h), Math.round(w * RULE_W), h, Color.WHITE);
    }

    /** Words that stand on the board by themselves: the fill with a thin dark edge round every letter. */
    static void paintEdged(final Graphics2D g, final String words, final float x, final float baseline, final Color fill) {
        final Shape letters = g.getFont().createGlyphVector(g.getFontRenderContext(), words).getOutline(x, baseline);
        final float edge = Math.max(1.4f, Math.min(2.4f, g.getFont().getSize2D() * 0.075f));
        g.setStroke(new BasicStroke(edge, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0, 0, 0, 225));
        g.draw(letters);
        g.setColor(fill);
        g.fill(letters);
    }

    /** One wrapped line of such words, any mana symbols among them as they are. */
    static void paintEdgedLine(final Graphics2D g, final List<String> line, final int x, final int baseline, final Color fill) {
        final FontMetrics fm = g.getFontMetrics();
        final int symbol = symbolSize(g);
        int lx = x;
        for (final String token : line) {
            final String key = ArenaText.symbolKey(token);
            if (key != null) {
                CardFaceSymbols.drawSymbol(key, g, lx + 1, baseline - symbol + Math.round(symbol * 0.08f), symbol);
                lx += symbol + 2;
            } else {
                if (!token.isBlank()) {
                    paintEdged(g, token, lx, baseline, fill);
                }
                lx += fm.stringWidth(token);
            }
        }
    }

    /** A dark haze that follows the letters themselves, so words hold up on the board without a plate behind them. */
    private static void drawHalo(final Graphics2D g, final List<String> line, final int x, final int baseline, final int r) {
        g.setColor(new Color(0, 0, 0, 46));
        for (int dy = -r; dy <= r; dy += r) {
            for (int dx = -r; dx <= r; dx += r) {
                if (dx != 0 || dy != 0) {
                    drawLine(g, line, x + dx, baseline + dy);
                }
            }
        }
    }

    /** A thin line that fades out at both ends. */
    static void paintRule(final Graphics2D g, final int cx, final int y, final int width, final int h, final Color c) {
        final float x0 = cx - width / 2f, x1 = x0 + width;
        g.setPaint(new LinearGradientPaint(x0, 0, x1, 0, new float[] {0f, 0.1f, 0.9f, 1f},
                new Color[] {ArenaTheme.alpha(c, 0), ArenaTheme.alpha(c, 215), ArenaTheme.alpha(c, 215), ArenaTheme.alpha(c, 0)}));
        g.fillRect(Math.round(x0), y, width, Math.max(1, Math.round(h * 0.0012f)));
    }

    // ---- text with mana symbols: as ArenaText, but the symbols stand on the baseline at text height

    private static final Pattern TOKEN = Pattern.compile("\\{[^}]+\\}|\\s+|[^\\s{]+");

    private static int symbolSize(final Graphics2D g) {
        return Math.round(g.getFont().getSize2D() * 0.95f);
    }

    static List<List<String>> wrap(final String text, final Graphics2D g, final int width) {
        final FontMetrics fm = g.getFontMetrics();
        final int symbol = symbolSize(g) + 2;
        final List<List<String>> lines = new ArrayList<>();
        for (final String paragraph : text.split("\\r?\\n")) {
            List<String> line = new ArrayList<>();
            int lineW = 0;
            final Matcher m = TOKEN.matcher(paragraph.trim());
            while (m.find()) {
                final String token = m.group();
                final boolean space = token.isBlank();
                final int tw = ArenaText.symbolKey(token) != null ? symbol : fm.stringWidth(space ? " " : token);
                if (!space && lineW + tw > width && !line.isEmpty()) {
                    lines.add(line);
                    line = new ArrayList<>();
                    lineW = 0;
                }
                if (space && line.isEmpty()) {
                    continue;
                }
                line.add(space ? " " : token);
                lineW += tw;
            }
            lines.add(line);
        }
        return lines;
    }

    static int lineWidth(final List<String> line, final Graphics2D g) {
        final FontMetrics fm = g.getFontMetrics();
        final int symbol = symbolSize(g) + 2;
        int w = 0;
        for (final String token : line) {
            w += ArenaText.symbolKey(token) != null ? symbol : fm.stringWidth(token);
        }
        return w;
    }

    /** Draws one wrapped line with its baseline at {@code baseline}. */
    static void drawLine(final Graphics2D g, final List<String> line, final int x, final int baseline) {
        drawLine(g, line, x, baseline, false);
    }

    /** As above; on a light ground each symbol stands on a dark disc, as printed on a card. */
    static void drawLine(final Graphics2D g, final List<String> line, final int x, final int baseline, final boolean onLight) {
        final FontMetrics fm = g.getFontMetrics();
        final int symbol = symbolSize(g);
        final Color text = g.getColor();
        int lx = x;
        for (final String token : line) {
            final String key = ArenaText.symbolKey(token);
            if (key != null) {
                final int sx = lx + 1, sy = baseline - symbol + Math.round(symbol * 0.08f);
                if (onLight) {
                    g.setColor(new Color(0x201A12));
                    g.fill(new java.awt.geom.Ellipse2D.Float(sx - 1f, sy - 1f, symbol + 2f, symbol + 2f));
                    g.setColor(text);
                    g.drawImage(printedSymbol(key, symbol), sx, sy, symbol, symbol, null);
                } else {
                    CardFaceSymbols.drawSymbol(key, g, sx, sy, symbol);
                }
                lx += symbol + 2;
            } else {
                g.drawString(token, lx, baseline);
                lx += fm.stringWidth(token);
            }
        }
    }

    private static final Map<String, BufferedImage> PRINTED = new HashMap<>();

    /**
     * A mana symbol for a light ground. A coloured one keeps its colours with its pale tones pulled
     * down. One of no colour ({2}, {C}, {T}...) is pale line art that would vanish into cream, so it
     * is turned round: the pale figure on the dark disc.
     */
    private static BufferedImage printedSymbol(final String key, final int size) {
        final String id = key + "@" + size;
        BufferedImage img = PRINTED.get(id);
        if (img == null) {
            final int big = size * 2;
            img = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = img.createGraphics();
            quality(g);
            CardFaceSymbols.drawSymbol(key, g, 0, 0, big);
            g.dispose();
            final boolean colourless = !key.matches(".*[WUBRG].*");
            final int[] px = img.getRGB(0, 0, big, big, null, 0, big);
            for (int i = 0; i < px.length; i++) {
                final int a = px[i] >>> 24;
                final int r = (px[i] >> 16) & 0xFF, gr = (px[i] >> 8) & 0xFF, b = px[i] & 0xFF;
                if (colourless) {
                    // the figure is what was dark in it; what was pale goes back to the disc
                    final float figure = Math.max(0f, Math.min(1f, (205f - (r * 3 + gr * 6 + b) / 10f) / 110f));
                    px[i] = (Math.round(a * figure) << 24) | 0xFFF7E6;
                } else {
                    px[i] = (a << 24) | (deepen(r) << 16) | (deepen(gr) << 8) | deepen(b);
                }
            }
            img.setRGB(0, 0, big, big, px, 0, big);
            PRINTED.put(id, img);
        }
        return img;
    }

    private static int deepen(final int v) {
        return Math.max(0, Math.min(255, Math.round((v - 150) * 1.45f + 136)));
    }

    // ---- card faces, glows and the row language

    /** A card image on a transparent margin with smooth rounded corners. */
    static BufferedImage cardFace(final BufferedImage src, final int w, final int h) {
        return cardFace(src, w, h, 1);
    }

    /** As above, premultiplied and {@code q} times the size, for a card that is going to be rotated. */
    static BufferedImage cardFace(final BufferedImage src, final int w, final int h, final int q) {
        final BufferedImage out = new BufferedImage((w + 2 * FACE_PAD) * q, (h + 2 * FACE_PAD) * q, BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D g = out.createGraphics();
        try {
            quality(g);
            g.scale(q, q);
            final float arc = cardArc(w);
            g.setColor(new Color(0x0B0C10));
            g.fill(new RoundRectangle2D.Float(FACE_PAD, FACE_PAD, w, h, arc, arc));
            g.setComposite(AlphaComposite.SrcAtop);
            if (src != null) {
                final float scale = Math.min(w / (float) src.getWidth(), h / (float) src.getHeight());
                final float iw = src.getWidth() * scale, ih = src.getHeight() * scale;
                final AffineTransform at = AffineTransform.getTranslateInstance(FACE_PAD + (w - iw) / 2f, FACE_PAD + (h - ih) / 2f);
                at.scale(scale, scale);
                g.drawImage(src, at, null);
            }
        } finally {
            g.dispose();
        }
        return out;
    }

    static int cardArc(final int w) {
        return Math.round(w * 0.085f);
    }

    /** The corners of a card-like tile, or of one lying on its side. */
    static int tileArc(final int w, final int h) {
        return w > h ? Math.round(h * 0.2f) : Math.round(w * TILE_ARC);
    }

    private static final Map<String, BufferedImage> GLOWS = new HashMap<>();

    /**
     * The light of a rounded rectangle of this size, on an image {@code spread} larger each way:
     * solid inside and out to {@code rim} from its edge, then falling away softly to nothing at the
     * image's edge. Made once for each size and colour.
     */
    static BufferedImage edgeGlow(final int w, final int h, final int arc, final Color c, final int rim, final int spread, final float rimAlpha, final float outerAlpha) {
        final String key = w + "x" + h + "/" + arc + "/" + c.getRGB() + "/" + rim + "/" + spread + "/" + rimAlpha + "/" + outerAlpha;
        BufferedImage img = GLOWS.get(key);
        if (img != null) {
            return img;
        }
        if (GLOWS.size() > 24) {
            GLOWS.clear();
        }
        final int iw = w + 2 * spread, ih = h + 2 * spread;
        img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        final int[] px = new int[iw * ih];
        final float r = Math.min(arc, Math.min(w, h)) / 2f, hx = w / 2f - r, hy = h / 2f - r;
        final float fall = Math.max(1f, spread - rim);
        final int rgb = c.getRGB() & 0xFFFFFF;
        for (int y = 0; y < ih; y++) {
            final float py = Math.abs(y + 0.5f - ih / 2f) - hy, oy = Math.max(py, 0f);
            for (int x = 0; x < iw; x++) {
                final float qx = Math.abs(x + 0.5f - iw / 2f) - hx, ox = Math.max(qx, 0f);
                final float d = (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, py), 0f) - r;
                float a = rimAlpha;
                if (d > rim) {
                    final float t = Math.min(1f, (d - rim) / fall);
                    a = outerAlpha * (1f - t) * (1f - t);
                    if (d < rim + 1f) {
                        a += (rimAlpha - a) * (rim + 1f - d); // the rim's own edge stays smooth
                    }
                }
                px[y * iw + x] = (Math.round(a * 255f) << 24) | rgb;
            }
        }
        img.setRGB(0, 0, iw, ih, px, 0, iw);
        GLOWS.put(key, img);
        return img;
    }

    /**
     * The light of a rim round a rounded rectangle of this size, on an image larger each way by the
     * rim and its bloom. Across the rim a thin core runs brightest down the middle, the saturated
     * colour either side of it, and from there the bloom falls away along a curve with a long tail,
     * never reaching an edge that could be seen. A trace of noise keeps so shallow a slope from banding.
     */
    private static BufferedImage rimLight(final int w, final int h, final int arc, final Glow glow) {
        final String key = "rim/" + w + "x" + h + "/" + arc + "/" + glow;
        BufferedImage img = GLOWS.get(key);
        if (img != null) {
            return img;
        }
        if (GLOWS.size() > 24) {
            GLOWS.clear();
        }
        final int rim = glow.rimWidth(), spread = rim + glow.fall();
        final int iw = w + 2 * spread, ih = h + 2 * spread;
        final int[] px = new int[iw * ih];
        final float r = Math.min(arc, Math.min(w, h)) / 2f, hx = w / 2f - r, hy = h / 2f - r;
        final float middle = rim / 2f, coreHalf = glow.coreWidth() / 2f + 0.7f, fall = glow.fall() + middle;
        final Color c0 = glow.rim(), c1 = glow.core();
        int seed = 0x2F6E2B1;
        for (int y = 0; y < ih; y++) {
            final float py = Math.abs(y + 0.5f - ih / 2f) - hy, oy = Math.max(py, 0f);
            for (int x = 0; x < iw; x++) {
                final float qx = Math.abs(x + 0.5f - iw / 2f) - hx, ox = Math.max(qx, 0f);
                final float d = Math.max(0f, (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, py), 0f) - r);
                final float core = smooth(1f - Math.abs(d - middle) / coreHalf);
                final float body = smooth((rim + 1f - d) / 2f) * 0.97f;
                final float t = Math.min(1f, Math.max(0f, d - middle) / fall);
                final float bloom = glow.alpha() * (1f - t) * (1f - t) * (1f - t);
                final float a = body + (1f - body) * bloom;
                seed ^= seed << 13;
                seed ^= seed >>> 17;
                seed ^= seed << 5;
                final int alpha = Math.max(0, Math.min(255, Math.round(a * 255f + (a > 0f ? ((seed & 0xFF) / 255f - 0.5f) : 0f))));
                final float m = core * body;
                px[y * iw + x] = (alpha << 24)
                        | (Math.round(c0.getRed() + (c1.getRed() - c0.getRed()) * m) << 16)
                        | (Math.round(c0.getGreen() + (c1.getGreen() - c0.getGreen()) * m) << 8)
                        | Math.round(c0.getBlue() + (c1.getBlue() - c0.getBlue()) * m);
            }
        }
        img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, iw, ih, px, 0, iw);
        GLOWS.put(key, img);
        return img;
    }

    /** What that light does to the card's own edge: a little of it spills in over the border and dies away within the width of the rim. */
    private static BufferedImage rimSpill(final int w, final int h, final int arc, final Glow glow) {
        final String key = "spill/" + w + "x" + h + "/" + arc + "/" + glow;
        BufferedImage img = GLOWS.get(key);
        if (img != null) {
            return img;
        }
        if (GLOWS.size() > 24) {
            GLOWS.clear();
        }
        final int[] px = new int[w * h];
        final float r = Math.min(arc, Math.min(w, h)) / 2f, hx = w / 2f - r, hy = h / 2f - r;
        final float reach = Math.max(2f, glow.rimWidth() * 0.8f);
        final int rgb = glow.rim().getRGB() & 0xFFFFFF;
        for (int y = 0; y < h; y++) {
            final float py = Math.abs(y + 0.5f - h / 2f) - hy, oy = Math.max(py, 0f);
            for (int x = 0; x < w; x++) {
                final float qx = Math.abs(x + 0.5f - w / 2f) - hx, ox = Math.max(qx, 0f);
                final float d = (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, py), 0f) - r;
                final float t = Math.max(0f, 1f + Math.min(0f, d) / reach);
                final float a = d > 0.5f ? 0f : 0.62f * t * t;
                px[y * w + x] = (Math.round(a * 255f) << 24) | rgb;
            }
        }
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        GLOWS.put(key, img);
        return img;
    }

    private static float smooth(final float v) {
        final float t = Math.max(0f, Math.min(1f, v));
        return t * t * (3f - 2f * t);
    }

    /** Where the shards lie under a row: {across its width, below its feet as a share of the height, length as a share of the height, turn in degrees clockwise from upright}. */
    private static final float[][] SHARDS = {
        {0.08f, 0.02f, 0.042f, -68f}, {0.2f, 0.054f, 0.03f, 42f}, {0.35f, 0.026f, 0.05f, 76f}, {0.47f, 0.058f, 0.028f, -32f},
        {0.62f, 0.022f, 0.04f, 108f}, {0.74f, 0.056f, 0.046f, -82f}, {0.91f, 0.028f, 0.034f, 56f}
    };

    /**
     * Splinters of the light lying at the feet of a row that can be picked from, always in the same
     * places and only below the cards, so they never come near any card's words. {@code colour} says
     * what colour the light is at a given x.
     */
    static void paintRowShards(final Graphics2D g, final int x0, final int x1, final int foot, final int h, final IntFunction<Color> colour) {
        quality(g);
        for (final float[] s : SHARDS) {
            final int x = Math.round(x0 + (x1 - x0) * s[0]);
            ArenaArt.paintShard(g, x, foot + h * s[1], h * s[2], (float) Math.toRadians(s[3]), colour.apply(x));
        }
    }

    /**
     * Parchment for a text box: a soft gradient with fine grain and the slow mottling of old paper,
     * and an inner bevel as if the box were pressed into its frame.
     */
    static void paintParchment(final Graphics2D g, final Rectangle r, final Color top, final Color bottom, final float arc, final long seed) {
        paintMottled(g, r, top, bottom, arc, seed, -1f);
    }

    /** The same mottling for what stands proud rather than lying pressed in: a frame, lit along its top and left. */
    static void paintFrameMetal(final Graphics2D g, final Rectangle r, final Color top, final Color bottom, final float arc, final long seed) {
        paintMottled(g, r, top, bottom, arc, seed, 1f);
    }

    private static void paintMottled(final Graphics2D g, final Rectangle r, final Color top, final Color bottom, final float arc, final long seed, final float raised) {
        if (r.width <= 0 || r.height <= 0) {
            return;
        }
        final BufferedImage img = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D o = img.createGraphics();
        quality(o);
        o.setColor(Color.WHITE);
        o.fill(new RoundRectangle2D.Float(0, 0, r.width, r.height, arc, arc));
        o.dispose();
        final int[] px = img.getRGB(0, 0, r.width, r.height, null, 0, r.width);
        final Random noise = new Random(seed);
        final int cell = Math.max(8, r.width / 9), cols = r.width / cell + 2, rows = r.height / cell + 2;
        final float[] lattice = new float[cols * rows];
        for (int i = 0; i < lattice.length; i++) {
            lattice[i] = noise.nextFloat() - 0.5f;
        }
        final float bevel = Math.max(3f, Math.min(r.width, r.height) * 0.028f);
        for (int y = 0; y < r.height; y++) {
            final float t = r.height > 1 ? y / (r.height - 1f) : 0f, fy = y / (float) cell;
            final int iy = (int) fy;
            final float dy = fy - iy;
            for (int x = 0; x < r.width; x++) {
                final float fx = x / (float) cell;
                final int ix = (int) fx;
                final float dx = fx - ix;
                final float upper = lattice[iy * cols + ix] * (1f - dx) + lattice[iy * cols + ix + 1] * dx;
                final float lower = lattice[(iy + 1) * cols + ix] * (1f - dx) + lattice[(iy + 1) * cols + ix + 1] * dx;
                final float mottle = upper * (1f - dy) + lower * dy;
                final float light = mottle * 15f + (noise.nextFloat() - 0.5f) * 11f
                        + raised * (40f * lip(y, bevel) + 22f * lip(x, bevel) - 26f * lip(r.height - 1 - y, bevel) - 14f * lip(r.width - 1 - x, bevel));
                px[y * r.width + x] = (px[y * r.width + x] & 0xFF000000)
                        | (shade(top.getRed(), bottom.getRed(), t, light) << 16)
                        | (shade(top.getGreen(), bottom.getGreen(), t, light) << 8)
                        | shade(top.getBlue(), bottom.getBlue(), t, light * 0.9f);
            }
        }
        img.setRGB(0, 0, r.width, r.height, px, 0, r.width);
        g.drawImage(img, r.x, r.y, null);
    }

    private static float lip(final int fromEdge, final float bevel) {
        final float v = Math.max(0f, 1f - fromEdge / bevel);
        return v * v;
    }

    private static int shade(final int a, final int b, final float t, final float light) {
        return Math.max(0, Math.min(255, Math.round(a + (b - a) * t + light)));
    }

    /**
     * The light round a card: a rim of {@code rimWidth} in {@code rim} with a {@code core} of
     * {@code coreWidth} down its middle, then a bloom from {@code alpha} down to nothing over {@code fall}.
     */
    record Glow(Color core, Color rim, int rimWidth, int coreWidth, int fall, float alpha) { }

    /** The light of what can be picked, in a window {@code h} high: a rim of 7 and a bloom out to 1.5% of a wide window's width at 1080p. */
    static Glow selectableGlow(final int h, final float strength) {
        return new Glow(CYAN_CORE, CYAN, Math.max(3, Math.round(h * 0.0064f)), Math.max(1, Math.round(h * 0.0016f)), Math.max(8, Math.round(h * 0.0267f)), Math.min(0.95f, 0.62f * strength));
    }

    /** The light of what has been picked: the same in gold, a little wider and much stronger. */
    static Glow chosenGlow(final int h) {
        return new Glow(PICKED_CORE, PICKED, Math.max(3, Math.round(h * 0.0072f)), Math.max(1, Math.round(h * 0.002f)), Math.max(10, Math.round(h * 0.032f)), 0.9f);
    }

    /** The rim of a card under the mouse that cannot be picked. */
    static Glow plainGlow(final int h) {
        return new Glow(Color.WHITE, new Color(0xDDE6F2), Math.max(1, Math.round(h * 0.002f)), 1, Math.max(8, Math.round(h * 0.014f)), 0.3f);
    }

    /** The same light for a small flat tile, where there is little room around it. */
    static Glow smallGlow(final Glow glow, final int h) {
        return new Glow(glow.core(), glow.rim(), Math.max(2, Math.round(h * 0.003f)), 1, Math.max(6, Math.round(h * 0.009f)), glow.alpha() * 0.85f);
    }

    /** One card or tile drawn at an angle over its shadow and glow, kept until any of that changes. */
    static final class Sprite {
        private final float arcShare;
        private String key;
        private BufferedImage image;

        Sprite() {
            this(0.085f);
        }

        /** {@code arcShare} is the width of the corners' arcs as a share of the width of the face. */
        Sprite(final float arcShare) {
            this.arcShare = arcShare;
        }

        /**
         * Draws {@code face} (with its FACE_PAD margin, {@code q} times the size) centred on
         * {@code cx, cy}. {@code glow} may be null; the card then only has its soft shadow.
         */
        void draw(final Graphics2D g, final BufferedImage face, final int q, final int cx, final int cy, final float angle, final float scale,
                final Glow glow, final boolean dimmed) {
            final float a = Math.round(angle * 20f) / 20f, s = Math.round(scale * 100f) / 100f;
            final String k = System.identityHashCode(face) + "/" + a + "/" + s + "/" + glow + "/" + dimmed;
            if (!k.equals(key)) {
                key = k;
                image = render(face, q, a, s, glow, dimmed);
            }
            g.drawImage(image, cx - image.getWidth() / 2, cy - image.getHeight() / 2, null);
        }

        private BufferedImage render(final BufferedImage face, final int q, final float angle, final float scale, final Glow glow, final boolean dimmed) {
            final int fw = face.getWidth() / q - 2 * FACE_PAD, fh = face.getHeight() / q - 2 * FACE_PAD;
            final int sw = Math.round(fw * scale), sh = Math.round(fh * scale);
            final int shadowSpread = Math.max(8, Math.round(sh * 0.039f)); // 16px on a card of 38% at 1080p
            final int cast = Math.max(2, Math.round(shadowSpread * 0.4f));
            final int spread = glow == null ? 0 : glow.rimWidth() + glow.fall();
            final int reach = Math.max(spread, shadowSpread + cast);
            final double rad = Math.toRadians(Math.abs(angle));
            final int size = (int) Math.ceil(sw * Math.cos(rad) + sh * Math.sin(rad)) + 2 * reach + 8;
            final int sizeH = (int) Math.ceil(sw * Math.sin(rad) + sh * Math.cos(rad)) + 2 * reach + 8;
            final BufferedImage out = new BufferedImage(size, sizeH, BufferedImage.TYPE_INT_ARGB_PRE);
            final Graphics2D g = out.createGraphics();
            try {
                quality(g);
                g.translate(size / 2d, sizeH / 2d);
                g.rotate(Math.toRadians(angle));
                final int arc = sw > sh ? Math.round(sh * 0.2f) : Math.round(sw * arcShare);
                // the shadow falls down and left, onto the cards below this one in the pile
                final BufferedImage shadow = edgeGlow(sw, sh, arc, Color.BLACK, 0, shadowSpread, 0.85f, 0.85f);
                g.drawImage(shadow, AffineTransform.getTranslateInstance(-shadow.getWidth() / 2d - cast, -shadow.getHeight() / 2d + cast), null);
                if (glow != null) {
                    final BufferedImage light = rimLight(sw, sh, arc, glow);
                    g.drawImage(light, AffineTransform.getTranslateInstance(-light.getWidth() / 2d, -light.getHeight() / 2d), null);
                }
                final AffineTransform at = new AffineTransform();
                at.scale(scale / q, scale / q);
                at.translate(-face.getWidth() / 2d, -face.getHeight() / 2d);
                g.drawImage(face, at, null);
                if (dimmed) {
                    g.setColor(new Color(0, 0, 0, 150));
                    g.fill(new RoundRectangle2D.Float(-sw / 2f, -sh / 2f, sw, sh, arc, arc));
                } else if (glow == null) {
                    // a thin lit edge on every card, so that black borders do not run into each other
                    g.setStroke(new BasicStroke(1f));
                    g.setColor(new Color(236, 242, 250, 165));
                    g.draw(new RoundRectangle2D.Float(-sw / 2f + 0.5f, -sh / 2f + 0.5f, sw - 1f, sh - 1f, arc, arc));
                } else {
                    final BufferedImage spill = rimSpill(sw, sh, arc, glow);
                    g.drawImage(spill, AffineTransform.getTranslateInstance(-sw / 2d, -sh / 2d), null);
                    // one smooth line over the seam, which on a tilted card would otherwise show as a row of notches
                    g.setStroke(new BasicStroke(1.5f));
                    g.setColor(glow.rim());
                    g.draw(new RoundRectangle2D.Float(-sw / 2f, -sh / 2f, sw, sh, arc, arc));
                }
            } finally {
                g.dispose();
            }
            return out;
        }
    }

    /** Whether a point lies in a rectangle of half-size {@code hw, hh}, centred on {@code cx, cy} and rotated. */
    static boolean inRotated(final float px, final float py, final float cx, final float cy, final float hw, final float hh, final float angle) {
        final double a = Math.toRadians(-angle);
        final double dx = px - cx, dy = py - cy;
        final double rx = dx * Math.cos(a) - dy * Math.sin(a), ry = dx * Math.sin(a) + dy * Math.cos(a);
        return Math.abs(rx) <= hw && Math.abs(ry) <= hh;
    }

    /**
     * The prompt's card: lit by a cool soft bloom but with no selection edge, so it does not read
     * as an option. On the right of a row a small tab says it is the source of the decision.
     */
    static void paintSource(final Graphics2D g, final BufferedImage face, final Rectangle r, final boolean tab) {
        final int fw = face.getWidth() - 2 * FACE_PAD, fh = face.getHeight() - 2 * FACE_PAD;
        final int spread = Math.round(r.height * 0.036f);
        final int arc = cardArc(fw);
        final BufferedImage bloom = edgeGlow(fw, fh, arc, new Color(0x9CC4F0), 0, spread, 0.5f, 0.5f);
        g.drawImage(bloom, r.x - spread, r.y - spread, null);
        if (tab) {
            paintSourceTab(g, r.x, r.y + Math.round(r.height * 0.13f), sourceTabSize(r.height));
        }
        g.drawImage(face, r.x - FACE_PAD, r.y - FACE_PAD, null);
    }

    /** A small dark tab with two gold chevrons, tucked behind the left edge of a card at {@code x}. */
    private static void paintSourceTab(final Graphics2D g, final int x, final int y, final int s) {
        quality(g);
        final float tw = s * SOURCE_TAB_W, arc = s * 0.32f;
        final RoundRectangle2D shape = new RoundRectangle2D.Float(x - tw, y, tw + arc, s, arc, arc);
        g.setPaint(new GradientPaint(0, y, new Color(0x2B2D33), 0, y + s, new Color(0x0E0F13)));
        g.fill(shape);
        g.setStroke(new BasicStroke(1f));
        g.setColor(ArenaTheme.alpha(PICKED, 200));
        g.draw(shape);
        g.setStroke(new BasicStroke(Math.max(1.6f, s * 0.085f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(PICKED);
        final float cy = y + s / 2f, leg = s * 0.2f;
        for (int i = 0; i < 2; i++) {
            final float cx = x - tw * 0.62f + i * s * 0.24f;
            final GeneralPath p = new GeneralPath();
            p.moveTo(cx - leg * 0.55f, cy - leg);
            p.lineTo(cx + leg * 0.55f, cy);
            p.lineTo(cx - leg * 0.55f, cy + leg);
            g.draw(p);
        }
    }

    /** How wide the number under a card is, in a window this high. */
    static int badgeSize(final int h) {
        return Math.round(h * BADGE);
    }

    /** A number in a dark disc with a bright ring. */
    static void paintBadge(final Graphics2D g, final int place, final int cx, final int cy, final int d) {
        final Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(cx - d / 2 - 2, cy - d / 2 + 1, d + 4, d + 4);
        g.setPaint(new GradientPaint(0, cy - d / 2f, new Color(0x2A2C33), 0, cy + d / 2f, new Color(0x0C0D11)));
        g.fillOval(cx - d / 2, cy - d / 2, d, d);
        g.setColor(ArenaTheme.GOLD_BRIGHT);
        g.setStroke(new BasicStroke(Math.max(1.5f, d * 0.055f)));
        g.drawOval(cx - d / 2, cy - d / 2, d, d);
        g.setFont(ArenaTheme.numeralFont(Font.BOLD, d * 0.56f));
        final FontMetrics fm = g.getFontMetrics();
        final String n = String.valueOf(place);
        g.setColor(Color.WHITE);
        g.drawString(n, cx - fm.stringWidth(n) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    /** A word above a row of cards, centred on {@code cx}. */
    static void paintRowLabel(final Graphics2D g, final String label, final int cx, final int baseline, final int h) {
        quality(g);
        g.setFont(ArenaTheme.displayFont(Font.BOLD, h * 0.024f));
        paintEdged(g, label, cx - g.getFontMetrics().stringWidth(label) / 2f, baseline, Color.WHITE);
    }

    /** What the first end of a row means, spoken for the last end: "Resolve first" gives "Resolve last". */
    static String lastCaption(final String first) {
        if (first != null && first.toLowerCase().endsWith("first")) {
            final boolean upper = Character.isUpperCase(first.charAt(first.length() - 5));
            return first.substring(0, first.length() - 5) + (upper ? "Last" : "last");
        }
        return Localizer.getInstance().getMessage("lblArenaLast");
    }

    /** How far below a card's bottom edge the rail runs. */
    static int railDrop(final int h) {
        return Math.max(3, Math.round(h * RAIL_DROP));
    }

    /** How far below a card's bottom edge the middle of the words under a row in order lies, clear of the numbers on its rail. */
    static int orderRowDrop(final int h) {
        return railDrop(h) + badgeSize(h) / 2 + Math.round(h * 0.018f);
    }

    /** The thin gold rail that the numbers of a row in order sit on, with a small stop at each end. */
    static void paintRail(final Graphics2D g, final int x0, final int x1, final int y, final int h) {
        quality(g);
        final float t = Math.max(1.5f, h * 0.0016f), stop = t * 2.2f;
        g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 70));
        g.fill(new RoundRectangle2D.Float(x0, y - t * 1.5f, x1 - x0, t * 3f, t * 3f, t * 3f));
        g.setColor(ArenaTheme.GOLD_BRIGHT);
        g.fill(new RoundRectangle2D.Float(x0, y - t / 2f, x1 - x0, t, t, t));
        g.fill(new Ellipse2D.Float(x0 - stop, y - stop, 2f * stop, 2f * stop));
        g.fill(new Ellipse2D.Float(x1 - stop, y - stop, 2f * stop, 2f * stop));
    }

    /** What the two ends of a row in order mean, each under the number at its end. */
    static void paintRailEnds(final Graphics2D g, final String first, final String last, final int firstX, final int lastX, final int cy, final int h) {
        quality(g);
        g.setFont(ArenaTheme.displayFont(Font.BOLD, h * 0.021f));
        final int baseline = Math.round(cy + capHeight(g) / 2f);
        final FontMetrics fm = g.getFontMetrics();
        paintEdged(g, first, firstX - fm.stringWidth(first) / 2f, baseline, Color.WHITE);
        if (lastX != firstX) {
            paintEdged(g, last, lastX - fm.stringWidth(last) / 2f, baseline, Color.WHITE);
        }
    }

    /** The one round button, for steppers and rows that scroll: a dark disc in a gold rim, with a bold chevron. */
    static void paintRoundButton(final Graphics2D g, final float cx, final float cy, final float d, final int dx, final int dy, final boolean lit, final boolean off) {
        quality(g);
        final Graphics2D c = (Graphics2D) g.create();
        try {
            if (off) {
                c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            }
            final float rim = Math.max(2f, d * 0.05f), inner = d - rim;
            final Ellipse2D disc = new Ellipse2D.Float(cx - inner / 2f, cy - inner / 2f, inner, inner);
            c.setPaint(new GradientPaint(0, cy - d / 2f, new Color(lit ? 0x3A332A : 0x1F1C18), 0, cy + d / 2f, new Color(lit ? 0x1E1A15 : 0x0A0908)));
            c.fill(disc);
            c.setStroke(new BasicStroke(rim));
            c.setPaint(new GradientPaint(0, cy - d / 2f, lit ? Color.WHITE : ArenaTheme.GOLD_BRIGHT, 0, cy + d / 2f, ArenaTheme.GOLD));
            c.draw(disc);
            c.translate(cx, cy);
            if (dy != 0) {
                c.rotate(dy > 0 ? Math.PI / 2 : -Math.PI / 2);
            } else if (dx < 0) {
                c.rotate(Math.PI);
            }
            final float leg = d * 0.2f;
            final GeneralPath p = new GeneralPath();
            p.moveTo(-leg * 0.45f, -leg);
            p.lineTo(leg * 0.55f, 0);
            p.lineTo(-leg * 0.45f, leg);
            c.setStroke(new BasicStroke(Math.max(3f, d * 0.105f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            c.setColor(Color.WHITE);
            c.draw(p);
        } finally {
            c.dispose();
        }
    }

    /** A card-shaped hollow that says something can be dropped here. */
    static void paintSlot(final Graphics2D g, final int cx, final int cy, final int cw, final int ch) {
        final int arc = cardArc(cw);
        final RoundRectangle2D shape = new RoundRectangle2D.Float(cx - cw / 2f, cy - ch / 2f, cw, ch, arc, arc);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(shape);
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {7f, 7f}, 0f));
        g.draw(shape);
    }

    // ---- small controls

    /** A row container that fades its edges when it scrolls, so nothing is ever cut by a hard line. */
    @SuppressWarnings("serial")
    static final class FadeStrip extends JPanel {
        private final boolean horizontal;
        private final Consumer<Graphics2D> onTop;
        private float fade = 0.06f;
        private BufferedImage buffer;

        FadeStrip(final boolean horizontal, final Consumer<Graphics2D> onTop) {
            super(null);
            this.horizontal = horizontal;
            this.onTop = onTop;
            setOpaque(false);
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false; // the children overlap
        }

        @Override
        protected void paintChildren(final Graphics g) {
            final Rectangle vis = getVisibleRect();
            final boolean before = horizontal ? vis.x > 0 : vis.y > 0;
            final boolean after = horizontal ? vis.x + vis.width < getWidth() : vis.y + vis.height < getHeight();
            if ((!before && !after) || vis.width <= 0 || vis.height <= 0) {
                super.paintChildren(g);
                if (onTop != null) {
                    onTop.accept((Graphics2D) g);
                }
                return;
            }
            if (buffer == null || buffer.getWidth() != vis.width || buffer.getHeight() != vis.height) {
                buffer = new BufferedImage(vis.width, vis.height, BufferedImage.TYPE_INT_ARGB);
            }
            final Graphics2D b = buffer.createGraphics();
            try {
                b.setComposite(AlphaComposite.Clear);
                b.fillRect(0, 0, vis.width, vis.height);
                b.setComposite(AlphaComposite.SrcOver);
                final Graphics2D content = (Graphics2D) b.create();
                content.translate(-vis.x, -vis.y);
                content.setClip(vis.x, vis.y, vis.width, vis.height);
                super.paintChildren(content);
                if (onTop != null) {
                    onTop.accept(content);
                }
                content.dispose();
                // the edges fall away softly on the sides that have more
                final float len = horizontal ? vis.width : vis.height;
                final Color in = new Color(0, 0, 0, 0), out = Color.BLACK;
                b.setComposite(AlphaComposite.DstIn);
                b.setPaint(new LinearGradientPaint(0, 0, horizontal ? len : 0, horizontal ? 0 : len,
                        new float[] {0f, fade, 1f - fade, 1f},
                        new Color[] {before ? in : out, out, out, after ? in : out}));
                b.fillRect(0, 0, vis.width, vis.height);
            } finally {
                b.dispose();
            }
            g.drawImage(buffer, vis.x, vis.y, null);
        }

        void setFade(final float fade0) {
            fade = fade0;
        }
    }

    /** A small round arrow at the edge of a row that scrolls. */
    @SuppressWarnings("serial")
    static final class Chevron extends JComponent {
        private final int dx, dy;
        private boolean hover;

        Chevron(final int dx, final int dy, final Runnable action) {
            this.dx = dx;
            this.dy = dy;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    action.run();
                }
            });
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                paintRoundButton(g, getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) - 2, dx, dy, hover, false);
            } finally {
                g.dispose();
            }
        }
    }

    /** The remember box: a gold outline around a dark well with a gold tick, and its words in the display face. */
    @SuppressWarnings("serial")
    static final class CheckBox extends JCheckBox {
        private static final float TEXT_AT = 1.5f;
        private boolean hover;

        CheckBox(final String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        /** The size this wants when its box is {@code box} high. */
        Dimension sizeFor(final int box) {
            final Font font = ArenaTheme.displayFont(Font.PLAIN, box * 0.92f);
            return new Dimension(Math.round(box * TEXT_AT) + getFontMetrics(font).stringWidth(getText()) + 4, box + 6);
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                quality(g);
                final int box = getHeight() - 6, x = 1, y = 3;
                final float arc = box * 0.24f, line = Math.max(1.6f, box * 0.09f);
                final RoundRectangle2D shape = new RoundRectangle2D.Float(x + line / 2f, y + line / 2f, box - line, box - line, arc, arc);
                g.setPaint(new GradientPaint(0, y, new Color(0x0C0B09), 0, y + box, new Color(0x1E1B16)));
                g.fill(shape);
                g.setStroke(new BasicStroke(line));
                g.setColor(hover ? ArenaTheme.GOLD_BRIGHT : ArenaTheme.GOLD);
                g.draw(shape);
                if (isSelected()) {
                    final GeneralPath tick = new GeneralPath();
                    tick.moveTo(x + box * 0.27f, y + box * 0.52f);
                    tick.lineTo(x + box * 0.44f, y + box * 0.69f);
                    tick.lineTo(x + box * 0.74f, y + box * 0.31f);
                    g.setStroke(new BasicStroke(box * 0.24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 70));
                    g.draw(tick);
                    g.setStroke(new BasicStroke(Math.max(1.8f, box * 0.13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(ArenaTheme.GOLD_BRIGHT);
                    g.draw(tick);
                }
                g.setFont(ArenaTheme.displayFont(Font.PLAIN, box * 0.92f));
                paintEdged(g, getText(), Math.round(box * TEXT_AT), Math.round((getHeight() + capHeight(g)) / 2f), Color.WHITE);
            } finally {
                g.dispose();
            }
        }
    }

    /** A slim thumb on a faint track with no arrow buttons, for text that scrolls. */
    static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(final int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(final int orientation) {
            return createZeroButton();
        }

        private static JButton createZeroButton() {
            final JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(final Graphics g, final JComponent c, final Rectangle r) {
            g.setColor(new Color(255, 255, 255, 18));
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                g.fillRoundRect(r.x + r.width / 2 - 2, r.y, 4, r.height, 4, 4);
            } else {
                g.fillRoundRect(r.x, r.y + r.height / 2 - 2, r.width, 4, 4, 4);
            }
        }

        @Override
        protected void paintThumb(final Graphics g, final JComponent c, final Rectangle r) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 120));
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                g2.fillRoundRect(r.x + r.width / 2 - 3, r.y, 6, r.height, 6, 6);
            } else {
                g2.fillRoundRect(r.x, r.y + r.height / 2 - 3, r.width, 6, 6, 6);
            }
        }
    }

    /** Paints a pill, with its label centred by what the eye sees. */
    static void paintPill(final Graphics2D g, final int w, final int h, final String text, final boolean primary,
            final boolean hover, final boolean pressed, final boolean toggled, final boolean enabled) {
        quality(g);
        ArenaTheme.paintButton(g, w, h, primary, hover, pressed, false, toggled, enabled);
        if (text == null || text.isEmpty()) {
            return;
        }
        float size = Math.max(12f, Math.min(h * 0.5f, 30f));
        while (size > 10f && ArenaTheme.paintLabel(g, text, size, 0f, 0f, false) > w - h * 0.7f) {
            size -= 1f;
        }
        final float tx = (w - ArenaTheme.paintLabel(g, text, size, 0f, 0f, false)) / 2f;
        g.setFont(ArenaTheme.displayFont(Font.BOLD, size));
        final int ty = Math.round((h + capHeight(g)) / 2f);
        if (enabled) {
            g.setColor(new Color(60, 22, 0, primary ? 190 : 150));
            ArenaTheme.paintLabel(g, text, size, tx, ty + 1, true);
            g.setColor(Color.WHITE);
        } else {
            g.setColor(new Color(255, 255, 255, 178));
        }
        ArenaTheme.paintLabel(g, text, size, tx, ty, true);
    }

    /** The overlays' buttons: an FButton in everything but its label, which sits in the true middle of the pill. */
    @SuppressWarnings("serial")
    static final class Pill extends FButton {
        private boolean primary;
        private boolean hover;

        Pill() {
            this("");
        }

        Pill(final String text) {
            super(text);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hover = true;
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hover = false;
                }
            });
        }

        @Override
        public void setPrimary(final boolean b0) {
            primary = b0;
            super.setPrimary(b0);
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                paintPill(g, getWidth(), getHeight(), getText(), primary, hover && isEnabled(), getModel().isPressed() && getModel().isArmed(), isToggled(), isEnabled());
            } finally {
                g.dispose();
            }
        }
    }
}
