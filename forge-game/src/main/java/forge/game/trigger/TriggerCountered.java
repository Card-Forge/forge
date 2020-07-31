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
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.Map;

/**
 * <p>
 * Trigger_Countered class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerCountered.java 17802 2012-10-31 08:05:14Z Max mtg $
 */
public class TriggerCountered extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Countered.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerCountered(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidCard")) {
            if (!matchesValid(runParams.get(AbilityKey.Card), getParam("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (hasParam("ValidPlayer")) {
            if (!matchesValid(runParams.get(AbilityKey.Player), getParam("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (hasParam("ValidCause")) {
            if (runParams.get(AbilityKey.Cause) == null) {
                return false;
            }
            if (!matchesValid(runParams.get(AbilityKey.Cause), getParam("ValidCause").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        
        if (hasParam("ValidType")) {
            // TODO: if necessary, expand the syntax to account for multiple valid types (e.g. Spell,Ability)
            SpellAbility ctrdSA = (SpellAbility) runParams.get(AbilityKey.CounteredSA);
            String validType = getParam("ValidType");
            if (ctrdSA != null) {
                if (validType.equals("Spell") && !ctrdSA.isSpell()) {
                    return false;
                } else if (validType.equals("Ability") && !ctrdSA.isAbility()) {
                    return false;
                }
            }
            
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(
            runParams,
            AbilityKey.Card,
            AbilityKey.Cause,
            AbilityKey.Player,
            AbilityKey.CounteredSA
        );
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblCountered")).append(": ").append(sa.getTriggeringObject(AbilityKey.Card)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblCause")).append(": ").append(sa.getTriggeringObject(AbilityKey.Cause));
        return sb.toString();
    }
}
