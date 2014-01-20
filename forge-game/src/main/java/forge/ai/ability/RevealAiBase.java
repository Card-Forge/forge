package forge.ai.ability;


import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public abstract class RevealAiBase extends SpellAbilityAi {

    protected  boolean revealHandTargetAI(final Player ai, final SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        Player opp = ai.getOpponent();
        final int humanHandSize = opp.getCardsIn(ZoneType.Hand).size();

        if (tgt != null) {
            // ability is targeted
            sa.resetTargets();

            final boolean canTgtHuman = opp.canBeTargetedBy(sa);

            if (!canTgtHuman || (humanHandSize == 0)) {
                return false;
            } else {
                sa.getTargets().add(opp);
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
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        revealHandTargetAI(ai, sa);
        return true;
    }
}
