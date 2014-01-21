package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class GameLossAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Player opp = ai.getOpponent();
        if (opp.cantLose()) {
            return false;
        }

        // Only one SA Lose the Game card right now, which is Door to
        // Nothingness

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            sa.getTargets().add(opp);
        }

        // In general, don't return true.
        // But this card wins the game, I can make an exception for that
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        // Phage the Untouchable
        // (Final Fortune would need to attach it's delayed trigger to a
        // specific turn, which can't be done yet)

        if (!mandatory && ai.getOpponent().cantLose()) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            sa.getTargets().add(ai.getOpponent());
        }

        return true;
    }
}
