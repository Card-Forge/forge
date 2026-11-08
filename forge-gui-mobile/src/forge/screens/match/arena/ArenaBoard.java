package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;

import forge.Forge;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

/**
 * Where the parts of a player's area go on the Arena board, as fractions of the window, so 21:9, 16:9 and smaller
 * landscape screens all get the same board (port of the Swing {@code VField.ArenaFieldLayout},
 * {@code PlayArea.layoutArenaBoard} and the {@code CardArea} hand fan). The two fields tile the window: the opponent's
 * the top {@link ArenaTheme#FIELD_SPLIT}, yours the rest. With three or four players the opponents share the top as
 * "seats" side by side. Everything is returned in window coordinates unless a method says otherwise; nothing here
 * allocates.
 */
public final class ArenaBoard {
    private ArenaBoard() { }

    // ---- portrait --------------------------------------------------------------------------------------------------
    /** Portrait diameter, as a share of the window's height. */
    public static final float PORTRAIT = 0.125f, PORTRAIT_SEAT = 0.066f;
    /** Where the middle of the local portrait is, as a share of the window's height. */
    public static final float LOCAL_PORTRAIT_Y = 0.778f;

    // ---- battlefield rows (PlayArea) ---------------------------------------------------------------------------------
    /** A battlefield tile's height (the local front row), as a share of the window's height, and how far a crowded row shrinks. */
    public static final float TILE_H = 0.145f, TILE_H_SEAT = 0.11f, TILE_H_MIN = 0.07f;
    /** Tiles are wider than tall: art, name and stats. */
    public static final float TILE_RATIO = 1.27f;
    /** Half the gap the back row leaves for the portrait. */
    public static final float CLEAR = 0.205f, CLEAR_SEAT = 0.008f;
    /** The board recedes: each row's size against the local front row. */
    public static final float OWN_BACK = 0.85f, FOE_FRONT = 0.92f, FOE_BACK = 0.64f, SEAT_BACK = 0.74f;
    /** In combat the fighters step out towards the other army, and stand a little larger. */
    public static final float COMBAT_STEP = 0.015f, COMBAT_SCALE = 1.05f;
    /** Rows' tops in the window: the local front and back row, an opponent's back and front row. */
    public static final float LOCAL_FRONT_Y = 0.425f, LOCAL_BACK_Y = 0.605f, FOE_BACK_Y = 0.08f, FOE_FRONT_Y = 0.205f,
            SEAT_BACK_Y = 0.1f, SEAT_FRONT_Y = 0.205f;
    /** Space between tiles, as a share of a tile's width; stacked copies show this much of each. */
    public static final float TILE_GAP = 0.085f, STACK_STEP = 0.1f;
    /** A stack of tokens shows at most this many edges behind its front card; its count badge says the rest. */
    public static final int TOKEN_PEEK = 2;

    // ---- the hand fan (CardArea.doFanLayout) ------------------------------------------------------------------------
    /** The local hand's strip: from this share of the window's height down, between these shares of its width. */
    public static final float HAND_TOP = 0.835f, HAND_LEFT = 0.14f, HAND_RIGHT = 0.86f;
    public static final float FAN_STEP = 0.78f, FAN_MIN_STEP = 0.3f, FAN_ARC = 0.06f, FAN_LIFT = 0.08f, FAN_SINK = 0.52f,
            FAN_COST_ROW = 0.09f;
    /** Lean per place away from the middle of the hand, and the most a card leans (degrees). */
    public static final float FAN_LEAN = 2.2f, FAN_MAX_LEAN = 9f;
    /** How much a hovered hand card grows besides rising. */
    public static final float FAN_HOVER_GROWTH = 0.06f;
    /** Space between the hand and the cards castable from elsewhere, and their spacing (shares of a card's width). */
    public static final float FAN_EXTRAS_GAP = 0.42f, FAN_EXTRAS_STEP = 0.62f;
    private static final ZoneType[] CASTABLE_FROM = {ZoneType.Command, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Library};

