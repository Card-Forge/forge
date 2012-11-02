package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.util.MyRandom;

public class DrainManaAi extends SpellAiLogic {
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
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
            final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);

            if (!defined.contains(opp)) {
                return false;
            }
        } else {
            tgt.resetTargets();
            tgt.addTarget(opp);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    @Override
    public boolean doTriggerAI(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai)) {
            return false;
        }
        final Player opp = ai.getOpponent();

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        if (null == tgt) {
            if (mandatory) {
                return true;
            } else {
                final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);

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
    public boolean chkAIDrawback(Map<String,String> params, SpellAbility sa, Player ai) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            final ArrayList<Player> defined = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);

            if (defined.contains(ai)) {
                return false;
            }
        } else {
            tgt.resetTargets();
            tgt.addTarget(ai.getOpponent());
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }
}