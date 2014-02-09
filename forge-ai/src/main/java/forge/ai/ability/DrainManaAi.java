package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

import java.util.List;
import java.util.Random;

public class DrainManaAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final Player opp = ai.getOpponent();
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (tgt == null) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap
            // those.
            final List<Player> defined = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

            if (!defined.contains(opp)) {
                return false;
            }
        } else {
            sa.resetTargets();
            sa.getTargets().add(opp);
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Player opp = ai.getOpponent();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        if (null == tgt) {
            if (mandatory) {
                return true;
            } else {
                final List<Player> defined = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

                if (!defined.contains(opp)) {
                    return false;
                }
            }

            return true;
        } else {
            sa.resetTargets();
            sa.getTargets().add(opp);
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        boolean randomReturn = true;

        if (tgt == null) {
            final List<Player> defined = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

            if (defined.contains(ai)) {
                return false;
            }
        } else {
            sa.resetTargets();
            sa.getTargets().add(ai.getOpponent());
        }

        return randomReturn;
    }
}