    /**
     * What follows the local hand on its right (Swing CHand.getArenaCastables): the player's commanders while they wait
     * in the command zone, then every other card outside the hand the prompt offers to play now (flashback, cards exiled
     * to be played, a companion...), from any player's zones.
     */
    public static List<CardView> handCastables(final PlayerView player, final List<CardView> hand) {
        final List<CardView> extras = new ArrayList<>();
        // read from the command zone itself: the list of commanders can hold a view of the card from before it last
        // changed zones; views are equal by card id, so the list still says which cards are commanders
        final FCollectionView<CardView> command = player.getCards(ZoneType.Command);
        if (player.getCommanders() != null && command != null) {
            for (final CardView card : command) {
                if (player.getCommanders().contains(card) && !extras.contains(card)) {
                    extras.add(card);
                }
            }
        }
        // the prompt's short list of actionable cards, not the zones: a Commander library is 99 cards
        final List<CardView> actionable = MatchController.instance.getSelection().actionable();
        for (int i = 0, n = actionable.size(); i < n; i++) {
            final CardView card = actionable.get(i);
            if (isCastableFrom(card.getZone()) && !hand.contains(card) && !extras.contains(card)) {
                extras.add(card);
            }
        }
        return extras;
    }

    private static boolean isCastableFrom(final ZoneType zone) {
        for (final ZoneType z : CASTABLE_FROM) {
            if (z == zone) {
                return true;
            }
        }
        return false;
    }

    // ---- window ----------------------------------------------------------------------------------------------------
    public static float windowW() {
        return Math.max(1, Forge.getScreenWidth());
    }

    public static float windowH() {
        return Math.max(1, Forge.getScreenHeight());
    }

    // ---- who is at the table (worked out once per frame: every seat, chip and tile asks) ----------------------------
    private static long tableFrame = -1;
    private static GameView tableGame;
    private static boolean seatGame, declaringAttack;
    private static PlayerView nextPlayer;
    private static long nextReadAt = Long.MIN_VALUE / 2;

    /** Brings the per-frame answers up to date: an index walk over the players, no allocation. */
    private static void refreshTable() {
        final long frame = Gdx.graphics == null ? -1 : Gdx.graphics.getFrameId();
        final GameView game = MatchController.instance.getGameView();
        if (frame == tableFrame && game == tableGame && frame >= 0) {
            return;
        }
        tableFrame = frame;
        if (game != tableGame) {
            tableGame = game;
            nextReadAt = Long.MIN_VALUE / 2;
        }
        seatGame = false;
        declaringAttack = false;
        final FCollectionView<PlayerView> players = game == null ? null : game.getPlayers();
        if (players == null) {
            nextPlayer = null;
            return;
        }
        final int n = players.size();
        int living = 0;
        for (int i = 0; i < n; i++) {
            if (!players.get(i).getHasLost()) {
                living++;
            }
        }
        seatGame = (living == 0 ? n : living) > 2;
        if (TimeUtils.timeSinceMillis(nextReadAt) >= ArenaPlayerInfo.REFRESH_MS) {
            nextReadAt = TimeUtils.millis();
            nextPlayer = readNextTurn(game, players);
        }
        // you are choosing attackers (your turn, or one you control): the prompt says which defenders it allows (the
        // engine's list, with costs and goad); with only one there is nothing to choose, so the board says nothing
        declaringAttack = MatchController.instance.getSelection().defenders().size() > 1;
        refreshCombat(game);
    }

    /**
     * Who takes the next turn: the engine's answer where the game runs here (it counts extra turns and a reversed turn
     * order), else the next player still in the game in seat order.
     */
    private static PlayerView readNextTurn(final GameView game, final FCollectionView<PlayerView> players) {
        try {
            final forge.game.Game local = game.getGame();
            if (local != null && local.getPhaseHandler() != null) {
                final forge.game.player.Player next = local.getPhaseHandler().getNextTurn();
                if (next != null) {
                    return next.getView();
                }
            }
        } catch (final RuntimeException e) {
            // the engine changed under us: the seat order below
        }
        final PlayerView turn = game.getPlayerTurn();
        final int n = players.size();
        int turnAt = -1;
        for (int i = 0; i < n; i++) {
            if (players.get(i) == turn) {
                turnAt = i;
            }
        }
        for (int k = 1; turnAt >= 0 && k < n; k++) {
            final PlayerView p = players.get((turnAt + k) % n);
            if (!p.getHasLost()) {
                return p;
            }
        }
        return null;
    }

