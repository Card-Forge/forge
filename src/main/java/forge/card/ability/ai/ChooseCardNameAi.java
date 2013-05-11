package forge.card.ability.ai;

import forge.Card;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilMana;
import forge.game.player.AIPlayer;

public class ChooseCardNameAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        Card source = sa.getSourceCard();
        if (sa.hasParam("AILogic")) {
            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }

            String logic = sa.getParam("AILogic");
            if (logic.equals("MomirAvatar")) {
                // Set PayX here to maximum value.
                int tokenSize = ComputerUtilMana.determineLeftoverMana(sa, ai);
                
                // Some basic strategy for Momir
                if (tokenSize >= 11) {
                    tokenSize = 11;
                } else if (tokenSize >= 9) {
                    tokenSize = 9;
                } else if (tokenSize >= 8) {
                    tokenSize = 8;
                } else if (tokenSize >= 6) {
                    tokenSize = 6;
                } else if (tokenSize >= 4) {
                    tokenSize = 4; 
                } else if (tokenSize >= 2) {
                   tokenSize = 2;
                } else {
                    return false;
                }
                source.setSVar("PayX", Integer.toString(tokenSize));
            }

            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgt.resetTargets();
                if (tgt.canOnlyTgtOpponent()) {
                    tgt.addTarget(ai.getOpponent());
                } else {
                    tgt.addTarget(ai);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        // TODO - there is no AILogic implemented yet
        return false;
    }

}
