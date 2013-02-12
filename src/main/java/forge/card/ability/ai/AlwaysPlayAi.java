package forge.card.ability.ai;


import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

public class AlwaysPlayAi extends SpellAiLogic {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        return true;
    }
}
