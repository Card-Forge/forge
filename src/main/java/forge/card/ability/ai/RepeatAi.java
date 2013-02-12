package forge.card.ability.ai;


import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;

public class RepeatAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Player opp = ai.getOpponent();
        if (tgt != null) {
            if (!opp.canBeTargetedBy(sa)) {
                return false;
            }
            tgt.resetTargets();
            tgt.addTarget(opp);
        }
        return true;
    }
}
