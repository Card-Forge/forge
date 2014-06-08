package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ChooseDirectionAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final String logic = sa.getParam("AILogic");
        if (logic == null) {
            return false;
        } else {
            // TODO: default ai
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return canPlayAI(ai, sa);
    }
}
