package forge.ai.ability;

import forge.game.ability.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class FlipACoinAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {

        if (sa.hasParam("AILogic")) {
            if (sa.getParam("AILogic").equals("Never")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }
}
