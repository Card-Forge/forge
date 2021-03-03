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

import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * <p>
 * Trigger_DamageDone class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerDamageDone.java 21390 2013-05-08 07:44:50Z Max mtg $
 */
public class TriggerDamageDealtOnce extends Trigger {

    /**
     * <p>
     * Constructor for TriggerDamageDealtOnce.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerDamageDealtOnce(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @SuppressWarnings("unchecked")
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final Card srcs = (Card) runParams.get(AbilityKey.DamageSource);

        if (hasParam("CombatDamage")) {
            if (getParam("CombatDamage").equals("True")) {
                if (!((Boolean) runParams.get(AbilityKey.IsCombatDamage))) {
                    return false;
                }
            } else if (getParam("CombatDamage").equals("False")) {
                if (((Boolean) runParams.get(AbilityKey.IsCombatDamage))) {
                    return false;
                }
            }
        }

        if (hasParam("ValidTarget")) {
            final Map<GameEntity, Integer> damageMap = (Map<GameEntity, Integer>) runParams.get(AbilityKey.DamageMap);

            if (getDamageAmount(damageMap) <= 0) {
                return false;
            }
        }

        if (hasParam("ValidSource")) {
            if (!matchesValid(srcs, getParam("ValidSource").split(","), getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        @SuppressWarnings("unchecked")
        final Map<GameEntity, Integer> damageMap = (Map<GameEntity, Integer>) runParams.get(AbilityKey.DamageMap);

        sa.setTriggeringObject(AbilityKey.Source, runParams.get(AbilityKey.DamageSource));
        sa.setTriggeringObject(AbilityKey.Targets, getDamageTargets(damageMap));
        sa.setTriggeringObject(AbilityKey.DamageAmount, getDamageAmount(damageMap));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblDamageSource")).append(": ").append(sa.getTriggeringObject(AbilityKey.Source)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblDamaged")).append(": ").append(sa.getTriggeringObject(AbilityKey.Targets)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblAmount")).append(": ").append(sa.getTriggeringObject(AbilityKey.DamageAmount));
        return sb.toString();
    }

    public int getDamageAmount(Map<GameEntity, Integer> damageMap) {
        int result = 0;
        for (Map.Entry<GameEntity, Integer> e : damageMap.entrySet()) {
            if (!hasParam("ValidTarget") || matchesValid(e.getKey(), getParam("ValidTarget").split(","), getHostCard())) {
                result += e.getValue();
            }
        }
        return result;
    }

    public Set<GameEntity> getDamageTargets(Map<GameEntity, Integer> damageMap) {
        if (!hasParam("ValidTarget")) {
            return Sets.newHashSet(damageMap.keySet());
        }
        Set<GameEntity> result = Sets.newHashSet();
        for (GameEntity e : damageMap.keySet()) {
            if (matchesValid(e, getParam("ValidTarget").split(","), getHostCard())) {
                result.add(e);
            }
        }
        return result;
    }
}
