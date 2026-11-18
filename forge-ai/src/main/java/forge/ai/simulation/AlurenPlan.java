package forge.ai.simulation;

import java.util.List;
import java.util.function.Predicate;

import forge.ai.RunawayLoop;
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
    private static final String[] KEYS = { ALUREN, HARPY, SAVAGE, FAMILIAR, WARDEN, CARRIER, CLOUD, STROKE, ACERERAK, WISH };

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
    public enum Stage { NONE, CARDS, MANA, DRAIN, ACERERAK }

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

    /** Rounds of the mana loop to cover the library of the opponent with Stroke of Genius: two mana a round. */
    private static int roundsNeeded(final Player opp) {
        return (opp.getCardsIn(ZoneType.Library).size() + 3 + 1) / 2;
    }

    /** Life enough for the rounds of the loop: a Soul Warden pays the life back, otherwise a round costs one. */
    private static boolean lifeOk(final Player ai, final Player opp) {
        return LoopRunner.inPlay(ai, WARDEN) != null || ai.getLife() >= roundsNeeded(opp) + LIFE_BUFFER;
    }

    /** Whether cards are still missing for the kill: Stroke of Genius, Cloud of Faeries, or the life for the rounds. */
    private static boolean needsCards(final Player ai, final Player opp) {
        return !LoopRunner.inHand(ai, STROKE) || !accessible(ai, CLOUD)
                || !lifeOk(ai, opp) && !accessible(ai, WARDEN);
    }

    /**
     * Which loop is to be run now, from what is in the hand and on the table. The mana loop wins (Stroke of Genius in
     * the hand, Cloud of Faeries and the life for it); until then the cards are drawn with Raven Familiar; Maggot
     * Carrier drains the opponent when there is no Stroke of Genius to find and the life lead is there for it.
     */
    public static Stage stage(final Player ai, final Player opp) {
        if (opp == null || ai.getLife() < 3) {
            return Stage.NONE;
        }
        if (!accessible(ai, HARPY)) {
            // no Harpy: Acererak is its own loop, and it is the only one
            return accessible(ai, ACERERAK) && opp.canLoseLife() && ai.getLife() > LIFE_BUFFER ? Stage.ACERERAK : Stage.NONE;
        }
        final int library = ai.getCardsIn(ZoneType.Library).size();
        final boolean canDeckThem = opp.canDraw() && !opp.cantLoseCheck(forge.game.player.GameLossReason.Milled)
                && !opp.getCardsIn(ZoneType.Library).isEmpty();
        if (canDeckThem && LoopRunner.inHand(ai, STROKE) && accessible(ai, CLOUD) && lifeOk(ai, opp)
                && lands(ai) >= 2) {
            return Stage.MANA;
        }
        if (needsCards(ai, opp) && library > LIBRARY_FLOOR && (accessible(ai, FAMILIAR) || accessible(ai, SAVAGE))) {
            return Stage.CARDS;
        }
        if (accessible(ai, CARRIER) && opp.canLoseLife()
                && (LoopRunner.inPlay(ai, WARDEN) != null || ai.getLife() > opp.getLife() + 2 * LIFE_BUFFER)) {
            return Stage.DRAIN;
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
                && ai.getCardsIn(ZoneType.Library).size() > LIBRARY_FLOOR + 2
                && stage(ai, ai.getWeakestOpponent()) == Stage.CARDS;
    }

    /**
     * The decks that dig for their kill (High Tide, storm, Ad Nauseam and the other combo decks, the Aluren loop): more
     * than seven cards in the hand is no reason to refuse a draw, they win the turn they draw into it. The library has to
     * stay deep enough, so a deck is never drawn out by this.
     */
    public static boolean digging(final Player ai) {
        if (ai == null || ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_FLOOR + 2) {
            return false;
        }
        if (drawLoop(ai) || TidePlan.isTideDeck(ai)) {
            return true;
        }
        return ai.getController() instanceof forge.ai.PlayerControllerAi
                && ((forge.ai.PlayerControllerAi) ai.getController()).getAi().deckPlan(ai).archetype
                == DeckPlan.Archetype.COMBO;
    }

    private static boolean inLibrary(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The piece of the kill that is still missing and that is in the pool (the sideboard for Living Wish, the library
     * for a tutor): the way in (Harpy or Acererak), the untapper when Stroke of Genius is there to use it, the digger
     * when it is not, the life gain, and the drain when no Stroke of Genius is left anywhere. Null: nothing is missing.
     */
    public static String wanted(final Player ai, final Iterable<Card> pool) {
        final boolean stroke = LoopRunner.inHand(ai, STROKE) || inLibrary(ai, STROKE);
        final String[] order = { HARPY, STROKE, CLOUD, FAMILIAR, WARDEN, CARRIER };
        for (final String name : order) {
            final boolean missing;
            switch (name) {
                case HARPY:
                    missing = !accessible(ai, HARPY) && !accessible(ai, ACERERAK);
                    break;
                case STROKE:
                    missing = !LoopRunner.inHand(ai, STROKE); // the one card of the kill that there is a single copy of
                    break;
                case CLOUD:
                    missing = stroke && !accessible(ai, CLOUD);
                    break;
                case FAMILIAR:
                    missing = !accessible(ai, FAMILIAR) && !LoopRunner.inHand(ai, STROKE);
                    break;
                case CARRIER:
                    missing = !stroke && !accessible(ai, CARRIER) && !accessible(ai, ACERERAK);
                    break;
                default:
                    missing = !accessible(ai, WARDEN);
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

    /** Intuition for the piece that is missing (the tutor takes three: the wanted one first, see picks), mana from the lands. */
    private static SpellAbility intuition(final Player ai, final List<SpellAbility> candidates,
                                          final Predicate<SpellAbility> approve) {
        final SpellAbility spell = LoopRunner.castFromHand(ai, candidates, INTUITION);
        if (spell == null || wanted(ai, ai.getCardsIn(ZoneType.Library)) == null) {
            return null;
        }
        return tryCast(ai, spell, approve) ? spell : null;
    }

    /** Living Wish before anything else: it fetches what the loops need, while the lands are still untapped. */
    private static SpellAbility livingWish(final Player ai, final List<SpellAbility> candidates,
                                           final Predicate<SpellAbility> approve) {
        final SpellAbility wish = LoopRunner.castFromHand(ai, candidates, WISH);
        if (wish == null || wanted(ai, ai.getCardsIn(ZoneType.Sideboard)) == null) {
            return null;
        }
        return tryCast(ai, wish, approve) ? wish : null;
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
        return harpy; // the life loop with Soul Warden: it comes back by itself and gains a life each time
    }

    /**
     * What to take from the library, the hand-piece that is missing for the kill first: Aluren, Cavern Harpy, Stroke of
     * Genius, Soul Warden, Cloud of Faeries, Raven Familiar, and Maggot Carrier as the other way to win.
     */
    public static CardCollection picks(final Player ai, final Iterable<Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0 || !isAlurenDeck(ai)) {
            return null;
        }
        final String[] order = { ALUREN, HARPY, STROKE, WARDEN, CLOUD, FAMILIAR, CARRIER };
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
        if (host == null || !host.getName().equals(HARPY) || !host.isInZone(ZoneType.Hand) || !ai.equals(host.getController())
                || !isAlurenDeck(ai) || LoopRunner.inPlay(ai, ALUREN) == null || LoopRunner.inPlay(ai, SAVAGE) != null) {
            return false;
        }
        for (final Card c : ai.getCreaturesInPlay()) {
            if (c.isBlack() || c.isBlue()) {
                return false; // something to send back
            }
        }
        // only worth waiting when a creature of the loop is in the hand to be cast first
        return LoopRunner.inHand(ai, FAMILIAR) || LoopRunner.inHand(ai, CLOUD) || LoopRunner.inHand(ai, CARRIER)
                || LoopRunner.inHand(ai, ACERERAK) || LoopRunner.inHand(ai, SAVAGE);
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Player opp,
                             final Predicate<SpellAbility> approve) {
        if (opp == null || !isAlurenDeck(ai)) {
            return null;
        }
        final SpellAbility wish = livingWish(ai, candidates, approve);
        if (wish != null) {
            return wish; // the setup comes first: the missing piece, then Aluren, then the loops
        }
        final SpellAbility tutor = intuition(ai, candidates, approve);
        if (tutor != null) {
            return tutor;
        }
        if (LoopRunner.inPlay(ai, ALUREN) == null) {
            final SpellAbility aluren = LoopRunner.castFromHand(ai, candidates, ALUREN);
            // Aluren with something to cast under it: two pieces of the deck in the hand
            if (aluren != null && piecesInHand(ai) >= 2 && tryCast(ai, aluren, approve)) {
                return aluren;
            }
            return null;
        }
        if (ai.getGame().getStack().getSpellsCastThisTurn().size() >= SPELL_CAP) {
            return null;
        }
        final SpellAbility stroke = strokeForTheLibrary(ai, candidates, opp, approve);
        if (stroke != null) {
            return stroke;
        }
        // Soul Warden pays the life of every round back: the first thing, and free
        final SpellAbility warden = freeCast(ai, candidates, WARDEN);
        if (LoopRunner.inPlay(ai, WARDEN) == null && tryCast(ai, warden, approve)) {
            return warden;
        }
        final Stage stage = stage(ai, opp);
        switch (stage) {
            case MANA: {
                final SpellAbility tap = tapForTheMana(ai, candidates, approve);
                return tap != null ? tap : round(ai, candidates, approve, CLOUD);
            }
            case CARDS: {
                // Wirewood Savage first: every Beast that enters (the Harpy, each time) draws a card
                final SpellAbility savage = freeCast(ai, candidates, SAVAGE);
                if (LoopRunner.inPlay(ai, SAVAGE) == null && tryCast(ai, savage, approve)) {
                    return savage;
                }
                // the Familiar digs deeper; with the Savage alone the Harpy is the whole loop, and it costs no life
                return round(ai, candidates, approve, accessible(ai, FAMILIAR) ? FAMILIAR : SAVAGE);
            }
            case DRAIN:
                return round(ai, candidates, approve, CARRIER);
            case ACERERAK: {
                final SpellAbility cast = freeCast(ai, candidates, ACERERAK); // it returns itself to the hand
                return tryCast(ai, cast, approve) ? cast : null;
            }
            default:
                return null;
        }
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
