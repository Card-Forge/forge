package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

public class DayTimeAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();

        if ((sa.getHostCard().isCreature() && sa.getPayCosts().hasTapCost()) || sa.getPayCosts().hasManaCost()) {
            // If it involves a cost that may put us at a disadvantage, better activate before own turn if possible
            if (!SpellAbilityAi.isSorcerySpeed(sa)) {
                return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == aiPlayer;
            }
        }

        // TODO: more logic for when it may not be desirable to do this. Special logic for Celestus?
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true; // TODO: more logic if it's ever a bad idea to trigger this (when non-mandatory)
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
