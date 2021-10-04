package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class AnimateAllAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");

        if ("CreatureAdvantage".equals(logic) && !aiPlayer.getCreaturesInPlay().isEmpty()) {
            // TODO: improve this or implement a better logic for abilities like Oko, the Trickster ultimate
            for (Card c : aiPlayer.getCreaturesInPlay()) {
                if (ComputerUtilCard.doesCreatureAttackAI(aiPlayer, c)) {
                    return true;
                }
            }
        }

        return "Always".equals(logic);
    } // end animateAllCanPlayAI()

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(aiPlayer, sa);
    }

}
