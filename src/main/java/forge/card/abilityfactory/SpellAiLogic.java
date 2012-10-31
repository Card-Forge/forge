package forge.card.abilityfactory;

import java.util.Map;

import forge.card.spellability.SpellAbility;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;

public abstract class SpellAiLogic {
    
    public abstract boolean canPlayAI(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa);
    
    public boolean doTriggerAI(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory){
        if (!ComputerUtil.canPayCost(sa, aiPlayer) && !mandatory) {
            // payment is usually not mandatory
            return false;
        }
        return doTriggerAINoCost(aiPlayer, params, sa, mandatory);
    }
    
    @SuppressWarnings("unused") // 'unused' parameters are used by overloads
    public boolean doTriggerAINoCost(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory) {
        return canPlayAI(aiPlayer, params, sa);
    }
    
    // consider safe
    @SuppressWarnings("unused") // 'unused' parameters are used by overloads
    public boolean chkAIDrawback(final Map<String, String> params, final SpellAbility sa, final Player aiPlayer) { return true; }
}