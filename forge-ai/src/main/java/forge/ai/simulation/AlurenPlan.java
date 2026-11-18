package forge.ai.simulation;

import java.util.List;
import java.util.function.Predicate;

import forge.ai.ComputerUtilMana;
import forge.ai.NamedDiscard;
import forge.ai.RunawayLoop;
import forge.game.cost.CostSacrifice;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Aluren deck (Mattias Jorstedt's, Pro Tour Houston 2002): with Aluren in play every creature of three or less is
 * free, and the deck wins by drawing its library and then the opponent's. Cavern Harpy's enter trigger returns a blue or
 * black creature, "pay 1 life: return Cavern Harpy to its hand" returns the Harpy, so a round is: cast the creature, cast
 * the Harpy (it sends the creature back), pay a life, again. Soul Warden pays the life of every round back.
 *
 * <ul>
 *   <li><b>Cards</b>: Raven Familiar in the round digs three cards deep each time, until Stroke of Genius, Cloud of
 *       Faeries and the life for the rounds are in hand ({@link #picks} says which card of the three).</li>
 *   <li><b>Mana</b>: Cloud of Faeries untaps two lands each time it is cast. Every land goes into the mana pool, each
 *       round adds two, and when the pool covers the opponent's library Stroke of Genius for that much draws all of it.</li>
 *   <li><b>Drain</b> (sideboard): Maggot Carrier in the round, each cast costs both players a life, with the Warden
 *       paying ours back.</li>
 * </ul>
 * <ul>
 *   <li><b>Acererak</b> (Aluren, Acererak the Archlich): it costs three, so Aluren makes it free, and it returns itself to
 *       the hand and ventures each time. No Harpy is needed; the venture goes through Lost Mine of Phandelver, whose
 *       Dark Pool takes a life from each opponent every fourth cast ({@link #preferredRoom}, {@link #dungeon}).</li>
 *   <li><b>Setup first</b>: Living Wish fetches the piece that is missing ({@link #wanted}): the untapper, the digger,
 *       the life gain, or the drain, before the loops start.</li>
 * </ul>
 * {@link #harpyBounce} chooses the creature the Harpy returns for the loop that is running. The caps on the spells of a
 * turn and the library floor keep it from running away. Untested against a real game.
 */
public final class AlurenPlan {
    private static final String ALUREN = "Aluren";
    private static final String HARPY = "Cavern Harpy";
    private static final String SAVAGE = "Wirewood Savage";
    private static final String FAMILIAR = "Raven Familiar";
    private static final String WARDEN = "Soul Warden";
    private static final String CARRIER = "Maggot Carrier";
    private static final String CLOUD = "Cloud of Faeries";
    private static final String STROKE = "Stroke of Genius";
    private static final String WISH = "Living Wish";
    private static final String ACERERAK = "Acererak the Archlich";
    private static final String INTUITION = "Intuition";
    private static final String BIRDS = "Birds of Paradise";
    private static final String WOODLOT = "Hickory Woodlot";
    private static final String THERAPY = "Cabal Therapy";
    private static final String BRAINSTORM = "Brainstorm";
    private static final int SAFE_LIFE = 20;
    private static final String[] KEYS = { ALUREN, HARPY, SAVAGE, FAMILIAR,
            WARDEN, CARRIER, CLOUD, STROKE, ACERERAK, WISH,
            BIRDS, WOODLOT, THERAPY, BRAINSTORM };

    /** The most spells of one turn this plan casts, and the cards that stay in the library out of reach of the draws. */
    private static final int SPELL_CAP = 150;
    private static final int RETURN_CAP = 80;
    private static final int LIBRARY_FLOOR = 3;
    /** The life kept in hand as a buffer on top of what a loop pays, when no Soul Warden pays it back. */
    private static final int LIFE_BUFFER = 4;

    private AlurenPlan() {
    }

    public static boolean isAlurenDeck(final Player ai) {
        boolean aluren = false;
        boolean harpy = false;
        boolean acererak = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Battlefield,
                ZoneType.Sideboard)) {
            aluren |= c.getName().equals(ALUREN);
            harpy |= c.getName().equals(HARPY);
            acererak |= c.getName().equals(ACERERAK);
        }
        return aluren && (harpy || acererak);
    }

    /** What the loop is working on: the cards to draw, the mana for Stroke of Genius, or the drain of Maggot Carrier. */
    public enum Stage { NONE, CARDS, MANA, DRAIN, LIFE, ACERERAK }

    private static boolean accessible(final Player ai, final String name) {
        return LoopRunner.inHand(ai, name) || LoopRunner.inPlay(ai, name) != null;
    }

    private static int piecesInHand(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            final String name = c.getName();
            if (name.equals(HARPY) || name.equals(SAVAGE) || name.equals(FAMILIAR) || name.equals(WARDEN)
                    || name.equals(CARRIER) || name.equals(CLOUD)) {
                n++;
            }
        }
        return n;
    }

    /** Stroke of Genius is in the hand or still in the library: the only two places the mana kill can get it from. */
    private static boolean strokeReachable(final Player ai) {
        return LoopRunner.inHand(ai, STROKE) || inLibrary(ai, STROKE);
    }

    private static boolean hasWish(final Player ai) {
        return LoopRunner.inHand(ai, WISH) || inLibrary(ai, WISH);
    }

    private static boolean inSideboard(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Sideboard)) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** The primary kill can be reached from the hand, library, or sideboard through Living Wish. */
    private static boolean drainRouteAlive(final Player ai) {
        return accessible(ai, CARRIER) || inLibrary(ai, CARRIER)
                || inSideboard(ai, CARRIER) && hasWish(ai);
    }

    private static boolean wardenRouteAlive(final Player ai) {
        return accessible(ai, WARDEN) || inLibrary(ai, WARDEN)
                || inSideboard(ai, WARDEN) && hasWish(ai);
    }

    private static boolean cloudRouteAlive(final Player ai) {
        return accessible(ai, CLOUD) || inLibrary(ai, CLOUD)
                || inSideboard(ai, CLOUD) && hasWish(ai);
    }

    private static boolean hasFinishRoute(final Player ai) {
        return drainRouteAlive(ai) || manaKillAlive(ai);
    }

    /**
     * Without Soul Warden, the Carrier and Harpy rounds cost approximately
     * two life for every one life removed from the opponent.
     */
    private static boolean drainLifeSafe(final Player ai, final Player opp) {
        return LoopRunner.inPlay(ai, WARDEN) != null
                || opp != null && ai.getLife() > 2 * opp.getLife() + LIFE_BUFFER;
    }

    private static boolean needsWarden(final Player ai, final Player opp) {
        if (LoopRunner.inPlay(ai, WARDEN) != null) {
            return false;
        }
        if (drainRouteAlive(ai) && !drainLifeSafe(ai, opp)) {
            return true;
        }
        return manaKillAlive(ai) && !lifeOk(ai, opp);
    }

    private static boolean needsSideboardWish(final Player ai, final Player opp) {
        return !accessible(ai, HARPY) && !accessible(ai, ACERERAK)
                    && inSideboard(ai, HARPY)
                || needsWarden(ai, opp) && inSideboard(ai, WARDEN)
                || drainRouteAlive(ai) && !accessible(ai, CARRIER)
                    && inSideboard(ai, CARRIER)
                || strokeReachable(ai) && !accessible(ai, CLOUD)
                    && inSideboard(ai, CLOUD)
                || hasFinishRoute(ai) && !accessible(ai, FAMILIAR)
                    && !accessible(ai, SAVAGE) && inSideboard(ai, FAMILIAR);
    }

    /**
     * The scan of the deck before the loops: whether the mana kill can still happen at all. There is one Stroke of Genius
     * and two Clouds of Faeries; once the Stroke is in the graveyard (Intuition puts two cards there, the cleanup step
     * discards, Brainstorm) or the Clouds are gone, no amount of drawing makes the kill.
     */
    public static boolean manaKillAlive(final Player ai) {
        return strokeReachable(ai) && cloudRouteAlive(ai);
    }

    /** Life enough for the rounds of the loop: a Soul Warden pays the life back, otherwise a round costs one. */
    private static boolean lifeOk(final Player ai, final Player opp) {
        if (LoopRunner.inPlay(ai, WARDEN) != null) {
            return true;
        }
        // a round costs a life and adds two mana: the lands and the pool already cover part of the library
        final int missing = Math.max(0, opp.getCardsIn(ZoneType.Library).size() + 3 - lands(ai)
                - ai.getManaPool().totalMana());
        return ai.getLife() >= (missing + 1) / 2 + 2;
    }

    /** Whether a real win condition still needs a card from the library. */
    private static boolean needsCards(final Player ai, final Player opp) {
        if (drainRouteAlive(ai)) {
            if (!accessible(ai, CARRIER)) {
                return true;
            }
            return needsWarden(ai, opp) && wardenRouteAlive(ai);
        }
        if (!manaKillAlive(ai)) {
            return false;
        }
        return !LoopRunner.inHand(ai, STROKE) || !accessible(ai, CLOUD)
                || needsWarden(ai, opp) && wardenRouteAlive(ai);
    }

    /** How many cards the next Harpy round will actually draw. */
    private static int drawsPerRound(final Player ai) {
        int savageCount = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(SAVAGE)) {
                savageCount++;
            }
        }
        // The first Savage is cast before the Harpy round starts.
        if (savageCount == 0 && LoopRunner.inHand(ai, SAVAGE)) {
            savageCount = 1;
        }
        // Raven Familiar enters once each round and takes one of the top three.
        return savageCount + (accessible(ai, FAMILIAR) ? 1 : 0);
    }

    /**
     * Retain the existing three-card safety floor plus a two-card reserve.
     * Multiple Savages increase the reserve because all their triggers fire
     * from the same Harpy entry.
     */
    private static boolean librarySafeForNextRound(final Player ai) {
        final int reserve = Math.max(2, drawsPerRound(ai));
        return ai.getCardsIn(ZoneType.Library).size() > LIBRARY_FLOOR + reserve;
    }

    private static boolean canStartCombo(final Player ai) {
        return accessible(ai, ACERERAK)
                || accessible(ai, HARPY) && hasFinishRoute(ai);
    }

    /**
     * Choose the actual win condition before optional life gain:
     * Maggot Carrier first, Stroke of Genius as the backup, and bounded
     * Soul Warden life gain only when neither engine can currently progress.
     */
    public static Stage stage(final Player ai, final Player opp) {
        if (opp == null || ai.getLife() < 3) {
            return Stage.NONE;
        }
        if (!accessible(ai, HARPY)) {
            // no Harpy: Acererak is its own loop, and it is the only one
            return accessible(ai, ACERERAK) && opp.canLoseLife() && ai.getLife() > LIFE_BUFFER ? Stage.ACERERAK : Stage.NONE;
        }

        // Primary kill: repeatedly enter Maggot Carrier. Soul Warden makes
        // the life payment sustainable; otherwise require a large life lead.
        if (accessible(ai, CARRIER) && opp.canLoseLife() && drainLifeSafe(ai, opp)) {
            return Stage.DRAIN;
        }

        // A Carrier in the hand drains first (above); otherwise the kill that is in the hand wins before any digging: Stroke of Genius with the Cloud of Faeries is ready, so nothing is
        // dug for, and no longer round of the Carrier is started (the screenshot of PR 12937: the loop kept digging for
        // the Carrier while Stroke and Cloud were already there).
        final boolean canDeckThem = opp.canDraw() && !opp.cantLoseCheck(forge.game.player.GameLossReason.Milled)
                && !opp.getCardsIn(ZoneType.Library).isEmpty();
        if (canDeckThem && LoopRunner.inHand(ai, STROKE) && accessible(ai, CLOUD) && lifeOk(ai, opp)
                && lands(ai) >= 2) {
            return Stage.MANA;
        }

        // Find the primary kill before committing to the Stroke backup.
        final boolean drawEngine = accessible(ai, FAMILIAR) || accessible(ai, SAVAGE);
        if (needsCards(ai, opp) && drawEngine && librarySafeForNextRound(ai)) {
            return Stage.CARDS;
        }

        // Optional life gain is finite and only useful when a real finish
        // condition remains in the deck. It must never become the win plan.
        if (LoopRunner.inPlay(ai, WARDEN) != null && ai.getLife() < SAFE_LIFE
                && hasFinishRoute(ai)) {
            return Stage.LIFE;
        }
        return accessible(ai, ACERERAK) && opp.canLoseLife() && ai.getLife() > LIFE_BUFFER ? Stage.ACERERAK : Stage.NONE;
    }

    /**
     * The loop is drawing cards for the kill: the hand may grow past seven (the turn ends with the win, not with the
     * discard), so the draws of Wirewood Savage and the like are taken. In the video of PR 12937 the AI held eight cards and
     * refused every "you may draw", the loop ran and drew nothing.
     */
    public static boolean drawLoop(final Player ai) {
        return ai != null && isAlurenDeck(ai) && LoopRunner.inPlay(ai, ALUREN) != null
                && stage(ai, ai.getWeakestOpponent()) == Stage.CARDS;
    }

    /**
     * The decks that dig for their kill (High Tide, storm, Ad Nauseam and the other combo decks, the Aluren loop): more
     * than seven cards in the hand is no reason to refuse a draw, they win the turn they draw into it. The library has to
     * stay deep enough, so a deck is never drawn out by this.
     */
    public static boolean digging(final Player ai) {
        if (ai == null) {
            return false;
        }

        if (isAlurenDeck(ai) && (accessible(ai, HARPY) || inLibrary(ai, HARPY))) {
            final Player opp = ai.getWeakestOpponent();
            if (LoopRunner.inPlay(ai, ALUREN) != null) {
                return drawLoop(ai) || LoopRunner.inHand(ai, BRAINSTORM)
                        && needsCards(ai, opp)
                        && ai.getCardsIn(ZoneType.Library).size() > LIBRARY_FLOOR + 3;
            }
            return LoopRunner.inHand(ai, BRAINSTORM)
                    && hasFinishRoute(ai)
                    && (!LoopRunner.inHand(ai, ALUREN) || !canStartCombo(ai))
                    && ai.getCardsIn(ZoneType.Library).size() > LIBRARY_FLOOR + 3;
        }

        if (ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_FLOOR + 2) {
            return false;
        }
        if (TidePlan.isTideDeck(ai)) {
            return true;
        }
        return ai.getController() instanceof forge.ai.PlayerControllerAi
                && ((forge.ai.PlayerControllerAi) ai.getController()).getAi().deckPlan(ai).archetype
                == DeckPlan.Archetype.COMBO;
    }

    private static boolean inSideboardHas(final Player ai, final String name) {
        return inSideboard(ai, name);
    }

    private static boolean inLibrary(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsWish(final Player ai, final Player opp) {
        return (!accessible(ai, HARPY) && !accessible(ai, ACERERAK) && inSideboardHas(ai, HARPY))
                || needsWarden(ai, opp) && inSideboardHas(ai, WARDEN)
                || drainRouteAlive(ai) && !accessible(ai, CARRIER) && inSideboardHas(ai, CARRIER)
                || strokeReachable(ai) && !accessible(ai, CLOUD) && inSideboardHas(ai, CLOUD)
                || hasFinishRoute(ai) && !accessible(ai, FAMILIAR)
                    && !accessible(ai, SAVAGE) && inSideboardHas(ai, FAMILIAR);
    }

    /**
     * Find the missing piece of a real kill. The Carrier route is preferred;
     * the Stroke route is selected only when the primary route is unavailable.
     */
    public static String wanted(final Player ai, final Iterable<Card> pool) {
        if (ai == null || pool == null) {
            return null;
        }
        final Player opp = ai.getWeakestOpponent();
        final String[] order = { ALUREN, HARPY, WARDEN, CARRIER, WISH, STROKE, CLOUD, FAMILIAR, SAVAGE };
        for (final String name : order) {
            final boolean missing;
            switch (name) {
                case ALUREN:
                    missing = !accessible(ai, ALUREN);
                    break;
                case HARPY:
                    missing = !accessible(ai, HARPY) && !accessible(ai, ACERERAK);
                    break;
                case WARDEN:
                    missing = needsWarden(ai, opp) && !accessible(ai, WARDEN);
                    break;
                case CARRIER:
                    missing = drainRouteAlive(ai) && !accessible(ai, CARRIER);
                    break;
                case WISH:
                    missing = !LoopRunner.inHand(ai, WISH) && needsWish(ai, opp);
                    break;
                case STROKE:
                    missing = !LoopRunner.inHand(ai, STROKE) && inLibrary(ai, STROKE);
                    break;
                case CLOUD:
                    missing = strokeReachable(ai) && !accessible(ai, CLOUD);
                    break;
                case FAMILIAR:
                    missing = !accessible(ai, FAMILIAR) && !accessible(ai, SAVAGE)
                            && hasFinishRoute(ai);
                    break;
                case SAVAGE:
                    missing = !accessible(ai, SAVAGE) && !accessible(ai, FAMILIAR)
                            && hasFinishRoute(ai);
                    break;
                default:
                    missing = false;
            }
            if (!missing) {
                continue;
            }
            for (final Card c : pool) {
                if (c.getName().equals(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * What the hand may lose at the cleanup step: one copy of every piece of the kill stays. In the screenshot of the run
     * that stopped the AI held three Intuitions and three Living Wishes and no Stroke of Genius or Cloud of Faeries, with
     * 26 cards in the graveyard: the draws past seven had been discarded again at the end of the turns.
     */
    public static CardCollection withoutKeys(final Player ai, final CardCollection hand) {
        if (ai == null || !isAlurenDeck(ai)) {
            return hand;
        }
        final CardCollection rest = new CardCollection(hand);
        for (final String name : KEYS) {
            for (final Card c : hand) {
                if (c.getName().equals(name)) {
                    rest.remove(c);
                    break;
                }
            }
        }
        return rest;
    }

    /**
     * The cast of a card of the hand: Aluren's free cast first (it is not always in the list of the AI), then the cast
     * for its mana cost. In the screenshot of PR 12937 three Living Wishes and three Intuitions stayed in the hand while
     * the loop ran out of things to do: both were taken from the list as "the spell of that name" and, when that one was
     * refused, nothing else was tried.
     */
    private static SpellAbility castAny(final Player ai, final List<SpellAbility> candidates, final String name,
                                        final Predicate<SpellAbility> approve) {
        if (LoopRunner.inPlay(ai, ALUREN) != null) {
            final SpellAbility free = freeCast(ai, candidates, name);
            if (tryCast(ai, free, approve)) {
                return free;
            }
        }
        final SpellAbility paid = LoopRunner.castFromHand(ai, candidates, name);
        return tryCast(ai, paid, approve) ? paid : null;
    }

    /** Intuition for the piece that is missing (the tutor takes three: the wanted one first, see picks), mana from the lands. */
    private static SpellAbility intuition(final Player ai, final List<SpellAbility> candidates,
                                          final Predicate<SpellAbility> approve) {
        if (!LoopRunner.inHand(ai, INTUITION) || wanted(ai, ai.getCardsIn(ZoneType.Library)) == null) {
            return null;
        }
        return castAny(ai, candidates, INTUITION, approve);
    }

    /** Living Wish before anything else: it fetches what the loops need, while the lands are still untapped. */
    private static SpellAbility livingWish(final Player ai, final List<SpellAbility> candidates,
                                           final Predicate<SpellAbility> approve) {
        if (!LoopRunner.inHand(ai, WISH) || wanted(ai, ai.getCardsIn(ZoneType.Sideboard)) == null) {
            return null;
        }
        return castAny(ai, candidates, WISH, approve);
    }

    /** The dungeon of the Acererak loop: Lost Mine of Phandelver, whose Dark Pool drains. Null: no opinion. */
    public static String dungeon(final Player ai) {
        return ai != null && isAlurenDeck(ai) && LoopRunner.inPlay(ai, ALUREN) != null && accessible(ai, ACERERAK)
                ? "Lost Mine of Phandelver" : null;
    }

    /** The room of Lost Mine to go to next: the Dark Pool, and the rooms that lead to it. Null: no opinion. */
    public static SpellAbility preferredRoom(final Player ai, final List<SpellAbility> rooms) {
        if (dungeon(ai) == null) {
            return null;
        }
        for (final String name : new String[] { "Dark Pool", "Goblin Lair", "Mine Tunnels" }) {
            for (final SpellAbility room : rooms) {
                if (name.equals(room.getParamOrDefault("RoomName", ""))) {
                    return room;
                }
            }
        }
        return null;
    }

    private static int lands(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand()) {
                n++;
            }
        }
        return n;
    }

    /**
     * The creature that Cavern Harpy's enter trigger returns to the hand: the one the running loop casts again (Cloud
     * of Faeries untaps two lands each time, Raven Familiar looks at three cards, Maggot Carrier drains), or the Harpy
     * itself when nothing else is wanted back. Null for no opinion.
     */
    public static Card harpyBounce(final Player ai, final Iterable<Card> choices) {
        if (ai == null || !isAlurenDeck(ai) || LoopRunner.inPlay(ai, ALUREN) == null) {
            return null;
        }
        final Stage stage = stage(ai, ai.getWeakestOpponent());
        final String wanted = stage == Stage.MANA ? CLOUD : stage == Stage.CARDS ? FAMILIAR : stage == Stage.DRAIN ? CARRIER : null;
        Card harpy = null;
        for (final Card c : choices) {
            if (wanted != null && c.getName().equals(wanted)) {
                return c;
            }
            if (c.getName().equals(HARPY)) {
                harpy = c;
            }
        }
        // LIFE is the only stage allowed to recycle Harpy into itself,
        // and it stops at SAFE_LIFE. A stopped plan must not loop endlessly.
        return (stage == Stage.LIFE || (stage == Stage.CARDS && LoopRunner.inPlay(ai, SAVAGE) != null)) ? harpy : null;
    }

    /**
     * What to take from the library, the hand-piece that is missing for the kill first: Aluren, Cavern Harpy, Stroke of
     * Genius, Soul Warden, Cloud of Faeries, Raven Familiar, and Maggot Carrier as the other way to win.
     */
    public static CardCollection picks(final Player ai, final Iterable<Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0 || !isAlurenDeck(ai)) {
            return null;
        }
        final String[] order = { ALUREN, HARPY, WARDEN, CARRIER, WISH,
                STROKE, CLOUD, FAMILIAR, SAVAGE };
        final CardCollection picks = new CardCollection();
        final String first = LoopRunner.inPlay(ai, ALUREN) != null || LoopRunner.inHand(ai, ALUREN) ? wanted(ai, pool) : null;
        if (first != null) {
            for (final Card c : pool) {
                if (c.getName().equals(first)) {
                    picks.add(c);
                    break;
                }
            }
        }
        for (final String name : order) {
            if (picks.size() >= n) {
                break;
            }
            final boolean have = name.equals(ALUREN) ? LoopRunner.inPlay(ai, name) != null || LoopRunner.inHand(ai, name)
                    : accessible(ai, name);
            if (have || picks.size() > 0 && picks.getFirst().getName().equals(name)) {
                continue;
            }
            for (final Card c : pool) {
                if (c.getName().equals(name)) {
                    picks.add(c);
                    break;
                }
            }
        }
        for (final Card c : pool) { // three for Intuition: what is left is no loss
            if (picks.size() >= n) {
                break;
            }
            if (!picks.contains(c)) {
                picks.add(c);
            }
        }
        return picks.size() == n ? picks : null;
    }

    /** The free cast of the creature (the one Aluren grants, not the one that pays its mana cost), or null. */
    private static SpellAbility freeCast(final Player ai, final List<SpellAbility> candidates, final String name) {
        SpellAbility fallback = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isSpell() || sa.isLandAbility() || !host.isInZone(ZoneType.Hand)
                    || !ai.equals(host.getController()) || !host.getName().equals(name)) {
                continue;
            }
            if (sa.getMayPlay() != null) {
                return sa;
            }
            if (sa.getPayCosts() != null && sa.getPayCosts().getTotalMana() != null
                    && sa.getPayCosts().getTotalMana().getCMC() == 0) {
                fallback = sa;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        // the list of the AI left it out (its own check of the card said no): ask the card itself what Aluren lets it do
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                for (final SpellAbility sa : c.getAllPossibleAbilities(ai, true)) {
                    if (sa.isSpell() && !sa.isLandAbility() && sa.getMayPlay() != null) {
                        VetoLog.note(ai, "aluren-free-cast-from-card " + name);
                        return sa;
                    }
                }
            }
        }
        VetoLog.note(ai, "aluren-no-free-cast " + name);
        return null;
    }

    private static boolean tryCast(final Player ai, final SpellAbility sa, final Predicate<SpellAbility> approve) {
        if (sa == null) {
            return false;
        }
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        if (approve.test(sa)) {
            return true;
        }
        VetoLog.note(ai, "aluren-step-refused " + sa.getHostCard().getName());
        return false;
    }

    /**
     * Cavern Harpy on its own is a loop that does nothing: it returns itself and nothing else enters or leaves (the
     * recording of PR 12937: the Harpy over and over, Raven Familiar in the hand). It waits until a creature that does
     * something is in play for it to send back, or Wirewood Savage draws for it.
     */
    public static boolean pointlessHarpy(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !host.getName().equals(HARPY) || !ai.equals(host.getController()) || !isAlurenDeck(ai)) {
            return false;
        }
        final boolean returnAbility = host.isInZone(ZoneType.Battlefield)
                && sa.isActivatedAbility() && !sa.isManaAbility();
        final boolean handCast = host.isInZone(ZoneType.Hand) && sa.isSpell();
        if (!returnAbility && !handCast) {
            return false;
        }
        if (LoopRunner.inPlay(ai, ALUREN) == null) {
            return !(handCast && paidSavageDraw(ai)); // the Savage draws for the Harpy that is cast for its mana
        }

        final Stage current = stage(ai, ai.getWeakestOpponent());
        if (returnAbility) {
            // The activated ability is only for the card/mana/drain engines.
            // The bounded life loop returns Harpy through its own ETB trigger.
            return current != Stage.CARDS && current != Stage.MANA && current != Stage.DRAIN;
        }

        switch (current) {
            case LIFE:
                return false;
            case MANA:
                return LoopRunner.inPlay(ai, CLOUD) == null;
            case CARDS:
                return LoopRunner.inPlay(ai, SAVAGE) == null
                        && LoopRunner.inPlay(ai, FAMILIAR) == null;
            case DRAIN:
                return LoopRunner.inPlay(ai, CARRIER) == null;
            default:
                return true;
        }
    }

    private static String lastState;

    /** One line of the veto log whenever the state of the plan changes: why it is doing what it does, or nothing. */
    private static void explain(final Player ai, final Player opp, final String step) {
        final StringBuilder sb = new StringBuilder("aluren-state stage=").append(stage(ai, opp)).append(" step=").append(step)
                .append(" stroke=").append(LoopRunner.inHand(ai, STROKE) ? "hand" : inLibrary(ai, STROKE) ? "library" : "GONE")
                .append(" cloud=").append(accessible(ai, CLOUD) ? "ready" : inLibrary(ai, CLOUD) ? "library" : "GONE")
                .append(" manaKill=").append(manaKillAlive(ai)).append(" life=").append(ai.getLife())
                .append(" library=").append(ai.getCardsIn(ZoneType.Library).size())
                .append(" pool=").append(ai.getManaPool().totalMana()).append(" oppLibrary=")
                .append(opp.getCardsIn(ZoneType.Library).size());
        final String line = sb.toString();
        if (!line.equals(lastState)) {
            lastState = line;
            VetoLog.note(ai, line);
        }
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Player opp,
                             final Predicate<SpellAbility> approve) {
        if (opp == null || !isAlurenDeck(ai)) {
            return null;
        }
        final SpellAbility step = step(ai, candidates, opp, approve);
        explain(ai, opp, step == null ? "none" : step.getHostCard().getName());
        return step;
    }

    private static SpellAbility step(final Player ai, final List<SpellAbility> candidates, final Player opp,
                                     final Predicate<SpellAbility> approve) {
        if (lethalByAttack(ai, opp)) {
            return null; // the loop would send the attackers back to the hand, sick: they attack first
        }
        // Ramp first, but do not delay Aluren when it is already payable with
        // a real combo route assembled.
        final SpellAbility ramp = setupRamp(ai, candidates, approve);
        if (ramp != null) {
            return ramp;
        }

        // Therapy uses NamedDiscard/AiCardMemory. Revealed cards remain known
        // after a miss and can be named again for flashback if still in hand.
        final SpellAbility therapy = cabalTherapy(ai, candidates, approve);
        if (therapy != null) {
            return therapy;
        }

        final SpellAbility wish = livingWish(ai, candidates, approve);
        if (wish != null) {
            return wish;
        }
        final SpellAbility tutor = intuition(ai, candidates, approve);
        if (tutor != null) {
            return tutor;
        }

        if (LoopRunner.inPlay(ai, ALUREN) == null) {
            final SpellAbility brainstorm = brainstorm(ai, candidates, opp, approve);
            if (brainstorm != null) {
                return brainstorm;
            }
            final SpellAbility aluren = LoopRunner.castFromHand(ai, candidates, ALUREN);
            if (aluren != null && canStartCombo(ai) && tryCast(ai, aluren, approve)) {
                return aluren;
            }
            if (paidSavageDraw(ai)) {
                final SpellAbility harpy = LoopRunner.castFromHand(ai, candidates, HARPY);
                if (harpy != null && tryCast(ai, harpy, approve)) {
                    return harpy;
                }
            }
            return null;
        }

        if (ai.getGame().getStack().getSpellsCastThisTurn().size() >= SPELL_CAP) {
            return null;
        }

        // Cast Soul Warden only when it protects a real finish route.
        if (LoopRunner.inPlay(ai, WARDEN) == null
                && LoopRunner.inHand(ai, WARDEN) && needsWarden(ai, opp)) {
            final SpellAbility warden = freeCast(ai, candidates, WARDEN);
            if (tryCast(ai, warden, approve)) {
                return warden;
            }
        }

        final SpellAbility brainstorm = brainstorm(ai, candidates, opp, approve);
        if (brainstorm != null) {
            return brainstorm;
        }

        final Stage stage = stage(ai, opp);
        switch (stage) {
            case MANA: {
                // Stroke is the backup kill; never take it ahead of a ready Carrier kill.
                final SpellAbility stroke = strokeForTheLibrary(ai, candidates, opp, approve);
                if (stroke != null) {
                    return stroke;
                }
                final SpellAbility tap = tapForTheMana(ai, candidates, approve);
                return tap != null ? tap : round(ai, candidates, approve, CLOUD);
            }
            case CARDS: {
                // Wirewood Savage first: every Beast that enters (the Harpy, each time) draws a card
                final SpellAbility savage = freeCast(ai, candidates, SAVAGE);
                if (LoopRunner.inPlay(ai, SAVAGE) == null && tryCast(ai, savage, approve)) {
                    return savage;
                }
                return round(ai, candidates, approve, accessible(ai, FAMILIAR) ? FAMILIAR : SAVAGE);
            }
            case DRAIN:
                return round(ai, candidates, approve, CARRIER);
            case LIFE: {
                // A finite safety buffer only. Harpy's ETB returns itself and
                // Soul Warden gains one life. Stage changes to NONE at 20.
                final SpellAbility harpy = freeCast(ai, candidates, HARPY);
                return tryCast(ai, harpy, approve) ? harpy : null;
            }
            case ACERERAK: {
                final SpellAbility cast = freeCast(ai, candidates, ACERERAK); // it returns itself to the hand
                return tryCast(ai, cast, approve) ? cast : null;
            }
            default:
                return null;
        }
    }

    /**
     * Before combat, with a board that kills by attacking (nothing of the opponent's can block, the power that is ready
     * covers the life): in the screenshot of the endless Harpy the opponent was at 3 life with no creatures.
     */
    private static boolean lethalByAttack(final Player ai, final Player opp) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.is(forge.game.phase.PhaseType.MAIN1) || !ph.isPlayerTurn(ai)) {
            return false;
        }
        for (final Card b : opp.getCreaturesInPlay()) {
            if (!b.isTapped()) {
                return false;
            }
        }
        int power = 0;
        for (final Card c : ai.getCreaturesInPlay()) {
            if (!c.isTapped() && !c.isSick() && c.getNetPower() > 0) {
                power += c.getNetPower();
            }
        }
        return power >= opp.getLife();
    }

    private static SpellAbility setupRamp(final Player ai, final List<SpellAbility> candidates,
            final Predicate<SpellAbility> approve) {
        if (LoopRunner.inPlay(ai, ALUREN) != null) {
            return null;
        }
        final SpellAbility aluren = LoopRunner.castFromHand(ai, candidates, ALUREN);
        if (aluren != null && canStartCombo(ai)) {
            aluren.setActivatingPlayer(ai);
            if (ComputerUtilMana.canPayManaCost(aluren, ai, 0, false)) {
                return null;
            }
        }

        // Hickory Woodlot is a land drop, not a mana ability from hand.
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && host.getName().equals(WOODLOT) && host.isLand()
                    && host.isInZone(ZoneType.Hand) && sa.isLandAbility()
                    && tryCast(ai, sa, approve)) {
                return sa;
            }
        }

        // Birds of Paradise is the preferred early creature ramp.
        final SpellAbility birds = LoopRunner.castFromHand(ai, candidates, BIRDS);
        return tryCast(ai, birds, approve) ? birds : null;
    }

    /**
     * Cabal Therapy is for before the combo goes off: it needs cards in the opponent's hand to hit, it never comes after
     * the first Harpy of the turn (the loop has begun), at most twice a turn, and the B for it must not be the mana that
     * Aluren is waiting for. The flashback is paid with a Birds of Paradise: the other creatures are the loop.
     */
    private static boolean therapyTime(final Player ai, final Player opp) {
        if (opp == null || opp.getCardsIn(ZoneType.Hand).isEmpty()) {
            return false;
        }
        int cast = 0;
        for (final SpellAbility sa : ai.getGame().getStack().getSpellsCastThisTurn()) {
            final Card h = sa.getHostCard();
            if (h != null && h.getName().equals(HARPY)) {
                return false;
            }
            if (h != null && h.getName().equals(THERAPY)) {
                cast++;
            }
        }
        if (cast >= 2) {
            return false;
        }
        if (LoopRunner.inPlay(ai, ALUREN) != null || !LoopRunner.inHand(ai, ALUREN)) {
            return true;
        }
        // Aluren in the hand: Therapy must not take the mana that is exactly enough for it (with less, Aluren waits anyway)
        int cmc = 4;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(ALUREN)) {
                cmc = c.getCMC();
            }
        }
        return ComputerUtilMana.getAvailableManaEstimate(ai) != cmc;
    }

    private static SpellAbility cabalTherapy(final Player ai, final List<SpellAbility> candidates,
            final Predicate<SpellAbility> approve) {
        if (!therapyTime(ai, ai.getWeakestOpponent())) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !host.getName().equals(THERAPY) || !sa.isSpell()
                    || !ai.equals(host.getController())) {
                continue;
            }
            final boolean fromHand = host.isInZone(ZoneType.Hand);
            final boolean flashback = host.isInZone(ZoneType.Graveyard)
                    && sa.getPayCosts() != null
                    && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class);
            if (!fromHand && !(flashback && LoopRunner.inPlay(ai, BIRDS) != null)) {
                continue;
            }

            if (flashback && sacrificesAPiece(ai, sa)) {
                continue; // the flashback sacrifices a creature of the loop: never
            }

            // Set the target after resetting the ability. NamedDiscard
            // recognizes revealed cards and prepares the discard sub-ability.
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (!NamedDiscard.consider(ai, sa).willingToPlay(sa)) {
                continue;
            }
            if (approve.test(sa)) {
                return sa;
            }
            VetoLog.note(ai, "aluren-step-refused " + THERAPY);
        }
        return null;
    }

    /** Whether the creature the AI would sacrifice for the cost is one of the pieces of the kill. */
    private static boolean sacrificesAPiece(final Player ai, final SpellAbility sa) {
        final CostSacrifice cost = sa.getPayCosts() == null ? null : sa.getPayCosts().getCostPartByType(CostSacrifice.class);
        if (cost == null || cost.payCostFromSource()) {
            return false;
        }
        final forge.game.card.CardCollectionView chosen = forge.ai.ComputerUtil.chooseSacrificeType(ai, cost.getType(), sa,
                sa.getTargetCard(), false, Math.max(1, cost.getAbilityAmount(sa)), CardCollection.EMPTY);
        if (chosen == null) {
            return false;
        }
        for (final Card c : chosen) {
            for (final String key : KEYS) {
                if (c.getName().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The mana is exactly what Aluren costs: nothing else is cast first. With less the Aluren waits anyway, with more the
     * cantrip is paid out of the rest (the AI sat on its Brainstorm with Aluren in the hand it could not cast yet).
     */
    private static boolean alurenNeedsAllTheMana(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(ALUREN)) {
                return ComputerUtilMana.getAvailableManaEstimate(ai) == c.getCMC();
            }
        }
        return false;
    }

    /**
     * Wirewood Savage without Aluren: the Harpy is cast for its mana, draws a card as the Beast that enters, and returns
     * itself (two mana for a card, again and again while the mana is there). Not with the mana Aluren needs, not with a
     * hand that would be discarded down at the end of the turn, not into the floor of the library.
     */
    private static boolean paidSavageDraw(final Player ai) {
        if (LoopRunner.inPlay(ai, ALUREN) != null || LoopRunner.inPlay(ai, SAVAGE) == null || ai.getLife() < 3
                || ai.getCardsIn(ZoneType.Hand).size() > 6
                || ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_FLOOR + 3) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(ALUREN) && ComputerUtilMana.getAvailableManaEstimate(ai) - 2 < c.getCMC()) {
                return false;
            }
        }
        return true;
    }

    private static SpellAbility brainstorm(final Player ai, final List<SpellAbility> candidates,
            final Player opp, final Predicate<SpellAbility> approve) {
        if (!LoopRunner.inHand(ai, BRAINSTORM)
                || ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_FLOOR + 3) {
            return null;
        }
        final boolean setupNeeded = LoopRunner.inPlay(ai, ALUREN) == null
                && (!LoopRunner.inHand(ai, ALUREN) || !canStartCombo(ai) || !alurenNeedsAllTheMana(ai));
        final boolean loopNeedsCards = LoopRunner.inPlay(ai, ALUREN) != null && needsCards(ai, opp);
        if (!setupNeeded && !loopNeedsCards) {
            return null;
        }
        final SpellAbility cast = LoopRunner.castFromHand(ai, candidates, BRAINSTORM);
        return tryCast(ai, cast, approve) ? cast : null;
    }

    /**
     * Every land that is untapped goes into the mana pool: Cloud of Faeries untaps two tapped lands each time it is
     * cast, which is the gain, and the pool keeps the mana for the rest of the phase, for Stroke of Genius.
     */
    private static SpellAbility tapForTheMana(final Player ai, final List<SpellAbility> candidates,
                                              final Predicate<SpellAbility> approve) {
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isManaAbility() && host.isLand() && !host.isTapped() && ai.equals(host.getController())
                    && host.isInZone(ZoneType.Battlefield)) {
                sa.setActivatingPlayer(ai);
                if (!RunawayLoop.exceeded(sa, LoopRunner.MAX_ACTIVATIONS) && approve.test(sa)) {
                    return sa;
                }
            }
        }
        return null;
    }

    /**
     * One step of a round of the loop around Cavern Harpy and a creature that does something when it enters: cast the
     * creature, cast the Harpy (its trigger sends the creature back), pay a life to send the Harpy back, and again.
     */
    private static SpellAbility round(final Player ai, final List<SpellAbility> candidates,
                                      final Predicate<SpellAbility> approve, final String creature) {
        final boolean creatureInHand = LoopRunner.inHand(ai, creature);
        final Card creatureInPlay = LoopRunner.inPlay(ai, creature);
        final Card harpyInPlay = LoopRunner.inPlay(ai, HARPY);
        final boolean harpyInHand = LoopRunner.inHand(ai, HARPY);
        if (creatureInHand) {
            final SpellAbility cast = freeCast(ai, candidates, creature);
            if (tryCast(ai, cast, approve)) {
                return cast;
            }
        }
        if (creatureInPlay != null && harpyInHand) {
            final SpellAbility cast = freeCast(ai, candidates, HARPY);
            if (tryCast(ai, cast, approve)) {
                return cast;
            }
        }
        if (creatureInPlay != null && harpyInPlay != null && ai.getLife() >= 2) {
            return harpyBackToTheHand(ai, candidates, approve);
        }
        return null;
    }

    /** "Pay 1 life: return Cavern Harpy to its owner's hand": the loop that casts it again. */
    private static SpellAbility harpyBackToTheHand(final Player ai, final List<SpellAbility> candidates,
                                                   final Predicate<SpellAbility> approve) {
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && host.getName().equals(HARPY) && sa.isAbility() && !sa.isManaAbility()
                    && host.isInZone(ZoneType.Battlefield) && ai.equals(host.getController())
                    && sa.getActivationsThisTurn() < RETURN_CAP && tryCast(ai, sa, approve)) {
                return sa;
            }
        }
        return null;
    }

    /** Stroke of Genius for the whole of the opponent's library, when the mana for it is in the pool. */
    private static SpellAbility strokeForTheLibrary(final Player ai, final List<SpellAbility> candidates,
                                                    final Player opp, final Predicate<SpellAbility> approve) {
        final SpellAbility stroke = LoopRunner.castFromHand(ai, candidates, STROKE);
        final int library = opp.getCardsIn(ZoneType.Library).size();
        if (stroke == null || library < 1 || !opp.canDraw()) {
            return null;
        }
        if (ai.getManaPool().totalMana() < library + 3 || LoopRunner.aimedAtPlayer(ai, stroke, opp) == null) {
            return null;
        }
        stroke.setXManaCostPaid(library);
        return approve.test(stroke) ? stroke : null;
    }
}
