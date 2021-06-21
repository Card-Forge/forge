package forge.ai.ability;

import forge.ai.AiAttackController;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

public class SkipPhaseAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return targetPlayer(aiPlayer, sa, false);
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return targetPlayer(aiPlayer, sa, mandatory);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
    
    public boolean targetPlayer(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
            sa.resetTargets();
            if (sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            }
            else if (mandatory && sa.canTarget(ai)) {
                sa.getTargets().add(ai); 
            }
            else {
                return false;
            }
        }
        return true;
    }
}
