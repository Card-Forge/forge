package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
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
            } else if (sa.getParam("AILogic").equals("PhaseOut")) {
            	if (!ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa).contains(sa.getHostCard())) {
            		return false;
            	}
            }
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }
}
