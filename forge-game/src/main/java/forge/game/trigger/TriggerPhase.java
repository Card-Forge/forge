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
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

/**
 * <p>
 * Trigger_Phase class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerPhase extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Phase.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerPhase(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final Player player = (Player) runParams.get(AbilityKey.Player);
        if (!matchesValidParam("ValidPlayer", player)) {
            return false;
        }
        // CR 805.4d: an ability that doesn't refer to that player triggers once per team step, for the first active player it matches
        if (getHostCard().getGame().getRules().hasSharedTeamTurns() && !refersToTriggering(AbilityKey.Player)) {
            for (final Player earlier : getHostCard().getGame().getPhaseHandler().getActivePlayers()) {
                if (earlier.equals(player)) {
                    break;
                }
                if (matchesValidParam("ValidPlayer", earlier)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final Map<AbilityKey, Object> objects, Map<AbilityKey, Object> runParams) {
        AbilityKey.copyKeys(runParams, objects, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblPhase")).append(": ").append(sa.getTriggeringObject(AbilityKey.Player));
        return sb.toString();
    }
}
