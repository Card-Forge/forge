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

    /** The most spells of one turn this plan casts, and the cards that stay in the library out of reach of the draws. */
    private static final int SPELL_CAP = 150;
    private static final int RETURN_CAP = 80;
    private static final int LIBRARY_FLOOR = 8;
    /** The life kept in hand as a buffer on top of what a loop pays, when no Soul Warden pays it back. */
    private static final int LIFE_BUFFER = 4;

    private AlurenPlan() {
    }

    public static boolean isAlurenDeck(final Player ai) {
        boolean aluren = false;
        boolean harpy = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Battlefield,
                ZoneType.Sideboard)) {
            aluren |= c.getName().equals(ALUREN);
            harpy |= c.getName().equals(HARPY);
        }
        return aluren && harpy;
    }

    /** What the loop is working on: the cards to draw, the mana for Stroke of Genius, or the drain of Maggot Carrier. */
    public enum Stage { NONE, CARDS, MANA, DRAIN }

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
        if (opp == null || !accessible(ai, HARPY) || ai.getLife() < 3) {
            return Stage.NONE;
        }
        final int library = ai.getCardsIn(ZoneType.Library).size();
        final boolean canDeckThem = opp.canDraw() && !opp.cantLoseCheck(forge.game.player.GameLossReason.Milled)
                && !opp.getCardsIn(ZoneType.Library).isEmpty();
        if (canDeckThem && LoopRunner.inHand(ai, STROKE) && accessible(ai, CLOUD) && lifeOk(ai, opp)
                && lands(ai) >= 2) {
            return Stage.MANA;
        }
        if (needsCards(ai, opp) && library > LIBRARY_FLOOR && accessible(ai, FAMILIAR)) {
            return Stage.CARDS;
        }
        if (accessible(ai, CARRIER) && opp.canLoseLife()
                && (LoopRunner.inPlay(ai, WARDEN) != null || ai.getLife() > opp.getLife() + 2 * LIFE_BUFFER)) {
            return Stage.DRAIN;
        }
        return Stage.NONE;
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
        for (final String name : order) {
            if (picks.size() >= n) {
                break;
            }
            final boolean have = name.equals(ALUREN) ? LoopRunner.inPlay(ai, name) != null || LoopRunner.inHand(ai, name)
                    : accessible(ai, name);
            if (have) {
                continue;
            }
            for (final Card c : pool) {
                if (c.getName().equals(name)) {
                    picks.add(c);
                    break;
                }
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
        return fallback;
    }

    private static boolean tryCast(final Player ai, final SpellAbility sa, final Predicate<SpellAbility> approve) {
        if (sa == null) {
            return false;
        }
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        return approve.test(sa);
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Player opp,
                             final Predicate<SpellAbility> approve) {
        if (opp == null || !isAlurenDeck(ai)) {
            return null;
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
            case CARDS:
                return round(ai, candidates, approve, FAMILIAR);
            case DRAIN:
                return round(ai, candidates, approve, CARRIER);
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
