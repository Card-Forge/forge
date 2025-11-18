package forge.ai.ability;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class EarthbendAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        CardCollection lands = aiPlayer.getLandsInPlay();
        if (lands.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        CardCollection fetchLands = CardLists.filter(lands, c -> {
                    for (final SpellAbility ability : c.getAllSpellAbilities()) {
                        if (ability.isActivatedAbility()) {
                            final Cost cost = ability.getPayCosts();
                            for (final CostPart part : cost.getCostParts()) {
                                if (!(part instanceof CostSacrifice)) {
                                    continue;
                                }
                                CostSacrifice sacCost = (CostSacrifice) part;
                                if (sacCost.payCostFromSource() && ComputerUtilCost.canPayCost(ability, c.getController(), false)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                });

        Card tgtLand = null;

        if (!fetchLands.isEmpty()) {
            // Prioritize fetchlands as they can be reused later
            tgtLand = ComputerUtilCard.getBestLandToAnimate(fetchLands);
        } else {
            tgtLand = ComputerUtilCard.getBestLandToAnimate(lands);
        }

        sa.getTargets().add(tgtLand);

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        AiAbilityDecision decision = canPlay(aiPlayer, sa);
        if (decision.willingToPlay() || mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

}
