package forge.card.ability.ai;


import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;

public class RearrangeTopOfLibraryAi extends SpellAiLogic {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {

        final Target tgt = sa.getTarget();

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            Player opp = ai.getOpponent();
            final boolean canTgtHuman = opp.canBeTargetedBy(sa);

            if (!canTgtHuman) {
                return false;
            } else {
                tgt.addTarget(opp);
            }
        } else {
            // if it's just defined, no big deal
        }

        return false;
    }
}
