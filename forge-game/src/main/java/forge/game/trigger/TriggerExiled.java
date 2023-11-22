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

import java.util.Map;

import forge.game.cost.IndividualCostPaymentInstance;
import forge.game.zone.CostPaymentStack;
import org.apache.commons.lang3.ArrayUtils;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

/**
 * <p>
 * Trigger_ChangesZone class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class TriggerExiled extends Trigger {

    /**
     * <p>
     * Constructor for TriggerExiled.
     * </p>
     *
     * @param params
     *            a {@link java.util.Map} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerExiled(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (hasParam("Origin")) {
            if (!getParam("Origin").equals("Any")) {
                if (getParam("Origin") == null) {
                    return false;
                }
                if (!ArrayUtils.contains(
                    getParam("Origin").split(","), runParams.get(AbilityKey.Origin)
                )) {
                    return false;
                }
            }
        }

        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Card))) {
            return false;
        }

        if (!matchesValidParam("ValidCause", runParams.get(AbilityKey.Cause))) {
            return false;
        }

        if (hasParam("WhileKeyword")) {
            final String keyword = getParam("WhileKeyword");
            boolean withKeyword = false;

            IndividualCostPaymentInstance currentPayment = (IndividualCostPaymentInstance) runParams.get(AbilityKey.IndividualCostPaymentInstance);

            SpellAbility sa;
            if (currentPayment != null) {
                sa = currentPayment.getPayment().getAbility();

                if (whileKeywordCheck(keyword, sa)) withKeyword = true;
            }

            if (!withKeyword) {
                CostPaymentStack stack = (CostPaymentStack) runParams.get(AbilityKey.CostStack);

                for (IndividualCostPaymentInstance individual : stack) {
                    sa = individual.getPayment().getAbility();

                    if (whileKeywordCheck(keyword, sa))  {
                        withKeyword = true;
                        break;
                    }
                }
            }

            if (!withKeyword) return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblExiled")).append(": ").append(sa.getTriggeringObject(AbilityKey.Card));
        return sb.toString();
    }

}
