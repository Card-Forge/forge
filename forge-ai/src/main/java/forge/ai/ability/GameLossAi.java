package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class GameLossAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Player opp = ai.getStrongestOpponent();
        if (opp.cantLose()) {
            return false;
        }

        // Only one SA Lose the Game card right now, which is Door to Nothingness

        if (sa.usesTargeting() && sa.canTarget(opp)) {
            sa.resetTargets();
            sa.getTargets().add(opp);
            return true;
        }

        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        Player loser = ai;
        
        // Phage the Untouchable
        if (ai.getGame().getCombat() != null) {
            loser = ai.getGame().getCombat().getDefenderPlayerByAttacker(sa.getHostCard());
        }

        if (!mandatory && (loser == ai || loser.cantLose())) {
            return false;
        }

        if (sa.usesTargeting() && sa.canTarget(loser)) {
            sa.resetTargets();
            sa.getTargets().add(loser);
        }

        return true;
    }
}
