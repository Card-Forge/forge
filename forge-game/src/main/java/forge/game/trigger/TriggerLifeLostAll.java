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

import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

import java.util.Map;

/**
 * <p>
 * Trigger_LifeLostAll class.
 * </p>
 *
 * 4/27/2023 - this trigger is written for only Ob Nixilis, Captive Kingpin – any future uses will probably need
 * additional logic
 *
 * @author Forge (Northmoc)
 * @version $Id$
 */
public class TriggerLifeLostAll extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_LifeLost.
     * </p>
     *
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerLifeLostAll(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final Map<Player, Integer> testMap = filteredMap((Map<Player, Integer>) runParams.get(AbilityKey.Map));
        if (testMap.isEmpty()) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        final Map<Player, Integer> map = filteredMap((Map<Player, Integer>) runParams.get(AbilityKey.Map));

        sa.setTriggeringObject(AbilityKey.Map, map);
        sa.setTriggeringObject(AbilityKey.Player, map.keySet());
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        final Map<Player, Integer> map = (Map<Player, Integer>) sa.getTriggeringObject(AbilityKey.Map);
        int n = 0;
        for (final Map.Entry<Player, Integer> e : map.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue());
            n++;
            if (map.size() > n) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private Map<Player, Integer> filteredMap(Map<Player, Integer> map) {
        Map<Player, Integer> passMap = Maps.newHashMap();
        for (final Map.Entry<Player, Integer> e : map.entrySet()) {
            if (matchesValidParam("ValidPlayer", e.getKey())) {
                if (hasParam("ValidAmountEach")) {
                    final String comp = getParam("ValidAmountEach");
                    final int value = AbilityUtils.calculateAmount(getHostCard(), comp.substring(2), this);
                    if (!Expressions.compare(e.getValue(), comp.substring(0, 2), value)) {
                        continue;
                    }
                }
                passMap.put(e.getKey(), e.getValue());
            }
        }
        return passMap;
    }
}
