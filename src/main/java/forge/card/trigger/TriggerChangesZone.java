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
package forge.card.trigger;

import java.util.Map;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.util.Expressions;

/**
 * <p>
 * Trigger_ChangesZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerChangesZone extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_ChangesZone.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerChangesZone(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("Origin")) {
            if (!this.mapParams.get("Origin").equals("Any")) {
                if (this.mapParams.get("Origin") == null) {
                    return false;
                }
                if (!this.mapParams.get("Origin").equals(runParams2.get("Origin"))) {
                    return false;
                }
            }
        }

        if (this.mapParams.containsKey("Destination")) {
            if (!this.mapParams.get("Destination").equals("Any")) {
                if (!this.mapParams.get("Destination").equals(runParams2.get("Destination"))) {
                    return false;
                }
            }
        }

        if (this.mapParams.containsKey("ValidCard")) {
            final Card moved = (Card) runParams2.get("Card");
            if (!moved.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard())) {
                return false;
            }
        }

        // Check number of lands ETB this turn on triggered card's controller
        if (mapParams.containsKey("CheckOnTriggeredCard")) {
            final String[] condition = mapParams.get("CheckOnTriggeredCard").split(" ", 2);

            final Card host = hostCard.getGame().getCardState(hostCard);
            final String comparator = condition.length < 2 ? "GE1" : condition[1];
            final int referenceValue = AbilityUtils.calculateAmount(host, comparator.substring(2), null);
            final Card triggered = (Card)runParams2.get("Card"); 
            final int actualValue = CardFactoryUtil.xCount((Card)triggered, host.getSVar(condition[0]));
            if (!Expressions.compare(actualValue, comparator.substring(0, 2), referenceValue)) {
                return false;
            }
        }        
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
    }
}
