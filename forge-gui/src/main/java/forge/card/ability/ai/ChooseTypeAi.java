package forge.card.ability.ai;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

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
            for (final Player p : AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa)) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

}
