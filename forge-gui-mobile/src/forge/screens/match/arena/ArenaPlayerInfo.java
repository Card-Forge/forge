package forge.screens.match.arena;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.ImageKeys;
import forge.assets.FImageComplex;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardFaceSymbols;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.card.mana.ManaAtom;
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
 * <li>the designations a Commander table passes round: the Monarch, the Initiative, the City's Blessing;</li>
 * <li>a seat's floating mana (yours and a single opponent's keep their pool beside the portrait).</li>
 * </ul>
 * A row may be led by a pill: the step a seat's turn is in, or that it plays next. {@link #drawEliminated} lists the
 * players who are out. Nothing here allocates per frame once the figures and names have been seen.
 */
public final class ArenaPlayerInfo {
    private ArenaPlayerInfo() { }

    /** What to draw: commander damage, counters, designations, floating mana. */
    public static final int CMDR = 1, COUNTERS = 2, STATUS = 4, MANA = 8, ALL = CMDR | COUNTERS | STATUS;

    /** How a row is led: nothing, the step of the turn (gold), or "plays next" (steel). */
    public static final int LEAD_NONE = 0, LEAD_PHASE = 1, LEAD_NEXT = 2;

    // ---- the chip's proportions, as shares of its height ---------------------------------------------------------
    /** Figures and labels. */
    private static final float FIGURE_TEXT = 0.62f, LABEL_TEXT = 0.56f;
    /** Gap between chips and between wrapped rows. */
    private static final float GAP = 0.18f;
    /** Padding before a circle or icon, after the text. */
    private static final float PAD_LEFT = 0.12f, PAD_RIGHT = 0.4f;
    /** Space between a circle or icon and its text. */
    private static final float TEXT_GAP = 0.2f;
    /** An icon's size, and the ring round a circle (a circle is the chip's height less this on each side). */
    private static final float ICON = 0.72f, RING_INSET = 0.09f;
    /** Rim lines. */
    private static final float RIM = 0.05f, CIRCLE_RIM = 0.045f;
    /** Commander damage turns red from this much (21 is lethal). */
    private static final int CMDR_NEAR_DAMAGE = 15;
    /** The most rows a wrapping row of chips takes. */
    private static final int MAX_ROWS = 2;

    private static final Color CMDR_COLOR = ArenaTheme.rgb(0xE8C27A), CMDR_NEAR = ArenaTheme.rgb(0xFF6A5C);
    private static final Color CHIP_FILL = ArenaTheme.rgba(8, 9, 14, 225);
    private static final Color POISON = ArenaTheme.rgb(0x6FD06F), ENERGY = ArenaTheme.rgb(0xF5D24B), EXPERIENCE = ArenaTheme.rgb(0xC9A0FF),
            RAD = ArenaTheme.rgb(0xB7E35A), TICKET = ArenaTheme.rgb(0xFF9F5A), STATUS_GOLD = ArenaTheme.rgb(0xF2D58C),
            MANA_RIM = ArenaTheme.rgb(0xB9C4D6), OUT_TEXT = ArenaTheme.rgb(0xB8B2A6), OUT_RIM = ArenaTheme.rgb(0x6A655E);
    private static final Color LEAD_TOP = ArenaTheme.rgb(0x8A6A2E), LEAD_BOTTOM = ArenaTheme.rgb(0x4A3612);
    private static final Color NEXT_TOP = ArenaTheme.rgb(0x4C5664), NEXT_BOTTOM = ArenaTheme.rgb(0x252B33), NEXT_RIM = ArenaTheme.rgb(0xAFC3DA);
    private static final Color STATUS_FILL = ArenaTheme.rgba(58, 44, 16, 235), OUT_FILL = ArenaTheme.rgba(20, 18, 16, 225);
    private static final Color S1 = new Color();

    private static final CounterEnumType[] COUNTER_TYPES = {CounterEnumType.POISON, CounterEnumType.ENERGY, CounterEnumType.EXPERIENCE,
            CounterEnumType.RAD, CounterEnumType.TICKET};
    private static final FSkinImage[] COUNTER_ICONS = {FSkinImage.POISON, FSkinImage.ENERGY, FSkinImage.COMMANDER, FSkinImage.RAD, FSkinImage.TICKET};
    private static final Color[] COUNTER_COLORS = {POISON, ENERGY, EXPERIENCE, RAD, TICKET};
    private static final String[] MANA_SYMBOLS = {"W", "U", "B", "R", "G", "C"};

