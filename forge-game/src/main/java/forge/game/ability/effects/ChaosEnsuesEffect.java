package forge.game.ability.effects;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.*;

public class ChaosEnsuesEffect extends SpellAbilityEffect {
    /** 311.7. Each plane card has a triggered ability that triggers “Whenever chaos ensues.” These are called
    chaos abilities. Each one is indicated by a chaos symbol to the left of the ability, though the symbol
    itself has no special rules meaning. This ability triggers if the chaos symbol is rolled on the planar
    die (see rule 901.9b), if a resolving spell or ability says that chaos ensues, or if a resolving spell or
    ability states that chaos ensues for a particular object. In the last case, the chaos ability can trigger
    even if that plane card is still in the planar deck but revealed. A chaos ability is controlled by the
    current planar controller. **/

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if (game.getActivePlanes() == null) { // not a planechase game, nothing happens
            return;
        }

        Map<Integer, EnumSet<ZoneType>> tweakedTrigs = new HashMap<>();

        List<Card> affected = Lists.newArrayList();
        if (sa.hasParam("Defined")) {
            for (final Card c : AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa)) {
                for (Trigger t : c.getTriggers()) {
                    if (t.getMode() == TriggerType.ChaosEnsues) { // also allow current zone for any Defined
                        Set<ZoneType> zones = t.getActiveZone();
                        tweakedTrigs.put(t.getId(), EnumSet.copyOf(zones));
                        zones.add(c.getZone().getZoneType());
                        affected.add(c);
                        game.getTriggerHandler().registerOneTrigger(t);
                    }
                }
            }
            if (affected.isEmpty()) { // if no Defined has chaos ability, don't trigger non Defined
                return;
            }
        }

        Object cause = sa.hasParam("Cause") ? sa.getParam("Cause") : sa;
        dispatchChaosEnsues(activator, cause, affected.isEmpty() ? null : affected);

        for (Map.Entry<Integer, EnumSet<ZoneType>> e : tweakedTrigs.entrySet()) {
            for (Card c : affected) {
                for (Trigger t : c.getTriggers()) {
                    if (t.getId() == e.getKey()) {
                        EnumSet<ZoneType> zones = e.getValue();
                        t.setActiveZone(zones);
                    }
                }
            }
        }
    }

    public static void dispatchChaosEnsues(Player player, Object cause, List<Card> affected) {
        final Game game = player.getGame();
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(player);
        repParams.put(AbilityKey.Cause, cause);
        final ReplacementResult result = game.getReplacementHandler().run(ReplacementType.ChaosEnsues, repParams);
        if (result == ReplacementResult.Replaced || result == ReplacementResult.Prevented) {
            return;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
        runParams.put(AbilityKey.Cause, repParams.get(AbilityKey.Cause));
        if (affected != null) {
            runParams.put(AbilityKey.Affected, affected);
        }
        game.getTriggerHandler().runTrigger(TriggerType.ChaosEnsues, runParams, false);
    }
}
