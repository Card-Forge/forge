package forge.ai.ability;

import java.util.List;

import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

public class DrainManaAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        final Card source = sa.getHostCard();
        final Player opp = ai.getWeakestOpponent();
        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (!sa.usesTargeting()) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap those.
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
        final Player opp = ai.getWeakestOpponent();

        final Card source = sa.getHostCard();

        if (!sa.usesTargeting()) {
            if (mandatory) {
                return true;
            } else {
                final List<Player> defined = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

                return defined.contains(opp);
            }
        } else {
            sa.resetTargets();
            sa.getTargets().add(opp);
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Card source = sa.getHostCard();

        boolean randomReturn = true;

        if (!sa.usesTargeting()) {
            final List<Player> defined = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

            if (defined.contains(ai)) {
                return false;
            }
        } else {
            sa.resetTargets();
            sa.getTargets().add(ai.getWeakestOpponent());
        }

        return randomReturn;
    }
}
