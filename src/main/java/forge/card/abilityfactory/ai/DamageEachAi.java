package forge.card.abilityfactory.ai;


import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class DamageEachAi extends DamageAiBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Target tgt = sa.getTarget();

        if (tgt != null && sa.canTarget(ai.getOpponent())) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai.getOpponent());
        }

        final String damage = sa.getParam("NumDmg");
        final int iDmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa);
        return this.shouldTgtP(ai, sa, iDmg, false);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // check AI life before playing this drawback?
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        return canPlayAI(ai, sa);
    }

}