    /** Whether this is a player at this screen (the one check every Arena view uses for "you"). */
    public static boolean isMe(final PlayerView p) {
        return p != null && MatchController.instance.isLocalPlayer(p);
    }

    /** Whether opponents sit side by side along the top (more than two players still in the game). */
    public static boolean isSeatGame() {
        refreshTable();
        return seatGame;
    }

    /** Who plays after the current turn (extra turns and turn order counted where the game runs here), or null. */
    public static PlayerView nextTurnPlayer() {
        refreshTable();
        return nextPlayer;
    }

    /** Whether the game waits on this player (they hold priority), not you: their seat says so at once. */
    public static boolean isWaitingOn(final PlayerView p) {
        final GameView game = MatchController.instance.getGameView();
        return p != null && game != null && !game.isGameOver() && !isMe(p) && !p.getHasLost() && p.getHasPriority();
    }

    /**
     * Whether you are choosing attackers now with more than one defender to choose from (the engine's list, sent with
     * the prompt): then every possible defender says whether and how it can be attacked, and a click sends the attack
     * there.
     */
    public static boolean isDeclaringAttack() {
        refreshTable();
        return declaringAttack;
    }

    /** While you declare attackers: what the engine allows for this player or permanent as the defender, or null. */
    public static Selection.DefenderOption attackOption(final GameEntityView entity) {
        if (entity == null || !isDeclaringAttack()) {
            return null;
        }
        final List<Selection.DefenderOption> options = MatchController.instance.getSelection().defenders();
        for (int i = 0, n = options.size(); i < n; i++) {
            final Selection.DefenderOption o = options.get(i);
            if (entity.equals(o.defender())) {
                return o;
            }
        }
        return null;
    }

    // ---- combat, read at most every ArenaPlayerInfo.REFRESH_MS ------------------------------------------------------
    private static long combatAt = Long.MIN_VALUE / 2;
    private static Object combatFor;
    private static final IdentityHashMap<CardView, CardView> BLOCKS = new IdentityHashMap<>();
    private static int myAttackers, myBlockers, attackCostTotal, incoming;
    private static boolean attackCostKnown;

    /**
     * Reads the combat (under its lock: the game thread changes it): which attacker each blocker blocks, how many of
     * yours attack or block, the generic mana your declared attacks will cost, and the damage coming at you unblocked.
     */
    private static void refreshCombat(final GameView game) {
        final CombatView combat = game == null ? null : game.getCombat();
        if (combat == combatFor && TimeUtils.timeSinceMillis(combatAt) < ArenaPlayerInfo.REFRESH_MS) {
            return;
        }
        combatFor = combat;
        combatAt = TimeUtils.millis();
        BLOCKS.clear();
        myAttackers = myBlockers = attackCostTotal = incoming = 0;
        attackCostKnown = true;
        if (combat == null) {
            return;
        }
        try {
            synchronized (combat) {
                final List<Selection.DefenderOption> options = MatchController.instance.getSelection().defenders();
                for (final CardView attacker : combat.getAttackers()) {
                    final GameEntityView defender = combat.getDefender(attacker);
                    final boolean mine = isMe(attacker.getController());
                    if (mine) {
                        myAttackers++;
                        final String cost = costFor(options, defender);
                        if (cost != null) {
                            final int generic = genericMana(cost);
                            if (generic < 0) {
                                attackCostKnown = false;
                            } else {
                                attackCostTotal += generic;
                            }
                        }
                    }
                    final FCollectionView<CardView> blockers = combat.getPlannedBlockers(attacker);
                    final boolean blocked = blockers != null && !blockers.isEmpty();
                    if (blocked) {
                        for (final CardView b : blockers) {
                            BLOCKS.put(b, attacker);
                            if (isMe(b.getController())) {
                                myBlockers++;
                            }
                        }
                    } else if (defender instanceof PlayerView p && isMe(p) && attacker.getCurrentState() != null) {
                        incoming += Math.max(0, attacker.getCurrentState().getPower());
                    }
                }
            }
        } catch (final RuntimeException e) {
            combatAt = Long.MIN_VALUE / 2; // changed under us: read again next frame
        }
    }

