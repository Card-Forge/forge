package forge.ai.ability;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;

public class ImmediateTriggerAi extends SpellAbilityAi {
    // TODO: this class is largely reused from DelayedTriggerAi, consider updating

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("Always")) {
            return true;
        }

        SpellAbility trigsa = getTriggerSpellAbility(sa, ai);
        if (trigsa == null) {
            return false;
        }

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
            if (!sa.isReplacementAbility() || sa.getPayCosts() == null) {
                return true;
            }
        }

        String logic = sa.getParamOrDefault("AILogic", "");

        SpellAbility trigsa = getTriggerSpellAbility(sa, ai);
        if (trigsa == null) {
            return false;
        }

        if (logic.equals("MaxX")) {
            sa.setXManaCostPaid(ComputerUtilCost.getMaxXValue(sa, ai, true));
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();

        boolean optional;
        if (sa.isReplacementAbility()) {
            optional = sa.getPayCosts() != null;
        } else {
            optional = "You".equals(sa.getParamOrDefault("OptionalDecider", "You"));
        }

        return aic.doTrigger(trigsa, !optional);
    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("Always")) {
            return true;
        }

        SpellAbility trigsa = getTriggerSpellAbility(sa, ai);
        if (trigsa == null) {
            return false;
        }

        return AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
    }

    static protected SpellAbility getTriggerSpellAbility(SpellAbility sa, Player ai) {
        final Card host = sa.getHostCard();
        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return null;
        }

        // only for this check
        if (trigsa.isTrigger()) {
            return trigsa;
        }

        // the trigger will be overwritten later by the effect

        Map<String, String> mapParams = Maps.newHashMap(sa.getMapParams());

        mapParams.remove("Cost");

        if (mapParams.containsKey("SpellDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        mapParams.put("Mode", TriggerType.Immediate.name());

        final Trigger immediateTrig = TriggerHandler.parseTrigger(mapParams, host, sa.isIntrinsic(), null);
        immediateTrig.setSpawningAbility(sa); // no need for LKI there
        trigsa.setTrigger(immediateTrig);
        trigsa.setActivatingPlayer(ai);

        return trigsa;
    }
}
