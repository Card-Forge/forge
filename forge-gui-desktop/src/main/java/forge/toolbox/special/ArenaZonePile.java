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
import forge.toolbox.ArenaMotion;
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
    private boolean omitCommanders;
    private CardView shownCard;
    private CachedCardImage shownImage;
    private int countSeen = -1;
    private final ArenaMotion.Fade bump = new ArenaMotion.Fade(this::repaint, 0f);

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

    /** The local player's commanders wait beside the hand, so this pile only counts what else is in the zone. */
    public void setOmitCommanders(final boolean omit) {
        this.omitCommanders = omit;
        update();
    }

    private int shownCount() {
        int count = player.getZoneSize(zone);
        if (omitCommanders && player.getCards(zone) != null && player.getCommanders() != null) {
            for (final CardView c : player.getCards(zone)) {
                if (player.getCommanders().contains(c)) {
                    count--;
                }
            }
        }
        return count;
    }

    /** The count jumps as a card lands on the pile. */
    public void bump() {
        bump.set(1f);
        bump.to(0f, 380);
    }

    /** Refreshes the count and, for face-up piles, the top card. */
    public void update() {
        final int counted = shownCount();
        if (countSeen >= 0 && counted != countSeen && isShowing()) {
            bump();
        }
        countSeen = counted;
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
            if (omitCommanders && cards != null && player.getCommanders() != null) {
                top = null;
                for (final CardView c : cards) {
                    if (!player.getCommanders().contains(c)) {
                        top = c;
                    }
                }
            }
            if (top != shownCard) {
                shownCard = top;
                shownImage = null;
            }
        }
        if (zone == ZoneType.Hand) {
            setToolTipText(zone.getTranslatedName() + ": " + shownCount());
        }
        // an empty pile takes no room; only the library is always there
        final boolean show = zone == ZoneType.Library || zone == ZoneType.Hand || shownCount() > 0;
        if (show != isVisible()) {
            setVisible(show);
            if (getParent() != null) {
                getParent().revalidate();
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
            final int count = shownCount();
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
        final int pad = 2;
        final int depth = zone == ZoneType.Library ? Math.min(5, 1 + count / 15) : Math.min(2, 1 + count / 6);
        int ch = getHeight() - 2 * pad - 3 - depth * 2;
        int cw = Math.round(ch * CARD_RATIO);
        if (cw > getWidth() - 2 * pad - depth * 2) {
            cw = getWidth() - 2 * pad - depth * 2;
            ch = Math.round(cw / CARD_RATIO);
        }
        if (cw <= 0 || ch <= 0) {
            return;
        }
        final int x = (getWidth() - cw - depth * 2) / 2;
        final int y = getHeight() - ch - pad;
        final int arc = Math.max(4, cw / 9);

        if (count == 0) {
            // empty slot: dashed outline
            g.setColor(ArenaTheme.alpha(ArenaTheme.GOLD, 70));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {4f, 4f}, 0f));
            g.draw(new RoundRectangle2D.Float(x, y, cw - 1, ch - 1, arc, arc));
            paintZoneName(g, x, y, cw, ch);
            return;
        }

        // thickness: the pile's edges rising up behind the top card
        ArenaTheme.paintGlow(g, x, y + 2, cw + depth * 2, ch, arc, Color.BLACK, 9, 210);
        // one solid block of page edges, lit along its far edge
        for (int o = depth * 2; o >= 1; o--) {
            g.setColor(o == depth * 2 ? new Color(150, 142, 124) : new Color(70, 66, 58));
            g.fill(new RoundRectangle2D.Float(x + o, y - o, cw, ch, arc, arc));
        }

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
        g.setColor(new Color(0, 0, 0, 200));
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
        // spread sideways as well as turned, so it reads as a held hand and not a stack
        final int stepX = shown < 2 ? 0 : Math.min(cw * 55 / 100, (getWidth() - cw) / (shown - 1));
        final double turn = Math.toRadians(4.5);
        final int cx = getWidth() / 2;
        for (int i = 0; i < shown; i++) {
            final float place = i - (shown - 1) / 2f;
            final int cardX = Math.round(cx + place * stepX);
            final AffineTransform old = g.getTransform();
            g.rotate(place * turn, cardX, getHeight() + ch / 5);
            paintCardBack(g, cardX - cw / 2, getHeight() - ch - 2, cw, ch, arc, true); // dimmed: it must not outshine the board
            g.setTransform(old);
        }
        // the fan is the count; only a hand too big to draw needs saying
        if (count > shown) {
            paintCountBadge(g, String.valueOf(count), cx, getHeight() - ch / 3);
        }
    }

    private void paintCardBack(final Graphics2D g, final int x, final int y, final int w, final int h, final int arc, final boolean dim) {
        final BufferedImage sleeve = FSkin.getSleeveImage(player.getSleeveIndex());
        final RoundRectangle2D r = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
        if (player.getSleeveIndex() == 0) {
            // the default sleeve is a small logo sprite that does not survive enlarging
            forge.toolbox.ArenaArt.paintCardBack(g, x, y, w, h, arc);
        } else if (sleeve != null) {
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
        final float bumped = bump.get();
        if (bumped <= 0f) {
            paintBadgeShape(g, text, centerX, centerY);
            return;
        }
        final java.awt.geom.AffineTransform flat = g.getTransform();
        final float grow = 1f + 0.38f * bumped;
        g.translate(centerX, centerY);
        g.scale(grow, grow);
        g.translate(-centerX, -centerY);
        paintBadgeShape(g, text, centerX, centerY);
        g.setTransform(flat);
    }

    private void paintBadgeShape(final Graphics2D g, final String text, final int centerX, final int centerY) {
        g.setFont(ArenaTheme.numeralFont(Font.BOLD, Math.max(15f, getHeight() * 0.15f)));
        final FontMetrics fm = g.getFontMetrics();
        final int bh = fm.getAscent() + 5;
        final int bw = Math.max(bh, fm.stringWidth(text) + 12);
        int bx = centerX - bw / 2;
        int by = Math.min(getHeight() - bh, centerY - bh / 2);
        bx = Math.max(0, Math.min(getWidth() - bw, bx));
        by = Math.max(0, by);
        final RoundRectangle2D badge = new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh);
        g.setColor(new Color(6, 6, 8, 235));
        g.fill(badge);
        g.setColor(new Color(255, 255, 255, 120));
        g.draw(badge);
        g.setColor(ArenaTheme.TEXT);
        g.drawString(text, bx + (bw - fm.stringWidth(text)) / 2, by + bh - 4 - fm.getDescent() / 2);
    }
}
