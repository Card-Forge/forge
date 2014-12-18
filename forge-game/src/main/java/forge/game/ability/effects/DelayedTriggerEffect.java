package forge.game.ability.effects;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Iterables;

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

        Map<String, String> mapParams = new HashMap<String, String>();
        sa.copyParamsToMap(mapParams);
        if (mapParams.containsKey("Cost")) {
            mapParams.remove("Cost");
        }

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
            for (final Object o : AbilityUtils.getDefinedObjects(sa.getHostCard(), triggerRemembered, sa)) {
                if (o instanceof SpellAbility) {
                    // "RememberObjects$ Remembered" don't remember spellability 
                    continue;
                }
                delTrig.addRemembered(o);
            }
        }

        if (sa.hasParam("RememberNumber")) {
            for (final Object o : sa.getHostCard().getRemembered()) {
                if (o instanceof Integer) {
                    delTrig.addRemembered(o);
                }
            }
        }

        if (mapParams.containsKey("Execute")) {
            SpellAbility overridingSA = AbilityFactory.getAbility(sa.getSVar(mapParams.get("Execute")), sa.getHostCard());
            overridingSA.setActivatingPlayer(sa.getActivatingPlayer());
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
