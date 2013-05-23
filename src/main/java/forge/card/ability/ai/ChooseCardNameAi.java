package forge.card.ability.ai;

import forge.Card;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

public class ChooseCardNameAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        Card source = sa.getSourceCard();
        if (sa.hasParam("AILogic")) {
            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }

            String logic = sa.getParam("AILogic");
            if (logic.equals("MomirAvatar")) {
                if (source.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN1)) {
                    return false;
                }
                // Set PayX here to maximum value.
                int tokenSize = ComputerUtilMana.determineLeftoverMana(sa, ai);
                
             // Some basic strategy for Momir
                if (tokenSize < 2) {
                    return false;
                }

                if (tokenSize > 11) {
                    tokenSize = 11;
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
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // TODO - there is no AILogic implemented yet
        return false;
    }

}