    private static String costFor(final List<Selection.DefenderOption> options, final GameEntityView defender) {
        for (int i = 0, n = options.size(); i < n; i++) {
            final Selection.DefenderOption o = options.get(i);
            if (o.defender().equals(defender)) {
                return o.state() == Selection.DefenderState.COSTS ? o.cost() : null;
            }
        }
        return null;
    }

    /** The generic mana in a cost like "{2}" (or "{1}{1}"), or -1 if it is anything else. */
    private static int genericMana(final String cost) {
        int total = 0, at = 0;
        while (at < cost.length()) {
            final int close = cost.indexOf('}', at);
            if (cost.charAt(at) != '{' || close < 0) {
                return -1;
            }
            try {
                total += Integer.parseInt(cost.substring(at + 1, close));
            } catch (final NumberFormatException e) {
                return -1;
            }
            at = close + 1;
        }
        return total;
    }

    /** Which attacker this card blocks, or null. */
    public static CardView blockedBy(final CardView blocker) {
        refreshTable();
        return BLOCKS.get(blocker);
    }

    /** How many of your creatures attack now, and how much generic mana that costs (-1: not just mana). */
    public static int myAttackerCount() {
        refreshTable();
        return myAttackers;
    }

    public static int myAttackCost() {
        refreshTable();
        return attackCostKnown ? attackCostTotal : -1;
    }

    public static int myBlockerCount() {
        refreshTable();
        return myBlockers;
    }

    /** Combat damage coming at you from unblocked attackers (0 outside combat). */
    public static int incomingDamage() {
        refreshTable();
        return incoming;
    }

    // ---- attack plates ------------------------------------------------------------------------------------------------
    private static final Map<Selection.DefenderOption, String> PLATE_TEXTS = new HashMap<>(), CURRENT_TEXTS = new HashMap<>();
    private static final Color CLOSED_RIM = ArenaTheme.rgb(0x6A655E), CLOSED_TEXT = ArenaTheme.rgb(0x9A958C), COST_RIM = ArenaTheme.rgb(0xFFB020);

    /**
     * What a possible defender says while you declare attackers, in one line, made once per state: {@link
     * #attackStateText}, then {@link #attackCostText} and {@link #attackGoadText} if any ("Click to attack · Pay {2}
     * each").
     */
    public static String attackPlateText(final Selection.DefenderOption o, final boolean current) {
        final Map<Selection.DefenderOption, String> cache = current ? CURRENT_TEXTS : PLATE_TEXTS;
        String s = cache.get(o);
        if (s == null) {
            if (cache.size() > 64) {
                cache.clear(); // options change each declaration: do not keep them all game
            }
            final Localizer l = Forge.getLocalizer();
            s = attackStateText(o, current);
            final String cost = attackCostText(o), goad = attackGoadText(o);
            if (cost != null) {
                s = l.getMessage("lblArenaJoin", s, cost);
            }
            if (goad != null) {
                s = l.getMessage("lblArenaJoin", s, goad);
            }
            cache.put(o, s);
        }
        return s;
    }

    private static String attackHere, clickToAttack, cantBeAttacked;

    /** "Your attack goes here" on the defender being filled, "Click to attack" on the others, "Can't be attacked". */
    public static String attackStateText(final Selection.DefenderOption o, final boolean current) {
        if (attackHere == null) {
            final Localizer l = Forge.getLocalizer();
            attackHere = l.getMessage("lblArenaAttackHere");
            clickToAttack = l.getMessage("lblArenaClickToAttack");
            cantBeAttacked = l.getMessage("lblArenaCantBeAttacked");
        }
        return o.state() == Selection.DefenderState.CLOSED ? cantBeAttacked : current ? attackHere : clickToAttack;
    }

    private static final Map<String, String> COST_TEXTS = new HashMap<>();
    private static final Map<Integer, String> GOAD_TEXTS = new HashMap<>();