    /**
     * The designations, found by the image key of the effect card the game puts in the player's command zone for them
     * ({@code Player.createMonarchEffect}, {@code createInitiativeEffect}, {@code createBlessingEffect}: token keys made
     * from {@link ImageKeys#MONARCH_IMAGE} and the like, optionally followed by "|set|number"), not by the card's name.
     */
    private static final String[] STATUS_IMAGES = {ImageKeys.MONARCH_IMAGE, ImageKeys.INITIATIVE_IMAGE, ImageKeys.BLESSING_IMAGE};
    private static final String[] STATUS_KEYS = {"lblArenaMonarch", "lblArenaInitiative", "lblArenaCitysBlessing"};
    private static final String[] statusLabels = new String[STATUS_KEYS.length];
    private static String nextLabel;

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

    /** "Plays next", localized once. */
    public static String nextLabel() {
        if (nextLabel == null) {
            nextLabel = Forge.getLocalizer().getMessage("lblArenaPlaysNext");
        }
        return nextLabel;
    }

    /** Which designation an effect card in the command zone stands for, or -1. */
    private static int statusOf(final CardView c) {
        final String key = c.getCurrentState() == null ? null : c.getCurrentState().getImageKey();
        if (key == null || !key.startsWith(ImageKeys.TOKEN_PREFIX)) {
            return -1;
        }
        final int from = ImageKeys.TOKEN_PREFIX.length();
        for (int i = 0; i < STATUS_IMAGES.length; i++) {
            final String name = STATUS_IMAGES[i];
            if (key.startsWith(name, from) && (key.length() == from + name.length() || key.charAt(from + name.length()) == '|')) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Draws the chips in a row, from {@code x} rightwards ({@code rightToLeft} false) or leftwards, wrapping onto a
     * further row below after {@code maxW} (0: no limit, one row; at most {@link #MAX_ROWS} rows, what does not fit is
     * left out). Rows are {@code h} tall and centred on {@code cy} for the first. {@code lead} ({@link #LEAD_PHASE} with
     * {@code leadText}, or {@link #LEAD_NEXT}) comes first. Returns the number of rows used.
     */
    public static int draw(final Graphics g, final PlayerView player, final int what, final int lead, final String leadText, final float x,
            final float cy, final float h, final boolean rightToLeft, final float maxW) {
        if (player == null) {
            return 0;
        }
        final Row row = ROW.start(g, x, cy, h, rightToLeft, maxW);
        if (lead == LEAD_PHASE && leadText != null) {
            row.leadChip(leadText, LEAD_TOP, LEAD_BOTTOM, ArenaTheme.GOLD);
        } else if (lead == LEAD_NEXT) {
            row.leadChip(nextLabel(), NEXT_TOP, NEXT_BOTTOM, NEXT_RIM);
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
                        final int status = statusOf(c);
                        if (status >= 0) {
                            row.statusChip(c, statusLabel(status));
                        }
                    }
                } catch (final RuntimeException e) {
                    // the zone changed under us: next frame
                }
            }
        }
        if ((what & MANA) != 0) {
            for (int i = 0; i < ManaAtom.MANATYPES.length; i++) {
                final int n = player.getMana(ManaAtom.MANATYPES[i]);
                if (n > 0) {
                    row.manaChip(MANA_SYMBOLS[i], figure(n));
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
                    row.commanderChip(cmdr, figure(damage), damage >= CMDR_NEAR_DAMAGE);
                }
            }
        }
    }

    /**
     * The players who are out, one dim plate each ("Vivien is out"), stacked down from {@code top} with their right
     * edges at {@code right}: a seat that goes leaves the board, so this is what still says who has gone.
     */
    public static void drawEliminated(final Graphics g, final GameView game, final float right, final float top, final float h) {
        if (game == null || game.getPlayers() == null) {
            return;
        }
        final FSkinFont f = ArenaCards.evenFont(h * LABEL_TEXT);
        float y = top;
        for (final PlayerView p : game.getPlayers()) {
            if (!p.getHasLost()) {
                continue;
            }
            final String text = outText(p);
            final float w = ArenaArt.cachedTextWidth(text, f) + h;
            g.fillSmoothRoundRect(OUT_FILL, right - w, y, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * RIM), OUT_RIM, right - w, y, w, h, h / 2f);
            g.drawText(text, f, OUT_TEXT, right - w, y, w, h, false, Align.center, true);
            y += h + Math.max(2f, h * GAP);
        }
    }

    /** "{name} is out", made once per name. */
    private static final Map<String, String> OUT_TEXTS = new HashMap<>();

    private static String outText(final PlayerView p) {
        final String name = p.getName();
        String s = OUT_TEXTS.get(name);
        if (s == null) {
            s = Forge.getLocalizer().getMessage("lblArenaPlayerOut", name);
            OUT_TEXTS.put(name, s);
        }
        return s;
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

        /**
         * Where a chip {@code w} wide goes: its left edge, after wrapping if needed; NaN when it does not fit (a wrapped
         * row holds at most {@link #MAX_ROWS} rows, which is all a seat's plate has room for above its lands).
         */
        private float place(final float w) {
            final float gap = Math.max(2f, h * GAP);
            if (rows == 0) {
                rows = 1;
            } else if (maxW > 0 && Math.abs(x - x0) + w > maxW) {
                if (rows >= MAX_ROWS) {
                    return Float.NaN;
                }
                rows++;
                x = x0;
                cy += h + gap;
            }
            if (maxW > 0 && w > maxW) {
                return Float.NaN;
            }
            final float left = rtl ? x - w : x;
            x = rtl ? x - w - gap : x + w + gap;
            return left;
        }

        private float ring() {
            return Math.max(1.5f, h * RING_INSET);
        }

        /** The chip's ground and rim. */
        private void ground(final float left, final float w, final Color fill, final Color rim) {
            final float top = cy - h / 2f;
            g.fillSmoothRoundRect(fill, left, top, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * RIM), rim, left, top, w, h, h / 2f);
        }

        void leadChip(final String text, final Color top, final Color bottom, final Color rim) {
            FSkinFont f = ArenaCards.evenFont(h * LABEL_TEXT);
            float w = ArenaArt.cachedTextWidth(text, f) + h;
            if (maxW > 0 && w > maxW) { // a long step name on a narrow plate: smaller type rather than past the plate's edge
                f = ArenaCards.evenFont(Math.max(8f, h * LABEL_TEXT * (maxW - h) / Math.max(1f, w - h)));
                w = Math.min(maxW, ArenaArt.cachedTextWidth(text, f) + h);
            }
            final float left = place(w);
            if (Float.isNaN(left)) {
                return;
            }
            g.fillSmoothRoundRectGradient(top, bottom, true, left, cy - h / 2f, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * RIM), rim, left, cy - h / 2f, w, h, h / 2f);
            ArenaArt.textBold(g, text, f, ArenaTheme.WHITE, left, cy - h / 2f, w, h, Align.center);
        }

        void commanderChip(final CardView cmdr, final String figure, final boolean near) {
            final FSkinFont f = ArenaCards.evenFont(h * FIGURE_TEXT);
            final float art = h - 2 * ring();
            final float w = h * PAD_LEFT + art + h * TEXT_GAP + ArenaArt.cachedTextWidth(figure, f) + h * PAD_RIGHT;
            final float left = place(w);
            if (Float.isNaN(left)) {
                return;
            }
            final Color c = near ? CMDR_NEAR : CMDR_COLOR;
            ground(left, w, CHIP_FILL, c);
            final float acx = left + h * PAD_LEFT + art / 2f;
            if (!circleArt(g, cmdr, acx, cy, art)) {
                circleInitial(g, cmdr, acx, cy, art);
            }
            g.drawSmoothCircle(Math.max(1f, h * CIRCLE_RIM), c, acx, cy, art / 2f);
            final float tx = acx + art / 2f + h * TEXT_GAP;
            ArenaArt.textBold(g, figure, f, c, tx, cy - h / 2f, left + w - tx - h * PAD_RIGHT * 0.75f, h, Align.center);
        }

        void iconChip(final FSkinImage icon, final String figure, final Color c) {
            final FSkinFont f = ArenaCards.evenFont(h * FIGURE_TEXT);
            final float is = h * ICON;
            final float w = h * PAD_LEFT + is + h * TEXT_GAP + ArenaArt.cachedTextWidth(figure, f) + h * PAD_RIGHT;
            final float left = place(w);
            if (Float.isNaN(left)) {
                return;
            }
            ground(left, w, CHIP_FILL, c);
            g.drawImage(icon, left + h * PAD_LEFT, cy - is / 2f, is, is);
            final float tx = left + h * PAD_LEFT + is + h * TEXT_GAP * 0.5f;
            ArenaArt.textBold(g, figure, f, c, tx, cy - h / 2f, left + w - tx - h * PAD_RIGHT * 0.75f, h, Align.center);
        }

        void manaChip(final String symbol, final String figure) {
            final FSkinFont f = ArenaCards.evenFont(h * FIGURE_TEXT);
            final float is = h * ICON;
            final float w = h * PAD_LEFT + is + h * TEXT_GAP + ArenaArt.cachedTextWidth(figure, f) + h * PAD_RIGHT;
            final float left = place(w);
            if (Float.isNaN(left)) {
                return;
            }
            ground(left, w, CHIP_FILL, MANA_RIM);
            CardFaceSymbols.drawSymbol(symbol, g, left + h * PAD_LEFT, cy - is / 2f, is, is);
            final float tx = left + h * PAD_LEFT + is + h * TEXT_GAP * 0.5f;
            ArenaArt.textBold(g, figure, f, ArenaTheme.WHITE, tx, cy - h / 2f, left + w - tx - h * PAD_RIGHT * 0.75f, h, Align.center);
        }

        void statusChip(final CardView effect, final String label) {
            final FSkinFont f = ArenaCards.evenFont(h * LABEL_TEXT);
            final float art = h - 2 * ring();
            final float w = h * PAD_LEFT + art + h * TEXT_GAP + ArenaArt.cachedTextWidth(label, f) + h * PAD_RIGHT;
            final float left = place(w);
            if (Float.isNaN(left)) {
                return;
            }
            final float top = cy - h / 2f;
            g.fillSmoothRoundRectGradient(STATUS_FILL, CHIP_FILL, true, left, top, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * RIM), STATUS_GOLD, left, top, w, h, h / 2f);
            final float acx = left + h * PAD_LEFT + art / 2f;
            if (!circleArt(g, effect, acx, cy, art)) {
                g.fillSmoothCircle(ArenaTheme.alpha(STATUS_GOLD, 0.35f, S1), acx, cy, art / 2f);
                final FSkinFont lf = ArenaCards.evenFont(art * FIGURE_TEXT);
                ArenaArt.textBold(g, initial(label), lf, STATUS_GOLD, acx - art / 2f, cy - art / 2f, art, art, Align.center);
            }
            g.drawSmoothCircle(Math.max(1f, h * CIRCLE_RIM), STATUS_GOLD, acx, cy, art / 2f);
            final float tx = acx + art / 2f + h * TEXT_GAP;
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
        if (c >= INITIALS.length) {
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
        final FSkinFont lf = ArenaCards.evenFont(d * FIGURE_TEXT);
        ArenaArt.textBold(g, initial(card.getName()), lf, ArenaTheme.WHITE, cx - d / 2f, cy - d / 2f, d, d, Align.center);
    }

    /** A card's art cropped to a circle; false if it has no picture (yet). */
    static boolean circleArt(final Graphics g, final CardView card, final float cx, final float cy, final float d) {
        final FImageComplex art;
        try {
            art = CardRenderer.getCardArt(card);
        } catch (final RuntimeException e) {
            return false;
        }
        if (art == null || art == CardImageRenderer.forgeArt) {
            return false; // the "no picture" stand-in: the colour and initial say more
        }
        final TextureRegion r = art.getTextureRegion();
        if (r == null || r.getRegionWidth() <= 0 || r.getRegionHeight() <= 0) {
            return false;
        }
        ArenaArt.circleImage(g, ArenaArt.squareOf(r), cx, cy, d);
        return true;
    }
}
