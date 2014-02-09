package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class AnimateAllAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return false;
    } // end animateAllCanPlayAI()

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return false;
    }

} // end class AbilityFactoryAnimate
