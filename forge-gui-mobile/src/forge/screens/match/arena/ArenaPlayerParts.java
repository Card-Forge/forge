package forge.screens.match.arena;

import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.gamemodes.match.Selection;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.util.collect.FCollectionView;

/**
 * What the Arena board shows of a player besides the battlefield, each a small view the player panel places: the zone
 * piles (library, graveyard, exile, command zone) as card piles with counts, the player's name on a plate in the corner,
 * and an opponent's hand as a fan of card backs. Ported from the Swing {@code ArenaZonePile} and VField's name plate.
 * They draw only while the Arena look is on; the player panel hides them otherwise.
 */
public final class ArenaPlayerParts {
    private ArenaPlayerParts() { }

    private static final float CARD_RATIO = 0.716f;
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color();
    private static final Color PAGE_LIT = ArenaTheme.rgb(0x968E7C), PAGE = ArenaTheme.rgb(0x46423A);
    private static final Color EXILE_TINT = ArenaTheme.rgba(150, 170, 220, 90);
    private static final Color BADGE = ArenaTheme.rgba(6, 6, 8, 235), BADGE_RIM = ArenaTheme.rgba(255, 255, 255, 120);
    private static final Color NAME_PLATE = ArenaTheme.rgba(6, 7, 11, 150);

    // ---- piles -----------------------------------------------------------------------------------------------------

    /**
     * A player's piles side by side: the library as a stack of sleeves, the graveyard and exile showing their top card,
     * the command zone its commander (with the commander tax). Each carries a count that jumps when it changes. A click
     * opens the Arena card browser on that zone; the wheel does too.
     */
    public static final class Piles extends FDisplayObject {
        private static final ZoneType[] ZONES = {ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Command};
        private final PlayerView player;
        private boolean rightToLeft;
        private final Consumer<ZoneType> fallback;
        private final int[] seen = {-1, -1, -1, -1};
        private final ArenaMotion.Fade[] bump = {new ArenaMotion.Fade(0f), new ArenaMotion.Fade(0f), new ArenaMotion.Fade(0f), new ArenaMotion.Fade(0f)};
        private final ArenaMotion.Fade[] hot = {new ArenaMotion.Fade(0f), new ArenaMotion.Fade(0f), new ArenaMotion.Fade(0f), new ArenaMotion.Fade(0f)};
        private final float[] cellX = new float[4], cellW = new float[4];
        private final boolean[] shown = new boolean[4];
        private final String[] countText = new String[4];
        // the pile under the pointer this frame (-1: none), whether it is lit, the commander tax and the texts saying it
        private int hovered = -1;
        private boolean hoveredPlayable;
        private final int[] taxSeen = {-1, -1};
        private final CardView[] taxSeenWho = new CardView[2];
        private long taxAt = Long.MIN_VALUE / 2;
        private String taxText, taxHint, playableHint;

        /**
         * @param rightToLeft piles fill from the right (an opponent's, in the top right corner)
         * @param fallback    what to do when the browser does not open (show the zone the old way)
         */
        public Piles(final PlayerView player0, final boolean rightToLeft0, final Consumer<ZoneType> fallback0) {
            player = player0;
            rightToLeft = rightToLeft0;
            fallback = fallback0;
        }

        /** Piles fill from the right (an opponent's, in the top right corner) or from the left (yours). */
        public void setRightToLeft(final boolean rightToLeft0) {
            rightToLeft = rightToLeft0;
        }

        private int count(final int i) {
            final ZoneType zone = ZONES[i];
            return player.getZoneSize(zone);
        }

        private boolean isShown(final int i) {
            // an empty pile takes no room; only the library is always there
            return i == 0 || count(i) > 0;
        }

        private void layoutCells() {
            final float h = getHeight();
            final float pad = Math.max(2f, ArenaTheme.px(2));
            final float cardH = h - 2 * pad - ArenaTheme.px(8);
            final float cw = Math.round(cardH * CARD_RATIO) + ArenaTheme.px(10);
            final float gap = Math.round(cw * 0.12f);
            float x = rightToLeft ? getWidth() : 0f;
            for (int i = 0; i < ZONES.length; i++) {
                shown[i] = isShown(i);
                if (!shown[i]) {
                    continue;
                }
                cellW[i] = cw;
                if (rightToLeft) {
                    x -= cw;
                    cellX[i] = x;
                    x -= gap;
                } else {
                    cellX[i] = x;
                    x += cw + gap;
                }
            }
        }

