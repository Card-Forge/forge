package forge.screens.match;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import forge.toolbox.ArenaArt;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FButton;
import forge.util.Localizer;
import forge.view.FView;

/**
 * The end of a game in the Arena look: over the same sharp dimmed board as every dialog, rays of
 * light with embers rising behind a trophy, the player's portrait set in it, a large banner of cast
 * metal and one score line just above the buttons. The orange pill goes on to the
 * next thing to play, the other actions are plain pills beside it, and the log waits behind one
 * dim icon in the corner. A mode's own panel (quest rewards, gauntlet standings...) takes the
 * right of the screen when it has one.
 */
@SuppressWarnings("serial")
final class ArenaEndScreen extends JPanel {
    enum Outcome { VICTORY, DEFEAT, NEUTRAL }

    /** Where the trophy has its middle, and so where the rays' light comes from, as a share of the height. */
    private static final float MEDALLION_Y = 0.355f;
    /** The height that the burst art takes its point of light from: 32% of the way down. */
    private static final float BURST_POINT = 0.32f;
    /** The middle of the banner and of the score line, close above the buttons. */
    private static final float BANNER_Y = 0.595f;
    private static final float SCORE_Y = 0.722f;
    private static final float BANNER_SIZE = 0.12f;
    private static final int EMBERS = 30;

    /** A small round button with a drawn icon, dim until the mouse comes to it. */
    private static final class LogButton extends JButton {
        private boolean hover;

