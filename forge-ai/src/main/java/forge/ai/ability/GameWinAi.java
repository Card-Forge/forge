package forge.ai.ability;


import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class GameWinAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        if (ai.cantWin()) {
            return false;
        }

        // TODO Check conditions are met on card (e.g. Coalition Victory)

        // TODO Consider likelihood of SA getting countered

        // In general, don't return true.
        // But this card wins the game, I can make an exception for that
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }

}
