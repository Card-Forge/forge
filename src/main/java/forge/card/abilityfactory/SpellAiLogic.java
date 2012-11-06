package forge.card.abilityfactory;

import java.util.Map;

import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;

public abstract class SpellAiLogic {

    public final boolean canPlayAIWithSubs(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa) {
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback()) {
            return false;
        }
        return canPlayAI(aiPlayer, params, sa);
    }
    
    public abstract boolean canPlayAI(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa);
    
    public final boolean doTriggerAI(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory){
        if (!ComputerUtil.canPayCost(sa, aiPlayer) && !mandatory) {
            return false;
        }

        return doTriggerNoCostWithSubs(aiPlayer, params, sa, mandatory);
    }
    
    public final boolean doTriggerNoCostWithSubs(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory)
    {
        if (!doTriggerAINoCost(aiPlayer, params, sa, mandatory)) {
            return false;
        }
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback() && !mandatory) {
            return false;
        }
        return true;
    }
    
    protected boolean doTriggerAINoCost(final Player aiPlayer, final Map<String, String> params, final SpellAbility sa, final boolean mandatory) {
        return canPlayAI(aiPlayer, params, sa) || mandatory;
    }
    
    public boolean chkAIDrawback(final Map<String, String> params, final SpellAbility sa, final Player aiPlayer) {
        return true;
    }
}