package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;

public class ImmediateTriggerEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("TriggerDescription")) {
            return sa.getParam("TriggerDescription");
        } else if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }

        return "";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        Map<String, String> mapParams = Maps.newHashMap(sa.getMapParams());

        if (mapParams.containsKey("SpellDescription") && !mapParams.containsKey("TriggerDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
        }
        mapParams.remove("SpellDescription");
        mapParams.remove("Cost");

        mapParams.put("Mode", TriggerType.Immediate.name());

        final Trigger immediateTrig = TriggerHandler.parseTrigger(mapParams, host, sa.isIntrinsic(), null);
        immediateTrig.setSpawningAbility(sa.copy(host, true));

        // Need to copy paid costs

        if (sa.hasParam("RememberObjects")) {
            for (final String rem : sa.getParam("RememberObjects").split(",")) {
                for (final Object o : AbilityUtils.getDefinedEntities(host, rem, sa)) {
                    immediateTrig.addRemembered(o);
                }
            }
        }

        if (sa.hasParam("RememberSVarAmount")) {
            immediateTrig.addRemembered(AbilityUtils.calculateAmount(host,
                    sa.getSVar(sa.getParam("RememberSVarAmount")), sa));
        }

        if (sa.hasAdditionalAbility("Execute")) {
            SpellAbility overridingSA = sa.getAdditionalAbility("Execute").copy(host, sa.getActivatingPlayer(), false);
            // need to set Parent to null, otherwise it might have wrong root ability
            if (overridingSA instanceof AbilitySub) {
                ((AbilitySub)overridingSA).setParent(null);
            }

            immediateTrig.setOverridingAbility(overridingSA);
        }

        // Instead of registering this, add to the delayed triggers as an immediate trigger type? Which means it'll fire as soon as possible
        game.getTriggerHandler().registerDelayedTrigger(immediateTrig);
    }
}
