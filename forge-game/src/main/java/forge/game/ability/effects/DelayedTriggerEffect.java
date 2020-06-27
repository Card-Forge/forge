package forge.game.ability.effects;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

import java.util.Map;

public class DelayedTriggerEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("TriggerDescription")) {
            return sa.getParam("TriggerDescription");
        }

        return "";

    }

    @Override
    public void resolve(SpellAbility sa) {
        Map<String, String> mapParams = Maps.newHashMap(sa.getMapParams());
        mapParams.remove("Cost");

        if (mapParams.containsKey("SpellDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        String triggerRemembered = null;

        // Set Remembered
        if (sa.hasParam("RememberObjects")) {
            triggerRemembered = sa.getParam("RememberObjects");
        }

        final Trigger delTrig = TriggerHandler.parseTrigger(mapParams, sa.getHostCard(), true);

        if (triggerRemembered != null) {
            for (final String rem : triggerRemembered.split(",")) {
                for (final Object o : AbilityUtils.getDefinedObjects(sa.getHostCard(), rem, sa)) {
                    if (o instanceof SpellAbility) {
                        // "RememberObjects$ Remembered" don't remember spellability 
                        continue;
                    }
                    delTrig.addRemembered(o);
                }
            }
        }

        if (sa.hasParam("RememberNumber")) {
            for (final Object o : sa.getHostCard().getRemembered()) {
                if (o instanceof Integer) {
                    delTrig.addRemembered(o);
                }
            }
        }

        if (mapParams.containsKey("Execute") || sa.hasAdditionalAbility("Execute")) {
            SpellAbility overridingSA = sa.getAdditionalAbility("Execute");
            overridingSA.setActivatingPlayer(sa.getActivatingPlayer());
            overridingSA.setDeltrigActivatingPlayer(sa.getActivatingPlayer()); // ensure that the original activator can be restored later
            // Set Transform timestamp when the delayed trigger is created
            if (ApiType.SetState == overridingSA.getApi()) {
                overridingSA.setSVar("StoredTransform", String.valueOf(sa.getHostCard().getTransformedTimestamp()));
            }

            if (sa.hasParam("CopyTriggeringObjects")) {
                overridingSA.setTriggeringObjects(sa.getTriggeringObjects());
            }

            delTrig.setOverridingAbility(overridingSA);
        }
        final TriggerHandler trigHandler  = sa.getActivatingPlayer().getGame().getTriggerHandler();
        if (mapParams.containsKey("DelayedTriggerDefinedPlayer")) { // on sb's next turn
            Player p = Iterables.getFirst(AbilityUtils.getDefinedPlayers(sa.getHostCard(), mapParams.get("DelayedTriggerDefinedPlayer"), sa), null);
            trigHandler.registerPlayerDefinedDelayedTrigger(p, delTrig);
        } else if (mapParams.containsKey("ThisTurn")) {
            trigHandler.registerThisTurnDelayedTrigger(delTrig);
        } else {
            trigHandler.registerDelayedTrigger(delTrig);
        }
    }
}
