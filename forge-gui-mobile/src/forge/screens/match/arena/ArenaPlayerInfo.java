package forge.screens.match.arena;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImageComplex;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardRenderer;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchController;
import forge.util.collect.FCollectionView;

/**
 * What a player's state is besides life, as a row of small chips the Arena board puts round a portrait (or, for a seat
 * in a game of three or four, under the name plate, where the small portrait leaves no room):
 * <ul>
 * <li>commander damage taken: one chip per opposing commander that has dealt some, the commander's art in a circle and
 *     the figure, gold, turning red from 15 (21 is lethal) - with three opponents you have to see whose it is;</li>
 * <li>player counters: poison, energy, experience, rad, tickets, each with its icon;</li>
 * <li>the designations a Commander table passes round: the Monarch, the Initiative, the City's Blessing.</li>
 * </ul>
 * Nothing here allocates per frame once the figures have been seen: figures come from a shared table of strings.
 */
public final class ArenaPlayerInfo {
    private ArenaPlayerInfo() { }

    /** What to draw: commander damage, counters, designations. */
    public static final int CMDR = 1, COUNTERS = 2, STATUS = 4, ALL = CMDR | COUNTERS | STATUS;

    private static final Color CMDR_COLOR = ArenaTheme.rgb(0xE8C27A), CMDR_NEAR = ArenaTheme.rgb(0xFF6A5C);
    private static final Color CHIP_FILL = ArenaTheme.rgba(8, 9, 14, 225);
    private static final Color POISON = ArenaTheme.rgb(0x6FD06F), ENERGY = ArenaTheme.rgb(0xF5D24B), EXPERIENCE = ArenaTheme.rgb(0xC9A0FF),
            RAD = ArenaTheme.rgb(0xB7E35A), TICKET = ArenaTheme.rgb(0xFF9F5A), STATUS_GOLD = ArenaTheme.rgb(0xF2D58C);
    private static final CounterEnumType[] COUNTER_TYPES = {CounterEnumType.POISON, CounterEnumType.ENERGY, CounterEnumType.EXPERIENCE,
            CounterEnumType.RAD, CounterEnumType.TICKET};
    private static final FSkinImage[] COUNTER_ICONS = {FSkinImage.POISON, FSkinImage.ENERGY, FSkinImage.COMMANDER, FSkinImage.RAD, FSkinImage.TICKET};
    private static final Color[] COUNTER_COLORS = {POISON, ENERGY, EXPERIENCE, RAD, TICKET};
    /** The effect cards in the command zone that make a designation, and their labels. */
    private static final String[] STATUS_CARDS = {"The Monarch", "The Initiative", "City's Blessing"};
    private static final String[] STATUS_KEYS = {"lblArenaMonarch", "lblArenaInitiative", "lblArenaCitysBlessing"};
    private static final String[] statusLabels = new String[STATUS_CARDS.length];
    private static final Color S1 = new Color();
    private static final Color LEAD_TOP = ArenaTheme.rgb(0x8A6A2E), LEAD_BOTTOM = ArenaTheme.rgb(0x4A3612);


    private static final String[] FIGURES = new String[200];

    /** A figure as text, from a shared table (no allocation for the usual range). */
    public static String figure(final int n) {
        if (n < 0 || n >= FIGURES.length) {
            return String.valueOf(n);
        }
        String s = FIGURES[n];
        if (s == null) {
            s = FIGURES[n] = String.valueOf(n);
        }
        return s;
    }

    private static String statusLabel(final int i) {
        if (statusLabels[i] == null) {
            statusLabels[i] = Forge.getLocalizer().getMessage(STATUS_KEYS[i]);
        }
        return statusLabels[i];
    }

    /**
     * Draws the chips in a row, from {@code x} rightwards ({@code rightToLeft} false) or leftwards, wrapping onto a
     * further row below after {@code maxW} (0: no limit, one row). Rows are {@code h} tall and centred on {@code cy}
     * for the first. Returns the number of rows used (0 if there was nothing to show).
     */
    public static int draw(final Graphics g, final PlayerView player, final int what, final float x, final float cy, final float h,
            final boolean rightToLeft, final float maxW) {
        return draw(g, player, what, null, x, cy, h, rightToLeft, maxW);
    }

