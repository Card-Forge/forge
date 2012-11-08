package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Random;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.util.MyRandom;

public class DrainManaAi extends SpellAiLogic {
    
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Player opp = ai.getOpponent();
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (tgt == null) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap
            // those.
            final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, sa.getParam("Defined"), sa);

            if (!defined.contains(opp)) {
                return false;
            }
        } else {
            tgt.resetTargets();
            tgt.addTarget(opp);
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Player opp = ai.getOpponent();

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        if (null == tgt) {
            if (mandatory) {
                return true;
            } else {
                final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, sa.getParam("Defined"), sa);

                if (!defined.contains(opp)) {
                    return false;
                }
            }

            return true;
        } else {
            tgt.resetTargets();
            tgt.addTarget(opp);
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, sa.getParam("Defined"), sa);

            if (defined.contains(ai)) {
                return false;
            }
        } else {
            tgt.resetTargets();
            tgt.addTarget(ai.getOpponent());
        }

        return randomReturn;
    }
}