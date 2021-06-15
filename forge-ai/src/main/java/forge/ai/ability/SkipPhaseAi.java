package forge.ai.ability;

import forge.ai.AiAttackController;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

public class SkipPhaseAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        if (sa.usesTargeting()) {
            final Player opp = AiAttackController.choosePreferredDefenderPlayer(aiPlayer);
            if (sa.canTarget(opp)) {
                sa.resetTargets();
                sa.getTargets().add(opp);
            }
            else {
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return canPlayAI(aiPlayer, sa) || mandatory;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
