package forge.toolbox.special;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import javax.swing.JComponent;

import org.apache.commons.lang3.StringUtils;

import forge.CachedCardImage;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FSkin;
import forge.util.collect.FCollectionView;

/**
 * A zone drawn as a pile of cards for the Arena board: the library as a stack of
 * sleeves, the graveyard and exile showing their top card, and an opponent's hand
 * as a fan of card backs. Each pile carries a count badge.
 */
@SuppressWarnings("serial")
public class ArenaZonePile extends JComponent {
    private static final float CARD_RATIO = 0.716f; // width / height of a card

    private final PlayerView player;
    private final ZoneType zone;
    private final Supplier<Iterable<PlayerView>> viewers;
    private CardView shownCard;
    private CachedCardImage shownImage;

    public ArenaZonePile(final PlayerView player, final ZoneType zone, final Supplier<Iterable<PlayerView>> viewers) {
        this.player = player;
        this.zone = zone;
        this.viewers = viewers;
        setOpaque(false);
        setFont(FSkin.getBoldFont(13).getBaseFont());
    }

    public ZoneType getZone() {
        return zone;
    }

    /** Refreshes the count and, for face-up piles, the top card. */
    public void update() {
        if (zone == ZoneType.Graveyard || zone == ZoneType.Exile || zone == ZoneType.Command) {
            final FCollectionView<CardView> cards = player.getCards(zone);
            CardView top = cards == null || cards.isEmpty() ? null : cards.getLast();
            if (zone == ZoneType.Command && cards != null && player.getCommanders() != null) {
                // show the commander rather than an emblem or effect that happens to be on top
                for (final CardView c : cards) {
                    if (player.getCommanders().contains(c)) {
                        top = c;
                        break;
                    }
                }
            }
            if (top != shownCard) {
                shownCard = top;
                shownImage = null;
            }
        }
        repaint();
    }

