package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
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
        }
        if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }

        return "";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        // CR 603.12a if the trigger event or events occur multiple times during the resolution of the spell or ability that created it,
        // the reflexive triggered ability will trigger once for each of those times
        int amt = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("TriggerAmount", "1"), sa);
        if (amt <= 0) {
            return;
        }

        Map<String, String> mapParams = Maps.newHashMap(sa.getMapParams());
        mapParams.put("Mode", TriggerType.Immediate.name());
        if (mapParams.containsKey("SpellDescription") && !mapParams.containsKey("TriggerDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
        }
        mapParams.remove("SpellDescription");
        mapParams.remove("Cost");

        SpellAbility overridingSA = null;
        if (sa.hasAdditionalAbility("Execute")) {
            overridingSA = sa.getAdditionalAbility("Execute").copy(host, sa.getActivatingPlayer(), false);
            // need to set Parent to null, otherwise it might have wrong root ability
            if (overridingSA instanceof AbilitySub) {
                ((AbilitySub)overridingSA).setParent(null);
            }
        }

        List<GameEntity> remember = null;
        if (sa.hasParam("RememberObjects")) {
            remember = AbilityUtils.getDefinedEntities(host, sa.getParam("RememberObjects").split(" & "), sa);
        }

        for (int i = 0; i < amt; i++) {
            final Trigger immediateTrig = TriggerHandler.parseTrigger(mapParams, host, sa.isIntrinsic(), null);
            immediateTrig.setSpawningAbility(sa.copy(host, true));
            if (overridingSA != null) {
                immediateTrig.setOverridingAbility(overridingSA);
            }

            if (remember != null) {
                immediateTrig.addRemembered(
                        sa.hasParam("RememberEach") ? remember.get(i) : remember
                );
            }

            if (sa.hasParam("RememberSVarAmount")) {
                immediateTrig.addRemembered(
                        AbilityUtils.calculateAmount(host, sa.getSVar(sa.getParam("RememberSVarAmount")), sa)
                );
            }

            // Instead of registering this, add to the delayed triggers as an immediate trigger type? Which means it'll fire as soon as possible
            game.getTriggerHandler().registerDelayedTrigger(immediateTrig);
        }
    }
}
