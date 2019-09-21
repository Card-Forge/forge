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

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Trigger_CounterRemovedOnce class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerCounterRemovedOnce.java 12297 2011-11-28 19:56:47Z jendave $
 */
public class TriggerCounterRemovedOnce extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_CounterRemovedOnce.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerCounterRemovedOnce(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card removedFrom = (Card) runParams2.get("Card");
        final CounterType removedType = (CounterType) runParams2.get("CounterType");

        if (hasParam("ValidCard")) {
            if (!removedFrom.isValid(getParam("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }

        if (hasParam("CounterType")) {
            final String type = getParam("CounterType");
            if (!type.equals(removedType.toString())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObjectsFrom(this, AbilityKey.Card, AbilityKey.Amount);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Removed from: ").append(sa.getTriggeringObject(AbilityKey.Card));
        sb.append(" Amount: ").append(sa.getTriggeringObject(AbilityKey.Amount));
        return sb.toString();
    }
}
