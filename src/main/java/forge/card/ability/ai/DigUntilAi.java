package forge.card.ability.ai;

import java.util.Random;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DigUntilAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        double chance = .4; // 40 percent chance with instant speed stuff
        if (AbilityUtils.isSorcerySpeed(sa)) {
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
