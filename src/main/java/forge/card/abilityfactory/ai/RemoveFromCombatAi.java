package forge.card.abilityfactory.ai;


import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class RemoveFromCombatAi extends SpellAiLogic {
    
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // disabled for the AI for now. Only for Gideon Jura at this time.
        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // AI should only activate this during Human's turn

        // TODO - implement AI
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance;

        // TODO - implement AI
        chance = false;

        return chance;
    }
}