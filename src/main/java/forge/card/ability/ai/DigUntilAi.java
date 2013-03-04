package forge.card.ability.ai;

import java.util.Random;

import forge.Card;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilMana;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DigUntilAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        Card source = sa.getSourceCard();
        double chance = .4; // 40 percent chance with instant speed stuff
        if (SpellAbilityAi.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        final Random r = MyRandom.getRandom();
        final boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        final Target tgt = sa.getTarget();
        Player libraryOwner = ai;
        Player opp = ai.getOpponent();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (!opp.canBeTargetedBy(sa)) {
                return false;
            } else {
                sa.getTarget().addTarget(opp);
            }
            libraryOwner = opp;
        }

        final String num = sa.getParam("Amount");
        if ((num != null) && num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            if (!(sa instanceof AbilitySub) || source.getSVar("PayX").equals("")) {
                int numCards = ComputerUtilMana.determineLeftoverMana(sa, ai);
                if (numCards <= 0) {
                    return false;
                }
                source.setSVar("PayX", Integer.toString(numCards));
            }
        }

        // return false if nothing to dig into
        if (libraryOwner.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (sa.isCurse()) {
                sa.getTarget().addTarget(ai.getOpponent());
            } else {
                sa.getTarget().addTarget(ai);
            }
        }

        return true;
    }
}
