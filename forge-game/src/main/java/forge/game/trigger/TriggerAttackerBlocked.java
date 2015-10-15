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
 * Trigger_AttackerBlocked class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerAttackerBlocked extends Trigger {

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
    public TriggerAttackerBlocked(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Attacker"), this.mapParams.get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (this.mapParams.containsKey("ValidBlocker")) {
            boolean valid = false;
            @SuppressWarnings("unchecked")
            final Iterable<Card> list = (Iterable<Card>) runParams2.get("Blockers");
            for (final Card b : list) {
                if (matchesValid(b, this.mapParams.get("ValidBlocker").split(","), this.getHostCard())) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Attacker", this.getRunParams().get("Attacker"));
        sa.setTriggeringObject("Blockers", this.getRunParams().get("Blockers"));
        sa.setTriggeringObject("NumBlockers", this.getRunParams().get("NumBlockers"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Attacker: ").append(this.getRunParams().get("Attacker"));
        sb.append(" Number Blockers: ").append(this.getRunParams().get("NumBlockers"));
        return sb.toString();
    }
}