        private final com.badlogic.gdx.math.Rectangle pileRect = new com.badlogic.gdx.math.Rectangle();

        /** The screen rect of the pile for a zone (cards fly to it), or null if this player has no such pile showing. */
        public com.badlogic.gdx.math.Rectangle getPileScreenRect(final ZoneType zone) {
            if (screenPos.width <= 0) {
                return null;
            }
            layoutCells();
            for (int i = 0; i < ZONES.length; i++) {
                if (ZONES[i] == zone) {
                    if (!shown[i]) {
                        shown[i] = true; //about to get the card that is on its way
                    }
                    return pileRect.set(screenPos.x + cellX[i], screenPos.y, cellW[i], getHeight());
                }
            }
            return null;
        }

        private int pileAt(final float x, final float y) {
            layoutCells();
            for (int i = 0; i < ZONES.length; i++) {
                if (shown[i] && x >= cellX[i] && x < cellX[i] + cellW[i] && y >= 0 && y <= getHeight()) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean tap(final float x, final float y, final int count) {
            final int i = pileAt(x, y);
            if (i < 0) {
                return false;
            }
            open(ZONES[i]);
            return true;
        }

        private void open(final ZoneType zone) {
            if (!ArenaCardBrowser.browseZone(player, zone) && fallback != null) {
                fallback.accept(zone);
            }
        }

        @Override
        public boolean longPress(final float x, final float y) {
            final int i = pileAt(x, y);
            if (i < 0) {
                return false;
            }
            final CardView top = topCard(i);
            if (top != null) {
                CardZoom.show(top);
                return true;
            }
            return false;
        }

        @Override
        public boolean pan(final float x, final float y, final float dx, final float dy, final boolean moreVertical) {
            return false;
        }

        private CardView topCard(final int i) {
            final ZoneType zone = ZONES[i];
            if (zone == ZoneType.Library) {
                return null;
            }
            final FCollectionView<CardView> cards = player.getCards(zone);
            if (cards == null || cards.isEmpty()) {
                return null;
            }
            if (zone == ZoneType.Command && player.getCommanders() != null) {
                // the commander rather than an emblem or effect that happens to be on top
                for (final CardView c : cards) {
                    if (player.getCommanders().contains(c)) {
                        return c;
                    }
                }
            }
            try {
                return cards.getLast();
            } catch (final RuntimeException e) {
                return null; // changed under us; next frame
            }
        }

        @Override
        public void draw(final Graphics g) {
            if (!ArenaTheme.isActive()) {
                return;
            }
            layoutCells();
            hovered = -1;
            final float mx = Forge.mouseMovedX - screenPos.x, my = Forge.mouseMovedY - screenPos.y;
            final boolean over = Forge.magnify && mx >= 0 && my >= 0 && mx < getWidth() && my < getHeight();
            for (int i = 0; i < ZONES.length; i++) {
                final int n = count(i);
                if (seen[i] >= 0 && n != seen[i]) {
                    bump[i].set(1f);
                    bump[i].to(0f, 380);
                }
                if (n != seen[i] || countText[i] == null) {
                    countText[i] = String.valueOf(n);
                }
                seen[i] = n;
                if (!shown[i]) {
                    continue;
                }
                final boolean lit = over && mx >= cellX[i] && mx < cellX[i] + cellW[i];
                if (hot[i].goal() != (lit ? 1f : 0f)) {
                    hot[i].to(lit ? 1f : 0f, ArenaMotion.HOVER_MS);
                }
                drawPile(g, i, n, cellX[i], cellW[i], getHeight());
            }
            if (hovered >= 0) {
                drawHint(g);
            }
        }

        /**
         * Under the pointer, a lit pile says what its light means (something in it can be played - click to open), and
         * the command zone what the commander costs on top now (the tax and how many casts made it).
         */
        private void drawHint(final Graphics g) {
            final String text;
            if (ZONES[hovered] == ZoneType.Command && taxHint != null) {
                text = taxHint;
            } else if (hoveredPlayable) {
                if (playableHint == null) {
                    playableHint = Forge.getLocalizer().getMessage("lblArenaHintPlayable");
                }
                text = playableHint;
            } else {
                return;
            }
            final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(18), getHeight() * 0.13f));
            final float hh = ArenaArt.hintHeight(f);
            final boolean below = screenPos.y < ArenaBoard.windowH() / 2f;
            final float top = below ? getHeight() + ArenaTheme.px(6) : -hh - ArenaTheme.px(6);
            ArenaArt.hint(g, text, f, cellX[hovered] + cellW[hovered] / 2f, top, -screenPos.x, ArenaBoard.windowW() - screenPos.x);
        }

