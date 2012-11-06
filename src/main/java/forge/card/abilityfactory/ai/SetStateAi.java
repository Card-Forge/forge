package forge.card.abilityfactory.ai;

import java.util.Map;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class SetStateAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        return false;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        // Gross generalization, but this always considers alternate
        // states more powerful
        return !sa.getSourceCard().isInAlternateState();
    }


    /* (non-Javadoc)
    * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
    */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, Map<String, String> params, SpellAbility sa, boolean mandatory) {
        return true;
    }
}