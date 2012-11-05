package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class AnimateAllAi extends SpellAiLogic {
    
    @Override
    public boolean canPlayAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        boolean useAbility = false;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            useAbility &= subAb.chkAIDrawback();
        }

        return useAbility;
    } // end animateAllCanPlayAI()

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        boolean chance = false;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance || mandatory;
    }

} // end class AbilityFactoryAnimate