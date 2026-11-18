package forge.ai.simulation;

import java.util.List;
import java.util.function.Predicate;

import forge.ai.ComputerUtilMana;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Aluren deck (Mattias Jorstedt's): with Aluren in play every creature of three or less is free, and the creatures
 * of the deck feed on each other. Wirewood Savage draws for every Beast, Cavern Harpy is one, and its enter trigger
 * bounces a blue or black creature (Raven Familiar digs again, Soul Warden pays the life back); "pay 1 life: return
 * Cavern Harpy to its hand" lets it be cast again. The AI had Aluren and the Harpy in hand and cast neither the way the
 * deck does: Aluren was one more four-drop for the rollouts, the free casts were weighed as creature spells, and the
 * life payment of the Harpy was never made.
 *
 * <p>Everything here is a step of {@link LoopRunner}: Aluren when two pieces wait in the hand, then the free creatures
 * in the order of the deck, then the Harpy back to the hand while a draw engine or a creature to bounce is in play,
 * with a cap on the casts of a turn and the library kept out of reach of its own draws. Stroke of Genius for the
 * opponent's library ends it when the mana is there. Untested against a real game: the caps are what keep it safe.
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
    private static final int SPELL_CAP = 70;
    private static final int RETURN_CAP = 30;
    private static final int LIBRARY_FLOOR = 8;

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

    private static boolean piece(final String name) {
        return name.equals(HARPY) || name.equals(SAVAGE) || name.equals(FAMILIAR) || name.equals(WARDEN)
                || name.equals(CARRIER) || name.equals(CLOUD);
    }

    private static int piecesInHand(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (piece(c.getName())) {
                n++;
            }
        }
        return n;
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
        final int library = ai.getCardsIn(ZoneType.Library).size();
        final boolean savage = LoopRunner.inPlay(ai, SAVAGE) != null;
        final boolean warden = LoopRunner.inPlay(ai, WARDEN) != null;
        final boolean bounceTarget = LoopRunner.inPlay(ai, FAMILIAR) != null || LoopRunner.inPlay(ai, CLOUD) != null
                || LoopRunner.inPlay(ai, CARRIER) != null;
        // the cheap engines first: the one that draws for the Beasts, the one that pays the life back
        for (final String name : new String[] { SAVAGE, WARDEN }) {
            final SpellAbility sa = freeCast(ai, candidates, name);
            if (tryCast(ai, sa, approve)) {
                return sa;
            }
        }
        if (library > LIBRARY_FLOOR) {
            final SpellAbility familiar = freeCast(ai, candidates, FAMILIAR);
            if (tryCast(ai, familiar, approve)) {
                return familiar;
            }
        }
        final SpellAbility cloud = freeCast(ai, candidates, CLOUD);
        if (tryCast(ai, cloud, approve)) {
            return cloud;
        }
        // Maggot Carrier drains both players: with Soul Warden to pay it back, or when it is the last point
        if (warden || opp.getLife() <= 2) {
            final SpellAbility carrier = freeCast(ai, candidates, CARRIER);
            if (tryCast(ai, carrier, approve)) {
                return carrier;
            }
        }
        // Cavern Harpy bounces a blue or black creature: it needs a draw engine to be worth it, or a creature to bounce
        if (library > LIBRARY_FLOOR && ai.getLife() > 3 && (savage || bounceTarget)) {
            final SpellAbility harpy = freeCast(ai, candidates, HARPY);
            if (tryCast(ai, harpy, approve)) {
                return harpy;
            }
        }
        return harpyBackToTheHand(ai, candidates, approve, library, savage || bounceTarget, warden);
    }

    /** "Pay 1 life: return Cavern Harpy to its owner's hand": the loop that casts it again. */
    private static SpellAbility harpyBackToTheHand(final Player ai, final List<SpellAbility> candidates,
                                                   final Predicate<SpellAbility> approve, final int library,
                                                   final boolean engine, final boolean warden) {
        if (!engine || library <= LIBRARY_FLOOR || ai.getLife() < (warden ? 3 : 6)) {
            return null;
        }
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

    /** Stroke of Genius for the whole of the opponent's library, when the mana for it is there. */
    private static SpellAbility strokeForTheLibrary(final Player ai, final List<SpellAbility> candidates,
                                                    final Player opp, final Predicate<SpellAbility> approve) {
        final SpellAbility stroke = LoopRunner.castFromHand(ai, candidates, STROKE);
        final int library = opp.getCardsIn(ZoneType.Library).size();
        if (stroke == null || library < 1 || !opp.canDraw()) {
            return null;
        }
        final int mana = ComputerUtilMana.getAvailableManaEstimate(ai) + ai.getManaPool().totalMana();
        if (mana < library + 3 || LoopRunner.aimedAtPlayer(ai, stroke, opp) == null) {
            return null;
        }
        stroke.setXManaCostPaid(library);
        return approve.test(stroke) ? stroke : null;
    }
}
