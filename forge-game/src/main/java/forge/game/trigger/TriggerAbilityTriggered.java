/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.trigger;

import com.google.common.collect.ImmutableList;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.TextUtil;

import java.util.*;

/**
 * <p>
 * TriggerAbilityTriggered class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class TriggerAbilityTriggered extends Trigger {

    public TriggerAbilityTriggered(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final SpellAbility spellAbility = (SpellAbility) runParams.get(AbilityKey.SpellAbility);
        if (spellAbility == null) {
            System.out.println("TriggerAbilityTriggered performTest encountered spellAbility == null. runParams2 = " + runParams);
            return false;
        }
        final Card source = spellAbility.getHostCard();
        final Iterable<Card> causes = (Iterable<Card>) runParams.get(AbilityKey.Cause);
        final Game game = source.getGame();

        if (hasParam("ValidMode")) {
            List<String> validModes = Arrays.asList(getParam("ValidMode").split(","));
            String mode = (String) runParams.get(AbilityKey.Mode);
            if (!validModes.contains(mode)) {
                return false;
            }
        }

        if (hasParam("ValidDestination")) {
            List<String> validDestinations = Arrays.asList(getParam("ValidDestination").split(","));
            List<String> destinations = Arrays.asList(((String)runParams.get(AbilityKey.Destination)).split(","));
            if (Collections.disjoint(validDestinations, destinations)) {
                return false;
            }
        }

        if (!matchesValidParam("ValidSource", source)) {
            return false;
        }

        if (hasParam("ValidCause")) {
            boolean match = false;
            for (Card cause : causes) {
                if(matchesValidParam("ValidCause", cause)) {
                    match = true;
                }
            }
            if (!match) {
                return false;
            }
        }

        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        final SpellAbility triggeredSA = (SpellAbility) runParams.get(AbilityKey.SpellAbility);
        sa.setTriggeringObject(AbilityKey.Source, triggeredSA.getHostCard());
        sa.setTriggeringObjectsFrom(
                runParams,
                AbilityKey.SpellAbility,
                AbilityKey.Cause);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblSpellAbility")).append(": ").append(sa.getTriggeringObject(AbilityKey.SpellAbility));
        return sb.toString();
    }

    public static void addTriggeringObject(Trigger regtrig, SpellAbility sa, Map<AbilityKey, Object> runParams) {
        Map<AbilityKey, Object> newRunParams = AbilityKey.newMap();
        newRunParams.put(AbilityKey.Mode, regtrig.getMode().toString());
        if (regtrig.getMode() == TriggerType.ChangesZone) {
            newRunParams.put(AbilityKey.Destination, runParams.get(AbilityKey.Destination));
            newRunParams.put(AbilityKey.Cause, ImmutableList.of(runParams.get(AbilityKey.Card)));
        } else if (regtrig.getMode() == TriggerType.ChangesZoneAll) {
            final CardZoneTable table = (CardZoneTable) runParams.get(AbilityKey.Cards);
            Set<String> destinations = new HashSet<>();
            for (ZoneType dest : ZoneType.values()) {
                if (table.containsColumn(dest) && !table.column(dest).isEmpty()) {
                    destinations.add(dest.toString());
                }
            }
            newRunParams.put(AbilityKey.Destination, TextUtil.join(destinations, ","));
            newRunParams.put(AbilityKey.Cause, table.allCards());
        }
        sa.setTriggeringObject(AbilityKey.TriggeredParams, newRunParams);
    }
}
