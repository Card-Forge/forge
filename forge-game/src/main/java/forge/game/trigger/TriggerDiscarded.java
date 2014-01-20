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
import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Discarded class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerDiscarded.java 23787 2013-11-24 07:09:23Z Max mtg $
 */
public class TriggerDiscarded extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Discarded.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerDiscarded(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Card"), this.mapParams.get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams2.get("Player"), this.mapParams.get("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidCause")) {
            if (runParams2.get("Cause") == null) {
                return false;
            }
            if (!matchesValid(runParams2.get("Cause"), this.mapParams.get("ValidCause").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (this.mapParams.containsKey("IsMadness")) {
            Boolean madness = (Boolean) runParams2.get("IsMadness");
            if (this.mapParams.get("IsMadness").equals("True") ^ madness) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
        sa.setTriggeringObject("Cause", this.getRunParams().get("Cause"));
    }
}
