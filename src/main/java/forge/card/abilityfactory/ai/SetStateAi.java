package forge.card.abilityfactory.ai;


import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

public class SetStateAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {
        // Gross generalization, but this always considers alternate
        // states more powerful
        return !sa.getSourceCard().isInAlternateState();
    }


    /* (non-Javadoc)
    * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
    */
    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }
}
