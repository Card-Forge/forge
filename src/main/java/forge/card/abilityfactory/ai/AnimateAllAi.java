package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

public class AnimateAllAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        return false;
    } // end animateAllCanPlayAI()

    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        return false;
    }

} // end class AbilityFactoryAnimate
