package forge.card.abilityfactory.ai;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.phase.CombatUtil;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;

public class StoreSVarAi extends SpellAiLogic {
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        //Tree of Redemption

        final Card source = sa.getSourceCard();
        if (ComputerUtil.waitForBlocking(sa) || ai.getLife() + 1 >= source.getNetDefense() 
                || (ai.getLife() > 5 && !CombatUtil.lifeInSeriousDanger(ai, Singletons.getModel().getGame().getCombat()))) {
            return false;
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    };


    
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {

        return true;
    }

}