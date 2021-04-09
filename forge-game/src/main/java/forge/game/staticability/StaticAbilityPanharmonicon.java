package forge.game.staticability;

import com.google.common.collect.ImmutableList;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StaticAbilityPanharmonicon {
    public static boolean applyPanharmoniconAbility(final StaticAbility stAb, final Trigger trigger, final Map<AbilityKey, Object> runParams) {
        final Card card = stAb.getHostCard();
        final Game game = card.getGame();

        final Card trigHost = trigger.getHostCard();
        final TriggerType trigMode = trigger.getMode();

        // What card is the source of the trigger?
        if (!stAb.matchesValidParam("ValidCard", trigHost)) {
            return false;
        }

        // Is our trigger's mode among the other modes?
        if (stAb.hasParam("ValidMode")) {
            List<String> modes = new ArrayList<>(Arrays.asList(stAb.getParam("ValidMode").split(",")));
            if (!modes.contains(trigMode.toString())) {
                return false;
            }
        }

        if (trigMode.equals(TriggerType.ChangesZone)) {
            // Cause of the trigger – the card changing zones
            final Card trigCause = (Card) runParams.get(AbilityKey.Card);
            if (stAb.hasParam("ValidCause")) {
                if (!trigCause.isValid(stAb.getParam("ValidCause").split(","),
                        game.getPhaseHandler().getPlayerTurn(), trigHost, null)) {
                    return false;
                }
            }

            if (stAb.hasParam("Origin")) {
                final String origin = (String) runParams.get(AbilityKey.Origin);
                if (!origin.equals(stAb.getParam("Origin"))) {
                    return false;
                }
            }
            if (stAb.hasParam("Destination")) {
                final String destination = (String) runParams.get(AbilityKey.Destination);
                if (!destination.equals(stAb.getParam("Destination"))) {
                    return false;
                }
            }
        } else if (trigMode.equals(TriggerType.ChangesZoneAll)) {
            // Check if the cards have a trigger at all
            final String origin = stAb.hasParam("Origin") ? stAb.getParam("Origin") : null;
            final String destination = stAb.hasParam("Destination") ? stAb.getParam("Destination") : null;
            final CardZoneTable table = (CardZoneTable) runParams.get(AbilityKey.Cards);

            // If the origin isn't specified, it's null – not making a list out of that.
            if (origin == null) {
                if (table.filterCards(null, ZoneType.smartValueOf(destination), stAb.getParam("ValidCause"), trigHost, null).isEmpty()) {
                    return false;
                }
            } else {
                if (table.filterCards(ImmutableList.of(ZoneType.smartValueOf(origin)), ZoneType.smartValueOf(destination), stAb.getParam("ValidCause"), trigHost, null).isEmpty()) {
                    return false;
                }
            }
        } else if (trigMode.equals(TriggerType.SpellCastOrCopy)
        || trigMode.equals(TriggerType.SpellCast) || trigMode.equals(TriggerType.SpellCopy)) {
            // Check if the spell cast and the caster match
            final SpellAbility sa = (SpellAbility) runParams.get(AbilityKey.CastSA);
            if (stAb.hasParam("ValidCause")) {
                if (!sa.getHostCard().isValid(stAb.getParam("ValidCause").split(","),
                        game.getPhaseHandler().getPlayerTurn(), trigHost, null)) {
                    return false;
                }
            }
            if (stAb.hasParam("ValidActivator")) {
                if (!sa.getActivatingPlayer().isValid(stAb.getParam("ValidActivator").split(","),
                        game.getPhaseHandler().getPlayerTurn(), trigHost, null)) {
                    return false;
                }
            }
        }

        return true;
    }
}
