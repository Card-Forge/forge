package forge.game.staticability;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table.Cell;

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
        CardCollectionView cardList = null;
        // if LTB look back
        if (regtrig.getMode() == TriggerType.ChangesZone && "Battlefield".equals(regtrig.getParam("Origin"))) {
            if (runParams.containsKey(AbilityKey.LastStateBattlefield)) {
                cardList = (CardCollectionView) runParams.get(AbilityKey.LastStateBattlefield);
            }
            if (cardList == null) {
                cardList = game.getLastStateBattlefield();
            }
        } else {
            cardList = game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES);
        }

        for (final Card ca : cardList) {
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
        // CR 603.2e
        if (stAb.hasParam("ValidCard") && regtrig.getSpawningAbility() != null) {
            return false;
        }

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
            if (!stAb.matchesValidParam("Destination", runParams.get(AbilityKey.Destination))) {
                return false;
            }
            if (!stAb.matchesValidParam("Origin", runParams.get(AbilityKey.Origin))) {
                return false;
            }
            if ("Graveyard".equals(runParams.get(AbilityKey.Destination))
                    && "Battlefield".equals(runParams.get(AbilityKey.Origin))) {
                // Allow triggered ability of a dying creature that triggers
                // only when that creature is put into a graveyard from anywhere
                if ("Card.Self".equals(regtrig.getParam("ValidCard"))
                        && (!regtrig.hasParam("Origin") || "Any".equals(regtrig.getParam("Origin")))) {
                    return false;
                }
            }
        } else if (triggerType.equals(TriggerType.ChangesZoneAll)) {
            final String destination = stAb.getParamOrDefault("Destination", null);
            final CardZoneTable table = (CardZoneTable) runParams.get(AbilityKey.Cards);

            // find out if any other cards would still trigger it
            boolean found = false;
            for (Cell<ZoneType, ZoneType, CardCollection> cell : table.cellSet()) {
                // this currently assumes the table will not contain multiple destinations
                // however with some effects (e.g. Goblin Welder) that should indeed be the case
                // once Forge handles that correctly this section needs to account for that
                // (by doing a closer check of the triggered ability first)
                if (cell.getColumnKey() != ZoneType.valueOf(destination)) {
                    found = true;
                } else if (Iterables.any(cell.getValue(),
                        Predicates.not(CardPredicates.isType(stAb.getParam("ValidCause"))))) {
                    found = true;
                }
                if (found) break;
            }
            if (found) return false;
        }
        return true;
    }
}
