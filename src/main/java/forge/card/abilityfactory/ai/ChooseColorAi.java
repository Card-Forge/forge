package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.util.MyRandom;

public class ChooseColorAi extends SpellAiLogic {
    
    
    @Override
    public boolean canPlayAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        if (!params.containsKey("AILogic")) {
            return false;
        }
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }
        return chance;
    }
    
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }
    
    
    @Override
    protected boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(ai, params, sa);
    }

}