        LogButton() {
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

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                ArenaDialogKit.quality(g);
                final int d = Math.min(getWidth(), getHeight()) - 2;
                final Ellipse2D disc = new Ellipse2D.Float((getWidth() - d) / 2f, (getHeight() - d) / 2f, d, d);
                g.setColor(new Color(8, 9, 12, hover ? 235 : 90));
                g.fill(disc);
                g.setStroke(new BasicStroke(1.2f));
                g.setColor(new Color(255, 255, 255, hover ? 210 : 56));
                g.draw(disc);
                ArenaArt.paintLogIcon(g, getWidth() / 2, getHeight() / 2, Math.round(d * 0.6f), hover ? Color.WHITE : new Color(232, 230, 224, 110));
            } finally {
                g.dispose();
            }
        }
    }

    private final Outcome outcome;
    private final String banner;
    private final Image avatar;
    private final FButton btnNext;
    private final FButton btnRestart;
    private final FButton btnQuit;
    private final JComponent custom;
    private final JScrollPane log;
    private final LogButton btnLog = new LogButton();
    private final FButton btnCopy;
    private final BufferedImage backdrop;
    private final ArenaDialogKit.TabBarDim tabBarDim = new ArenaDialogKit.TabBarDim();
    private BufferedImage scene;
    private String score = "";
    private boolean sceneCustom;
    private boolean logShown;
    private Rectangle logPanel;

    ArenaEndScreen(final Outcome outcome, final String banner, final Image avatar, final FButton btnNext, final FButton btnRestart,
            final FButton btnQuit, final JComponent custom, final JScrollPane log, final Runnable copyLog, final String copyText) {
        super(null);
        this.outcome = outcome;
        this.banner = banner;
        this.avatar = avatar;
        this.btnNext = btnNext;
        this.btnRestart = btnRestart;
        this.btnQuit = btnQuit;
        this.custom = custom;
        this.log = log;
        backdrop = ArenaDialogKit.captureStage();
        setOpaque(true);

        final Localizer localizer = Localizer.getInstance();
        btnLog.setToolTipText(localizer.getMessage("lblArenaShowLog"));
        btnLog.addActionListener(e -> toggleLog());
        btnCopy = new ArenaDialogKit.Pill(copyText);
        btnCopy.addActionListener(e -> copyLog.run());
        btnCopy.setVisible(false);
        log.setVisible(false);

        // the mode's own controller may change the buttons at any time
        final PropertyChangeListener relayout = e -> relayout();
        final ComponentAdapter relayoutOnShow = new ComponentAdapter() {
            @Override
            public void componentShown(final ComponentEvent e) {
                relayout();
            }

            @Override
            public void componentHidden(final ComponentEvent e) {
                relayout();
            }
        };
        for (final FButton b : List.of(btnNext, btnRestart, btnQuit)) {
            b.addPropertyChangeListener("enabled", relayout);
            b.addPropertyChangeListener("text", relayout);
            b.addComponentListener(relayoutOnShow);
        }
        add(btnCopy);
        add(log);
        add(btnNext);
        add(btnRestart);
        add(btnQuit);
        add(btnLog);
        if (custom != null) {
            add(custom);
        }
        addMouseListener(new MouseAdapter() { });
        // the tab bar stays above the overlay, so it is dimmed for as long as this shows
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    tabBarDim.attach();
                } else {
                    tabBarDim.detach();
                }
            }
        });
    }

    private void relayout() {
        revalidate();
        repaint();
    }

    /** The one score line, once the game log has it. */
    void setScore(final String score0) {
        score = score0 == null ? "" : score0;
        scene = null;
        repaint();
    }

    private void toggleLog() {
        logShown = !logShown;
        btnLog.setToolTipText(Localizer.getInstance().getMessage(logShown ? "lblArenaHideLog" : "lblArenaShowLog"));
        log.setVisible(logShown);
        btnCopy.setVisible(logShown);
        if (custom != null) {
            custom.setVisible(!logShown); // the log lies over it
        }
        relayout();
    }

    private boolean hasCustom() {
        return custom != null;
    }

    /** The forward action: the next game when there is one, else a new match; quitting only when nothing else is left. */
    private FButton primary() {
        for (final FButton b : List.of(btnNext, btnRestart)) {
            if (b.isVisible() && b.isEnabled()) {
                return b;
            }
        }
        return btnQuit;
    }

    @Override
    public void doLayout() {
        final int w = getWidth(), h = getHeight();
        final int cx = hasCustom() ? Math.round(w * 0.22f) : w / 2;

        // one orange pill goes on; the rest are plain pills beside it, all of one size in the shared slot
        final FButton main = primary();
        final List<JComponent> row = new ArrayList<>();
        for (final FButton b : List.of(btnNext, btnRestart, btnQuit)) {
            b.setPrimary(b == main);
            if (b == main || (b.isVisible() && b.isEnabled())) {
                row.add(b);
            } else {
                b.setBounds(0, 0, 0, 0); // a next game that cannot be started is not offered
            }
        }
        final int bw = hasCustom() && row.size() > 2 ? Math.round(w * 0.12f) : ArenaDialogKit.buttonW(w);
        ArenaDialogKit.placeRowAt(cx, w, h, bw, row);

        // in the board's top corner, clear of the navigation bar if there is one
        final int icon = Math.round(h * 0.036f), edge = Math.round(h * 0.012f);
        final int contentY = SwingUtilities.convertPoint(FView.SINGLETON_INSTANCE.getPnlContent(), 0, 0, this).y;
        btnLog.setBounds(w - icon - edge, Math.max(0, contentY) + edge, icon, icon);

        if (hasCustom()) {
            custom.setBounds(Math.round(w * 0.455f), Math.round(h * 0.075f), Math.round(w * 0.515f), Math.round(h * 0.85f));
        }
        logPanel = new Rectangle(Math.round(w * 0.27f), Math.round(h * 0.09f), Math.round(w * 0.46f), Math.round(h * 0.66f));
        final int pad = Math.round(h * 0.022f), copyH = Math.round(h * 0.04f);
        log.setBounds(logPanel.x + pad, logPanel.y + pad, logPanel.width - 2 * pad, logPanel.height - 2 * pad - copyH - pad / 2);
        final int copyW = getFontMetrics(ArenaTheme.displayFont(Font.BOLD, Math.max(12f, Math.min(copyH * 0.46f, 26f)))).stringWidth(btnCopy.getText()) + 2 * copyH;
        btnCopy.setBounds(logPanel.x + logPanel.width - pad - copyW, logPanel.y + logPanel.height - pad - copyH, copyW, copyH);
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        final int w = getWidth(), h = getHeight();
        if (scene == null || scene.getWidth() != w || scene.getHeight() != h || sceneCustom != hasCustom()) {
            sceneCustom = hasCustom();
            scene = renderScene(w, h);
        }
        g0.drawImage(scene, 0, 0, null);
        if (logShown && logPanel != null) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                ArenaDialogKit.quality(g);
                final float arc = h * 0.03f;
                final RoundRectangle2D shape = new RoundRectangle2D.Float(logPanel.x, logPanel.y, logPanel.width, logPanel.height, arc, arc);
                g.setColor(new Color(0x0A0B0E));
                g.fill(shape);
                g.setColor(new Color(255, 255, 255, 40));
                g.setStroke(new BasicStroke(1f));
                g.draw(shape);
            } finally {
                g.dispose();
            }
        }
    }

    private BufferedImage renderScene(final int w, final int h) {
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = out.createGraphics();
        try {
            ArenaDialogKit.quality(g);
            ArenaDialogKit.paintBackdrop(g, backdrop, w, h);
            final boolean side = hasCustom();
            final int cx = side ? Math.round(w * 0.22f) : w / 2;
            final int avatarSize = portraitSize(h, side);
            final int avatarCy = Math.round(h * MEDALLION_Y);
            final int bannerCy = Math.round(h * BANNER_Y);
            // the rays' light comes from behind the trophy, and the embers rise behind its metal
            final Graphics2D bg = (Graphics2D) g.create();
            bg.translate(cx - w / 2, 0);
            // twice, since each pass only reaches about 15% once clear of the trophy, and this should come to about 30%
            ArenaArt.paintVictoryBurst(bg, w, Math.round(avatarCy / BURST_POINT), coreColour());
            ArenaArt.paintVictoryBurst(bg, w, Math.round(avatarCy / BURST_POINT), coreColour());
            bg.dispose();
            paintEmbers(g, cx, avatarCy, avatarSize);
            ArenaArt.paintTrophyFrame(g, cx, avatarCy, avatarSize, outcome == Outcome.VICTORY);
            paintPortrait(g, cx, avatarCy, avatarSize);
            paintBanner(g, cx, bannerCy, side ? Math.round(w * 0.4f) : Math.round(w * 0.8f), h, w);
            if (!score.isEmpty()) {
                g.setFont(ArenaTheme.numeralFont(Font.PLAIN, h * 0.03f));
                final FontMetrics fm = g.getFontMetrics();
                final int sy = Math.round(h * SCORE_Y) + fm.getAscent() / 2;
                final float sx = cx - fm.stringWidth(score) / 2f;
                g.setColor(new Color(0, 0, 0, 170));
                g.drawString(score, sx, sy + 2);
                ArenaDialogKit.paintEdged(g, score, sx, sy, new Color(0xF4F1EA));
            }
            if (side) {
                final float arc = h * 0.03f;
                final RoundRectangle2D panel = new RoundRectangle2D.Float(w * 0.445f, h * 0.06f, w * 0.535f, h * 0.88f, arc, arc);
                g.setColor(new Color(8, 9, 12, 240));
                g.fill(panel);
                g.setColor(new Color(255, 255, 255, 34));
                g.setStroke(new BasicStroke(1f));
                g.draw(panel);
            }
        } finally {
            g.dispose();
        }
        return out;
    }

    private Color coreColour() {
        switch (outcome) {
            case VICTORY: return new Color(0xFFC860);
            case DEFEAT: return new Color(0x7E93B8);
            default: return new Color(0xC9CDD6);
        }
    }

    /** The banner's metal at the top of its letters and at the bottom. */
    private Color[] bannerMetal() {
        switch (outcome) {
            case VICTORY: return new Color[] {new Color(0xFFE9A8), new Color(0xD9A441)};
            case DEFEAT: return new Color[] {new Color(0xEEF1F6), new Color(0x9AA6BA)};
            default: return new Color[] {Color.WHITE, new Color(0xB9BDC6)};
        }
    }

    /** The embers' colour, and how bright they get. */
    private Color emberColour() {
        switch (outcome) {
            case VICTORY: return new Color(0xFFC766);
            case DEFEAT: return new Color(0xA9B4C8);
            default: return new Color(0xD8D4C8);
        }
    }

    /** A portrait as large as its picture will take without showing its pixels, within what the layout allows. */
    private int portraitSize(final int h, final boolean side) {
        final int most = Math.round(h * (side ? 0.17f : 0.18f)), least = Math.round(h * 0.14f);
        final int src = avatar == null ? 0 : Math.max(avatar.getWidth(null), avatar.getHeight(null));
        return src <= 0 ? most : Math.max(least, Math.min(most, src * 2));
    }

    /** The picture cut to a circle, set in the trophy with a thin dark line between it and the metal. */
    private void paintPortrait(final Graphics2D g, final int cx, final int cy, final int size) {
        final Ellipse2D disc = new Ellipse2D.Float(cx - size / 2f, cy - size / 2f, size, size);
        if (avatar == null) {
            g.setColor(new Color(0x14110D));
            g.fill(disc);
        } else {
            final BufferedImage round = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
            final Graphics2D o = round.createGraphics();
            try {
                ArenaDialogKit.quality(o);
                o.setColor(Color.WHITE);
                o.fill(new Ellipse2D.Float(0, 0, size, size));
                o.setComposite(java.awt.AlphaComposite.SrcIn);
                o.drawImage(crisp(avatar, size), 0, 0, size, size, null);
            } finally {
                o.dispose();
            }
            g.drawImage(round, cx - size / 2, cy - size / 2, null);
        }
        g.setStroke(new BasicStroke(Math.max(1.5f, size * 0.008f)));
        g.setColor(new Color(0x1A120A));
        g.draw(disc);
    }

    /** Small soft sparks rising behind the trophy and out from under it, each with a faint trail beneath it, thinning out as they climb; always in the same places. */
    private void paintEmbers(final Graphics2D g, final int cx, final int cy, final int size) {
        final Random r = new Random(0xE3BE25L);
        final Color c = emberColour();
        final float strength = outcome == Outcome.VICTORY ? 1f : 0.6f;
        for (int i = 0; i < EMBERS; i++) {
            final float climb = r.nextFloat();
            // turn about left and right, so that they lie evenly round the portrait
            float dx = (i % 2 == 0 ? -1f : 1f) * (0.08f + 0.92f * r.nextFloat()) * size * (2.5f - 0.8f * climb);
            final float dy = size * (0.9f - 2.5f * climb);
            final float radius = size * (0.007f + 0.015f * r.nextFloat() * r.nextFloat());
            final float alpha = strength * (0.95f - 0.55f * climb) * (0.55f + 0.45f * r.nextFloat());
            if (dx * dx + dy * dy < size * size * 0.42f) {
                dx = dx < 0f ? dx - size * 0.72f : dx + size * 0.72f; // clear of the picture
            }
            final float x = cx + dx, y = cy + dy, halo = radius * 2.8f;
            final float trail = radius * (2.5f + 2.5f * r.nextFloat());
            g.setPaint(new GradientPaint(0, y, ArenaTheme.alpha(c, Math.round(64 * alpha)), 0, y + trail, ArenaTheme.alpha(c, 0)));
            g.fill(new RoundRectangle2D.Float(x - radius * 0.32f, y, radius * 0.64f, trail, radius * 0.64f, radius * 0.64f));
            g.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), halo, new float[] {0f, 0.32f, 1f},
                    new Color[] {ArenaTheme.alpha(new Color(0xFFF3D0), Math.round(255 * alpha)), ArenaTheme.alpha(c, Math.round(170 * alpha)), ArenaTheme.alpha(c, 0)}));
            g.fill(new Ellipse2D.Float(x - halo, y - halo, 2f * halo, 2f * halo));
        }
    }

    /**
     * The picture at exactly this size. Large ones come down in halves and small ones go up in
     * doubles, bicubic all the way, so that neither shows its pixels; going up, the edges are drawn
     * back a little with no halo.
     */
    private static Image crisp(final Image src, final int size) {
        final int sw = src.getWidth(null), sh = src.getHeight(null);
        if (sw <= 0 || sh <= 0) {
            return src;
        }
        BufferedImage img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        final boolean enlarged = Math.max(sw, sh) < size;
        while (img.getWidth() != size || img.getHeight() != size) {
            final int nw = img.getWidth() < size ? Math.min(size, img.getWidth() * 2) : Math.max(size, img.getWidth() / 2);
            final int nh = img.getHeight() < size ? Math.min(size, img.getHeight() * 2) : Math.max(size, img.getHeight() / 2);
            final BufferedImage next = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB_PRE);
            g = next.createGraphics();
            ArenaDialogKit.quality(g);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = next;
        }
        if (!enlarged) {
            return img;
        }
        final int[] px = img.getRGB(0, 0, size, size, null, 0, size);
        final int[] soft = px.clone();
        ArenaDialogKit.boxBlur(soft, size, size, Math.max(1, size / 90));
        for (int i = 0; i < px.length; i++) {
            px[i] = (px[i] & 0xFF000000) | (sharpen((px[i] >> 16) & 0xFF, (soft[i] >> 16) & 0xFF) << 16)
                    | (sharpen((px[i] >> 8) & 0xFF, (soft[i] >> 8) & 0xFF) << 8) | sharpen(px[i] & 0xFF, soft[i] & 0xFF);
        }
        img.setRGB(0, 0, size, size, px, 0, size);
        return img;
    }

    private static int sharpen(final int v, final int soft) {
        return clamp(v + (v - soft) * 2 / 5);
    }

    private static int clamp(final int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    private void paintBanner(final Graphics2D g, final int cx, final int cy, final int maxW, final int h, final int w) {
        float size = h * BANNER_SIZE;
        Font font = ArenaDialogKit.titleFont(size);
        g.setFont(font);
        while (size > h * 0.05f && g.getFontMetrics().stringWidth(banner) > maxW) {
            size -= 2f;
            font = ArenaDialogKit.titleFont(size);
            g.setFont(font);
        }
        final GlyphVector glyphs = font.createGlyphVector(g.getFontRenderContext(), banner);
        final float cap = ArenaDialogKit.capHeight(g), tw = (float) glyphs.getLogicalBounds().getWidth();
        final float baseline = cy + cap / 2f;
        final Shape word = glyphs.getOutline(cx - tw / 2f, baseline);
        paintBloom(g, word, coreColour(), h);
        // a shadow of 2px, one crisp dark line outside the letters, then the letters as cast metal
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(0, 2);
        g.setColor(new Color(0, 0, 0, 170));
        g.fill(word);
        g.translate(0, -2);
        g.setStroke(new BasicStroke(Math.max(1.5f, h * 0.0024f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x24160A));
        g.draw(word);
        paintCastMetal(g, word, h);

        ArenaDialogKit.paintRule(g, cx, Math.round(baseline + size * 0.32f), Math.min(Math.round(w * 0.4f), maxW), h,
                outcome == Outcome.VICTORY ? ArenaTheme.GOLD_BRIGHT : new Color(0xC4CEDF));
    }

    /**
     * Letters as cast metal. Their shape, softened, is taken for a height: where it slopes towards the
     * light, up and a little left, the metal goes pale, and where it slopes away it goes dark, which
     * makes a bevel round every stroke. Along each top edge runs a rim of near white, as far as the
     * letter's edge moves in that short distance down.
     */
    private void paintCastMetal(final Graphics2D g, final Shape word, final int h) {
        final Rectangle b = word.getBounds();
        final int pad = 2, mw = b.width + 2 * pad, mh = b.height + 2 * pad;
        final BufferedImage mask = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_RGB);
        final Graphics2D m = mask.createGraphics();
        ArenaDialogKit.quality(m);
        m.translate(pad - b.x, pad - b.y);
        m.setColor(Color.WHITE);
        m.fill(word);
        m.dispose();
        final int[] cover = mask.getRGB(0, 0, mw, mh, null, 0, mw);
        final int[] height = cover.clone();
        ArenaDialogKit.softBlur(height, mw, mh, Math.max(1, Math.round(h * 0.002f)));
        final Color[] metal = bannerMetal();
        final Color lit = blend(metal[0], Color.WHITE, 0.65f), shade = blend(metal[1], Color.BLACK, 0.6f);
        final Color rim = new Color(0xFFFCF0);
        final int rimDrop = Math.max(1, Math.round(h * 0.0019f));
        final int[] out = new int[mw * mh];
        for (int y = 0; y < mh; y++) {
            final float t = y / (mh - 1f);
            // pale above, deep at the middle and a little reflected light from below
            final Color base = t < 0.56f ? blend(metal[0], metal[1], t / 0.56f) : blend(metal[1], metal[0], (t - 0.56f) / 0.44f * 0.32f);
            for (int x = 0; x < mw; x++) {
                final int i = y * mw + x, a = cover[i] & 0xFF;
                if (a == 0) {
                    continue;
                }
                final int dx = (height[y * mw + Math.min(mw - 1, x + 1)] & 0xFF) - (height[y * mw + Math.max(0, x - 1)] & 0xFF);
                final int dy = (height[Math.min(mh - 1, y + 1) * mw + x] & 0xFF) - (height[Math.max(0, y - 1) * mw + x] & 0xFF);
                final float light = Math.max(-1f, Math.min(1f, (dy * 0.9f + dx * 0.35f) / 255f * 2.6f));
                Color c = light >= 0f ? blend(base, lit, light) : blend(base, shade, -light);
                final int above = y >= rimDrop ? cover[(y - rimDrop) * mw + x] & 0xFF : 0;
                c = blend(c, rim, Math.max(0, a - above) / 255f * 0.95f);
                out[i] = (a << 24) | (c.getRGB() & 0xFFFFFF);
            }
        }
        final BufferedImage img = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, mw, mh, out, 0, mw);
        g.drawImage(img, b.x - pad, b.y - pad, null);
    }

    private static Color blend(final Color a, final Color b, final float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t), Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** A soft light behind a shape: a wide haze and only a little of a closer glow, so that the letters stay crisp. */
    private static void paintBloom(final Graphics2D g, final Shape shape, final Color c, final int h) {
        final int wide = Math.max(4, Math.round(h * 0.016f)), close = Math.max(2, wide / 3), margin = 3 * wide + 2;
        final Rectangle b = shape.getBounds();
        final int mw = b.width + 2 * margin, mh = b.height + 2 * margin;
        final BufferedImage mask = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_RGB);
        final Graphics2D m = mask.createGraphics();
        ArenaDialogKit.quality(m);
        m.translate(margin - b.x, margin - b.y);
        m.setColor(Color.WHITE);
        m.fill(shape);
        m.dispose();
        final int[] far = mask.getRGB(0, 0, mw, mh, null, 0, mw);
        final int[] near = far.clone();
        ArenaDialogKit.softBlur(far, mw, mh, wide);
        ArenaDialogKit.softBlur(near, mw, mh, close);
        final int rgb = c.getRGB() & 0xFFFFFF;
        for (int i = 0; i < far.length; i++) {
            final int a = Math.round((far[i] & 0xFF) * 0.5f + (near[i] & 0xFF) * 0.08f);
            far[i] = (Math.min(255, a) << 24) | rgb;
        }
        final BufferedImage glow = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_ARGB);
        glow.setRGB(0, 0, mw, mh, far, 0, mw);
        g.drawImage(glow, b.x - margin, b.y - margin, null);
    }
}
