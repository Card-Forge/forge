package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class GameLossAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        final Player opp = ai.getStrongestOpponent();
        if (opp.cantLose()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // Only one SA Lose the Game card right now, which is Door to Nothingness

        if (sa.usesTargeting() && sa.canTarget(opp)) {
            sa.resetTargets();
            sa.getTargets().add(opp);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        Player loser = ai;
        
        // Phage the Untouchable
        if (ai.getGame().getCombat() != null) {
            loser = ai.getGame().getCombat().getDefenderPlayerByAttacker(sa.getHostCard());
        }

        if (!mandatory && (loser == ai || loser.cantLose())) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (sa.usesTargeting() && sa.canTarget(loser)) {
            sa.resetTargets();
            sa.getTargets().add(loser);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
