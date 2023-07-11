package forge.ai.ability;

import com.google.common.collect.Iterables;

import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.game.ability.AbilityUtils;
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
        final boolean bFlag = revealHandTargetAI(ai, sa, false);

        if (!bFlag) {
            return false;
        }

        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (playReusable(ai, sa)) {
            randomReturn = true;
        }
        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // logic to see if it should reveal Miracle Card
        if (sa.hasParam("MiracleCost")) {
            final Card c = sa.getHostCard();
            for (SpellAbility s : c.getBasicSpells()) {
                Spell spell = (Spell) s;
                s.setActivatingPlayer(ai, true);
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

        if ("Kefnet".equals(sa.getParam("AILogic"))) {
            final Card c = Iterables.getFirst(
                AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("RevealDefined"), sa), null
            );

            if (c == null || (!c.isInstant() && !c.isSorcery())) {
                return false;
            }
            for (SpellAbility s : c.getBasicSpells()) {
                Spell spell = (Spell) s.copy(ai);
                // timing restrictions still apply
                if (!spell.getRestrictions().checkTimingRestrictions(c, spell))
                    continue;

                // use hard coded reduce cost
                spell.putParam("ReduceCost", "2");

                if (AiPlayDecision.WillPlay == ((PlayerControllerAi) ai.getController()).getAi()
                        .canPlayFromEffectAI(spell, false, false)) {
                    return true;
                }
            }
            return false;

        }

        if (!revealHandTargetAI(ai, sa, mandatory)) {
            return false;
        }

        return true;
    }

}
