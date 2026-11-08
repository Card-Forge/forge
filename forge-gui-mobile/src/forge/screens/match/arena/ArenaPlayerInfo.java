package forge.screens.match.arena;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;

import forge.Forge;
import forge.Graphics;
import forge.ImageKeys;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardFaceSymbols;
import forge.card.mana.ManaAtom;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
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
 * players who are out.
 *
 * <h2>Cost</h2>
 * Each caller keeps a {@link Row}. Reading the game (iterating its collections, looking up art) and laying the chips
 * out - shrinking them until a wrapping row fits - happens in {@link Row#rebuild} at most every {@link #REFRESH_MS}, or
 * at once when the row's place, size or lead changes; every other frame only draws what the row holds, which allocates
 * nothing.
 */
public final class ArenaPlayerInfo {
    private ArenaPlayerInfo() { }

    /** What to draw: commander damage, counters, designations, floating mana. */
    public static final int CMDR = 1, COUNTERS = 2, STATUS = 4, MANA = 8, ALL = CMDR | COUNTERS | STATUS;

    /** How a row is led: nothing, the step of the turn (gold), or "plays next" (steel). */
    public static final int LEAD_NONE = 0, LEAD_PHASE = 1, LEAD_NEXT = 2, LEAD_ATTACK = 3;

    /** How often a row reads the game again (the figures it shows change on game events, not per frame). */
    public static final long REFRESH_MS = 100;

    // ---- the chip's proportions, as shares of its height ---------------------------------------------------------
    /** Figures and labels. */
    private static final float FIGURE_TEXT = 0.62f, LABEL_TEXT = 0.56f;
    /** Gap between chips and between wrapped rows. */
    private static final float GAP = 0.18f;
    /** Padding before a picture, and after the text; a chip with no picture pads its text by half a height each side. */
    private static final float PAD_LEFT = 0.12f, PAD_RIGHT = 0.4f, PAD_PLAIN = 0.5f;
    /** Space between a picture and its text. */
    private static final float TEXT_GAP = 0.2f;
    /** An icon's size, and the ring round a circle picture (a circle is the chip's height less this on each side). */
    private static final float ICON = 0.72f, RING_INSET = 0.09f;
    /** Rim lines. */
    private static final float RIM = 0.05f, CIRCLE_RIM = 0.045f;
    /** Commander damage turns red from this much (21 is lethal). */
    private static final int CMDR_NEAR_DAMAGE = 15;
    /** The most rows a wrapping row of chips takes. */
    private static final int MAX_ROWS = 2;
    /** How a row that does not fit shrinks: this much a step, at most this many steps (down to about half its size). */
    private static final float FIT_SHRINK = 0.88f;
    private static final int FIT_STEPS = 5;
    /** The most chips a row holds. */
    private static final int MAX_CHIPS = 24;

    private static final Color CMDR_COLOR = ArenaTheme.rgb(0xE8C27A), CMDR_NEAR = ArenaTheme.rgb(0xFF6A5C);
    private static final Color CHIP_FILL = ArenaTheme.rgba(8, 9, 14, 225);
    private static final Color POISON = ArenaTheme.rgb(0x6FD06F), ENERGY = ArenaTheme.rgb(0xF5D24B), EXPERIENCE = ArenaTheme.rgb(0xC9A0FF),
            RAD = ArenaTheme.rgb(0xB7E35A), TICKET = ArenaTheme.rgb(0xFF9F5A), STATUS_GOLD = ArenaTheme.rgb(0xF2D58C),
            MANA_RIM = ArenaTheme.rgb(0xB9C4D6), OUT_TEXT = ArenaTheme.rgb(0xB8B2A6), OUT_RIM = ArenaTheme.rgb(0x6A655E);
    private static final Color LEAD_TOP = ArenaTheme.rgb(0x8A6A2E), LEAD_BOTTOM = ArenaTheme.rgb(0x4A3612);
    private static final Color NEXT_TOP = ArenaTheme.rgb(0x4C5664), NEXT_BOTTOM = ArenaTheme.rgb(0x252B33), NEXT_RIM = ArenaTheme.rgb(0xAFC3DA);
    private static final Color STATUS_FILL = ArenaTheme.rgba(58, 44, 16, 235), OUT_FILL = ArenaTheme.rgba(20, 18, 16, 225);

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

    /**
     * The commander tax on {@code commander} for {@code player}: two more for each time it has already been cast from
     * the command zone (each partner counts its own), 0 for a card that is not one of their commanders.
     */
    public static int commanderTax(final PlayerView player, final CardView commander) {
        final List<CardView> commanders = player.getCommanders();
        return commanders != null && commanders.contains(commander) ? 2 * player.getCommanderCast(commander) : 0;
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

    // ---- a row ------------------------------------------------------------------------------------------------------

    /** What a chip shows before its text. */
    private static final int PIC_NONE = 0, PIC_CARD = 1, PIC_ICON = 2, PIC_SYMBOL = 3;

    /**
     * One caller's row of chips, as last read and laid out. Keep one per place a row is drawn (a portrait's two sides,
     * a seat's plate) and pass it to {@link #draw} each frame.
     */
    public static final class Row {
        // what was asked for last, so a change of place or lead lays the row out again at once
        private PlayerView player;
        private int what = -1, lead = -1;
        private String leadText;
        private float x0 = Float.NaN, cy0, h0, maxW0;
        private boolean rtl0;
        private long builtAt;
        // the chips: what they show, and where they went
        private int n;
        private final int[] pic = new int[MAX_CHIPS];
        private final CardView[] card = new CardView[MAX_CHIPS];
        private final TextureRegion[] art = new TextureRegion[MAX_CHIPS];
        private final String[] initial = new String[MAX_CHIPS];
        private final Color[] tint = new Color[MAX_CHIPS];
        private final FSkinImage[] icon = new FSkinImage[MAX_CHIPS];
        private final String[] symbol = new String[MAX_CHIPS];
        private final String[] text = new String[MAX_CHIPS];
        private final boolean[] label = new boolean[MAX_CHIPS];
        private final Color[] top = new Color[MAX_CHIPS], bottom = new Color[MAX_CHIPS], rim = new Color[MAX_CHIPS], ink = new Color[MAX_CHIPS];
        private final float[] cx = new float[MAX_CHIPS], cyAt = new float[MAX_CHIPS], cw = new float[MAX_CHIPS];
        private final boolean[] shown = new boolean[MAX_CHIPS], clipped = new boolean[MAX_CHIPS];
        private float h;
        private int rows;
        private FSkinFont figureFont, labelFont, initialFont;

        /** Whether the row has to read the game and lay itself out again before this frame's draw. */
        private boolean stale(final PlayerView p, final int what1, final int lead1, final String leadText1, final float x, final float cy,
                final float h1, final boolean rtl, final float maxW) {
            return p != player || what1 != what || lead1 != lead || leadText1 != leadText || x != x0 || cy != cy0 || h1 != h0
                    || rtl != rtl0 || maxW != maxW0 || TimeUtils.timeSinceMillis(builtAt) >= REFRESH_MS;
        }

        private void add(final int pic1, final String text1, final boolean label1, final Color top1, final Color bottom1, final Color rim1,
                final Color ink1) {
            if (n >= MAX_CHIPS) {
                return;
            }
            pic[n] = pic1;
            text[n] = text1;
            label[n] = label1;
            top[n] = top1;
            bottom[n] = bottom1;
            rim[n] = rim1;
            ink[n] = ink1;
            if (pic1 != PIC_CARD) {
                card[n] = null; // a card chip keeps its slot's picture if it shows the same card again (setCard)
                art[n] = null;
            }
            initial[n] = null;
            icon[n] = null;
            symbol[n] = null;
            n++;
        }

        /** Reads the player's state into chips and lays them out, shrinking them until a wrapping row fits. */
        private void rebuild(final PlayerView p, final int what1, final int lead1, final String leadText1, final float x, final float cy,
                final float h1, final boolean rtl, final float maxW) {
            player = p;
            what = what1;
            lead = lead1;
            leadText = leadText1;
            x0 = x;
            cy0 = cy;
            h0 = h1;
            rtl0 = rtl;
            maxW0 = maxW;
            builtAt = TimeUtils.millis();
            n = 0;
            if (lead1 == LEAD_PHASE && leadText1 != null) {
                add(PIC_NONE, leadText1, true, LEAD_TOP, LEAD_BOTTOM, ArenaTheme.GOLD, ArenaTheme.WHITE);
            } else if (lead1 == LEAD_NEXT) {
                add(PIC_NONE, nextLabel(), true, NEXT_TOP, NEXT_BOTTOM, NEXT_RIM, ArenaTheme.WHITE);
            } else if (lead1 == LEAD_ATTACK && leadText1 != null) { // while you declare attackers: whether and how to attack them
                final Selection.DefenderOption o = ArenaBoard.attackOption(p);
                final boolean current = MatchController.instance.isHighlighted(p);
                add(PIC_NONE, leadText1, true, CHIP_FILL, CHIP_FILL, o == null ? ArenaTheme.SELECTABLE : ArenaBoard.attackPlateRim(o, current),
                        o == null ? ArenaTheme.WHITE : ArenaBoard.attackPlateInk(o));
                final String cost = o == null ? null : ArenaBoard.attackCostText(o), goad = o == null ? null : ArenaBoard.attackGoadText(o);
                if (cost != null) { // what it takes, as chips of their own so they wrap on a narrow plate
                    add(PIC_NONE, cost, true, CHIP_FILL, CHIP_FILL, ArenaBoard.attackPlateRim(o, false), ArenaTheme.WHITE);
                }
                if (goad != null) {
                    add(PIC_NONE, goad, true, CHIP_FILL, CHIP_FILL, ArenaBoard.attackPlateRim(o, false), ArenaTheme.WHITE);
                }
            }
            if ((what1 & CMDR) != 0) {
                readCommanderDamage(p);
            }
            if ((what1 & COUNTERS) != 0) {
                for (int i = 0; i < COUNTER_TYPES.length; i++) {
                    final int k = p.getCounters(COUNTER_TYPES[i]);
                    if (k > 0) {
                        add(PIC_ICON, figure(k), false, CHIP_FILL, CHIP_FILL, COUNTER_COLORS[i], COUNTER_COLORS[i]);
                        icon[n - 1] = COUNTER_ICONS[i];
                    }
                }
            }
            if ((what1 & STATUS) != 0) {
                readStatus(p);
            }
            if ((what1 & MANA) != 0) {
                for (int i = 0; i < ManaAtom.MANATYPES.length; i++) {
                    final int k = p.getMana(ManaAtom.MANATYPES[i]);
                    if (k > 0) {
                        add(PIC_SYMBOL, figure(k), false, CHIP_FILL, CHIP_FILL, MANA_RIM, ArenaTheme.WHITE);
                        symbol[n - 1] = MANA_SYMBOLS[i];
                    }
                }
            }
            // two commanders with the same initial (partners Tymna and Thrasios) show two letters each, so they tell apart
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (pic[i] == PIC_CARD && pic[j] == PIC_CARD && card[i] != card[j] && initial[i] != null && initial[i].equals(initial[j])) {
                        initial[i] = twoLetters(card[i].getName());
                        initial[j] = twoLetters(card[j].getName());
                    }
                }
            }
            // the one shrink-to-fit step: smaller chips until nothing is left out (or the floor), keeping the top
            float fit = h1;
            for (int step = 0; ; step++) {
                place(x, cy - (h1 - fit) / 2f, fit, rtl, maxW);
                if (maxW <= 0 || allShown() || step >= FIT_STEPS) {
                    break;
                }
                fit *= FIT_SHRINK;
            }
        }

        private boolean allShown() {
            for (int i = 0; i < n; i++) {
                if (!shown[i]) {
                    return false;
                }
            }
            return true;
        }

        private void readCommanderDamage(final PlayerView p) {
            final GameView game = MatchController.instance.getGameView();
            final FCollectionView<PlayerView> players = game == null ? null : game.getPlayers();
            if (players == null) {
                return;
            }
            for (int i = 0; i < players.size(); i++) {
                final PlayerView other = players.get(i);
                final List<CardView> commanders = other == p ? null : other.getCommanders();
                for (int j = 0, m = commanders == null ? 0 : commanders.size(); j < m; j++) {
                    final CardView cmdr = commanders.get(j);
                    final int damage = p.getCommanderDamage(cmdr);
                    if (damage > 0) {
                        final Color c = damage >= CMDR_NEAR_DAMAGE ? CMDR_NEAR : CMDR_COLOR;
                        add(PIC_CARD, figure(damage), false, CHIP_FILL, CHIP_FILL, c, c);
                        setCard(cmdr, null);
                    }
                }
            }
        }

        private void readStatus(final PlayerView p) {
            final FCollectionView<CardView> command = p.getCards(ZoneType.Command);
            if (command == null) {
                return;
            }
            try {
                for (int i = 0; i < command.size(); i++) {
                    final CardView c = command.get(i);
                    final int status = statusOf(c);
                    if (status >= 0) {
                        final String l = statusLabel(status);
                        add(PIC_CARD, l, true, STATUS_FILL, CHIP_FILL, STATUS_GOLD, STATUS_GOLD);
                        setCard(c, l);
                    }
                }
            } catch (final IndexOutOfBoundsException e) {
                // the zone changed under us: the next rebuild has it
            }
        }

        /** A card's picture for the chip just added: its art, or (none loaded yet) its colours and initial. */
        private void setCard(final CardView c, final String nameForInitial) {
            final int i = n - 1;
            if (card[i] != c || art[i] == null) { // the same card as last time: keep its picture
                card[i] = c;
                art[i] = ArenaArt.circleArtOf(c);
            }
            initial[i] = initial(nameForInitial != null ? nameForInitial : c.getName());
            if (tint[i] == null) {
                tint[i] = new Color(); // one per slot, reused by every rebuild
            }
            try {
                if (nameForInitial != null) {
                    ArenaTheme.alpha(STATUS_GOLD, 0.35f, tint[i]);
                } else {
                    ArenaTheme.mix(ArenaTheme.tileTint(c.getCurrentState()), ArenaTheme.BLACK, 0.35f, tint[i]);
                }
            } catch (final RuntimeException e) {
                tint[i].set(CMDR_COLOR);
            }
        }

        /** Places the chips in rows of {@code h}; marks those that do not fit (beyond {@link #MAX_ROWS}) as not shown. */
        private void place(final float x, final float cy, final float h1, final boolean rtl, final float maxW) {
            h = h1;
            figureFont = ArenaCards.evenFont(h1 * FIGURE_TEXT);
            labelFont = ArenaCards.evenFont(h1 * LABEL_TEXT);
            initialFont = ArenaCards.evenFont((h1 - 2 * Math.max(1.5f, h1 * RING_INSET)) * FIGURE_TEXT);
            final float gap = Math.max(2f, h1 * GAP);
            float at = x, y = cy;
            rows = 0;
            for (int i = 0; i < n; i++) {
                float w = width(i);
                clipped[i] = maxW > 0 && w > maxW;
                if (clipped[i]) {
                    w = maxW; // a lone chip wider than the row: its text is cut at the edge
                }
                if (rows == 0) {
                    rows = 1;
                } else if (maxW > 0 && Math.abs(at - x) + w > maxW) {
                    if (rows >= MAX_ROWS) {
                        for (int k = i; k < n; k++) {
                            shown[k] = false;
                        }
                        return;
                    }
                    rows++;
                    at = x;
                    y += h1 + gap;
                }
                cx[i] = rtl ? at - w : at;
                cyAt[i] = y;
                cw[i] = w;
                shown[i] = true;
                at = rtl ? at - w - gap : at + w + gap;
            }
        }

        private float width(final int i) {
            final float tw = ArenaArt.cachedTextWidth(text[i], label[i] ? labelFont : figureFont);
            if (pic[i] == PIC_NONE) {
                return tw + 2 * h * PAD_PLAIN;
            }
            return h * PAD_LEFT + picSize(i) + h * TEXT_GAP + tw + h * PAD_RIGHT;
        }

        private float picSize(final int i) {
            return pic[i] == PIC_CARD ? h - 2 * Math.max(1.5f, h * RING_INSET) : h * ICON;
        }

        /** Draws what the row holds. No game reads, no allocation. */
        private void drawChips(final Graphics g) {
            for (int i = 0; i < n; i++) {
                if (shown[i]) {
                    chip(g, i);
                }
            }
        }

        /** One chip: its ground and rim, its picture if any, its text. */
        private void chip(final Graphics g, final int i) {
            final float left = cx[i], w = cw[i], cy = cyAt[i], top0 = cy - h / 2f;
            if (top[i] == bottom[i]) {
                g.fillSmoothRoundRect(top[i], left, top0, w, h, h / 2f);
            } else {
                g.fillSmoothRoundRectGradient(top[i], bottom[i], true, left, top0, w, h, h / 2f);
            }
            g.drawSmoothRoundRect(Math.max(1f, h * RIM), rim[i], left, top0, w, h, h / 2f);
            float tx = left + h * PAD_PLAIN;
            if (pic[i] != PIC_NONE) {
                final float s = picSize(i), pcx = left + h * PAD_LEFT + s / 2f;
                switch (pic[i]) {
                    case PIC_CARD:
                        if (art[i] != null) {
                            ArenaArt.circleImage(g, art[i], pcx, cy, s);
                        } else {
                            g.fillSmoothCircle(tint[i], pcx, cy, s / 2f);
                            ArenaArt.textBold(g, initial[i], initialFont, label[i] ? rim[i] : ArenaTheme.WHITE,
                                    pcx - s / 2f, cy - s / 2f, s, s, Align.center);
                        }
                        g.drawSmoothCircle(Math.max(1f, h * CIRCLE_RIM), rim[i], pcx, cy, s / 2f);
                        break;
                    case PIC_ICON:
                        g.drawImage(icon[i], pcx - s / 2f, cy - s / 2f, s, s);
                        break;
                    default:
                        CardFaceSymbols.drawSymbol(symbol[i], g, pcx - s / 2f, cy - s / 2f, s, s);
                        break;
                }
                tx = pcx + s / 2f + h * TEXT_GAP;
            }
            final float textW = left + w - tx - (pic[i] == PIC_NONE ? h * PAD_PLAIN : h * PAD_RIGHT * 0.75f);
            if (clipped[i]) {
                g.startClip(left, top0, w, h);
            }
            if (label[i] && text[i].indexOf('{') >= 0) { // a cost ("Pay {2} each"): mana symbols, as on the cards
                ArenaArt.symbolText(g, text[i], labelFont, ink[i], tx, top0, Math.max(textW, 1f), h);
            } else if (label[i]) {
                ArenaArt.textShadowed(g, text[i], labelFont, ink[i], tx, top0, Math.max(textW, 1f), h, pic[i] == PIC_NONE ? Align.center : Align.left);
            } else {
                ArenaArt.textBold(g, text[i], figureFont, ink[i], tx, top0, Math.max(textW, 1f), h, Align.center);
            }
            if (clipped[i]) {
                g.endClip();
            }
        }
    }

    /**
     * Draws a row of chips from {@code x} rightwards ({@code rightToLeft} false) or leftwards, wrapping onto a second
     * row below after {@code maxW} (0: no limit, one row); rows are {@code h} tall, the first centred on {@code cy}.
     * {@code lead} ({@link #LEAD_PHASE} or {@link #LEAD_ATTACK} with {@code leadText}, or {@link #LEAD_NEXT}) comes first. A row that does not
     * fit takes smaller chips. Returns the number of rows used.
     */
    public static int draw(final Graphics g, final Row row, final PlayerView player, final int what, final int lead, final String leadText,
            final float x, final float cy, final float h, final boolean rightToLeft, final float maxW) {
        if (player == null) {
            return 0;
        }
        if (row.stale(player, what, lead, leadText, x, cy, h, rightToLeft, maxW)) {
            row.rebuild(player, what, lead, leadText, x, cy, h, rightToLeft, maxW);
        }
        row.drawChips(g);
        return row.rows;
    }

    // ---- who is out -------------------------------------------------------------------------------------------------

    private static final String[] outTexts = new String[8];
    private static int outCount;
    private static long outBuiltAt;
    private static GameView outGame;
    /** "{name} is out", made once per name. */
    private static final Map<String, String> OUT_TEXTS = new HashMap<>();

    /**
     * The players who are out, one dim plate each ("Vivien is out"), stacked down from {@code top} with their right
     * edges at {@code right}: a seat that goes leaves the board, so this is what still says who has gone. The list is
     * read again at most every {@link #REFRESH_MS}.
     */
    public static void drawEliminated(final Graphics g, final GameView game, final float right, final float top, final float h) {
        if (game != outGame || TimeUtils.timeSinceMillis(outBuiltAt) >= REFRESH_MS) {
            outGame = game;
            outBuiltAt = TimeUtils.millis();
            outCount = 0;
            final FCollectionView<PlayerView> players = game == null ? null : game.getPlayers();
            for (int i = 0, m = players == null ? 0 : players.size(); i < m && outCount < outTexts.length; i++) {
                final PlayerView p = players.get(i);
                if (p.getHasLost()) {
                    outTexts[outCount++] = outText(p);
                }
            }
        }
        final FSkinFont f = ArenaCards.evenFont(h * LABEL_TEXT);
        float y = top;
        for (int i = 0; i < outCount; i++) {
            final String text = outTexts[i];
            final float w = ArenaArt.cachedTextWidth(text, f) + h;
            g.fillSmoothRoundRect(OUT_FILL, right - w, y, w, h, h / 2f);
            g.drawSmoothRoundRect(Math.max(1f, h * RIM), OUT_RIM, right - w, y, w, h, h / 2f);
            g.drawText(text, f, OUT_TEXT, right - w, y, w, h, false, Align.center, true);
            y += h + Math.max(2f, h * GAP);
        }
    }

    private static String outText(final PlayerView p) {
        final String name = p.getName();
        String s = OUT_TEXTS.get(name);
        if (s == null) {
            s = Forge.getLocalizer().getMessage("lblArenaPlayerOut", name);
            OUT_TEXTS.put(name, s);
        }
        return s;
    }

    // ---- pictures ---------------------------------------------------------------------------------------------------

    private static final String[] INITIALS = new String[128];

    /** The first two letters of a name ("Ty", "Th"), for initials that would otherwise be the same. Made on rebuild only. */
    private static String twoLetters(final String name) {
        if (name == null || name.length() < 2) {
            return initial(name);
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1, 2);
    }

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
}
