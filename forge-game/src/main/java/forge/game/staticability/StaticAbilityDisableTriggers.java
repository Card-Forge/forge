package forge.game.staticability;

import com.google.common.collect.ImmutableList;

import forge.game.Game;
import forge.game.card.*;
import forge.game.ability.AbilityKey;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Map;

public class StaticAbilityDisableTriggers {

    static String MODE = "DisableTriggers";

    public static boolean disabled(final Game game, final TriggerType triggerType, final Trigger regtrig) {
        //for AiController ETB tests
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(regtrig.getHostCard());
        runParams.put(AbilityKey.Destination, ZoneType.Battlefield);
        return disabled(game, triggerType, regtrig, runParams);
    }

    public static boolean disabled(final Game game, final TriggerType triggerType, final Trigger regtrig, final Map<AbilityKey, Object> runParams)  {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }

                if (isDisabled(stAb, triggerType, regtrig, runParams)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isDisabled(final StaticAbility stAb, final TriggerType triggerType, final Trigger regtrig, final Map<AbilityKey, Object> runParams) {
        if (!stAb.matchesValidParam("ValidCard", regtrig.getHostCard())) {
            return false;
        }

        if (stAb.hasParam("ValidMode")) {
            if (!ArrayUtils.contains(stAb.getParam("ValidMode").split(","), regtrig.getMode().toString())) {
                return false;
            }
        }

        if (triggerType.equals(TriggerType.ChangesZone)) {
            // Cause of the trigger – the card changing zones
            if (!stAb.matchesValidParam("ValidCause", runParams.get(AbilityKey.Card))) {
                return false;
            }
            if (!stAb.matchesValidParam("Origin", runParams.get(AbilityKey.Origin))) {
                return false;
            }
            if (!stAb.matchesValidParam("Destination", runParams.get(AbilityKey.Destination))) {
                return false;
            }
        } else if (triggerType.equals(TriggerType.ChangesZoneAll)) {
            // Check if the cards have a trigger at all
            final String origin = stAb.getParamOrDefault("Origin", null);
            final String destination = stAb.getParamOrDefault("Destination", null);
            final CardZoneTable table = (CardZoneTable) runParams.get(AbilityKey.Cards);

            if (table.filterCards(origin == null ? null : ImmutableList.of(ZoneType.smartValueOf(origin)),
                    ZoneType.smartValueOf(destination), stAb.getParam("ValidCause"), stAb.getHostCard(),
                    stAb).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
