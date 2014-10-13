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

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Trigger_AttackerBlocked class. Should trigger once for each blocking creature.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerAttackerBlocked.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class TriggerAttackerBlockedByCreature extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_AttackerBlocked.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerAttackerBlockedByCreature(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
    	final Object a = runParams2.get("Attacker"),
    			b = runParams2.get("Blocker");
    	if (!(a instanceof Card && b instanceof Card)) {
    		return false;
    	}

    	final Card attacker = (Card) a,
    			blocker = (Card) b;
        if (this.mapParams.containsKey("ValidCard")) {
        	final String validCard = this.mapParams.get("ValidCard");
        	if (validCard.equals("LessPowerThanBlocker")) {
        		if (attacker.getNetPower() >= blocker.getNetPower()) {
        			return false;
        		}
        	} else if (!matchesValid(attacker, validCard.split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidBlocker")) {
        	final String validBlocker = this.mapParams.get("ValidBlocker");
        	if (validBlocker.equals("LessPowerThanAttacker")) {
        		if (blocker.getNetPower() >= attacker.getNetPower()) {
        			return false;
        		}
        	} else if (!matchesValid(blocker, validBlocker.split(","), this.getHostCard())) {
        		return false;
        	}
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Attacker", this.getRunParams().get("Attacker"));
        sa.setTriggeringObject("Blocker", this.getRunParams().get("Blocker"));
    }
}
