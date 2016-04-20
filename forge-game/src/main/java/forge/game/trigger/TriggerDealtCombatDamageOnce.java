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
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

import java.util.Set;

/**
 * <p>
 * Trigger_DamageDone class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerDamageDone.java 21390 2013-05-08 07:44:50Z Max mtg $
 */
public class TriggerDealtCombatDamageOnce extends Trigger {

    /**
     * <p>
     * Constructor for TriggerCombatDamageDoneOnc.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerDealtCombatDamageOnce(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card srcs = (Card) runParams2.get("DamageSource");
        final Set<GameEntity> tgt = (Set<GameEntity>) runParams2.get("DamageTargets");

        if (this.mapParams.containsKey("ValidTarget")) {
            boolean valid = false;
            for (GameEntity c : tgt) {
                if (c.isValid(this.mapParams.get("ValidTarget").split(","), this.getHostCard().getController(),this.getHostCard(), null)) {
                    valid = true;
                }
            }
            if (!valid) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidSource")) {
            if (!matchesValid(srcs, this.mapParams.get("ValidSource").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("DamageAmount")) {
            final String fullParam = this.mapParams.get("DamageAmount");

            final String operator = fullParam.substring(0, 2);
            final int operand = Integer.parseInt(fullParam.substring(2));
            final int actualAmount = (Integer) runParams2.get("DamageAmount");

            if (!Expressions.compare(actualAmount, operator, operand)) {
                return false;
            }

            System.out.print("DealtCombatDamageOnce Amount Operator: ");
            System.out.println(operator);
            System.out.print("DealtCombatDamageOnce Amount Operand: ");
            System.out.println(operand);
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Source", this.getRunParams().get("DamageSource"));
        sa.setTriggeringObject("Targets", this.getRunParams().get("DamageTargets"));
        sa.setTriggeringObject("DamageAmount", this.getRunParams().get("DamageAmount"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Damage Source: ").append(sa.getTriggeringObject("Source")).append(", ");
        sb.append("Damaged: ").append(sa.getTriggeringObject("Targets")).append(", ");
        sb.append("Amount: ").append(sa.getTriggeringObject("Amount"));
        return sb.toString();
    }
}
