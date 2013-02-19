package forge.card.ability.ai;

import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;

public class ChooseCardNameAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {

        if (sa.hasParam("AILogic")) {
            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
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
