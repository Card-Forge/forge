package forge.game.staticability;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class StaticAbilityMustTarget {
    static String MODE = "MustTarget";

    public static boolean filterMustTargetCards(Player targetingPlayer, List<Card> targets, final SpellAbility spellAbility) {
        //Only applied when the targeting player and controller are the same
        if (targetingPlayer != spellAbility.getHostCard().getController()) {
            return false;
        }

        List<Pair<String, ZoneType>> restrictions = getAllRestrictions(spellAbility);
        return applyMustTargetCardAbility(restrictions, targets, spellAbility);
    }

    public static boolean meetsMustTargetRestriction(final SpellAbility spellAbility) {
        // Copied spell is not affected.
        // (ChangeTarget does not go this path so not checked here.)
        if (spellAbility.isCopied()) return true;

        final Game game = spellAbility.getHostCard().getGame();
        List<Pair<String, ZoneType>> restrictions = getAllRestrictions(spellAbility);

        if (restrictions.isEmpty()) return true;

        SpellAbility currentAbility = spellAbility;
        boolean usesTargeting = false;
        do {
            if (currentAbility.usesTargeting() && !currentAbility.hasParam("TargetingPlayer")) {
                usesTargeting = true;
                // Check if currentAbility can target any MustTarget cards
                TargetRestrictions tgt = currentAbility.getTargetRestrictions();
                List<ZoneType> zone = tgt.getZone();
                List<Card> validCards = CardLists.getValidCards(game.getCardsIn(zone), tgt.getValidTgts(), currentAbility.getActivatingPlayer(), currentAbility.getHostCard(), currentAbility);
                List<Card> choices = CardLists.getTargetableCards(validCards, currentAbility);

                isRestrictionsMet(restrictions, choices, currentAbility);
            }
            currentAbility = currentAbility.getSubAbility();
        } while(currentAbility != null);

        return !usesTargeting || restrictions.isEmpty();
    }

    private static List<Pair<String, ZoneType>> getAllRestrictions(final SpellAbility spellAbility) {
        final Game game = spellAbility.getHostCard().getGame();
        List<Pair<String, ZoneType>> restrictions = new ArrayList<>();

        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || !stAb.matchesValidParam("ValidSA", spellAbility) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                Pair<String, ZoneType> newRestriction = Pair.of(stAb.getParam("ValidTarget"), ZoneType.smartValueOf(stAb.getParam("ValidZone")));
                if (!restrictions.contains(newRestriction)) {
                    restrictions.add(newRestriction);
                }
            }
        }

        return restrictions;
    }

    private static boolean isRestrictionsMet(List<Pair<String, ZoneType>> restrictions, List<Card> targets, final SpellAbility spellAbility) {
        for (int i = restrictions.size() - 1; i >= 0; i--) {
            Pair<String, ZoneType> restriction = restrictions.get(i);
            // First, check satisfied restrictions that is already targeted by spellAbility
            boolean found = false;
            for (final Card card : spellAbility.getTargets().getTargetCards()) {
                if (card.getType().hasStringType(restriction.getLeft()) && card.isInZone(restriction.getRight())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                restrictions.remove(i);
                continue;
            }

            // Second check if their are any targetable card with type in zone
            found = false;
            for (final Card card : targets) {
                if (card.getType().hasStringType(restriction.getLeft()) && card.isInZone(restriction.getRight())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                restrictions.remove(i);
            }
        }

        return restrictions.isEmpty();
    }

    private static boolean applyMustTargetCardAbility(List<Pair<String, ZoneType>> restrictions, List<Card> targets, final SpellAbility spellAbility) {
        if (isRestrictionsMet(restrictions, targets, spellAbility)) {
            return false;
        }

        // If remaining restrictions are larger than possible target numbers, then all targets are cleared (means not possible to target any one)
        final int maxTargets = spellAbility.getMaxTargets();
        final int targeted = spellAbility.getTargets().size();
        if (restrictions.size() > maxTargets - targeted) {
            targets.clear();
            return true;
        }

        // Filter out all cards not satisfying any of the restrictions
        boolean filtered = false;
        for (int i = targets.size() - 1; i >= 0; i--) {
            final Card card = targets.get(i);
            boolean satisfied = false;
            for (Pair<String, ZoneType> restriction : restrictions) {
                if (card.getType().hasStringType(restriction.getLeft()) && card.isInZone(restriction.getRight())) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                targets.remove(i);
                filtered = true;
            }
        }
        return filtered;
    }

}
