package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

public class SacrificeAllAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");

        if (abCost != null) {
            // AI currently disabled for some costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, sa)) {
                return false;
            }
        }
        
        if (logic.equals("HellionEruption")) {
            if (ai.getCreaturesInPlay().size() < 5 || ai.getCreaturesInPlay().size() * 150 < ComputerUtilCard.evaluateCreatureList(ai.getCreaturesInPlay())) {
                return false;
            }
        }
        
        if (!DestroyAllAi.doMassRemovalLogic(ai, sa)) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        return ((MyRandom.getRandom().nextFloat() < .9667) && chance);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        //TODO: Add checks for bad outcome
        return true;
    }
}
