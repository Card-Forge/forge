package forge.screens.match;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Multiset;

import forge.CachedCardImage;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.zone.ZoneType;
import forge.gui.card.CardDetailUtil;
import forge.toolbox.ArenaTheme;
import forge.toolbox.ArenaText;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.CardPanel;
import forge.view.arcane.HandArea;

/**
 * Arena-style inspect: a large copy of the hovered card floats beside it (above it for
 * cards in hand). Cards out of the hand also get a status panel next to the image with
 * what is true of them right now: current stats and counters, tapped, summoning sick,
 * in combat, face down, and the rules text with everything granted to them.
 * It has no mouse listeners, so clicks pass through to the board.
 */
@SuppressWarnings("serial")
public final class ArenaCardPreview extends JComponent {
    private static final float HEIGHT_OF_WINDOW = 0.52f;
    private static final int GAP = 12;
    private static final int PANEL_W = 250;
    private static ArenaCardPreview instance;

    private CardView card;
    private CachedCardImage image;
    private Status status;
    private boolean playable;
    private String statsNow; // shown over the printed power and toughness when they differ
    private Color statsColour;
    private int imageX;
    private int panelX;
    private int imageW;
    private int imageH;

    /** The status panel's content, worked out once per hover. */
    private record Status(List<String> chips, String body) { }

    private ArenaCardPreview() {
        setOpaque(false);
        setVisible(false);
    }

    private static ArenaCardPreview get() {
        if (instance == null) {
            instance = new ArenaCardPreview();
            FView.SINGLETON_INSTANCE.getLpnDocument().add(instance, FView.TARGETING_LAYER);
        }
        return instance;
    }

    public static void showFor(final CMatchUI matchUI, final CardPanel panel) {
        final CardView cv = panel.getCard();
        if (!ArenaTheme.isMatchBoard() || cv == null || !matchUI.mayView(cv) || panel.getParent() == null) {
            dismiss();
            return;
        }
        final ArenaCardPreview preview = get();
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        final int h = Math.round(lpn.getHeight() * HEIGHT_OF_WINDOW);
        final int w = Math.round(h / CardPanel.ASPECT_RATIO);
        final Point pos = SwingUtilities.convertPoint(panel.getParent(), panel.getCardX(), panel.getCardY(), lpn);
        final int cw = panel.getCardWidth(), ch = panel.getCardHeight();
        final int boardRight = Math.round(lpn.getWidth() * ArenaTheme.RAIL_RIGHT);
        final boolean inHand = panel.getParent() instanceof HandArea;
        final Status status = inHand || cv.getZone() == ZoneType.Hand ? null : describe(matchUI, cv);
        final int panelW = status == null ? 0 : PANEL_W + GAP;
        final int blockW = w + panelW;

        // a hand card rises in place, over its own slot; anything on the board shows at the side of the window away from it
        int x, y;
        boolean panelLeft = false;
        if (inHand) {
            x = pos.x + cw / 2 - w / 2;
            y = lpn.getHeight() - h - GAP;
        } else {
            final boolean onRight = pos.x + cw / 2 > boardRight * 0.6f;
            panelLeft = !onRight;
            x = onRight ? GAP : boardRight - blockW - GAP;
            y = Math.round(lpn.getHeight() * 0.14f);
        }
        x = Math.max(GAP, Math.min(boardRight - blockW - GAP, x));
        y = Math.max(GAP, Math.min(lpn.getHeight() - h - GAP, y));

        if (preview.card != cv || preview.imageH != h) {
            preview.card = cv;
            preview.image = new CachedCardImage(cv, matchUI.getLocalPlayers(), w, h) {
                @Override
                public void onImageFetched() {
                    preview.repaint();
                }
            };
        }
        preview.status = status;
        preview.playable = matchUI.isActionable(cv);
        final CardStateView shown = cv.getCurrentState();
        preview.statsNow = null;
        if (shown.isCreature() && !cv.isFaceDown()) {
            final forge.item.PaperCard printed = forge.model.FModel.getMagicDb().getCommonCards().getCard(shown.getName());
            final int basePower = printed == null ? shown.getPower() : printed.getRules().getIntPower();
            final int baseToughness = printed == null ? shown.getToughness() : printed.getRules().getIntToughness();
            if (shown.getPower() != basePower || shown.getToughness() != baseToughness || cv.getDamage() > 0) {
                preview.statsNow = shown.getPower() + "/" + shown.getToughness();
                preview.statsColour = cv.getDamage() > 0 ? new Color(0xB3261E) : shown.getPower() >= basePower && shown.getToughness() >= baseToughness ? new Color(0x1450A8) : new Color(0x8A1C14);
            }
        }
        preview.imageW = w;
        preview.imageH = h;
        preview.imageX = GAP + (panelLeft ? panelW : 0);
        preview.panelX = panelLeft ? GAP : GAP + w + GAP;
        // leave room around everything for the shadow
        preview.setBounds(x - GAP, y - GAP, blockW + 2 * GAP, h + 2 * GAP);
        preview.setVisible(true);
        preview.repaint();
    }

