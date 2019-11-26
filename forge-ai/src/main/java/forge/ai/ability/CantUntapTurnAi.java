package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.CostPutCounter;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CantUntapTurnAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        if (sa.usesTargeting()) {
            CardCollection oppCards = ai.getOpponents().getCardsIn(ZoneType.Battlefield);

            CardCollection relevantToHold = CardLists.filter(oppCards,
                    Predicates.and(CardPredicates.Presets.TAPPED, new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    if (card.isCreature()) {
                        return true;
                    }
                    for (final SpellAbility ab : card.getSpellAbilities()) {
                        if (ab.isAbility() && (ab.getPayCosts() != null) && ab.getPayCosts().hasTapCost()) {
                            return true;
                        }
                    }
                    return false;
                }
            }));

            Card bestToTap = ComputerUtilCard.getBestAI(relevantToHold);
            Card validTarget = ComputerUtilCard.getBestAI(CardLists.filter(oppCards, CardPredicates.Presets.TAPPED));
            if (validTarget == null) {
                validTarget = ComputerUtilCard.getBestAI(oppCards);
            }

            if (bestToTap != null) {
                sa.getTargets().add(bestToTap);
                return true;
            } else if (sa.hasParam("Planeswalker")
                    && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostPutCounter.class)) {
                sa.getTargets().add(validTarget);
                return true;
            }

            return false;
        }


        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(aiPlayer, sa);
    }
}