    /** As {@link #draw(Graphics, PlayerView, int, float, float, float, boolean, float)}, led by a gold pill (e.g. the step a seat's turn is in). */
    public static int draw(final Graphics g, final PlayerView player, final int what, final String lead, final float x, final float cy,
            final float h, final boolean rightToLeft, final float maxW) {
        if (player == null) {
            return 0;
        }
        final Row row = ROW.start(g, x, cy, h, rightToLeft, maxW);
        if (lead != null) {
            row.leadChip(lead);
        }
        if ((what & CMDR) != 0) {
            drawCommanderDamage(row, player);
        }
        if ((what & COUNTERS) != 0) {
            for (int i = 0; i < COUNTER_TYPES.length; i++) {
                final int n = player.getCounters(COUNTER_TYPES[i]);
                if (n > 0) {
                    row.iconChip(COUNTER_ICONS[i], figure(n), COUNTER_COLORS[i]);
                }
            }
        }
        if ((what & STATUS) != 0) {
            final FCollectionView<CardView> command = player.getCards(ZoneType.Command);
            if (command != null) {
                try {
                    for (final CardView c : command) {
                        final String name = c.getName();
                        for (int i = 0; i < STATUS_CARDS.length; i++) {
                            if (STATUS_CARDS[i].equals(name)) {
                                row.statusChip(c, statusLabel(i));
                            }
                        }
                    }
                } catch (final RuntimeException e) {
                    // the zone changed under us: next frame
                }
            }
        }
        return row.rows();
    }

    private static void drawCommanderDamage(final Row row, final PlayerView player) {
        final GameView game = MatchController.instance.getGameView();
        if (game == null || game.getPlayers() == null) {
            return;
        }
        for (final PlayerView other : game.getPlayers()) {
            if (other == player || other.getCommanders() == null) {
                continue;
            }
            for (final CardView cmdr : other.getCommanders()) {
                final int damage = player.getCommanderDamage(cmdr);
                if (damage > 0) {
                    row.commanderChip(cmdr, figure(damage), damage >= 15);
                }
            }
        }
    }

    private static final Row ROW = new Row();

    /** Places chips one after another. */
    private static final class Row {
        private Graphics g;
        private float x0, x, cy, h, maxW;
        private boolean rtl;
        private int rows;

        Row start(final Graphics g0, final float x1, final float cy0, final float h0, final boolean rtl0, final float maxW0) {
            g = g0;
            x0 = x = x1;
            cy = cy0;
            h = h0;
            rtl = rtl0;
            maxW = maxW0;
            rows = 0;
            return this;
        }

        int rows() {
            return rows;
        }

        /** Where a chip {@code w} wide goes: its left edge, after wrapping if needed. */
        private float place(final float w) {
            final float gap = Math.max(2f, h * 0.18f);
            if (rows == 0) {
                rows = 1;
            } else if (maxW > 0 && Math.abs(x - x0) + w > maxW) {
                rows++;
                x = x0;
                cy += h + gap;
            }
            final float left = rtl ? x - w : x;
            x = rtl ? x - w - gap : x + w + gap;
            return left;
        }

        private FSkinFont font() {
            return ArenaCards.evenFont(h * 0.62f);
        }

        void commanderChip(final CardView cmdr, final String figure, final boolean near) {
            final FSkinFont f = font();
            final float art = h - 2 * Math.max(1.5f, h * 0.09f);
            final float w = h * 0.12f + art + h * 0.22f + ArenaArt.textWidth(figure, f) + h * 0.4f;
            final float left = place(w);
            final Color c = near ? CMDR_NEAR : CMDR_COLOR;
            final float top = cy - h / 2f;
            g.fillSmoothRoundRect(CHIP_FILL, left, top, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * 0.05f), c, left, top, w, h, h / 2f);
            final float acx = left + h * 0.12f + art / 2f + Math.max(1.5f, h * 0.09f) - h * 0.06f;
            if (!circleArt(g, cmdr, acx, cy, art)) {
                circleInitial(g, cmdr, acx, cy, art);
            }
            g.drawSmoothCircle(Math.max(1f, h * 0.045f), c, acx, cy, art / 2f);
            final float tx = acx + art / 2f + h * 0.18f;
            ArenaArt.textBold(g, figure, f, c, tx, top, left + w - tx - h * 0.3f, h, Align.center);
        }

