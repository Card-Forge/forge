package forge.card.ability.ai;


import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

public class MustAttackAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        // disabled for the AI for now. Only for Gideon Jura at this time.
        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {
        // AI should only activate this during Human's turn
        // TODO - implement AI
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {

        boolean chance;

        // TODO - implement AI
        chance = false;

        return chance;
    }
}
