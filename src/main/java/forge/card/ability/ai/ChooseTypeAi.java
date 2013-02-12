package forge.card.ability.ai;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;

public class ChooseTypeAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        if (!sa.hasParam("AILogic")) {
            return false;
        }

        return doTriggerAINoCost(aiPlayer, sa, false);
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai);
        } else {
            for (final Player p : AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa)) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

}
