package forge.ai.ability;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

public class ImmediateTriggerAi extends SpellAbilityAi {
    // TODO: this class is largely reused from DelayedTriggerAi, consider updating

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("Always")) {
            return true;
        }

        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return false;
        }

        trigsa.setActivatingPlayer(ai, true);

        if (trigsa instanceof AbilitySub) {
            return SpellApiToAi.Converter.get(trigsa.getApi()).chkDrawbackWithSubs(ai, (AbilitySub)trigsa);
        } else {
            return AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
        }
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // always add to stack, targeting happens after payment
        if (mandatory) {
            return true;
        }

        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return false;
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        trigsa.setActivatingPlayer(ai, true);

        return aic.doTrigger(trigsa, !"You".equals(sa.getParamOrDefault("OptionalDecider", "You")));
    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("Always")) {
            return true;
        }

        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return false;
        }

        if (logic.equals("WeakerCreature")) {
            Card ownCreature = ComputerUtilCard.getWorstCreatureAI(ai.getCreaturesInPlay());
            if (ownCreature == null) {
                return false;
            }

            int eval = ComputerUtilCard.evaluateCreature(ownCreature);
            boolean foundWorse = false;
            for (Card c : ai.getOpponents().getCreaturesInPlay()) {
                if (eval + 100 < ComputerUtilCard.evaluateCreature(c) ) {
                    foundWorse = true;
                    break;
                }
            }
            if (!foundWorse) {
                return false;
            }
        }

        trigsa.setActivatingPlayer(ai, true);
        return AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
    }

}
