package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;

public class GameLossAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Player opp = ai.getOpponent();
        if (opp.cantLose()) {
            return false;
        }

        // Only one SA Lose the Game card right now, which is Door to
        // Nothingness

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            tgt.addTarget(opp);
        }

        // In general, don't return true.
        // But this card wins the game, I can make an exception for that
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {

        // Phage the Untouchable
        // (Final Fortune would need to attach it's delayed trigger to a
        // specific turn, which can't be done yet)

        if (!mandatory && ai.getOpponent().cantLose()) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            tgt.addTarget(ai.getOpponent());
        }

        return true;
    }
}
