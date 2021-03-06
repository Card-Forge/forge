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

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;
import forge.util.Localizer;

/**
 * <p>
 * Trigger_CounterAdded class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerCounterAdded extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_CounterAdded.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerCounterAdded(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final CounterType addedType = (CounterType) runParams.get(AbilityKey.CounterType);

        if (hasParam("ValidCard")) {
            if (!runParams.containsKey(AbilityKey.Card))
                return false;

            final Card addedTo = (Card) runParams.get(AbilityKey.Card);
            if (!addedTo.isValid(getParam("ValidCard").split(","), getHostCard().getController(),
                    getHostCard(), null)) {
                return false;
            }
        }

        if (hasParam("ValidPlayer")) {
            if (!runParams.containsKey(AbilityKey.Player))
                return false;

            final Player addedTo = (Player) runParams.get(AbilityKey.Player);
            if (!addedTo.isValid(getParam("ValidPlayer").split(","), getHostCard().getController(),
                    getHostCard(), null)) {
                return false;
            }
        }
        
        if (hasParam("ValidSource")) {
            if (!runParams.containsKey(AbilityKey.Source))
                return false;

            final Card source = (Card) runParams.get(AbilityKey.Source);

            if (source == null) {
                return false;
            }

            if (!source.isValid(getParam("ValidSource").split(","), getHostCard().getController(),
                    getHostCard(), null)) {
                return false;
            }
        }
        
        if (hasParam("CounterType")) {
            final String type = getParam("CounterType");
            if (!type.equals(addedType.toString())) {
                return false;
            }
        }
        if (hasParam("CounterAmount") && runParams.containsKey(AbilityKey.CounterAmount)) {
            // this one is for Saga to trigger
            // the right ability for the counters on the card
            final String fullParam = getParam("CounterAmount");

            final String operator = fullParam.substring(0, 2);
            final int operand = Integer.parseInt(fullParam.substring(2));
            final int actualAmount = (Integer) runParams.get(AbilityKey.CounterAmount);

            if (!Expressions.compare(actualAmount, operator, operand)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblAddedOnce")).append(": ");
        if (sa.hasTriggeringObject(AbilityKey.Card))
            sb.append(sa.getTriggeringObject(AbilityKey.Card));
        if (sa.hasTriggeringObject(AbilityKey.Player))
            sb.append(sa.getTriggeringObject(AbilityKey.Player));
        return sb.toString();
    }
}
