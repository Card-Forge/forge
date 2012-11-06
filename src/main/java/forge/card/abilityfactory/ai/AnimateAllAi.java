package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class AnimateAllAi extends SpellAiLogic {
    
    @Override
    protected boolean canPlayAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        return false;
    } // end animateAllCanPlayAI()

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        return false;
    }

} // end class AbilityFactoryAnimate