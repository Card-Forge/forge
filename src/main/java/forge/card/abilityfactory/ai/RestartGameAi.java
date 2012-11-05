package forge.card.abilityfactory.ai;

import java.util.Map;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class RestartGameAi extends SpellAiLogic {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
     * (forge.game.player.Player, java.util.Map,
     * forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {
        // The only card that uses this is Karn Liberated
        
        // TODO Add Logic, check if AI is losing game state, or life
        
        // TODO Add Logic, check if any good cards will be available to be returned
        
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {          
        // This trigger AI is completely unused, but return true just in case
        return true;
    }
}