        void leadChip(final String text) {
            final FSkinFont f = ArenaCards.evenFont(h * 0.56f);
            final float w = ArenaArt.textWidth(text, f) + h;
            final float left = place(w);
            final float top = cy - h / 2f;
            g.fillSmoothRoundRectGradient(LEAD_TOP, LEAD_BOTTOM, true, left, top, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * 0.05f), ArenaTheme.GOLD, left, top, w, h, h / 2f);
            ArenaArt.textBold(g, text, f, ArenaTheme.WHITE, left, top, w, h, Align.center);
        }

        void iconChip(final FSkinImage icon, final String figure, final Color c) {
            final FSkinFont f = font();
            final float is = h * 0.72f;
            final float w = h * 0.22f + is + h * 0.16f + ArenaArt.textWidth(figure, f) + h * 0.4f;
            final float left = place(w);
            final float top = cy - h / 2f;
            g.fillSmoothRoundRect(CHIP_FILL, left, top, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * 0.05f), c, left, top, w, h, h / 2f);
            g.drawImage(icon, left + h * 0.22f, cy - is / 2f, is, is);
            final float tx = left + h * 0.22f + is + h * 0.1f;
            ArenaArt.textBold(g, figure, f, c, tx, top, left + w - tx - h * 0.3f, h, Align.center);
        }

        void statusChip(final CardView effect, final String label) {
            final FSkinFont f = ArenaCards.evenFont(h * 0.56f);
            final float art = h - 2 * Math.max(1.5f, h * 0.09f);
            final float w = h * 0.12f + art + h * 0.26f + ArenaArt.textWidth(label, f) + h * 0.45f;
            final float left = place(w);
            final float top = cy - h / 2f;
            g.fillSmoothRoundRectGradient(ArenaTheme.rgba(58, 44, 16, 235), CHIP_FILL, true, left, top, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * 0.05f), STATUS_GOLD, left, top, w, h, h / 2f);
            final float acx = left + h * 0.12f + art / 2f + Math.max(1.5f, h * 0.09f) - h * 0.06f;
            if (!circleArt(g, effect, acx, cy, art)) {
                g.fillSmoothCircle(ArenaTheme.alpha(STATUS_GOLD, 0.35f, S1), acx, cy, art / 2f);
                final FSkinFont lf = ArenaCards.evenFont(art * 0.62f);
                ArenaArt.textBold(g, initial(label), lf, STATUS_GOLD, acx - art / 2f, cy - art / 2f, art, art, Align.center);
            }
            g.drawSmoothCircle(Math.max(1f, h * 0.045f), STATUS_GOLD, acx, cy, art / 2f);
            final float tx = acx + art / 2f + h * 0.2f;
            ArenaArt.textShadowed(g, label, f, STATUS_GOLD, tx, top, left + w - tx, h, Align.left);
        }
    }

    private static final String[] INITIALS = new String[128];

    /** The first letter of a name, from a shared table for plain letters. */
    private static String initial(final String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        final char c = Character.toUpperCase(name.charAt(0));
        if (c >= 128) {
            return String.valueOf(c);
        }
        String s = INITIALS[c];
        if (s == null) {
            s = INITIALS[c] = String.valueOf(c);
        }
        return s;
    }

    /** No art loaded (yet): the card's colours in the circle and its initial, so commanders still tell apart. */
    private static void circleInitial(final Graphics g, final CardView card, final float cx, final float cy, final float d) {
        Color tint;
        try {
            tint = ArenaTheme.tileTint(card.getCurrentState());
        } catch (final RuntimeException e) {
            tint = CMDR_COLOR;
        }
        g.fillSmoothCircle(ArenaTheme.mix(tint, ArenaTheme.BLACK, 0.35f, S1), cx, cy, d / 2f);
        final FSkinFont lf = ArenaCards.evenFont(d * 0.62f);
        ArenaArt.textBold(g, initial(card.getName()), lf, ArenaTheme.WHITE, cx - d / 2f, cy - d / 2f, d, d, Align.center);
    }

    private static final TextureRegion SQUARE = new TextureRegion();


    /** A card's art cropped to a circle; false if it has no art loaded yet. */
    private static boolean circleArt(final Graphics g, final CardView card, final float cx, final float cy, final float d) {
        final FImageComplex art;
        try {
            art = CardRenderer.getCardArt(card);
        } catch (final RuntimeException e) {
            return false;
        }
        if (art == forge.card.CardImageRenderer.forgeArt) {
            return false; // the "no picture" stand-in: the colour and initial say more
        }
        final TextureRegion r = art == null ? null : art.getTextureRegion();

        if (r == null || r.getRegionWidth() <= 0 || r.getRegionHeight() <= 0) {
            return false;
        }
        final int w = r.getRegionWidth(), hh = r.getRegionHeight(), s = Math.min(w, hh);
        SQUARE.setTexture(r.getTexture());
        SQUARE.setRegion(r.getRegionX() + (w - s) / 2, r.getRegionY() + (hh - s) / 2, s, s);
        SQUARE.flip(r.isFlipX(), r.isFlipY());
        ArenaArt.circleImage(g, SQUARE, cx, cy, d);
        return true;
    }
}
