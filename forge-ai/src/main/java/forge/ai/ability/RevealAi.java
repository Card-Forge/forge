package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;

public class RevealAi extends RevealAiBase {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!revealHandTargetAI(ai, sa, false)) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }

        if (playReusable(ai, sa)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return super.checkApiLogic(ai, sa);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // logic to see if it should reveal Miracle Card
        if (sa.hasParam("MiracleCost")) {
            final Card c = sa.getHostCard();
            for (SpellAbility s : c.getBasicSpells()) {
                Spell spell = (Spell) s;
                s.setActivatingPlayer(ai);
                // timing restrictions still apply
                if (!s.getRestrictions().checkTimingRestrictions(c, s))
                    continue;

                spell = (Spell) spell.copyWithDefinedCost(new Cost(sa.getParam("MiracleCost"), false));

                AiPlayDecision decision = ((PlayerControllerAi) ai.getController()).getAi()
                        .canPlayFromEffectAI(spell, false, false);

                if (AiPlayDecision.WillPlay == decision) {
                    return new AiAbilityDecision(100, decision);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if ("Kefnet".equals(sa.getParam("AILogic"))) {
            final Card c = Iterables.getFirst(
                AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("RevealDefined"), sa), null
            );

            if (c == null || (!c.isInstant() && !c.isSorcery())) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            for (SpellAbility s : c.getBasicSpells()) {
                Spell spell = (Spell) s.copy(ai);
                // timing restrictions still apply
                if (!spell.getRestrictions().checkTimingRestrictions(c, spell))
                    continue;

                // use hard coded reduce cost
                spell.putParam("ReduceCost", "2");
                AiPlayDecision decision = ((PlayerControllerAi) ai.getController()).getAi()
                        .canPlayFromEffectAI(spell, false, false);

                if (AiPlayDecision.WillPlay == decision) {
                    return new AiAbilityDecision(100, decision);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (!revealHandTargetAI(ai, sa, mandatory)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

}
