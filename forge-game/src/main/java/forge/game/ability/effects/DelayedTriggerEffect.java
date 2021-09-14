package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardUtil;
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
        mapParams.remove("Cost");

        if (mapParams.containsKey("SpellDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        // in case the card moved before the delayed trigger can be created, need to check the latest card state for right timestamp
        Card gameCard = game.getCardState(host);
        Card lki = CardUtil.getLKICopy(gameCard);
        lki.clearControllers();
        lki.setOwner(sa.getActivatingPlayer());
        final Trigger delTrig = TriggerHandler.parseTrigger(mapParams, lki, sa.isIntrinsic(), null);
        delTrig.setSpawningAbility(sa.copy(lki, sa.getActivatingPlayer(), true));

        if (sa.hasParam("RememberObjects")) {
            for (final String rem : sa.getParam("RememberObjects").split(",")) {
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

        if (sa.hasAdditionalAbility("Execute")) {
            SpellAbility overridingSA = sa.getAdditionalAbility("Execute").copy(lki, sa.getActivatingPlayer(), false);
            // need to reset the parent, additionalAbility does set it to this
            if (overridingSA instanceof AbilitySub) {
                ((AbilitySub)overridingSA).setParent(null);
            }
            // Set Transform timestamp when the delayed trigger is created
            if (ApiType.SetState == overridingSA.getApi()) {
                overridingSA.setSVar("StoredTransform", String.valueOf(sa.getHostCard().getTransformedTimestamp()));
            }

            if (sa.hasParam("CopyTriggeringObjects")) {
                overridingSA.setTriggeringObjects(sa.getTriggeringObjects());
            }

            delTrig.setOverridingAbility(overridingSA);
        }
        final TriggerHandler trigHandler  = game.getTriggerHandler();
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
