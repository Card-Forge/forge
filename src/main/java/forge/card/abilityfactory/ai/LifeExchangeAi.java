package forge.card.abilityfactory.ai;

import java.util.Random;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.util.MyRandom;

public class LifeExchangeAi extends SpellAiLogic {

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
        final Random r = MyRandom.getRandom();
        final int myLife = aiPlayer.getLife();
        Player opponent = aiPlayer.getOpponent();
        final int hLife = opponent.getLife();

        if (!aiPlayer.canGainLife()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        /*
         * TODO - There is one card that takes two targets (Soul Conduit)
         * and one card that has a conditional (Psychic Transfer) that are
         * not currently handled
         */
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (opponent.canBeTargetedBy(sa)) {
                // never target self, that would be silly for exchange
                tgt.addTarget(opponent);
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

        return ((r.nextFloat() < .6667) && chance);
    }


}