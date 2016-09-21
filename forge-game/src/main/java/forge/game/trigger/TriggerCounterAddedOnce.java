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

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Trigger_CounterAdded class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerCounterAdded.java 23787 2013-11-24 07:09:23Z Max mtg $
 */
public class TriggerCounterAddedOnce extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_CounterAddedOnce.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerCounterAddedOnce(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final CounterType addedType = (CounterType) runParams2.get("CounterType");

        if (this.mapParams.containsKey("ValidCard")) {
            if (!runParams2.containsKey("Card"))
                return false;

            final Card addedTo = (Card) runParams2.get("Card");
            if (!addedTo.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidPlayer")) {
            if (!runParams2.containsKey("Player"))
                return false;

            final Player addedTo = (Player) runParams2.get("Player");
            if (!addedTo.isValid(this.mapParams.get("ValidPlayer").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("CounterType")) {
            final String type = this.mapParams.get("CounterType");
            if (!type.equals(addedType.toString())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        if (this.getRunParams().containsKey("Card"))
            sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
        if (this.getRunParams().containsKey("Player"))
            sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Added once: ");
        if (sa.hasTriggeringObject("Card"))
            sb.append(sa.getTriggeringObject("Card"));
        if (sa.hasTriggeringObject("Player"))
            sb.append(sa.getTriggeringObject("Player"));
        return sb.toString();
    }
}