        private void drawPile(final Graphics g, final int i, final int count, final float cellX0, final float cellW0, final float cellH) {
            final ZoneType zone = ZONES[i];
            final float pad = Math.max(2f, ArenaTheme.px(2));
            final int depth = zone == ZoneType.Library ? Math.min(5, 1 + count / 15) : Math.min(2, 1 + count / 6);
            final float d = Math.max(1f, ArenaTheme.px(1.4f));
            float ch = cellH - 2 * pad - ArenaTheme.px(8) - depth * d;
            float cw = Math.round(ch * CARD_RATIO);
            if (cw > cellW0 - 2 * pad - depth * d) {
                cw = cellW0 - 2 * pad - depth * d;
                ch = Math.round(cw / CARD_RATIO);
            }
            if (cw <= 0 || ch <= 0) {
                return;
            }
            final float lift = ArenaTheme.px(4) * hot[i].get();
            final float x = cellX0 + (cellW0 - cw - depth * d) / 2f;
            final float y = cellH - ch - pad - ArenaTheme.px(6) - lift;
            final float r = Math.max(ArenaTheme.px(3), cw / 18f);
            if (count == 0) {
                // an empty library: a dashed hollow with the zone's name
                ArenaArt.frame(g, x, y, cw, ch, r, ArenaTheme.alpha(ArenaTheme.GOLD, 0.27f, S1), Math.max(1f, ArenaTheme.px(1.2f)));
                final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(9), Math.min(ArenaTheme.px(12), cw / 5.5f)));
                g.drawText(zone.getTranslatedName(), f, ArenaTheme.TEXT_DIM, x, y, cw, ch, false, Align.center, true);
                return;
            }
            // the pile's thickness: page edges rising behind the top card, lit along the far edge
            g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.82f, S1), x, y + ArenaTheme.px(2), cw + depth * d, ch, r, ArenaTheme.px(9));
            for (int o = depth; o >= 1; o--) {
                g.fillSmoothRoundRect(o == depth ? PAGE_LIT : PAGE, x + o * d, y - o * d, cw, ch, r);
            }
            // something in it can be played now (the commander from the command zone, flashback, cards cast from exile):
            // the pile breathes in the light the hand's playable cards have
            final boolean playable = hasPlayable(zone);
            final float breath = playable ? 0.65f + 0.35f * ArenaMotion.pulse(1.5f) : 0f;
            if (playable) {
                g.drawGlow(ArenaTheme.alpha(ArenaTheme.PLAYABLE, 0.9f * breath, S3), x, y, cw, ch, r, ArenaTheme.px(10));
            }
            final CardView top = topCard(i);
            if (top != null && MatchController.instance.mayView(top)) {
                CardRenderer.drawCard(g, top, x, y, cw, ch, CardStackPosition.Top);
                if (zone == ZoneType.Exile) {
                    g.fillSmoothRoundRect(EXILE_TINT, x, y, cw, ch, r); // exile reads as a cold, faded pile
                }
            } else {
                ArenaCards.drawCardBack(g, player, x, y, cw, ch, r, false);
            }
            g.drawSmoothRoundRect(1f, ArenaTheme.alpha(ArenaTheme.BLACK, 0.78f, S1), x, y, cw, ch, r);
            if (playable) {
                g.drawSmoothRoundRect(Math.max(1.5f, ArenaTheme.px(2)), ArenaTheme.alpha(ArenaTheme.PLAYABLE, 0.6f + 0.4f * breath, S3), x, y, cw, ch, r);
            }
            if (hot[i].get() > 0f) {
                g.drawGlow(ArenaTheme.alpha(ArenaTheme.HOVER, 0.45f * hot[i].get(), S2), x, y, cw, ch, r, ArenaTheme.px(8));
            }
            String badge = countText[i];
            if (zone == ZoneType.Command) {
                // the tax of each commander waiting here ("Tax +2", partners "Tax +2 / +0"), read at most every
                // ArenaPlayerInfo.REFRESH_MS (the lookup boxes the card's id)
                if (TimeUtils.timeSinceMillis(taxAt) >= ArenaPlayerInfo.REFRESH_MS) {
                    taxAt = TimeUtils.millis();
                    readTax();
                }
                badge = taxText;
            }
            if (hot[i].goal() > 0f) {
                hovered = i;
                hoveredPlayable = playable;
            }
            if (badge != null) {
                countBadge(g, badge, x + cw / 2f, y + ch, cellH, bump[i].get());
            }
        }

        /**
         * The commander tax of the (up to two, with partners) commanders in the command zone: the badge ("Tax +2", "Tax
         * +2 / +0") when any has been cast, and the hover hint saying each one's tax and casts. Texts are made only when
         * a figure changes.
         */
        private void readTax() {
            final List<CardView> commanders = player.getCommanders();
            int n = 0;
            for (int i = 0, m = commanders == null ? 0 : commanders.size(); i < m && n < 2; i++) {
                final CardView c = commanders.get(i);
                if (c.getZone() == ZoneType.Command) {
                    taxWho[n] = c;
                    taxNow[n++] = ArenaPlayerInfo.commanderTax(player, c);
                }
            }
            final int a = n > 0 ? taxNow[0] : -1, b = n > 1 ? taxNow[1] : -1;
            final CardView whoA = n > 0 ? taxWho[0] : null, whoB = n > 1 ? taxWho[1] : null;
            if (a == taxSeen[0] && b == taxSeen[1] && whoA == taxSeenWho[0] && whoB == taxSeenWho[1]) {
                return; // the same commanders waiting, at the same tax
            }
            taxSeen[0] = a;
            taxSeen[1] = b;
            taxSeenWho[0] = whoA;
            taxSeenWho[1] = whoB;
            if (a <= 0 && b <= 0) {
                taxText = null;
                taxHint = null;
                return;
            }
            final Localizer l = Forge.getLocalizer();
            taxText = n > 1 ? l.getMessage("lblArenaCommanderTaxPair", String.valueOf(a), String.valueOf(b))
                    : l.getMessage("lblArenaCommanderTax", String.valueOf(a));
            String hint = null;
            for (int i = 0; i < n; i++) {
                final String one = l.getMessage("lblArenaHintCommanderTax", CardTranslation.getTranslatedName(taxWho[i].getName()),
                        String.valueOf(taxNow[i]), taxNow[i] / 2);
                hint = hint == null ? one : l.getMessage("lblArenaJoin", hint, one);
            }
            taxHint = hint;
        }

        private final int[] taxNow = new int[2];
        private final CardView[] taxWho = new CardView[2];

        /**
         * Whether a card in this zone of the player's is one the prompt offers to play now. Walks the prompt's short list
         * of actionable cards (empty most frames), not the piles: a Commander library is 99 cards.
         */
        private boolean hasPlayable(final ZoneType zone) {
            final List<CardView> actionable = MatchController.instance.getSelection().actionable();
            for (int i = 0, n = actionable.size(); i < n; i++) {
                final CardView c = actionable.get(i);
                if (c.getZone() == zone && player.equals(c.getOwner())) {
                    return true;
                }
            }
            return false;
        }

        private void countBadge(final Graphics g, final String text, final float cx, final float cy, final float cellH, final float bumped) {
            final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(15), cellH * 0.15f) * (1f + 0.38f * bumped));
            final float bh = f.getLineHeight() + ArenaTheme.px(2);
            final float bw = Math.max(bh, ArenaArt.cachedTextWidth(text, f) + bh * 0.7f);
            final float bx = cx - bw / 2f, by = Math.min(getHeight() - bh, cy - bh / 2f);
            g.fillSmoothRoundRect(BADGE, bx, by, bw, bh, bh / 2f);
            g.drawSmoothRoundRect(1f, BADGE_RIM, bx, by, bw, bh, bh / 2f);
            ArenaArt.textBold(g, text, f, ArenaTheme.TEXT, bx, by, bw, bh, Align.center);
        }
    }

    // ---- what you can cast from outside your hand -------------------------------------------------------------------

    /**
     * The cards the prompt offers you to play from outside your hand - your commander in the command zone, flashback
     * and the like in the graveyard, cards you may play from exile - each as a plate above your piles ("Cast Krenko, Mob
     * Boss · Tax +2"); a click casts it (the Swing board puts such cards beside the hand). Read again at most every
     * {@link ArenaPlayerInfo#REFRESH_MS}; drawing them allocates nothing.
     */
    public static final class Castables extends FDisplayObject {
        private static final ZoneType[] FROM = {ZoneType.Command, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Library};
        private static final int MOST = 3;
        /** A plate's height against the window's, and the gap between plates (shares of a plate's height). */
        private static final float PLATE_H = 0.032f, PLATE_GAP = 0.2f;
        private static final Color PLATE_FILL = ArenaTheme.rgba(6, 18, 26, 235);
        private final PlayerView player;
        private final CardView[] cards = new CardView[MOST];
        private final TextureRegion[] arts = new TextureRegion[MOST];
        private final String[] texts = new String[MOST];
        private final int[] taxes = new int[MOST];
        private int n;
        private long readAt;

        public Castables(final PlayerView player0) {
            player = player0;
        }

        /** The plates' height in all, for the most there can be (the panel sizes this view from it). */
        public static float heightFor(final float windowH) {
            final float h = Math.round(windowH * PLATE_H);
            return MOST * h + (MOST - 1) * Math.max(2f, h * PLATE_GAP);
        }

        private void read() {
            readAt = TimeUtils.millis();
            n = 0;
            final List<CardView> actionable = MatchController.instance.getSelection().actionable();
            for (int i = 0, m = actionable.size(); i < m && n < MOST; i++) {
                final CardView c = actionable.get(i);
                if (!isFrom(c.getZone()) || contains(c)) { // yours or not: anything the prompt lets you play from there
                    continue;
                }
                final int tax = c.getZone() == ZoneType.Command ? ArenaPlayerInfo.commanderTax(player, c) : 0;
                if (cards[n] != c || taxes[n] != tax || texts[n] == null) {
                    final boolean land = c.getCurrentState() != null && c.getCurrentState().isLand();
                    final String verb = Forge.getLocalizer().getMessage(land ? "lblArenaPlayCard" : "lblArenaCastCard", CardTranslation.getTranslatedName(c.getName()));
                    texts[n] = tax > 0 ? Forge.getLocalizer().getMessage("lblArenaJoin", verb,
                            Forge.getLocalizer().getMessage("lblArenaCommanderTax", String.valueOf(tax))) : verb;
                }
                arts[n] = ArenaArt.circleArtOf(c); //looked up at each read (a cache read), not kept past the next
                cards[n] = c;
                taxes[n] = tax;
                n++;
            }
            for (int i = n; i < MOST; i++) {
                cards[i] = null;
            }
        }

        private static boolean isFrom(final ZoneType zone) {
            for (final ZoneType z : FROM) {
                if (z == zone) {
                    return true;
                }
            }
            return false;
        }

        private boolean contains(final CardView c) {
            for (int i = 0; i < n; i++) {
                if (cards[i] == c) {
                    return true;
                }
            }
            return false;
        }

        private float plateH() {
            return Math.round(ArenaBoard.windowH() * PLATE_H);
        }

        /** The plate at y (from the bottom up: the first sits lowest, nearest the piles), or -1. */
        private int plateAt(final float y) {
            final float h = plateH(), gap = Math.max(2f, h * PLATE_GAP);
            for (int i = 0; i < n; i++) {
                final float top = getHeight() - (i + 1) * h - i * gap;
                if (y >= top && y < top + h) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean tap(final float x, final float y, final int count) {
            final int i = plateAt(y);
            if (i < 0 || cards[i] == null) {
                return false;
            }
            final CardView card = cards[i];
            ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().selectCard(card, null, null));
            return true;
        }

        @Override
        public void draw(final Graphics g) {
            if (!ArenaTheme.isActive()) {
                return;
            }
            if (TimeUtils.timeSinceMillis(readAt) >= ArenaPlayerInfo.REFRESH_MS) {
                read();
            }
            if (n == 0) {
                return;
            }
            final float h = plateH(), gap = Math.max(2f, h * PLATE_GAP);
            final FSkinFont f = ArenaCards.evenFont(h * 0.5f);
            final float mx = Forge.mouseMovedX - screenPos.x, my = Forge.mouseMovedY - screenPos.y;
            final int over = Forge.magnify && mx >= 0 && mx < getWidth() ? plateAt(my) : -1;
            final float breath = 0.65f + 0.35f * ArenaMotion.pulse(1.5f);
            for (int i = 0; i < n; i++) {
                final float top = getHeight() - (i + 1) * h - i * gap;
                final float w = Math.min(getWidth(), ArenaArt.cachedTextWidth(texts[i], f) + 2.2f * h);
                g.drawGlow(ArenaTheme.alpha(ArenaTheme.PLAYABLE, (i == over ? 0.95f : 0.6f) * breath, S1), 0, top, w, h, h / 2f, h * 0.35f);
                g.fillSmoothRoundRect(PLATE_FILL, 0, top, w, h, h / 2f);
                g.drawSmoothRoundRect(Math.max(1f, h * 0.06f), ArenaTheme.PLAYABLE, 0, top, w, h, h / 2f);
                final float d = h * 0.78f;
                if (ArenaArt.isLive(arts[i])) {
                    ArenaArt.circleImage(g, arts[i], h * 0.12f + d / 2f, top + h / 2f, d);
                } else {
                    g.fillSmoothCircle(ArenaTheme.alpha(ArenaTheme.PLAYABLE, 0.35f, S2), h * 0.12f + d / 2f, top + h / 2f, d / 2f);
                }
                g.drawText(texts[i], f, ArenaTheme.WHITE, h * 0.3f + d, top, w - d - h * 0.6f, h, false, Align.left, true);
            }
        }
    }

    // ---- an opponent's hand ----------------------------------------------------------------------------------------

    /** An opponent's hand as a fan of card backs hanging beside the portrait; a count only when it is too big to draw. */
    public static final class OpponentHand extends FDisplayObject {
        private static final int MOST_SHOWN = 10;
        private final PlayerView player;
        private boolean alwaysCount;
        // which cards are held face up (a hand you may look at): worked out when the hand changes or at the chips' refresh
        private final CardView[] faceUp = new CardView[MOST_SHOWN];
        private int readCount = -1;
        private long readAt;

        public OpponentHand(final PlayerView player0) {
            player = player0;
        }

        /** A seat's fan is too small to count the backs in: it always carries the figure. */
        public void setAlwaysCount(final boolean alwaysCount0) {
            alwaysCount = alwaysCount0;
        }

        @Override
        public void draw(final Graphics g) {
            if (!ArenaTheme.isActive()) {
                return;
            }
            final int count = player.getZoneSize(ZoneType.Hand);
            if (count == 0) {
                return;
            }
            final float h = getHeight(), w = getWidth();
            final float ch = Math.round(h * 0.9f), cw = Math.round(ch * CARD_RATIO);
            final float r = Math.max(ArenaTheme.px(3), cw / 18f);
            final int shown = Math.min(count, MOST_SHOWN);
            if (count != readCount || TimeUtils.timeSinceMillis(readAt) >= ArenaPlayerInfo.REFRESH_MS) {
                readFaceUp(shown);
                readCount = count;
                readAt = TimeUtils.millis();
            }
            // spread sideways as well as turned, so it reads as a held hand and not a stack
            final float stepX = shown < 2 ? 0 : Math.min(cw * 0.55f, (w - cw) / (shown - 1));
            final float cx = w / 2f;
            for (int i = 0; i < shown; i++) {
                final float place = i - (shown - 1) / 2f;
                final float cardX = Math.round(cx + place * stepX);
                final CardView card = faceUp[i];
                g.startRotateTransform(cardX, h + ch / 5f, -place * 4.5f);
                g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.5f, S1), cardX - cw / 2f, h - ch - 2 + ArenaTheme.px(2), cw, ch, r, ArenaTheme.px(5));
                if (card != null) {
                    CardRenderer.drawCard(g, card, cardX - cw / 2f, h - ch - 2, cw, ch, CardStackPosition.Top);
                } else {
                    ArenaCards.drawCardBack(g, player, cardX - cw / 2f, h - ch - 2, cw, ch, r, true);
                }
                g.endTransform();
            }

            if (count > shown || alwaysCount) {
                ArenaArt.countBadge(g, ArenaPlayerInfo.figure(count), cx, h - ch / 3f, Math.max(ArenaTheme.px(20), h * 0.3f));
            }
        }

        /** A hand you may look at (watching a game, a hand revealed for good) is held face up: which of its cards those are. */
        private void readFaceUp(final int shown) {
            final FCollectionView<CardView> cards = player.getCards(ZoneType.Hand);
            for (int i = 0; i < MOST_SHOWN; i++) {
                CardView card = null;
                if (cards != null && i < shown) {
                    try {
                        card = i < cards.size() ? cards.get(i) : null;
                    } catch (final IndexOutOfBoundsException e) {
                        card = null; // changed under us: the next read has it
                    }
                }
                faceUp[i] = card != null && MatchController.instance.mayView(card) ? card : null;
            }
        }
    }

    // ---- the name plate --------------------------------------------------------------------------------------------

    /**
     * The player's name in the corner on a dark plate with a gold diamond, readable whatever the board does behind it.
     * A seat (three or four players) has a small portrait with no phase ladder and no room round it, so its plate also
     * carries what the portrait cannot: the step its turn is in (while it is its turn), and under the name the chips of
     * {@link ArenaPlayerInfo} - commander damage taken, counters, the Monarch - wrapping onto a second row.
     */
    public static final class NamePlate extends FDisplayObject {
        private final PlayerView player;
        private float nameH; // seat: the name's row; the rest of the height is for the chips. 0: the whole height
        private PhaseType phaseFor;
        private final ArenaPlayerInfo.Row chips = new ArenaPlayerInfo.Row();
        // a crowded seat's "see all" button at the end of its name row
        private boolean boardShown;
        private float boardX, boardW;
        private int boardFor = -1;
        private String boardText, boardHint;
        private String phaseText;

        public NamePlate(final PlayerView player0) {
            player = player0;
        }

        /** A crowded seat: "Board 17" after the name, opening all its permanents in the browser (with a hint on hover). */
        private void drawBoardButton(final Graphics g, final float h, final float plateW) {
            final int count = player.getZoneSize(ZoneType.Battlefield);
            boardShown = count > SEAT_CROWDED;
            if (!boardShown) {
                return;
            }
            if (count != boardFor || boardText == null) {
                boardFor = count;
                boardText = Forge.getLocalizer().getMessage("lblArenaSeatBoard", String.valueOf(count));
            }
            final FSkinFont f = ArenaCards.evenFont(h * 0.5f);
            final float bh = h * 0.8f;
            // after the name, never over it: "Board 17", or just "17" on a narrow plate, or nothing if even that has no room
            String label = boardText;
            boardW = ArenaArt.cachedTextWidth(label, f) + bh;
            boardX = plateW + ArenaTheme.px(6);
            if (boardX + boardW > getWidth()) {
                label = ArenaPlayerInfo.figure(count);
                boardW = ArenaArt.cachedTextWidth(label, f) + bh;
            }
            if (boardX + boardW > getWidth()) {
                boardShown = false;
                return;
            }
            g.fillSmoothRoundRect(ArenaArt.HINT_FILL, boardX, (h - bh) / 2f, boardW, bh, bh / 2f);
            g.drawSmoothRoundRect(1f, ArenaTheme.GOLD, boardX, (h - bh) / 2f, boardW, bh, bh / 2f);
            g.drawText(label, f, ArenaTheme.WHITE, boardX, (h - bh) / 2f, boardW, bh, false, Align.center, true);
            final float mx = Forge.mouseMovedX - screenPos.x, my = Forge.mouseMovedY - screenPos.y;
            if (Forge.magnify && mx >= boardX && mx < boardX + boardW && my >= 0 && my < h) {
                if (boardHint == null) {
                    boardHint = Forge.getLocalizer().getMessage("lblArenaSeatBoardHint", player.getName());
                }
                ArenaArt.hint(g, boardHint, f, boardX + boardW / 2f, h + ArenaTheme.px(4), -screenPos.x, ArenaBoard.windowW() - screenPos.x);
            }
        }

        /**
         * A click on the plate picks the player when the prompt can take them (a defender to attack, a target, a player
         * to choose): what its "Click to attack" chip and the portrait's hint promise.
         */
        @Override
        public boolean tap(final float x, final float y, final int count) {
            if (boardShown && x >= boardX && x < boardX + boardW && y < nameH) { // a crowded seat's board, all of it, in the browser
                ArenaCardBrowser.browseZone(player, ZoneType.Battlefield);
                return true;
            }
            if (!ArenaBoard.isAttackOption(player) && !MatchController.instance.isSelectable(player)) {
                return false;
            }
            ThreadUtil.invokeInGameThread(() -> MatchController.instance.getGameController().selectPlayer(player, null));
            return true;
        }

        /** A seat's plate: the name in the top {@code nameH0}, chips below. 0 for a plain name plate. */
        public void setSeatRows(final float nameH0) {
            nameH = nameH0;
        }

        @Override
        public void draw(final Graphics g) {
            if (!ArenaTheme.isActive()) {
                return;
            }
            final String name = player.getName();
            final float h = (nameH > 0 ? nameH : getHeight()) - 2, d = Math.max(ArenaTheme.px(6), h / 3f);
            final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(13), h * 0.62f));
            final float textX = ArenaTheme.px(12) + d + ArenaTheme.px(8);
            final float plateW = Math.min(getWidth() - 1, textX + ArenaArt.cachedTextWidth(name, f) + ArenaTheme.px(14));
            g.fillSmoothRoundRect(NAME_PLATE, 0, 0, plateW, h, h / 2f);
            final float mx = ArenaTheme.px(12) + d / 2f, my = h / 2f;
            // a small gold diamond: a square turned on its corner (a rounded square at 45 degrees)
            g.startRotateTransform(mx, my, 45f);
            g.fillSmoothRoundRect(ArenaTheme.GOLD, mx - d * 0.36f, my - d * 0.36f, d * 0.72f, d * 0.72f, d * 0.08f);
            g.endTransform();
            ArenaArt.textShadowed(g, name, f, ArenaTheme.WHITE, textX, 0, plateW - textX, h, Align.left);
            if (nameH <= 0) {
                return;
            }
            // whose turn it is shows on the seat's frame; which step it is in leads the chips, as the ladder would show
            // it, and the seat that plays next says so (turn order round the table)
            final GameView game = MatchController.instance.getGameView();
            int lead = ArenaPlayerInfo.LEAD_NONE;
            String leadText = phaseText;
            final Selection.DefenderOption attack = ArenaBoard.attackOption(player);
            if (attack != null) {
                // while you declare attackers the seat says whether your attack can go to them and on what terms (its
                // small portrait has no room under it for the plate a single opponent's gets)
                lead = ArenaPlayerInfo.LEAD_ATTACK;
                leadText = ArenaBoard.attackStateText(attack, MatchController.instance.isHighlighted(player));
            } else if (game != null && !game.isGameOver()) {
                if (game.getPlayerTurn() == player && game.getPhase() != null) {
                    if (game.getPhase() != phaseFor) {
                        phaseFor = game.getPhase();
                        phaseText = phaseFor.nameForUi;
                    }
                    lead = ArenaPlayerInfo.LEAD_PHASE;
                    leadText = phaseText;
                } else if (ArenaBoard.nextTurnPlayer() == player) {
                    lead = ArenaPlayerInfo.LEAD_NEXT;
                }
            }
            final float chipH = Math.round(h * SEAT_CHIP_SHARE), gap = Math.max(2f, chipH * 0.18f);
            drawBoardButton(g, h, plateW);
            ArenaPlayerInfo.draw(g, chips, this, player, ArenaPlayerInfo.ALL | ArenaPlayerInfo.MANA, lead, leadText, ArenaTheme.px(4),
                    nameH + gap + chipH / 2f, chipH, false, getWidth() - ArenaTheme.px(4) - nameH * FAN_CLEARANCE);
        }
    }

    /** A seat with more permanents than this crowds its row (tiles shrink and overlap): its plate offers the whole board. */
    private static final int SEAT_CROWDED = 12;

    /** A seat's chips against the height of its name row; room kept clear for the hand fan's leaning cards (in name rows). */
    private static final float SEAT_CHIP_SHARE = 0.86f, FAN_CLEARANCE = 0.6f;
}