    private BufferedImage getTopCardImage(final int w, final int h) {
        if (shownCard == null || w <= 0 || h <= 0) {
            return null;
        }
        if (shownImage == null) {
            shownImage = new CachedCardImage(shownCard, viewers.get(), w, h) {
                @Override
                public void onImageFetched() {
                    repaint();
                }
            };
        }
        return shownImage.getImage();
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        final Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            final int count = player.getZoneSize(zone);
            if (zone == ZoneType.Hand) {
                paintHandFan(g, count);
            } else {
                paintPile(g, count);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintPile(final Graphics2D g, final int count) {
        final int pad = 3;
        int ch = getHeight() - 2 * pad - 4;
        int cw = Math.round(ch * CARD_RATIO);
        if (cw > getWidth() - 2 * pad - 4) {
            cw = getWidth() - 2 * pad - 4;
            ch = Math.round(cw / CARD_RATIO);
        }
        if (cw <= 0 || ch <= 0) {
            return;
        }
        final int x = (getWidth() - cw) / 2;
        final int y = (getHeight() - ch) / 2;
        final int arc = Math.max(4, cw / 9);

        if (count == 0) {
            // empty slot: dashed outline
            g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 70));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {4f, 4f}, 0f));
            g.draw(new RoundRectangle2D.Float(x, y, cw - 1, ch - 1, arc, arc));
            paintZoneName(g, x, y, cw, ch);
            return;
        }

        // thickness: a few offset layers behind the top card
        final int layers = Math.min(4, 1 + count / 12);
        for (int i = layers; i >= 1; i--) {
            final int o = i * 2;
            paintCardBack(g, x + o, y - o, cw, ch, arc, true);
        }
        ArenaTheme.paintGlow(g, x, y + 2, cw, ch, arc, Color.BLACK, 3, 120);

        final BufferedImage face = zone == ZoneType.Library ? null : getTopCardImage(cw, ch);
        if (face != null) {
            final Shape oldClip = g.getClip();
            g.clip(new RoundRectangle2D.Float(x, y, cw, ch, arc, arc));
            g.drawImage(face, x, y, cw, ch, null);
            g.setClip(oldClip);
            if (zone == ZoneType.Exile) {
                g.setColor(new Color(150, 170, 220, 90)); // exile reads as a cold, faded pile
                g.fill(new RoundRectangle2D.Float(x, y, cw, ch, arc, arc));
            }
        } else {
            paintCardBack(g, x, y, cw, ch, arc, false);
        }
        g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 120));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, cw - 1, ch - 1, arc, arc));

        if (zone == ZoneType.Command) {
            // commander tax: two more for each time the commander has already been cast
            final int tax = shownCard != null && player.getCommanders() != null && player.getCommanders().contains(shownCard)
                    ? 2 * player.getCommanderCast(shownCard) : 0;
            if (tax > 0) {
                paintCountBadge(g, "+" + tax, x + cw / 2, y + ch);
            }
        } else {
            paintCountBadge(g, String.valueOf(count), x + cw / 2, y + ch);
        }
    }

    private void paintHandFan(final Graphics2D g, final int count) {
        if (count == 0) {
            return;
        }
        final int ch = Math.round(getHeight() * 0.9f);
        final int cw = Math.round(ch * CARD_RATIO);
        final int arc = Math.max(4, cw / 9);
        final int shown = Math.min(count, 10);
        final double spread = Math.toRadians(Math.min(40, 6 * shown));
        final int cx = getWidth() / 2;
        final int pivotY = getHeight() + ch; // fan pivots far below so the backs splay gently
        for (int i = 0; i < shown; i++) {
            final double a = shown == 1 ? 0 : -spread / 2 + spread * i / (shown - 1);
            final AffineTransform old = g.getTransform();
            g.rotate(a, cx, pivotY);
            paintCardBack(g, cx - cw / 2, getHeight() - ch - 2, cw, ch, arc, false);
            g.setTransform(old);
        }
        paintCountBadge(g, String.valueOf(count), cx, getHeight() - ch / 3);
    }

    private void paintCardBack(final Graphics2D g, final int x, final int y, final int w, final int h, final int arc, final boolean dim) {
        final BufferedImage sleeve = FSkin.getSleeveImage(player.getSleeveIndex());
        final RoundRectangle2D r = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
        if (sleeve != null) {
            final Shape oldClip = g.getClip();
            g.clip(r);
            g.drawImage(sleeve, x, y, w, h, null);
            g.setClip(oldClip);
        } else {
            g.setColor(ArenaTheme.NAVY_LIGHT);
            g.fill(r);
        }
        g.setColor(dim ? new Color(0, 0, 0, 110) : new Color(0, 0, 0, 40));
        g.fill(r);
        g.setColor(new Color(0, 0, 0, 150));
        g.draw(r);
    }

    private void paintZoneName(final Graphics2D g, final int x, final int y, final int w, final int h) {
        final String name = StringUtils.capitalize(zone.getTranslatedName());
        g.setFont(getFont().deriveFont(Font.BOLD, Math.max(9f, Math.min(12f, w / 5.5f))));
        final FontMetrics fm = g.getFontMetrics();
        g.setColor(ArenaTheme.TEXT_DIM);
        g.drawString(name, x + (w - fm.stringWidth(name)) / 2, y + (h + fm.getAscent()) / 2 - 2);
    }

    private void paintCountBadge(final Graphics2D g, final String text, final int centerX, final int centerY) {
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        final FontMetrics fm = g.getFontMetrics();
        final int bh = fm.getAscent() + 6;
        final int bw = Math.max(bh, fm.stringWidth(text) + 12);
        int bx = centerX - bw / 2;
        int by = Math.min(getHeight() - bh, centerY - bh / 2);
        bx = Math.max(0, Math.min(getWidth() - bw, bx));
        by = Math.max(0, by);
        final RoundRectangle2D badge = new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh);
        g.setColor(new Color(8, 13, 25, 235));
        g.fill(badge);
        g.setColor(ArenaTheme.GOLD);
        g.draw(badge);
        g.setColor(ArenaTheme.TEXT);
        g.drawString(text, bx + (bw - fm.stringWidth(text)) / 2, by + bh - 4 - fm.getDescent() / 2);
    }
}
