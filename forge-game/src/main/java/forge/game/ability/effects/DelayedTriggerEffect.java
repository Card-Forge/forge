package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

public class DelayedTriggerEffect extends SpellAbilityEffect {

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
        Map<String, String> mapParams = Maps.newHashMap(sa.getMapParams());

        if (mapParams.containsKey("SpellDescription") && !mapParams.containsKey("TriggerDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
        }
        mapParams.remove("SpellDescription");
        mapParams.remove("Cost");

        final Trigger delTrig = TriggerHandler.parseTrigger(mapParams, host, sa.isIntrinsic(), null);
        delTrig.setSpawningAbility(sa.copy(host, true));
        delTrig.setActiveZone(null);

        if (sa.hasParam("RememberObjects")) {
            delTrig.addRemembered(
                    AbilityUtils.getDefinedEntities(host, sa.getParam("RememberObjects").split(" & "), sa)
            );
        }

        if (sa.hasParam("RememberNumber")) {
            for (final Object o : host.getRemembered()) {
                if (o instanceof Integer) {
                    delTrig.addRemembered(o);
                }
            }
        }

        if (sa.hasParam("RememberSVarAmount")) {
            delTrig.addRemembered(AbilityUtils.calculateAmount(host, sa.getSVar(sa.getParam("RememberSVarAmount")), sa));
        }

        if (sa.hasAdditionalAbility("Execute")) {
            SpellAbility overridingSA = sa.getAdditionalAbility("Execute").copy(host, sa.getActivatingPlayer(), false);
            // need to reset the parent, additionalAbility does set it to this
            if (overridingSA instanceof AbilitySub) {
                ((AbilitySub)overridingSA).setParent(null);
            }
            // Set Transform timestamp when the delayed trigger is created
            if (ApiType.SetState == overridingSA.getApi()) {
                overridingSA.setSVar("StoredTransform", String.valueOf(host.getTransformedTimestamp()));
            }

            delTrig.setOverridingAbility(overridingSA);
        }
        final TriggerHandler trigHandler  = game.getTriggerHandler();
        if (mapParams.containsKey("DelayedTriggerDefinedPlayer")) { // on sb's next turn
            Player p = Iterables.getFirst(AbilityUtils.getDefinedPlayers(host, mapParams.get("DelayedTriggerDefinedPlayer"), sa), null);
            trigHandler.registerPlayerDefinedDelayedTrigger(p, delTrig);
        } else if (mapParams.containsKey("ThisTurn")) {
            trigHandler.registerThisTurnDelayedTrigger(delTrig);
        } else if (mapParams.containsKey("NextTurn")) {
            final GameCommand nextTurnTrig = new GameCommand() {
                private static final long serialVersionUID = -5861518814760561373L;

                @Override
                public void run() {
                    trigHandler.registerThisTurnDelayedTrigger(delTrig);
                }
            };
            game.getCleanup().addUntil(nextTurnTrig);
        }  else if (mapParams.containsKey("UpcomingTurn")) {
            final GameCommand upcomingTurnTrig = new GameCommand() {
                private static final long serialVersionUID = -5860518814760461373L;

                @Override
                public void run() {
                    trigHandler.registerDelayedTrigger(delTrig);

                }
            };
            game.getCleanup().addUntil(upcomingTurnTrig);
        } else {
            trigHandler.registerDelayedTrigger(delTrig);
        }
    }
}