    /** "Pay {2} each" when every creature that could attack them has to pay (Propaganda...), else null; made once per cost. */
    public static String attackCostText(final Selection.DefenderOption o) {
        if (o.state() != Selection.DefenderState.COSTS || o.cost() == null) {
            return null;
        }
        String s = COST_TEXTS.get(o.cost());
        if (s == null) {
            s = Forge.getLocalizer().getMessage("lblArenaAttackCosts", o.cost());
            COST_TEXTS.put(o.cost(), s);
        }
        return s;
    }

    /** "Goaded ×2: attack others" when they goaded creatures of yours that can attack (those must attack someone else). */
    public static String attackGoadText(final Selection.DefenderOption o) {
        if (o.goaded() <= 0) {
            return null;
        }
        String s = GOAD_TEXTS.get(o.goaded());
        if (s == null) {
            s = Forge.getLocalizer().getMessage("lblArenaGoadedMustAttackOthers", String.valueOf(o.goaded()));
            GOAD_TEXTS.put(o.goaded(), s);
        }
        return s;
    }

    /** The rim of a defender's plate: chosen, open, at a cost, or closed. */
    public static Color attackPlateRim(final Selection.DefenderOption o, final boolean current) {
        return current ? ArenaTheme.CHOSEN : o.state() == Selection.DefenderState.CLOSED ? CLOSED_RIM
                : o.state() == Selection.DefenderState.COSTS ? COST_RIM : ArenaTheme.SELECTABLE;
    }

    /** The text colour of a defender's plate (dim when it cannot be attacked). */
    public static Color attackPlateInk(final Selection.DefenderOption o) {
        return o.state() == Selection.DefenderState.CLOSED ? CLOSED_TEXT : ArenaTheme.WHITE;
    }

    /** Whether this player or permanent could be clicked now to send the attack there (open or at a cost). */
    public static boolean isAttackOption(final GameEntityView entity) {
        final Selection.DefenderOption o = attackOption(entity);
        return o != null && o.state() != Selection.DefenderState.CLOSED;
    }

    /** Forgets what one game made (texts keyed by its defenders and names): a new match starts clean. */
    public static void clearGameCaches() {
        PLATE_TEXTS.clear();
        CURRENT_TEXTS.clear();
        COST_TEXTS.clear();
        GOAD_TEXTS.clear();
        BLOCKS.clear();
        combatFor = null;
        tableGame = null;
        nextPlayer = null;
        ArenaPlayerInfo.clearGameCaches();
    }

    // ---- portrait --------------------------------------------------------------------------------------------------
    public static float portraitDiameter(final boolean seat) {
        return Math.round(windowH() * (seat ? PORTRAIT_SEAT : PORTRAIT));
    }

    /** The portrait's centre y in the window: low over the local field, hanging from the top for an opponent. */
    public static float portraitCenterY(final boolean local, final boolean seat) {
        final float d = portraitDiameter(seat);
        if (local) {
            return Math.round(windowH() * LOCAL_PORTRAIT_Y);
        }
        return seat ? Math.round(windowH() * 0.008f + d / 2f) : Math.round(d * 0.62f);
    }

    // ---- rows ------------------------------------------------------------------------------------------------------
    /** The front row's tile height before a crowded row shrinks it. */
    public static float frontTileHeight(final boolean local, final boolean seat) {
        return Math.round(fullFrontTileHeight(local, seat) * cardScale());
    }

    /** {@link #frontTileHeight} at full card size: the room the row has, which smaller tiles sit in the middle of. */
    public static float fullFrontTileHeight(final boolean local, final boolean seat) {
        final float share = seat ? TILE_H_SEAT : local ? TILE_H : TILE_H * FOE_FRONT;
        return Math.round(windowH() * share);
    }

    // ---- card size (the slider under the corner buttons) -----------------------------------------------------------
    /** The smallest the card size slider makes the cards, against their full size. */
    public static final float CARD_SCALE_MIN = 0.6f;
    private static float cardScale = -1f;
    private static int cardScaleSaved = -1;

