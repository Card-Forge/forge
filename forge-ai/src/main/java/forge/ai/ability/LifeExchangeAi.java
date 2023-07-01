package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
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
        if (!aiPlayer.canGainLife()) {
            return false;
        }

        final int myLife = aiPlayer.getLife();
        final PlayerCollection targetableOpps = aiPlayer.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        final Player opponent = targetableOpps.max(PlayerPredicates.compareByLife());
        final int hLife = opponent == null ? 0 : opponent.getLife();

        // prevent run-away activations - first time will always return true
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        /*
         * TODO - There is one card that takes two targets (Soul Conduit)
         * and one card that has a conditional (Psychic Transfer) that are
         * not currently handled
         */
        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (opponent != null && opponent.canLoseLife()) {
                // never target self, that would be silly for exchange
                sa.getTargets().add(opponent);
            } else {
                return false;
            }
        }

        // if life is in danger, always activate
        if (myLife < 5 && hLife > myLife) {
            return true;
        }

        // cost includes sacrifice probably, so make sure it's worth it
        chance &= (hLife > (myLife + 8));

        return MyRandom.getRandom().nextFloat() < .6667 && chance;
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
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        Player opp = targetableOpps.max(PlayerPredicates.compareByLife());
        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (sa.canTarget(opp) && (mandatory || ai.getLife() < opp.getLife())) {
                sa.getTargets().add(opp);
                if (sa.canAddMoreTarget()) {
                    sa.getTargets().add(ai);
                }
            } else {
                return false;
            }
        }
        return true;
    }

}
