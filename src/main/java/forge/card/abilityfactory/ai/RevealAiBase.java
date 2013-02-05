package forge.card.abilityfactory.ai;


import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public abstract class RevealAiBase extends SpellAiLogic {

    protected  boolean revealHandTargetAI(final Player ai, final SpellAbility sa) {
        final Target tgt = sa.getTarget();

        Player opp = ai.getOpponent();
        final int humanHandSize = opp.getCardsIn(ZoneType.Hand).size();

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            final boolean canTgtHuman = opp.canBeTargetedBy(sa);

            if (!canTgtHuman || (humanHandSize == 0)) {
                return false;
            } else {
                tgt.addTarget(opp);
            }
        } else {
            // if it's just defined, no big deal
        }

        return true;
    } // revealHandTargetAI()

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        revealHandTargetAI(ai, sa);
        return true;
    }
}