    /** How large the cards are (battlefield tiles and the hand), from {@link #CARD_SCALE_MIN} to 1; kept in the preferences. */
    public static float cardScale() {
        if (cardScale < 0f) {
            int pct = 100;
            try {
                pct = Integer.parseInt(forge.model.FModel.getPreferences().getPref(forge.localinstance.properties.ForgePreferences.FPref.UI_ARENA_CARD_SIZE));
            } catch (final RuntimeException e) {
                // not a number, or no preferences yet: full size
            }
            cardScale = clampScale(pct / 100f);
            cardScaleSaved = Math.round(cardScale * 100f);
        }
        return cardScale;
    }

    private static float clampScale(final float s) {
        return Math.max(CARD_SCALE_MIN, Math.min(1f, Math.round(s * 100f) / 100f));
    }

    /** Sets the card size (whole percents); returns whether it changed. {@code save} writes it to the preferences now. */
    public static boolean setCardScale(final float s, final boolean save) {
        final float v = clampScale(s);
        final boolean changed = v != cardScale();
        cardScale = v;
        if (save) {
            saveCardScale();
        }
        return changed;
    }

    /** Writes the card size to the preferences, if it changed since last written. */
    public static void saveCardScale() {
        final int pct = Math.round(cardScale() * 100f);
        if (pct != cardScaleSaved) {
            cardScaleSaved = pct;
            forge.model.FModel.getPreferences().setPref(forge.localinstance.properties.ForgePreferences.FPref.UI_ARENA_CARD_SIZE, String.valueOf(pct));
            forge.model.FModel.getPreferences().save();
        }
    }

    /** The back row's tile height for a front row this tall. */
    public static float backTileHeight(final boolean local, final boolean seat, final float frontHeight) {
        final float share = seat ? SEAT_BACK : local ? OWN_BACK : FOE_BACK / FOE_FRONT;
        return Math.max(minTileHeight(), Math.round(frontHeight * share));
    }

    public static float minTileHeight() {
        return Math.round(windowH() * TILE_H_MIN * cardScale());
    }

    /** The smallest a back row's lands get before they start to overlap. */
    public static float smallestBackHeight() {
        return Math.round(windowH() * TILE_H * FOE_BACK * cardScale());
    }

    public static float frontRowTop(final boolean local, final boolean seat) {
        return Math.round(windowH() * (local ? LOCAL_FRONT_Y : seat ? SEAT_FRONT_Y : FOE_FRONT_Y));
    }

    public static float backRowTop(final boolean local, final boolean seat) {
        return Math.round(windowH() * (local ? LOCAL_BACK_Y : seat ? SEAT_BACK_Y : FOE_BACK_Y));
    }

    /** Half the gap the back row leaves round the portrait. */
    public static float portraitClear(final boolean seat) {
        return Math.round(windowH() * (seat ? CLEAR_SEAT : CLEAR));
    }

    private static final Rectangle DOCK = new Rectangle();

    /**
     * How far right your rows may run: short of the stack's dock while it is open over the board (in a game of seats,
     * where it sits low enough to cover them), else no limit.
     */
    public static float localRowRight() {
        final MatchScreen view = MatchController.getView();
        if (view == null || !isSeatGame() || !view.getStack().isVisible()) {
            return Float.MAX_VALUE;
        }
        ArenaMatchLayout.stackDock(windowW(), windowH(), DOCK);
        return Math.min(DOCK.x, view.getStack().getLeft()) - Math.round(windowW() * 0.01f);
    }

    /** The side margin of the rows. */
    public static float rowMargin(final boolean seat) {
        return Math.round(windowW() * (seat ? 0.016f : 0.024f));
    }

    /** How far right an opponent's back row may run in a field {@code fieldW} wide (short of their piles), or yours. */
    public static float backRowRight(final boolean local, final boolean seat, final float fieldW) {
        if (seat) {
            return fieldW - rowMargin(true) - Math.round(fieldW * 0.02f);
        }
        return local ? fieldW - rowMargin(false) : fieldW - Math.round(fieldW * 0.172f);
    }

