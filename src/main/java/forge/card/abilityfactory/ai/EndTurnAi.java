package forge.card.abilityfactory.ai;

import java.util.Map;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class EndTurnAi extends SpellAiLogic  {

    @Override
    public boolean doTriggerAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        return mandatory;
    }
    
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) { return false; }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player aiPlayer, Map<String, String> params, SpellAbility sa) {
        return false;
    }
}
