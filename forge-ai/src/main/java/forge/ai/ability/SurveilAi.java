package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

public class SurveilAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#doTriggerAINoCost(forge.game.player.Player, forge.game.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        if (sa.usesTargeting()) { // It doesn't appear that Scry ever targets
            // ability is targeted
            sa.resetTargets();

            sa.getTargets().add(ai);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#chkAIDrawback(forge.game.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return doTriggerAINoCost(ai, sa, false);
    }

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.game.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