    // ---- piles, names, extras --------------------------------------------------------------------------------------
    /** Where the library, graveyard and other piles go, in the window: bottom left for you, top right for an opponent. */
    public static Rectangle piles(final boolean local, final boolean seat, final float fieldX, final float fieldW, final Rectangle out) {
        final float w = windowW(), h = windowH();
        if (local) {
            return out.set(Math.round(h * 0.012f), Math.round(h * 0.822f), Math.round(w * 0.16f), Math.round(h * 0.135f));
        }
        if (seat) {
            final float d = portraitDiameter(true);
            final float zonesX = fieldX + fieldW / 2 + Math.round(d * 0.7f);
            final float reserve = fieldX + fieldW >= w - 4 ? Math.max(Math.round(h * 0.075f), Math.round(w * 0.06f)) : 14;
            return out.set(zonesX, Math.round(h * 0.008f), Math.max(0, fieldX + fieldW - zonesX - reserve), Math.round(h * 0.076f));
        }
        final float zonesH = Math.round(h * 0.118f), zonesW = Math.round(w * 0.15f);
        final float right = fieldX + fieldW - Math.max(Math.round(h * 0.075f), Math.round(fieldW * 0.06f));
        return out.set(right - zonesW, Math.round(h * 0.006f), zonesW, zonesH);
    }

    /** An opponent's hand: a small fan of card backs beside the portrait. */
    public static Rectangle opponentHand(final boolean seat, final float fieldX, final float fieldW, final Rectangle out) {
        final float w = windowW(), h = windowH(), d = portraitDiameter(seat), cx = fieldX + fieldW / 2;
        if (seat) {
            final float fanW = Math.round(h * 0.1f);
            return out.set(cx - Math.round(d * 0.7f) - fanW, Math.round(h * -0.012f), fanW, Math.round(h * 0.085f));
        }
        final float fanW = Math.round(h * 0.26f);
        return out.set(cx - Math.round(d * 1.6f) - Math.round(w * 0.015f) - fanW, Math.round(h * -0.045f), fanW, Math.round(h * 0.105f));
    }

    /** The height of a name plate's name (a seat's plate is taller: {@link #SEAT_NAME_ROWS} of these, chips under the name). */
    public static float nameHeight() {
        return Math.round(windowH() * 0.03f);
    }

    /** A seat's name plate holds the name and two rows of chips under it, down to just above the seat's back row. */
    public static final float SEAT_NAME_ROWS = 3.15f;

    /**
     * The hand of a player in the bottom seat who is not you (you are out and watch the rest): a small fan of their
     * cards left of the portrait, above the window's bottom edge, where your own fan would run.
     */
    public static Rectangle watchedHand(final float portraitCx, final Rectangle out) {
        final float h = windowH(), fanW = Math.round(h * 0.26f), fanH = Math.round(h * 0.105f);
        return out.set(portraitCx - Math.round(portraitDiameter(false) * 1.9f) - fanW, h - fanH - Math.round(h * 0.012f), fanW, fanH);
    }

    /** The player's name plate in the corner. */
    public static Rectangle name(final boolean local, final boolean seat, final float fieldX, final float fieldW, final Rectangle out) {
        final float w = windowW(), h = windowH(), nameH = nameHeight();
        if (local) {
            return out.set(Math.round(h * 0.014f), Math.round(h * 0.962f), Math.round(w * 0.16f), nameH);
        }
        if (seat) {
            final float d = portraitDiameter(true);
            return out.set(fieldX + Math.round(w * 0.0075f) + 10, 8, Math.max(40, fieldW / 2 - Math.round(d * 0.7f) - Math.round(h * 0.1f) - 16),
                    Math.round(nameH * SEAT_NAME_ROWS));
        }
        return out.set(fieldX + Math.round(h * 0.014f), Math.round(h * 0.008f), Math.round(w * 0.2f), nameH);
    }

    /** The local hand's strip (the cards sink below the window's bottom edge). */
    public static Rectangle hand(final Rectangle out) {
        final float w = windowW(), h = windowH();
        final float top = Math.round(h * HAND_TOP);
        return out.set(Math.round(w * HAND_LEFT), top, Math.round(w * (HAND_RIGHT - HAND_LEFT)), h - top);
    }

    /** Where what floats beside the portrait goes (mana in the pool, commander damage): its left end and middle. */
    public static float extrasLeft(final boolean seat, final float portraitCx) {
        return portraitCx + Math.round(portraitDiameter(seat) * 1.7f);
    }
}
