package forge.ai.simulation;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Abilities of cards in the hand that are not spells: landcycling and the other land searches that discard the card
 * (basic landcycling, plainscycling...), and reinforce.
 * <p>
 * A land search is a card for a card, so a rollout only sees the mana lost; it is worth it for a hand that has no land
 * to play. Reinforce puts counters on a creature for a discarded card: like a pump it is a trick for the moment the
 * blocks are down (see {@link FlatMonteCarloPicker#isCombatTrick}), not for the main phase.
 * <p>
 * Forecast is not handled: it can only be used in the AI's upkeep, and Monte Carlo decides in the main phases only.
 */
public final class HandAbilities {
    /** Evaluator points for a land in the hand of a player who has none: the land drops of the turns to come. */
    static final int LAND_NEED_BONUS = 30;
    /** A hand with no land is only short of one while the mana base is small. */
    static final int ENOUGH_LANDS = 7;

    private HandAbilities() {
    }

    private static final Set<String> LAND_WORDS = new HashSet<>(Arrays.asList(
            "Land", "Basic", "Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes"));

    /** Whether a ChangeType such as "Basic", "Land.Basic", "Plains" or "Forest,Island" can only name lands. */
    static boolean searchesLands(final String type) {
        if (type == null || type.isEmpty() || type.contains("Nonland")) {
            return false;
        }
        for (final String alternative : type.split(",")) {
            boolean land = false;
            for (final String word : alternative.split("[.+]")) {
                if (LAND_WORDS.contains(word)) {
                    land = true;
                    break;
                }
            }
            if (!land) {
                return false;
            }
        }
        return true;
    }

    /** Landcycling, basic landcycling and the like: discard the card to search the library for a land. */
    public static boolean isLandcycling(final SpellAbility sa) {
        if (!TutorPlan.isHandAbility(sa) || sa.getApi() != ApiType.ChangeZone || !sa.hasParam("Origin")
                || !"Library".equals(sa.getParam("Origin")) || !"Hand".equals(sa.getParam("Destination"))) {
            return false;
        }
        return searchesLands(sa.getParamOrDefault("ChangeType", ""));
    }

    /** Reinforce: discard the card and put counters on a target creature. */
    public static boolean isReinforce(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && sa.isActivatedAbility() && !sa.isSpell() && host.isInZone(ZoneType.Hand)
                && sa.getApi() == ApiType.PutCounter && "Hand".equals(sa.getParam("ActivationZone")) && sa.usesTargeting();
    }

    /** No land to play and a small mana base: the land is wanted. */
    static boolean landNeeded(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                return false;
            }
        }
        int lands = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand()) {
                lands++;
            }
        }
        return lands < ENOUGH_LANDS;
    }

    /** Whether discovery may offer the land search: the land is wanted, the library has one and the mana is spare. */
    public static boolean isDiscoverable(final Player ai, final SpellAbility sa) {
        if (!isLandcycling(sa) || !landNeeded(ai)) {
            return false;
        }
        final CardCollection pool = TutorPlan.pool(ai, sa);
        return !pool.isEmpty() && FlatMonteCarloPicker.sparesMana(ai, sa);
    }

    /** Value of the play, in evaluator points, added to the rollouts' value of the position. */
    static int bonus(final SpellAbility sa, final FlatMonteCarloPicker.Config cfg) {
        if (sa == null || cfg.strategicWeightPercent <= 0 || !isLandcycling(sa)) {
            return 0;
        }
        try {
            final Player ai = sa.getActivatingPlayer() != null ? sa.getActivatingPlayer() : sa.getHostCard().getController();
            return ai != null && landNeeded(ai) ? LAND_NEED_BONUS * cfg.strategicWeightPercent / 100 : 0;
        } catch (final RuntimeException e) {
            return 0; // a refinement only
        }
    }
}
