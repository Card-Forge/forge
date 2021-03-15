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
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

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
    public TriggerAttackerBlocked(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Attacker))) {
            return false;
        }

        if (hasParam("MinBlockers")) {
            if ((int) runParams.get(AbilityKey.NumBlockers) < Integer.valueOf(getParam("MinBlockers"))) {
                return false;
            }
        }

        if (hasParam("ValidBlocker")) {
            @SuppressWarnings("unchecked")
            int count = CardLists.getValidCardCount(
                    (Iterable<Card>) runParams.get(AbilityKey.Blockers),
                    getParam("ValidBlocker"),
                    getHostCard().getController(), getHostCard(), this
            );

            if ( count == 0 ) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(
            runParams,
            AbilityKey.Attacker,
            AbilityKey.Blockers,
            AbilityKey.Defender,
            AbilityKey.DefendingPlayer,
            AbilityKey.NumBlockers
        );
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblAttacker")).append(": ").append(sa.getTriggeringObject(AbilityKey.Attacker)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblNumberBlockers")).append(": ").append(sa.getTriggeringObject(AbilityKey.NumBlockers));
        return sb.toString();
    }
}
