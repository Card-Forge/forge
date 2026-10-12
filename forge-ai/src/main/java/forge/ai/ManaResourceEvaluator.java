package forge.ai;

import forge.card.mana.ManaAtom;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.function.Predicate;

/** Shared strategic scoring for mana resources; it does not determine payment feasibility. */
public final class ManaResourceEvaluator {
    private static final int NON_MANA_USE_SCORE = 13;

    private ManaResourceEvaluator() {
    }

    public static int evaluateManaBase(final Player player, final AiDeckStatistics statistics,
            final boolean includeNormalPhasing, final Predicate<Card> phasesInNormally) {
        int maxTotal = 0;
        int[] counts = new int[6];

        for (Card card : player.getCardsIn(ZoneType.Battlefield, false)) {
            if (card.isPhasedOut() && (!includeNormalPhasing || !phasesInNormally.test(card))) {
                continue;
            }
            int maxProduced = 0;
            for (SpellAbility manaAbility : card.getManaAbilities()) {
                manaAbility.setActivatingPlayer(card.getController());
                int manaCost = manaAbility.getPayCosts().getTotalMana().getCMC();
                maxProduced = Math.max(maxProduced, manaAbility.amountOfManaGenerated(true) - manaCost);
                for (AbilityManaPart manaPart : manaAbility.getAllManaParts()) {
                    for (String produced : manaPart.mana(manaAbility).split(" ")) {
                        // TODO: Account for "Any" and conditional production more precisely.
                        int index = ManaAtom.getIndexFromName(produced);
                        if (index != -1) {
                            counts[index]++;
                        }
                    }
                }
            }
            maxTotal += maxProduced;
        }

        int value = 0;
        for (int i = 0; i < counts.length; i++) {
            value += Math.min(counts[i], statistics.maxPips[i]) * 100;
        }
        value += Math.min(maxTotal, statistics.maxCost) * 100;
        return value + Math.max(0, maxTotal - statistics.maxCost) * 5;
    }

    static int scoreManaProducingCard(final Card card) {
        int score = 0;
        for (SpellAbility ability : card.getSpellAbilities()) {
            ability.setActivatingPlayer(card.getController());
            if (ability.isManaAbility()) {
                score += ability.calculateScoreForManaAbility();
                // TODO: Account for TriggersWhenSpent when the payment context makes that useful.
            } else if (!ability.isTrigger() && ability.isPossible()) {
                score += NON_MANA_USE_SCORE;
            }
        }
        if (card.isCreature()) {
            score += CombatUtil.canAttack(card) ? NON_MANA_USE_SCORE : 0;
            score += CombatUtil.canBlock(card) ? NON_MANA_USE_SCORE : 0;
        }
        return score;
    }

    static int[] futureColorPips(final Player player, final SpellAbility paidFor) {
        CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        hand.remove(paidFor.getHostCard());
        return AiDeckStatistics.fromCards(hand).maxPips;
    }
}
