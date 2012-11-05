package forge.card.abilityfactory;

import java.util.Map;

import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;

public abstract class SpellAiLogic {
    
    public abstract boolean canPlayAI(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa);
    
    public final boolean doTriggerAI(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory){
        if (!ComputerUtil.canPayCost(sa, aiPlayer) && !mandatory) {
            // payment is usually not mandatory
            return false;
        }
        
        boolean chance = doTriggerNoCostWithSubs(aiPlayer, params, sa, mandatory); 
        return chance;
    }
    
    public final boolean doTriggerNoCostWithSubs(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory)
    {
        boolean chance = doTriggerAINoCost(aiPlayer, params, sa, mandatory); 
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }
        return chance;
    }
    
    protected boolean doTriggerAINoCost(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory) {
        return canPlayAI(aiPlayer, params, sa) || mandatory;
    }
    
    // consider safe
    @SuppressWarnings("unused") // 'unused' parameters are used by overloads
    public boolean chkAIDrawback(final Map<String, String> params, final SpellAbility sa, final Player aiPlayer) { return true; }
}