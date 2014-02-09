package forge.ai.ability;


import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class DamageEachAi extends DamageAiBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt != null && sa.canTarget(ai.getOpponent())) {
            sa.resetTargets();
            sa.getTargets().add(ai.getOpponent());
        }

        final String damage = sa.getParam("NumDmg");
        final int iDmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);
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
