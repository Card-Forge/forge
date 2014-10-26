package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class FlipACoinAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {

        if (sa.hasParam("AILogic")) {
        	String AILogic = sa.getParam("AILogic");
            if (AILogic.equals("Never")) {
                return false;
            } else if (AILogic.equals("PhaseOut")) {
            	if (!ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa).contains(sa.getHostCard())) {
            		return false;
            	}
            } else if (AILogic.equals("KillOrcs")) {
            	if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN) ) {
            		return false;
            	}
            	sa.resetTargets();
            	for (Card c : ai.getOpponent().getCreaturesInPlay()) {
            		if (sa.canTarget(c)) {
            			sa.getTargets().add(c);
            			return true;
            		}
            	}
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
