package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;

public class ChooseCardNameAi extends SpellAiLogic {
    
    
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }
    
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        
        if (params.containsKey("AILogic")) {
            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }
            
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgt.resetTargets();
                if (tgt.canOnlyTgtOpponent()) {
                    tgt.addTarget(ai.getOpponent());
                } else {
                    tgt.addTarget(ai);
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        // TODO - there is no AILogic implemented yet
        return false;
    }

}