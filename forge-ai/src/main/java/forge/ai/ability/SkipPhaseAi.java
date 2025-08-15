package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiAttackController;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class SkipPhaseAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        return targetPlayer(aiPlayer, sa, false);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return targetPlayer(aiPlayer, sa, mandatory);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
    
    private AiAbilityDecision targetPlayer(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
            sa.resetTargets();
            if (sa.canTarget(opp)) {
                if (!mandatory) {
                    // TODO check wouldLoseLife + some Effect with Duration isn't already active
                }
                sa.getTargets().add(opp);
            }
            else if (mandatory && sa.canTarget(ai)) {
                sa.getTargets().add(ai); 
            }
            else {
                return new AiAbilityDecision(0, forge.ai.AiPlayDecision.CantPlayAi);
            }
        }
        return new AiAbilityDecision(100, forge.ai.AiPlayDecision.WillPlay);
    }
}