    public static void dismiss() {
        if (instance != null && instance.isVisible()) {
            instance.setVisible(false);
            instance.card = null;
            instance.image = null;
            instance.status = null;
        }
    }

    /** Only what the printed card does not already say: counters, its state right now, abilities it has been given. Null if nothing. */
    private static Status describe(final CMatchUI matchUI, final CardView cv) {
        final Localizer localizer = Localizer.getInstance();
        final CardStateView state = cv.getCurrentState();
        final boolean mayView = matchUI.mayView(cv);

        final List<String> chips = new ArrayList<>();
        if (cv.getCounters() != null) {
            for (final Multiset.Entry<CounterType> e : cv.getCounters().entrySet()) {
                chips.add(e.getCount() + "x " + e.getElement().getCounterOnCardDisplayName());
            }
        }
        if (cv.isFaceDown()) { chips.add(localizer.getMessage("lblArenaFaceDown")); }
        if (cv.isTapped()) { chips.add(localizer.getMessage("lblArenaTapped")); }
        if (cv.isSick()) { chips.add(localizer.getMessage("lblArenaSummoningSick")); }
        if (cv.isAttacking()) { chips.add(localizer.getMessage("lblArenaAttacking")); }
        if (cv.isBlocking()) { chips.add(localizer.getMessage("lblArenaBlocking")); }
        if (cv.isPhasedOut()) { chips.add(localizer.getMessage("lblArenaPhasedOut")); }
        if (cv.getDamage() > 0) { chips.add(localizer.getMessage("lblArenaDamage", cv.getDamage())); }

        final String printed = state.getOracleText() == null ? "" : state.getOracleText();
        final StringBuilder granted = new StringBuilder();
        final String composed = CardDetailUtil.composeCardText(state, matchUI.getGameView(), mayView);
        if (composed != null) {
            for (final String line : composed.split("\\r?\\n")) {
                final String trimmed = line.trim();
                // counters are already in the chips
                if (!trimmed.isEmpty() && !printed.contains(trimmed) && !trimmed.contains(" counters:")) {
                    granted.append(granted.length() == 0 ? "" : "\n").append(trimmed);
                }
            }
        }
        if (chips.isEmpty() && granted.length() == 0) {
            return null;
        }
        return new Status(chips, granted.toString());
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        final BufferedImage img = image == null ? null : image.getImage();
        final Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (img != null) {
                final int w = imageW, h = imageH, arc = Math.round(w * 0.07f);
                ArenaTheme.paintGlow(g, imageX, GAP + 6, w, h, arc, Color.BLACK, GAP - 1, 240);
                if (playable) {
                    ArenaTheme.paintGlow(g, imageX, GAP, w, h, arc, ArenaTheme.PLAYABLE, GAP - 2, 230);
                }
                final RoundRectangle2D shape = new RoundRectangle2D.Float(imageX, GAP, w, h, arc, arc);
                final Shape oldClip = g.getClip();
                g.clip(shape);
                g.setColor(Color.BLACK);
                g.fill(shape);
                // keep the image's own aspect ratio, centred in the frame
                final float scale = Math.min(w / (float) img.getWidth(), h / (float) img.getHeight());
                final int iw = Math.round(img.getWidth() * scale), ih = Math.round(img.getHeight() * scale);
                g.drawImage(img, imageX + (w - iw) / 2, GAP + (h - ih) / 2, iw, ih, null);
                g.setClip(oldClip);
                g.setColor(new Color(0, 0, 0, 200));
                g.draw(shape);
                if (statsNow != null) {
                    // what it is right now, over the printed box
                    // large enough to hide the printed box entirely
                    final int bw = Math.round(w * 0.235f), bh = Math.round(h * 0.066f);
                    final int bx = imageX + w - bw - Math.round(w * 0.032f), by = GAP + h - bh - Math.round(h * 0.036f);
                    g.setColor(new Color(4, 4, 6));
                    g.fillRoundRect(bx - 2, by - 2, bw + 4, bh + 4, bh, bh);
                    g.setPaint(new java.awt.GradientPaint(0, by, Color.WHITE, 0, by + bh, new Color(0xA8A49A)));
                    g.fillRoundRect(bx, by, bw, bh, bh, bh);
                    final int rim = Math.max(2, Math.round(bh * 0.09f));
                    g.setPaint(new java.awt.GradientPaint(0, by + rim, new Color(0xFBF8EF), 0, by + bh - rim, new Color(0xE0DACC)));
                    g.fillRoundRect(bx + rim, by + rim, bw - 2 * rim, bh - 2 * rim, bh - 2 * rim, bh - 2 * rim);
                    g.setFont(ArenaTheme.numeralFont(Font.BOLD, bh * 0.68f));
                    final FontMetrics sfm = g.getFontMetrics();
                    g.setColor(statsColour);
                    g.drawString(statsNow, bx + (bw - sfm.stringWidth(statsNow)) / 2, by + (bh + sfm.getAscent() - sfm.getDescent()) / 2);
                }
            }
            if (status != null) {
                paintStatus(g, panelX, GAP, imageX, imageW, imageH);
            }
        } finally {
            g.dispose();
        }
    }

    /** A slim panel as tall as what it says: state chips, then any rules text the printed card does not have. */
    private void paintStatus(final Graphics2D g, final int slotX, final int y, final int cardX, final int cardW, final int maxH) {
        final int pad = 10;
        final Font chipFont = ArenaTheme.numeralFont(Font.BOLD, 14f);
        final Font bodyFont = ArenaTheme.rulesFont(Font.PLAIN, 14f);
        final FontMetrics chipFm = g.getFontMetrics(chipFont), bodyFm = g.getFontMetrics(bodyFont);
        // as wide as the chips need when there is no text; the card's side of it touches the card
        int widest = 0;
        for (final String chip : status.chips()) {
            widest = Math.max(widest, chipFm.stringWidth(chip) + 14);
        }
        final boolean hasBody = status.body() != null && !status.body().isEmpty();
        final int w = hasBody ? PANEL_W : Math.min(PANEL_W, widest + 2 * pad);
        final int x = slotX < cardX ? cardX - 4 - w : slotX;
        final int inner = w - 2 * pad;
        final List<List<String>> bodyLines = status.body() == null || status.body().isEmpty() ? new ArrayList<>() : ArenaText.wrap(status.body(), bodyFm, inner);

        // work out the height first, so the panel fits its contents
        final int chipH = chipFm.getHeight() + 4;
        int rows = 0, cx = x + pad;
        for (final String chip : status.chips()) {
            final int chipW = chipFm.stringWidth(chip) + 14;
            if (rows == 0 || cx + chipW > x + w - pad) {
                rows++;
                cx = x + pad;
            }
            cx += chipW + 4;
        }
        final int chipsH = rows * (chipH + 4);
        final int bodyH = ArenaText.height(bodyLines, bodyFm);
        final int h = Math.min(maxH, pad + chipsH + (bodyH > 0 && chipsH > 0 ? 6 : 0) + bodyH + pad);

        ArenaTheme.paintGlow(g, x, y + 2, w, h, 12, Color.BLACK, 8, 150);
        g.setColor(new Color(10, 12, 18, 240));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, 12, 12));

        int cy = y + pad;
        if (rows > 0) {
            g.setFont(chipFont);
            cx = x + pad;
            boolean first = true;
            for (final String chip : status.chips()) {
                final int chipW = chipFm.stringWidth(chip) + 14;
                if (!first && cx + chipW > x + w - pad) {
                    cx = x + pad;
                    cy += chipH + 4;
                }
                first = false;
                g.setColor(new Color(40, 44, 54));
                g.fillRoundRect(cx, cy, chipW, chipH, 6, 6);
                g.setColor(Color.WHITE);
                g.drawString(chip, cx + 7, cy + (chipH + chipFm.getAscent() - chipFm.getDescent()) / 2);
                cx += chipW + 4;
            }
            cy += chipH + 10;
        }
        if (bodyH > 0) {
            g.setFont(bodyFont);
            g.setColor(ArenaTheme.TEXT);
            ArenaText.draw(g, bodyLines, x + pad, cy, y + h - pad + 2);
        }
    }

    private static String clip(final String text, final FontMetrics fm, final int width) {
        String s = text == null ? "" : text;
        while (s.length() > 2 && fm.stringWidth(s) > width) {
            s = s.substring(0, s.length() - 2) + "…";
        }
        return s;
    }
}
