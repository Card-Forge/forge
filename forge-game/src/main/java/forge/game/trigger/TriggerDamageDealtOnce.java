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
import forge.util.Expressions;
import forge.util.Localizer;

import java.util.Map;
import java.util.Set;

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
    public TriggerDamageDealtOnce(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @SuppressWarnings("unchecked")
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final Card srcs = (Card) runParams.get(AbilityKey.DamageSource);
        final Set<GameEntity> tgt = (Set<GameEntity>) runParams.get(AbilityKey.DamageTargets);

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
            boolean valid = false;
            for (GameEntity c : tgt) {
                if (c.isValid(getParam("ValidTarget").split(","), this.getHostCard().getController(),this.getHostCard(), null)) {
                    valid = true;
                }
            }
            if (!valid) {
                return false;
            }
        }

        if (hasParam("ValidSource")) {
            if (!matchesValid(srcs, getParam("ValidSource").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (hasParam("DamageAmount")) {
            final String fullParam = getParam("DamageAmount");

            final String operator = fullParam.substring(0, 2);
            final int operand = Integer.parseInt(fullParam.substring(2));
            final int actualAmount = (Integer) runParams.get(AbilityKey.DamageAmount);

            if (!Expressions.compare(actualAmount, operator, operand)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObjectsFrom(this, AbilityKey.DamageAmount);
        sa.setTriggeringObject(AbilityKey.Source, getFromRunParams(AbilityKey.DamageSource));
        sa.setTriggeringObject(AbilityKey.Targets, getFromRunParams(AbilityKey.DamageTargets));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblDamageSource")).append(": ").append(sa.getTriggeringObject(AbilityKey.Source)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblDamaged")).append(": ").append(sa.getTriggeringObject(AbilityKey.Targets)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblAmount")).append(": ").append(sa.getTriggeringObject(AbilityKey.DamageAmount));
        return sb.toString();
    }
}
