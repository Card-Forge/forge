package forge.ai.ability;

import java.util.Random;

import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

public class RevealAi extends RevealAiBase {

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        // we can reuse this function here...
        final boolean bFlag = revealHandTargetAI(ai, sa/* , true, false */);

        if (!bFlag) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (SpellAbilityAi.playReusable(ai, sa)) {
            randomReturn = true;
        }
        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // logic to see if it should reveal Mircacle Card
        if (sa.hasParam("MiracleCost")) {
            final Card c = sa.getHostCard();
            for (SpellAbility s : c.getBasicSpells()) {
                Spell spell = (Spell) s;
                s.setActivatingPlayer(ai);
                // timing restrictions still apply
                if (!s.getRestrictions().checkTimingRestrictions(c, s))
                    continue;

                spell = (Spell) spell.copyWithDefinedCost(new Cost(sa.getParam("MiracleCost"), false));

                if (AiPlayDecision.WillPlay == ((PlayerControllerAi) ai.getController()).getAi()
                        .canPlayFromEffectAI(spell, false, false)) {
                    return true;
                }
            }
            return false;
        }

        if (!revealHandTargetAI(ai, sa/*, false, mandatory*/)) {
            return false;
        }

        return true;
    }

}
