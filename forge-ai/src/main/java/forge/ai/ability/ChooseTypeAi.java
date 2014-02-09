package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ChooseTypeAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        if (!sa.hasParam("AILogic")) {
            return false;
        }

        return doTriggerAINoCost(aiPlayer, sa, false);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            sa.resetTargets();
            sa.getTargets().add(ai);
        } else {
            for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa)) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

}
