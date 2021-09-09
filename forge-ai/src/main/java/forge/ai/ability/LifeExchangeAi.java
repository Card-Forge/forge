package forge.ai.ability;

import forge.ai.AiAttackController;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

public class LifeExchangeAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
     * (forge.game.player.Player, java.util.Map,
     * forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final int myLife = aiPlayer.getLife();
        Player opponent = AiAttackController.choosePreferredDefenderPlayer(aiPlayer);
        final int hLife = opponent.getLife();

        if (!aiPlayer.canGainLife()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        /*
         * TODO - There is one card that takes two targets (Soul Conduit)
         * and one card that has a conditional (Psychic Transfer) that are
         * not currently handled
         */
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (opponent.canBeTargetedBy(sa)) {
                // never target self, that would be silly for exchange
                sa.getTargets().add(opponent);
                if (!opponent.canLoseLife()) {
                    return false;
                }
            }
        }

        // if life is in danger, always activate
        if ((myLife < 5) && (hLife > myLife)) {
            return true;
        }

        // cost includes sacrifice probably, so make sure it's worth it
        chance &= (hLife > (myLife + 8));

        return ((MyRandom.getRandom().nextFloat() < .6667) && chance);
    }

    /**
     * <p>
     * exchangeLifeDoTriggerAINoCost.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     *
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa,
    final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        if (tgt != null) {
            sa.resetTargets();
            if (sa.canTarget(opp) && (mandatory || ai.getLife() < opp.getLife())) {
                sa.getTargets().add(opp);
            } else {
                return false;
            }
        }
        return true;
    }

}
